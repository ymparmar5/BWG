package com.example.focus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.focus.automation.AutomationManager;
import com.example.focus.data.FocusPreferences;
import com.example.focus.engine.ScheduleBlock;
import com.example.focus.engine.UsageTrackerEngine;
import com.example.focus.model.AppItem;
import com.example.focus.service.FocusAccessibilityService;
import com.example.focus.ui.AppDrawerAdapter;
import com.example.focus.ui.BlockedInterceptActivity;
import com.example.focus.ui.FavoritesAdapter;
import com.example.focus.ui.MindfulDelayActivity;
import com.example.focus.util.AppHelper;
import com.example.focus.util.NotificationHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private FocusPreferences prefs;
    private View rootLayout;
    private View homeLayout;
    private View drawerLayout;
    private EditText searchInput;
    private AppDrawerAdapter drawerAdapter;
    private FavoritesAdapter favoritesAdapter;
    private GestureDetector gestureDetector;

    private List<AppItem> allInstalledApps = new ArrayList<>();
    private final Map<String, AppItem> appMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new FocusPreferences(this);

        initViews();
        setupEdgeToEdgeInsets();
        setupGestures();
        setupBackPress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
    }

    private void setupEdgeToEdgeInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            homeLayout.setPadding(
                    homeLayout.getPaddingLeft(),
                    top + 24,
                    homeLayout.getPaddingRight(),
                    bottom + 16
            );

            drawerLayout.setPadding(
                    drawerLayout.getPaddingLeft(),
                    top + 16,
                    drawerLayout.getPaddingRight(),
                    bottom + 16
            );

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initViews() {
        rootLayout = findViewById(R.id.root_layout);
        homeLayout = findViewById(R.id.home_layout);
        drawerLayout = findViewById(R.id.drawer_layout);
        searchInput = findViewById(R.id.search_input);

        // Clock & Date click handlers
        findViewById(R.id.text_clock).setOnClickListener(v -> openClock());
        findViewById(R.id.text_date).setOnClickListener(v -> openCalendar());
        findViewById(R.id.clock_container).setOnLongClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        });

        // Phone & Messages bottom shortcuts
        findViewById(R.id.btn_phone).setOnClickListener(v -> AppHelper.openDialer(this));
        findViewById(R.id.btn_messages).setOnClickListener(v -> AppHelper.openMessages(this));

        // Favorites RecyclerView
        RecyclerView favoritesRecycler = findViewById(R.id.favorites_recycler);
        favoritesRecycler.setLayoutManager(new LinearLayoutManager(this));
        favoritesAdapter = new FavoritesAdapter(new FavoritesAdapter.OnFavoriteClickListener() {
            @Override
            public void onFavoriteClick(AppItem app) {
                attemptLaunch(app);
            }

            @Override
            public void onFavoriteLongClick(AppItem app) {
                showFavoriteOptionsDialog(app);
            }
        });
        favoritesRecycler.setAdapter(favoritesAdapter);

        // Drawer RecyclerView
        RecyclerView drawerRecycler = findViewById(R.id.drawer_recycler);
        drawerRecycler.setLayoutManager(new LinearLayoutManager(this));
        drawerAdapter = new AppDrawerAdapter(new AppDrawerAdapter.OnAppClickListener() {
            @Override
            public void onAppClick(AppItem app) {
                closeDrawer();
                attemptLaunch(app);
            }

            @Override
            public void onAppLongClick(AppItem app) {
                showAppManagementDialog(app);
            }
        });
        drawerRecycler.setAdapter(drawerAdapter);

        // Live Search Filter with Auto-Launch
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString() : "";
                int countMatches = drawerAdapter.filter(query);

                // Auto-Launch if exactly 1 result and user has typed at least 2 characters
                if (prefs.isAutoLaunchSingle() && countMatches == 1 && query.trim().length() >= 2) {
                    AppItem match = drawerAdapter.getFirstVisibleApp();
                    if (match != null && !"com.example.focus.settings".equals(match.getPackageName())) {
                        closeDrawer();
                        attemptLaunch(match);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                AppItem first = drawerAdapter.getFirstVisibleApp();
                if (first != null) {
                    closeDrawer();
                    attemptLaunch(first);
                    return true;
                }
            }
            return false;
        });
    }

    private void setupGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 90;
            private static final int SWIPE_VELOCITY = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // Double-Tap to lock screen
                FocusAccessibilityService service = FocusAccessibilityService.getInstance();
                if (service != null && service.lockScreenDevice()) {
                    return true;
                } else {
                    Toast.makeText(MainActivity.this, "Enable Accessibility to lock screen", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                if (e1 == null || e2 == null) return false;
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(vX) > SWIPE_VELOCITY) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        return true;
                    }
                } else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(vY) > SWIPE_VELOCITY) {
                        if (diffY < 0) {
                            openDrawer();
                            return true;
                        } else {
                            if (drawerLayout.getVisibility() == View.VISIBLE) {
                                closeDrawer();
                            } else {
                                expandNotificationPanel();
                            }
                            return true;
                        }
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (drawerLayout.getVisibility() != View.VISIBLE) {
            gestureDetector.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setupBackPress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    1000,
                    () -> {
                        if (drawerLayout.getVisibility() == View.VISIBLE) {
                            closeDrawer();
                        }
                    }
            );
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.getVisibility() == View.VISIBLE) {
                    closeDrawer();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.getVisibility() == View.VISIBLE) {
            closeDrawer();
        }
        // Never call super.onBackPressed() on home screen
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (drawerLayout.getVisibility() == View.VISIBLE) {
                closeDrawer();
            }
            return true; // Completely consume KEYCODE_BACK so launcher NEVER exits
        }
        return super.onKeyDown(keyCode, event);
    }

    public void openDrawer() {
        if (drawerLayout.getVisibility() == View.VISIBLE) return;
        drawerLayout.setVisibility(View.VISIBLE);
        searchInput.setText("");
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public void closeDrawer() {
        if (drawerLayout.getVisibility() != View.VISIBLE) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
        drawerLayout.setVisibility(View.GONE);
        searchInput.clearFocus();
    }

    private void onSwipeLeft() {
        String leftPkg = prefs.getSwipeLeftPackage();
        if (leftPkg != null && !leftPkg.isEmpty()) {
            AppItem app = appMap.get(leftPkg);
            if (app != null) {
                attemptLaunch(app);
            } else {
                AppHelper.launchPackage(this, leftPkg);
            }
        } else {
            Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception ignored) {}
        }
    }

    private void onSwipeRight() {
        String rightPkg = prefs.getSwipeRightPackage();
        if (rightPkg != null && !rightPkg.isEmpty()) {
            AppItem app = appMap.get(rightPkg);
            if (app != null) {
                attemptLaunch(app);
            } else {
                AppHelper.launchPackage(this, rightPkg);
            }
        } else {
            AppHelper.openDialer(this);
        }
    }

    private void expandNotificationPanel() {
        FocusAccessibilityService service = FocusAccessibilityService.getInstance();
        if (service != null && service.expandNotificationShade()) {
            return;
        }

        try {
            Object sbservice = getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            Method showsb = statusbarManager.getMethod("expandNotificationsPanel");
            showsb.invoke(sbservice);
        } catch (Exception ignored) {}
    }

    private void openClock() {
        Intent intent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private void openCalendar() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_CALENDAR);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private void attemptLaunch(AppItem app) {
        if (app == null) return;

        if ("com.example.focus.settings".equals(app.getPackageName())) {
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        // 1. Check Usage Limits & Schedule Block
        UsageTrackerEngine.ViolationResult violation =
                UsageTrackerEngine.checkLimitOrScheduleViolation(this, prefs, app.getPackageName());
        if (violation != null) {
            Intent intercept = new Intent(this, BlockedInterceptActivity.class);
            intercept.putExtra(BlockedInterceptActivity.EXTRA_APP_LABEL, app.getDisplayLabel());
            intercept.putExtra(BlockedInterceptActivity.EXTRA_TITLE, violation.title);
            intercept.putExtra(BlockedInterceptActivity.EXTRA_DETAIL, violation.detail);
            intercept.putExtra(BlockedInterceptActivity.EXTRA_UNLOCK_TIME, violation.unlockTime);
            startActivity(intercept);
            return;
        }

        // 2. Check Mindful Launch Delay
        int delaySeconds = prefs.getMindfulDelaySeconds(app.getPackageName());
        if (delaySeconds > 0) {
            Intent mindfulIntent = new Intent(this, MindfulDelayActivity.class);
            mindfulIntent.putExtra(MindfulDelayActivity.EXTRA_PACKAGE, app.getPackageName());
            mindfulIntent.putExtra(MindfulDelayActivity.EXTRA_LABEL, app.getDisplayLabel());
            mindfulIntent.putExtra(MindfulDelayActivity.EXTRA_SECONDS, delaySeconds);
            startActivity(mindfulIntent);
            return;
        }

        // 3. Normal Launch
        AppHelper.launchApp(this, app);
    }

    private void loadApps() {
        allInstalledApps = AppHelper.getInstalledApps(this, prefs);
        allInstalledApps.add(new AppItem("com.example.focus.settings", "SettingsActivity", "Focus Settings"));
        Collections.sort(allInstalledApps);

        appMap.clear();
        for (AppItem app : allInstalledApps) {
            appMap.put(app.getPackageName(), app);
        }

        Set<String> hiddenPackages = prefs.getHiddenApps();
        List<AppItem> visibleDrawerApps = new ArrayList<>();
        for (AppItem app : allInstalledApps) {
            if (!hiddenPackages.contains(app.getPackageName())) {
                visibleDrawerApps.add(app);
            }
        }
        drawerAdapter.setApps(visibleDrawerApps);

        // Load Favorites (Max 7)
        List<String> favoritePkgs = prefs.getFavorites();
        List<AppItem> favoriteApps = new ArrayList<>();
        for (String pkg : favoritePkgs) {
            AppItem app = appMap.get(pkg);
            if (app != null && favoriteApps.size() < 7) {
                favoriteApps.add(app);
            }
        }
        favoritesAdapter.setFavorites(favoriteApps);
    }

    private void showAppManagementDialog(AppItem app) {
        boolean isFav = prefs.isFavorite(app.getPackageName());
        String favAction = isFav ? getString(R.string.remove_from_favorites) : getString(R.string.pin_to_favorites);
        boolean isNotifBlocked = prefs.getBlockedNotificationPackages().contains(app.getPackageName());
        String notifAction = isNotifBlocked ? getString(R.string.unblock_notifications) : getString(R.string.block_notifications);

        CharSequence[] options = new CharSequence[]{
                favAction,
                getString(R.string.rename_app),
                getString(R.string.set_limits),
                getString(R.string.set_mindful_delay),
                notifAction,
                getString(R.string.hide_app),
                getString(R.string.app_info),
                getString(R.string.uninstall)
        };

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(app.getDisplayLabel())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            if (isFav) {
                                prefs.removeFavorite(app.getPackageName());
                            } else {
                                boolean added = prefs.addFavorite(app.getPackageName());
                                if (!added) {
                                    Toast.makeText(this, "Maximum 7 favorites allowed", Toast.LENGTH_SHORT).show();
                                }
                            }
                            loadApps();
                            break;
                        case 1:
                            showRenameAppDialog(app);
                            break;
                        case 2:
                            showSetLimitsDialog(app);
                            break;
                        case 3:
                            showMindfulDelayDialog(app);
                            break;
                        case 4:
                            toggleNotificationFilter(app);
                            break;
                        case 5:
                            prefs.hideApp(app.getPackageName());
                            loadApps();
                            break;
                        case 6:
                            AppHelper.openAppInfo(this, app.getPackageName());
                            break;
                        case 7:
                            AppHelper.uninstallApp(this, app.getPackageName());
                            break;
                    }
                })
                .show();
    }

    private void showFavoriteOptionsDialog(AppItem app) {
        CharSequence[] options = new CharSequence[]{
                getString(R.string.remove_from_favorites),
                getString(R.string.rename_app)
        };

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(app.getDisplayLabel())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        prefs.removeFavorite(app.getPackageName());
                        loadApps();
                    } else if (which == 1) {
                        showRenameAppDialog(app);
                    }
                })
                .show();
    }

    private void showRenameAppDialog(AppItem app) {
        final EditText input = new EditText(this);
        input.setText(app.getDisplayLabel());
        input.setTextColor(Color.WHITE);
        input.setSelectAllOnFocus(true);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(60, 20, 60, 20);
        container.addView(input);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Rename App")
                .setView(container)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    prefs.setAppAlias(app.getPackageName(), newName.isEmpty() ? null : newName);
                    loadApps();
                })
                .setNeutralButton("Reset", (d, w) -> {
                    prefs.setAppAlias(app.getPackageName(), null);
                    loadApps();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMindfulDelayDialog(AppItem app) {
        CharSequence[] durations = new CharSequence[]{"Disabled", "5 Seconds", "10 Seconds", "15 Seconds"};
        int currentDelay = prefs.getMindfulDelaySeconds(app.getPackageName());
        int checked = currentDelay == 5 ? 1 : (currentDelay == 10 ? 2 : (currentDelay == 15 ? 3 : 0));

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Mindful Launch Delay")
                .setSingleChoiceItems(durations, checked, (dialog, which) -> {
                    int sec = which == 1 ? 5 : (which == 2 ? 10 : (which == 3 ? 15 : 0));
                    prefs.setMindfulDelaySeconds(app.getPackageName(), sec);
                    dialog.dismiss();
                    Toast.makeText(this, "Mindful delay updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleNotificationFilter(AppItem app) {
        if (!NotificationHelper.hasNotificationListenerPermission(this)) {
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Notification Access Required")
                    .setMessage("Please grant Notification Access to filter notifications.")
                    .setPositiveButton("Open Settings", (d, w) -> NotificationHelper.openNotificationListenerSettings(this))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        boolean isBlocked = prefs.getBlockedNotificationPackages().contains(app.getPackageName());
        if (isBlocked) {
            prefs.unblockNotification(app.getPackageName());
            Toast.makeText(this, "Notifications allowed", Toast.LENGTH_SHORT).show();
        } else {
            prefs.blockNotification(app.getPackageName());
            Toast.makeText(this, "Notifications blocked", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSetLimitsDialog(AppItem app) {
        if (!UsageTrackerEngine.hasUsageAccessPermission(this)) {
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Usage Access Required")
                    .setMessage("Please grant Usage Access in system settings to enforce app limits.")
                    .setPositiveButton("Open Settings", (d, w) ->
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 20, 60, 20);

        final EditText hourlyInput = new EditText(this);
        hourlyInput.setHint("Hourly limit in minutes (0 for none)");
        hourlyInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        hourlyInput.setTextColor(Color.WHITE);
        hourlyInput.setHintTextColor(getResources().getColor(R.color.gray_muted));
        int currentHourly = prefs.getHourlyLimitMinutes(app.getPackageName());
        if (currentHourly > 0) hourlyInput.setText(String.valueOf(currentHourly));
        layout.addView(hourlyInput);

        final EditText weeklyInput = new EditText(this);
        weeklyInput.setHint("Weekly limit in minutes (0 for none)");
        weeklyInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        weeklyInput.setTextColor(Color.WHITE);
        weeklyInput.setHintTextColor(getResources().getColor(R.color.gray_muted));
        int currentWeekly = prefs.getWeeklyLimitMinutes(app.getPackageName());
        if (currentWeekly > 0) weeklyInput.setText(String.valueOf(currentWeekly));
        layout.addView(weeklyInput);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(app.getDisplayLabel() + " Limits")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String hStr = hourlyInput.getText().toString().trim();
                    String wStr = weeklyInput.getText().toString().trim();
                    int h = hStr.isEmpty() ? 0 : Integer.parseInt(hStr);
                    int wk = wStr.isEmpty() ? 0 : Integer.parseInt(wStr);
                    prefs.setHourlyLimitMinutes(app.getPackageName(), h);
                    prefs.setWeeklyLimitMinutes(app.getPackageName(), wk);

                    FocusAccessibilityService service = FocusAccessibilityService.getInstance();
                    if (service != null) {
                        service.updateServicePackageFilter();
                    }
                    Toast.makeText(this, "Limits saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
