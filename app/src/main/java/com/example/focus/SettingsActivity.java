package com.example.focus;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.focus.automation.AutomationManager;
import com.example.focus.data.FocusPreferences;
import com.example.focus.engine.UsageTrackerEngine;
import com.example.focus.model.AppItem;
import com.example.focus.model.NotificationDigestItem;
import com.example.focus.service.FocusAccessibilityService;
import com.example.focus.util.AppHelper;
import com.example.focus.util.NotificationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private FocusPreferences prefs;
    private final Map<String, String> appNameMap = new HashMap<>();
    private List<AppItem> installedApps = new ArrayList<>();

    private TextView statusLauncher;
    private TextView statusAccessibility;
    private TextView statusUsage;
    private TextView statusNotif;
    private TextView statusDnd;
    private TextView statusBattery;

    private SwitchCompat switchAutoLaunch;
    private TextView textSwipeLeft;
    private TextView textSwipeRight;

    private SwitchCompat switchBlocker;
    private SwitchCompat switchBlockerDebug;

    private SwitchCompat switchNotifFilter;
    private TextView textNotifMode;

    private SwitchCompat switchDnd;
    private TextView textDndStart;
    private TextView textDndEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new FocusPreferences(this);
        loadAppList();
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatuses();
        updateSwipeLabels();
        updateNotifModeLabel();
    }

    private void loadAppList() {
        installedApps = AppHelper.getInstalledApps(this, prefs);
        appNameMap.clear();
        for (AppItem app : installedApps) {
            appNameMap.put(app.getPackageName(), app.getDisplayLabel());
        }
    }

    private String getAppLabel(String packageName) {
        String label = appNameMap.get(packageName);
        return label != null ? label : packageName;
    }

    private void initViews() {
        statusLauncher = findViewById(R.id.status_perm_launcher);
        statusAccessibility = findViewById(R.id.status_perm_accessibility);
        statusUsage = findViewById(R.id.status_perm_usage);
        statusNotif = findViewById(R.id.status_perm_notif);
        statusDnd = findViewById(R.id.status_perm_dnd);
        statusBattery = findViewById(R.id.status_perm_battery);

        // 1. Permission clicks
        findViewById(R.id.btn_perm_launcher).setOnClickListener(v -> requestDefaultLauncher());
        findViewById(R.id.btn_perm_accessibility).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.btn_perm_usage).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        findViewById(R.id.btn_perm_notif).setOnClickListener(v ->
                NotificationHelper.openNotificationListenerSettings(this));
        findViewById(R.id.btn_perm_dnd).setOnClickListener(v ->
                AutomationManager.openDndSettings(this));
        findViewById(R.id.btn_perm_battery).setOnClickListener(v -> requestIgnoreBatteryOptimization());

        // 2. Home & Drawer
        switchAutoLaunch = findViewById(R.id.switch_auto_launch);
        switchAutoLaunch.setChecked(prefs.isAutoLaunchSingle());
        switchAutoLaunch.setOnCheckedChangeListener((btn, isChecked) -> prefs.setAutoLaunchSingle(isChecked));

        textSwipeLeft = findViewById(R.id.btn_assign_swipe_left);
        textSwipeLeft.setOnClickListener(v -> showAssignSwipeAppDialog(true));

        textSwipeRight = findViewById(R.id.btn_assign_swipe_right);
        textSwipeRight.setOnClickListener(v -> showAssignSwipeAppDialog(false));

        findViewById(R.id.btn_manage_favorites).setOnClickListener(v -> showManageFavoritesDialog());
        findViewById(R.id.btn_manage_hidden).setOnClickListener(v -> showManageHiddenAppsDialog());
        findViewById(R.id.btn_manage_renamed).setOnClickListener(v -> showManageRenamedAppsDialog());

        // 3. Content Blocker
        switchBlocker = findViewById(R.id.switch_blocker);
        switchBlocker.setChecked(prefs.isBlockerEnabled());
        switchBlocker.setOnCheckedChangeListener((btn, isChecked) -> prefs.setBlockerEnabled(isChecked));

        switchBlockerDebug = findViewById(R.id.switch_blocker_debug);
        switchBlockerDebug.setChecked(prefs.isBlockerDebugMode());
        switchBlockerDebug.setOnCheckedChangeListener((btn, isChecked) -> prefs.setBlockerDebugMode(isChecked));

        findViewById(R.id.btn_view_inspector_logs).setOnClickListener(v -> showInspectorLogsDialog());
        findViewById(R.id.btn_edit_json_rules).setOnClickListener(v -> showEditJsonRulesDialog());

        // 4. Usage Limits
        findViewById(R.id.btn_manage_limits).setOnClickListener(v -> showManageLimitsDialog());

        // 5. Notification Filter
        switchNotifFilter = findViewById(R.id.switch_notif_filter);
        switchNotifFilter.setChecked(prefs.isNotificationFilterEnabled());
        switchNotifFilter.setOnCheckedChangeListener((btn, isChecked) -> prefs.setNotificationFilterEnabled(isChecked));

        textNotifMode = findViewById(R.id.btn_toggle_notif_mode);
        textNotifMode.setOnClickListener(v -> {
            int current = prefs.getNotificationFilterMode();
            int next = current == FocusPreferences.NOTIF_MODE_BLOCKLIST ?
                    FocusPreferences.NOTIF_MODE_STRICT_ALLOWLIST : FocusPreferences.NOTIF_MODE_BLOCKLIST;
            prefs.setNotificationFilterMode(next);
            updateNotifModeLabel();
        });

        findViewById(R.id.btn_manage_notifs).setOnClickListener(v -> showManageNotifAppsDialog());
        findViewById(R.id.btn_view_safety_digest).setOnClickListener(v -> showSafetyDigestDialog());

        // 6. Automation
        switchDnd = findViewById(R.id.switch_dnd);
        switchDnd.setChecked(prefs.isDndAutomationEnabled());
        switchDnd.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked && !AutomationManager.hasDndPermission(this)) {
                switchDnd.setChecked(false);
                Toast.makeText(this, "Grant DND permission first", Toast.LENGTH_SHORT).show();
                AutomationManager.openDndSettings(this);
                return;
            }
            prefs.setDndAutomationEnabled(isChecked);
            AutomationManager.scheduleAlarms(this);
        });

        textDndStart = findViewById(R.id.text_dnd_start_time);
        textDndEnd = findViewById(R.id.text_dnd_end_time);
        updateDndTimeLabels();

        findViewById(R.id.btn_dnd_start_time).setOnClickListener(v -> {
            new TimePickerDialog(this, (tp, h, m) -> {
                prefs.setDndStartTime(h, m);
                updateDndTimeLabels();
                AutomationManager.scheduleAlarms(this);
            }, prefs.getDndStartHour(), prefs.getDndStartMinute(), true).show();
        });

        findViewById(R.id.btn_dnd_end_time).setOnClickListener(v -> {
            new TimePickerDialog(this, (tp, h, m) -> {
                prefs.setDndEndTime(h, m);
                updateDndTimeLabels();
                AutomationManager.scheduleAlarms(this);
            }, prefs.getDndEndHour(), prefs.getDndEndMinute(), true).show();
        });
    }

    private void updateSwipeLabels() {
        String left = prefs.getSwipeLeftPackage();
        textSwipeLeft.setText("Swipe Left App: " + (left != null ? getAppLabel(left) : "Default (Camera)"));

        String right = prefs.getSwipeRightPackage();
        textSwipeRight.setText("Swipe Right App: " + (right != null ? getAppLabel(right) : "Default (Phone)"));
    }

    private void updateNotifModeLabel() {
        int mode = prefs.getNotificationFilterMode();
        textNotifMode.setText("Filter Mode: " + (mode == FocusPreferences.NOTIF_MODE_BLOCKLIST ? "Blocklist (Noisy Apps)" : "Strict Allowlist (Calls/SMS only)"));
    }

    private void updateDndTimeLabels() {
        textDndStart.setText(String.format(Locale.US, "%02d:%02d", prefs.getDndStartHour(), prefs.getDndStartMinute()));
        textDndEnd.setText(String.format(Locale.US, "%02d:%02d", prefs.getDndEndHour(), prefs.getDndEndMinute()));
    }

    private void updatePermissionStatuses() {
        setPermStatus(statusLauncher, isDefaultLauncher(), "Granted", "Tap to set as default");
        setPermStatus(statusAccessibility, isAccessibilityServiceEnabled(), "Granted", "Tap to enable in Settings");
        setPermStatus(statusUsage, UsageTrackerEngine.hasUsageAccessPermission(this), "Granted", "Tap to enable in Settings");
        setPermStatus(statusNotif, NotificationHelper.hasNotificationListenerPermission(this), "Granted", "Tap to enable in Settings");
        setPermStatus(statusDnd, AutomationManager.hasDndPermission(this), "Granted", "Tap to enable in Settings");
        setPermStatus(statusBattery, isIgnoringBatteryOptimizations(), "Ignoring Optimizations", "Tap to disable optimization");
    }

    private void setPermStatus(TextView view, boolean granted, String textGranted, String textMissing) {
        view.setText(granted ? "[GRANTED] " + textGranted : "[ACTION REQUIRED] " + textMissing);
        view.setTextColor(getResources().getColor(granted ? R.color.white : R.color.gray_muted));
    }

    private boolean isDefaultLauncher() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolve = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolve != null && getPackageName().equals(resolve.activityInfo.packageName);
    }

    private void requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME));
                return;
            }
        }
        startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
    }

    private boolean isAccessibilityServiceEnabled() {
        ComponentName expectedComponent = new ComponentName(this, FocusAccessibilityService.class);
        String flat = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (flat == null) return false;
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(flat);
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledComponent = ComponentName.unflattenFromString(componentNameString);
            if (enabledComponent != null && enabledComponent.equals(expectedComponent)) {
                return true;
            }
        }
        return false;
    }

    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private void requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        }
    }

    private void showAssignSwipeAppDialog(boolean isLeft) {
        CharSequence[] names = new CharSequence[installedApps.size() + 1];
        names[0] = isLeft ? "Default (Camera)" : "Default (Phone)";
        for (int i = 0; i < installedApps.size(); i++) {
            names[i + 1] = installedApps.get(i).getDisplayLabel();
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(isLeft ? "Assign Swipe Left App" : "Assign Swipe Right App")
                .setItems(names, (d, which) -> {
                    if (which == 0) {
                        if (isLeft) prefs.setSwipeLeftPackage(null);
                        else prefs.setSwipeRightPackage(null);
                    } else {
                        String pkg = installedApps.get(which - 1).getPackageName();
                        if (isLeft) prefs.setSwipeLeftPackage(pkg);
                        else prefs.setSwipeRightPackage(pkg);
                    }
                    updateSwipeLabels();
                })
                .show();
    }

    private void showManageFavoritesDialog() {
        List<String> favs = prefs.getFavorites();
        if (favs.isEmpty()) {
            Toast.makeText(this, "No favorites assigned", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] items = new CharSequence[favs.size()];
        for (int i = 0; i < favs.size(); i++) {
            items[i] = getAppLabel(favs.get(i));
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Favorites (Max 7)")
                .setItems(items, (d, which) -> {
                    String pkg = favs.get(which);
                    prefs.removeFavorite(pkg);
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Clear All", (d, w) -> {
                    prefs.setFavorites(new ArrayList<>());
                    Toast.makeText(this, "All favorites cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showManageHiddenAppsDialog() {
        Set<String> hidden = prefs.getHiddenApps();
        if (hidden.isEmpty()) {
            Toast.makeText(this, "No apps are hidden", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> list = new ArrayList<>(hidden);
        CharSequence[] items = new CharSequence[list.size()];
        for (int i = 0; i < list.size(); i++) {
            items[i] = getAppLabel(list.get(i));
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Hidden Apps")
                .setItems(items, (d, which) -> {
                    String pkg = list.get(which);
                    prefs.unhideApp(pkg);
                    Toast.makeText(this, "App unhidden", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showManageRenamedAppsDialog() {
        List<AppItem> renamed = new ArrayList<>();
        for (AppItem app : installedApps) {
            String alias = prefs.getAppAlias(app.getPackageName());
            if (alias != null && !alias.isEmpty()) {
                renamed.add(app);
            }
        }

        if (renamed.isEmpty()) {
            Toast.makeText(this, "No custom app renames set", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] items = new CharSequence[renamed.size()];
        for (int i = 0; i < renamed.size(); i++) {
            AppItem it = renamed.get(i);
            items[i] = it.getDisplayLabel() + " (Original: " + it.getOriginalLabel() + ")";
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Renamed Apps")
                .setItems(items, (d, which) -> {
                    String pkg = renamed.get(which).getPackageName();
                    prefs.setAppAlias(pkg, null);
                    Toast.makeText(this, "Reset to original name", Toast.LENGTH_SHORT).show();
                    loadAppList();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showInspectorLogsDialog() {
        List<String> logs = prefs.getInspectorBuffer();
        if (logs.isEmpty()) {
            Toast.makeText(this, "No node signatures logged yet. Open YouTube/Instagram/WhatsApp with Debug Mode ON.", Toast.LENGTH_LONG).show();
            return;
        }

        CharSequence[] items = logs.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Recent 30 Inspected Nodes")
                .setItems(items, null)
                .setNeutralButton("Clear Logs", (d, w) -> {
                    prefs.clearInspectorBuffer();
                    Toast.makeText(this, "Inspector logs cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showEditJsonRulesDialog() {
        final EditText input = new EditText(this);
        input.setText(prefs.getRulesJson());
        input.setTextColor(getResources().getColor(R.color.white));
        input.setTextSize(12f);
        input.setPadding(30, 20, 30, 20);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("JSON Rule Store")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newJson = input.getText().toString().trim();
                    prefs.setRulesJson(newJson);
                    Toast.makeText(this, "Rules updated", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Reset Defaults", (d, w) -> {
                    prefs.resetRulesToDefault();
                    Toast.makeText(this, "Rules reset to default", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showManageLimitsDialog() {
        Set<String> pkgs = prefs.getAllMonitoredPackages();
        if (pkgs.isEmpty()) {
            Toast.makeText(this, "No apps currently have limits or schedules set", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> list = new ArrayList<>(pkgs);
        CharSequence[] items = new CharSequence[list.size()];
        for (int i = 0; i < list.size(); i++) {
            String pkg = list.get(i);
            int h = prefs.getHourlyLimitMinutes(pkg);
            int w = prefs.getWeeklyLimitMinutes(pkg);
            items[i] = getAppLabel(pkg) + " (Hourly: " + h + "m, Weekly: " + w + "m)";
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Active Limits & Schedules")
                .setItems(items, (d, which) -> {
                    String pkg = list.get(which);
                    new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                            .setTitle(getAppLabel(pkg))
                            .setMessage("Remove all limits and schedules for this app?")
                            .setPositiveButton("Remove", (dialog, w) -> {
                                prefs.setHourlyLimitMinutes(pkg, 0);
                                prefs.setWeeklyLimitMinutes(pkg, 0);
                                prefs.setScheduleBlock(pkg, null);
                                Toast.makeText(this, "Limits cleared", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showManageNotifAppsDialog() {
        int mode = prefs.getNotificationFilterMode();
        Set<String> set = mode == FocusPreferences.NOTIF_MODE_BLOCKLIST ?
                prefs.getBlockedNotificationPackages() : prefs.getAllowedNotificationPackages();

        if (set.isEmpty()) {
            Toast.makeText(this, "No apps configured for " + (mode == FocusPreferences.NOTIF_MODE_BLOCKLIST ? "blocklist" : "allowlist"), Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> list = new ArrayList<>(set);
        CharSequence[] items = new CharSequence[list.size()];
        for (int i = 0; i < list.size(); i++) {
            items[i] = getAppLabel(list.get(i));
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(mode == FocusPreferences.NOTIF_MODE_BLOCKLIST ? "Blocked Notification Apps" : "Allowed Notification Apps")
                .setItems(items, (d, which) -> {
                    String pkg = list.get(which);
                    if (mode == FocusPreferences.NOTIF_MODE_BLOCKLIST) {
                        prefs.unblockNotification(pkg);
                    } else {
                        Set<String> allowed = prefs.getAllowedNotificationPackages();
                        allowed.remove(pkg);
                        prefs.setAllowedNotificationPackages(allowed);
                    }
                    Toast.makeText(this, "App removed from list", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showSafetyDigestDialog() {
        List<NotificationDigestItem> items = prefs.getDigestItems();
        if (items.isEmpty()) {
            Toast.makeText(this, "Safety Digest is empty (no notifications filtered recently)", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] entries = new CharSequence[items.size()];
        for (int i = 0; i < items.size(); i++) {
            NotificationDigestItem item = items.get(i);
            entries[i] = String.format("[%s] %s\n%s: %s",
                    item.getFormattedTime(),
                    getAppLabel(item.getPackageName()),
                    item.getTitle(),
                    item.getText()
            );
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Notification Safety Digest")
                .setItems(entries, null)
                .setNeutralButton("Clear Digest", (d, w) -> {
                    prefs.clearDigest();
                    Toast.makeText(this, "Digest cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }
}
