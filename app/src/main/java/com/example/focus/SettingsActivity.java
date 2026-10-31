package com.example.focus;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.focus.automation.AutomationManager;
import com.example.focus.data.FocusPreferences;
import com.example.focus.engine.RuleModel;
import com.example.focus.engine.ScheduleBlock;
import com.example.focus.engine.UsageTrackerEngine;
import com.example.focus.model.AppItem;
import com.example.focus.model.AppUsageStat;
import com.example.focus.model.NotificationDigestItem;
import com.example.focus.model.TodoItem;
import com.example.focus.service.FocusAccessibilityService;
import com.example.focus.ui.BlockedInterceptActivity;
import com.example.focus.util.AppHelper;
import com.example.focus.util.NotificationHelper;
import com.example.focus.util.ThemeHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private FocusPreferences prefs;
    private final Map<String, String> appNameMap = new HashMap<>();
    private List<AppItem> installedApps = new ArrayList<>();

    // Permissions
    private TextView statusLauncher;
    private TextView statusAccessibility;
    private TextView statusOverlay;
    private TextView statusUsage;
    private TextView statusNotif;
    private TextView statusDnd;
    private TextView statusBattery;

    // Home & Appearance
    private SwitchCompat switchDarkMode;
    private SwitchCompat switchAutoLaunch;

    // Blocker
    private SwitchCompat switchBlocker;
    private SwitchCompat switchBlockYt;
    private SwitchCompat switchBlockIg;
    private SwitchCompat switchBlockWa;
    private SwitchCompat switchBlockerDebug;

    // Bedtime
    private SwitchCompat switchBedtime;
    private TextView textBedtimeStart;
    private TextView textBedtimeEnd;

    // Mindfulness Continuous Timer
    private SwitchCompat switchMindfulness;
    private TextView textMindfulnessDuration;

    // Notifications
    private SwitchCompat switchNotifFilter;
    private TextView textNotifMode;

    // DND
    private SwitchCompat switchDnd;
    private TextView textDndStart;
    private TextView textDndEnd;

    private boolean isScreenTimeSortLowToHigh = false;

    private int getDialogTheme() {
        return prefs != null && prefs.isDarkMode()
                ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = new FocusPreferences(this);
        ThemeHelper.applyTheme(this, prefs.isDarkMode());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        loadAppList();
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatuses();
        updateNotifModeLabel();
        updateBedtimeLabels();
        updateMindfulnessLabel();
        updateDndTimeLabels();
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
        // 1. Permissions
        statusLauncher = findViewById(R.id.status_perm_launcher);
        statusAccessibility = findViewById(R.id.status_perm_accessibility);
        statusOverlay = findViewById(R.id.status_perm_overlay);
        statusUsage = findViewById(R.id.status_perm_usage);
        statusNotif = findViewById(R.id.status_perm_notif);
        statusDnd = findViewById(R.id.status_perm_dnd);
        statusBattery = findViewById(R.id.status_perm_battery);

        findViewById(R.id.btn_perm_launcher).setOnClickListener(v -> requestDefaultLauncher());
        findViewById(R.id.btn_perm_accessibility).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.btn_perm_overlay).setOnClickListener(v -> requestOverlayPermission());
        findViewById(R.id.btn_perm_usage).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        findViewById(R.id.btn_perm_notif).setOnClickListener(v ->
                NotificationHelper.openNotificationListenerSettings(this));
        findViewById(R.id.btn_perm_dnd).setOnClickListener(v ->
                AutomationManager.openDndSettings(this));
        findViewById(R.id.btn_perm_battery).setOnClickListener(v -> requestIgnoreBatteryOptimization());

        // 2. Home Screen & Appearance
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        switchDarkMode.setChecked(prefs.isDarkMode());
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.setDarkMode(isChecked);
            recreate();
        });

        switchAutoLaunch = findViewById(R.id.switch_auto_launch);
        switchAutoLaunch.setChecked(prefs.isAutoLaunchSingle());
        switchAutoLaunch.setOnCheckedChangeListener((btn, isChecked) -> prefs.setAutoLaunchSingle(isChecked));

        findViewById(R.id.btn_manage_favorites).setOnClickListener(v -> showManageFavoritesDialog());
        findViewById(R.id.btn_manage_hidden).setOnClickListener(v -> showManageHiddenAppsDialog());
        findViewById(R.id.btn_manage_renamed).setOnClickListener(v -> showManageRenamedAppsDialog());
        findViewById(R.id.btn_manage_todo).setOnClickListener(v -> showTodoDialog());
        findViewById(R.id.btn_view_screentime_report).setOnClickListener(v -> showScreenTimeReportDialog());

        // 3. Blocking UI & Distraction Controls
        switchBlocker = findViewById(R.id.switch_blocker);
        switchBlocker.setChecked(prefs.isBlockerEnabled());
        switchBlocker.setOnCheckedChangeListener((btn, isChecked) -> prefs.setBlockerEnabled(isChecked));

        switchBlockYt = findViewById(R.id.switch_block_yt);
        switchBlockYt.setChecked(prefs.isBlockYouTubeShortsEnabled());
        switchBlockYt.setOnCheckedChangeListener((btn, isChecked) -> prefs.setBlockYouTubeShortsEnabled(isChecked));

        switchBlockIg = findViewById(R.id.switch_block_ig);
        switchBlockIg.setChecked(prefs.isBlockInstagramReelsEnabled());
        switchBlockIg.setOnCheckedChangeListener((btn, isChecked) -> prefs.setBlockInstagramReelsEnabled(isChecked));

        switchBlockWa = findViewById(R.id.switch_block_wa);
        switchBlockWa.setChecked(prefs.isBlockWhatsAppStatusEnabled());
        switchBlockWa.setOnCheckedChangeListener((btn, isChecked) -> prefs.setBlockWhatsAppStatusEnabled(isChecked));

        findViewById(R.id.btn_preview_blocking).setOnClickListener(v -> previewBlockingScreen());
        findViewById(R.id.btn_manage_blocked_apps).setOnClickListener(v -> showManageBlockedAppsDialog());

        switchBlockerDebug = findViewById(R.id.switch_blocker_debug);
        switchBlockerDebug.setChecked(prefs.isBlockerDebugMode());
        switchBlockerDebug.setOnCheckedChangeListener((btn, isChecked) -> prefs.setBlockerDebugMode(isChecked));

        findViewById(R.id.btn_view_inspector_logs).setOnClickListener(v -> showInspectorLogsDialog());
        findViewById(R.id.btn_edit_json_rules).setOnClickListener(v -> showEditJsonRulesDialog());

        // 4. Bedtime ("Bad Time") Downtime Blocker
        switchBedtime = findViewById(R.id.switch_bedtime);
        switchBedtime.setChecked(prefs.isBedtimeModeEnabled());
        switchBedtime.setOnCheckedChangeListener((btn, isChecked) -> prefs.setBedtimeModeEnabled(isChecked));

        textBedtimeStart = findViewById(R.id.text_bedtime_start);
        textBedtimeEnd = findViewById(R.id.text_bedtime_end);
        updateBedtimeLabels();

        findViewById(R.id.btn_bedtime_start).setOnClickListener(v -> {
            new TimePickerDialog(this, getDialogTheme(), (tp, h, m) -> {
                prefs.setBedtimeStartTime(h, m);
                updateBedtimeLabels();
            }, prefs.getBedtimeStartHour(), prefs.getBedtimeStartMinute(), true).show();
        });

        findViewById(R.id.btn_bedtime_end).setOnClickListener(v -> {
            new TimePickerDialog(this, getDialogTheme(), (tp, h, m) -> {
                prefs.setBedtimeEndTime(h, m);
                updateBedtimeLabels();
            }, prefs.getBedtimeEndHour(), prefs.getBedtimeEndMinute(), true).show();
        });

        // 5. Mindfulness Continuous Screen Timer
        switchMindfulness = findViewById(R.id.switch_mindfulness_reminder);
        switchMindfulness.setChecked(prefs.isMindfulnessReminderEnabled());
        switchMindfulness.setOnCheckedChangeListener((btn, isChecked) -> prefs.setMindfulnessReminderEnabled(isChecked));

        textMindfulnessDuration = findViewById(R.id.text_mindfulness_duration);
        updateMindfulnessLabel();

        findViewById(R.id.btn_mindfulness_duration).setOnClickListener(v -> showMindfulnessIntervalDialog());

        // 6. Usage Limits & Schedules
        findViewById(R.id.btn_manage_limits).setOnClickListener(v -> showManageLimitsDialog());

        // 7. Notification Filter
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

        // 8. Automation (DND)
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
            new TimePickerDialog(this, getDialogTheme(), (tp, h, m) -> {
                prefs.setDndStartTime(h, m);
                updateDndTimeLabels();
                AutomationManager.scheduleAlarms(this);
            }, prefs.getDndStartHour(), prefs.getDndStartMinute(), true).show();
        });

        findViewById(R.id.btn_dnd_end_time).setOnClickListener(v -> {
            new TimePickerDialog(this, getDialogTheme(), (tp, h, m) -> {
                prefs.setDndEndTime(h, m);
                updateDndTimeLabels();
                AutomationManager.scheduleAlarms(this);
            }, prefs.getDndEndHour(), prefs.getDndEndMinute(), true).show();
        });
    }

    private void updateBedtimeLabels() {
        if (textBedtimeStart != null) {
            textBedtimeStart.setText(String.format(Locale.US, "%02d:%02d", prefs.getBedtimeStartHour(), prefs.getBedtimeStartMinute()));
        }
        if (textBedtimeEnd != null) {
            textBedtimeEnd.setText(String.format(Locale.US, "%02d:%02d", prefs.getBedtimeEndHour(), prefs.getBedtimeEndMinute()));
        }
    }

    private void updateMindfulnessLabel() {
        if (textMindfulnessDuration != null) {
            textMindfulnessDuration.setText("Every " + prefs.getMindfulnessReminderMinutes() + " Minutes");
        }
    }

    private void updateNotifModeLabel() {
        int mode = prefs.getNotificationFilterMode();
        textNotifMode.setText("Filter Mode: " + (mode == FocusPreferences.NOTIF_MODE_BLOCKLIST ? "Blocklist (Noisy Apps)" : "Strict Allowlist (Calls/SMS only)"));
    }

    private void updateDndTimeLabels() {
        if (textDndStart != null) {
            textDndStart.setText(String.format(Locale.US, "%02d:%02d", prefs.getDndStartHour(), prefs.getDndStartMinute()));
        }
        if (textDndEnd != null) {
            textDndEnd.setText(String.format(Locale.US, "%02d:%02d", prefs.getDndEndHour(), prefs.getDndEndMinute()));
        }
    }

    private void updatePermissionStatuses() {
        setPermStatus(statusLauncher, isDefaultLauncher(), "Granted", "Tap to set as default");
        setPermStatus(statusAccessibility, isAccessibilityServiceEnabled(), "Granted", "Tap to enable in Settings");
        setPermStatus(statusOverlay, hasOverlayPermission(), "Granted", "Tap to grant Display Over Other Apps");
        setPermStatus(statusUsage, UsageTrackerEngine.hasUsageAccessPermission(this), "Granted", "Tap to enable in Settings");
        setPermStatus(statusNotif, NotificationHelper.hasNotificationListenerPermission(this), "Granted", "Tap to enable in Settings");
        setPermStatus(statusDnd, AutomationManager.hasDndPermission(this), "Granted", "Tap to enable in Settings");
        setPermStatus(statusBattery, isIgnoringBatteryOptimizations(), "Ignoring Optimizations", "Tap to disable optimization");
    }

    private void setPermStatus(TextView view, boolean granted, String textGranted, String textMissing) {
        if (view == null) return;
        view.setText(granted ? "[GRANTED] " + textGranted : "[ACTION REQUIRED] " + textMissing);
        int grantedColor = prefs.isDarkMode() ? Color.WHITE : 0xFF111111;
        view.setTextColor(granted ? grantedColor : getResources().getColor(R.color.gray_muted));
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
        Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Overlay permission not required on this Android version", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        return FocusAccessibilityService.getInstance() != null;
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
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Intent fallback = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallback);
            }
        }
    }

    private void previewBlockingScreen() {
        Intent preview = new Intent(this, BlockedInterceptActivity.class);
        preview.putExtra(BlockedInterceptActivity.EXTRA_APP_LABEL, "YouTube Shorts");
        preview.putExtra(BlockedInterceptActivity.EXTRA_TITLE, "Distraction Blocked");
        preview.putExtra(BlockedInterceptActivity.EXTRA_DETAIL, "Short-form video feeds are blocked to keep you focused.");
        preview.putExtra(BlockedInterceptActivity.EXTRA_USAGE_TODAY, "Today's YouTube usage: 42m");
        preview.putExtra(BlockedInterceptActivity.EXTRA_UNLOCK_TIME, "Stay Mindful");
        preview.putExtra(BlockedInterceptActivity.EXTRA_IS_MINDFUL_ALERT, false);
        preview.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(preview);
    }

    // --- MINDFULNESS INTERVAL DIALOG ---
    private void showMindfulnessIntervalDialog() {
        String[] options = {"Every 15 Minutes", "Every 30 Minutes (Recommended)", "Every 45 Minutes", "Every 60 Minutes"};
        int[] minutes = {15, 30, 45, 60};

        int selectedIndex = 1;
        int currentMin = prefs.getMindfulnessReminderMinutes();
        for (int i = 0; i < minutes.length; i++) {
            if (minutes[i] == currentMin) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle("Continuous Screen Time Reminder")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    prefs.setMindfulnessReminderMinutes(minutes[which]);
                    updateMindfulnessLabel();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- SCREEN TIME & UNLOCK COUNT REPORT DIALOG ---
    private void showScreenTimeReportDialog() {
        if (!UsageTrackerEngine.hasUsageAccessPermission(this)) {
            new AlertDialog.Builder(this, getDialogTheme())
                    .setTitle("Usage Access Required")
                    .setMessage("To view screen time and unlock reports, grant Usage Access permission in Settings.")
                    .setPositiveButton("Grant Access", (d, w) -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        long totalScreenTimeSec = UsageTrackerEngine.getTodayTotalDeviceUsageSeconds(this);
        int unlockCount = UsageTrackerEngine.getTodayUnlockCount(this);
        List<AppUsageStat> stats = UsageTrackerEngine.getTodayAppUsageStats(this, isScreenTimeSortLowToHigh);

        StringBuilder sb = new StringBuilder();
        sb.append("Total Screen Time Today: ").append(UsageTrackerEngine.formatDuration(totalScreenTimeSec)).append("\n");
        sb.append("Device Unlocks Today: ").append(unlockCount).append(" times\n\n");
        sb.append("--- App Breakdown (").append(isScreenTimeSortLowToHigh ? "Lowest to Highest" : "Highest to Lowest").append(") ---\n");

        if (stats.isEmpty()) {
            sb.append("No app usage recorded today yet.");
        } else {
            for (AppUsageStat stat : stats) {
                sb.append("• ").append(stat.getAppLabel()).append(": ")
                        .append(UsageTrackerEngine.formatDuration(stat.getUsageSeconds()))
                        .append(" (").append(stat.getPercentage()).append("%)\n");
            }
        }

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle("Screen Time & Unlock Report")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton(isScreenTimeSortLowToHigh ? "Sort: High to Low" : "Sort: Low to High", (d, w) -> {
                    isScreenTimeSortLowToHigh = !isScreenTimeSortLowToHigh;
                    showScreenTimeReportDialog();
                })
                .show();
    }

    // --- MINIMALIST TO-DO LIST DIALOG ---
    private void showTodoDialog() {
        List<TodoItem> list = prefs.getTodoList();

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(36, 16, 36, 16);

        EditText input = new EditText(this);
        input.setHint("Add new focus task...");
        int textColor = prefs.isDarkMode() ? Color.WHITE : 0xFF111111;
        input.setTextColor(textColor);
        input.setHintTextColor(getResources().getColor(R.color.gray_muted));
        container.addView(input);

        ListView listView = new ListView(this);
        container.addView(listView);

        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return list.size();
            }

            @Override
            public Object getItem(int position) {
                return list.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) convertView;
                if (tv == null) {
                    tv = new TextView(SettingsActivity.this);
                    tv.setTextSize(16);
                    tv.setPadding(12, 20, 12, 20);
                }
                TodoItem item = list.get(position);
                tv.setText((item.isCompleted() ? "[✓] " : "[ ] ") + item.getText());
                tv.setTextColor(item.isCompleted() ? getResources().getColor(R.color.gray_muted) : textColor);
                if (item.isCompleted()) {
                    tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                }
                return tv;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            TodoItem item = list.get(position);
            prefs.toggleTodo(item.getId());
            item.setCompleted(!item.isCompleted());
            adapter.notifyDataSetChanged();
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            TodoItem item = list.get(position);
            new AlertDialog.Builder(SettingsActivity.this, getDialogTheme())
                    .setTitle("Delete Task")
                    .setMessage("Delete \"" + item.getText() + "\"?")
                    .setPositiveButton("Delete", (d, w) -> {
                        prefs.deleteTodo(item.getId());
                        list.remove(position);
                        adapter.notifyDataSetChanged();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle("Focus Tasks & To-Do List")
                .setView(container)
                .setPositiveButton("Add Task", (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(text)) {
                        prefs.addTodo(text);
                        Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // --- MANAGE BLOCKED APPS (STRICT APP BLOCKER) ---
    private void showManageBlockedAppsDialog() {
        loadAppList();
        Set<String> blockedSet = prefs.getBlockedApps();

        String[] labels = new String[installedApps.size()];
        boolean[] checked = new boolean[installedApps.size()];

        for (int i = 0; i < installedApps.size(); i++) {
            AppItem app = installedApps.get(i);
            labels[i] = app.getDisplayLabel();
            checked[i] = blockedSet.contains(app.getPackageName());
        }

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle("Manage Blocked Apps")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    Set<String> newBlocked = new HashSet<>();
                    for (int i = 0; i < installedApps.size(); i++) {
                        if (checked[i]) {
                            newBlocked.add(installedApps.get(i).getPackageName());
                        }
                    }
                    prefs.setBlockedApps(newBlocked);
                    Toast.makeText(this, newBlocked.size() + " apps blocked", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- FAVORITES, HIDDEN & RENAMED APPS ---
    private void showManageFavoritesDialog() {
        loadAppList();
        List<String> favs = prefs.getFavorites();

        String[] labels = new String[installedApps.size()];
        boolean[] checked = new boolean[installedApps.size()];

        for (int i = 0; i < installedApps.size(); i++) {
            AppItem app = installedApps.get(i);
            labels[i] = app.getDisplayLabel();
            checked[i] = favs.contains(app.getPackageName());
        }

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle("Manage Favorites (Max 7)")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    List<String> newFavs = new ArrayList<>();
                    for (int i = 0; i < installedApps.size(); i++) {
                        if (checked[i]) {
                            newFavs.add(installedApps.get(i).getPackageName());
                            if (newFavs.size() == 7) break;
                        }
                    }
                    prefs.setFavorites(newFavs);
                    Toast.makeText(this, "Favorites updated (" + newFavs.size() + "/7)", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showManageHiddenAppsDialog() {
        loadAppList();
        Set<String> hidden = prefs.getHiddenApps();

        String[] labels = new String[installedApps.size()];
        boolean[] checked = new boolean[installedApps.size()];

        for (int i = 0; i < installedApps.size(); i++) {
            AppItem app = installedApps.get(i);
            labels[i] = app.getDisplayLabel();
            checked[i] = hidden.contains(app.getPackageName());
        }

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle("Manage Hidden Apps")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    Set<String> newHidden = new HashSet<>();
                    for (int i = 0; i < installedApps.size(); i++) {
                        if (checked[i]) {
                            newHidden.add(installedApps.get(i).getPackageName());
                        }
                    }
                    prefs.setHiddenApps(newHidden);
                    Toast.makeText(this, "Hidden apps updated (" + newHidden.size() + ")", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showManageRenamedAppsDialog() {
        Map<String, String> aliases = prefs.getAppAliases();
        if (aliases.isEmpty()) {
            Toast.makeText(this, "No renamed apps. Long-press an app in the drawer to rename it.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] items = new String[aliases.size()];
        String[] pkgs = new String[aliases.size()];
        int idx = 0;
        for (Map.Entry<String, String> e : aliases.entrySet()) {
            pkgs[idx] = e.getKey();
            items[idx] = e.getValue() + " (" + getAppLabel(e.getKey()) + ")";
            idx++;
        }

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle("Reset Renamed Aliases")
                .setItems(items, (dialog, which) -> {
                    String pkg = pkgs[which];
                    prefs.removeAlias(pkg);
                    Toast.makeText(this, "Alias removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // --- USAGE LIMITS & SCHEDULES ---
    private void showManageLimitsDialog() {
        loadAppList();
        String[] labels = new String[installedApps.size()];
        for (int i = 0; i < installedApps.size(); i++) {
            AppItem app = installedApps.get(i);
            labels[i] = app.getDisplayLabel();
        }

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle("Select App to Configure Limits")
                .setItems(labels, (dialog, which) -> {
                    AppItem app = installedApps.get(which);
                    showConfigureLimitDialog(app);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showConfigureLimitDialog(AppItem app) {
        String pkg = app.getPackageName();
        int hourly = prefs.getHourlyLimitMinutes(pkg);
        int weekly = prefs.getWeeklyLimitMinutes(pkg);
        ScheduleBlock schedule = prefs.getScheduleBlock(pkg);

        String message = "Current Limits:\n" +
                "• Hourly: " + (hourly > 0 ? hourly + "m" : "None") + "\n" +
                "• Weekly: " + (weekly > 0 ? weekly + "m" : "None") + "\n" +
                "• Focus Window: " + (schedule != null ? schedule.getFormattedWindow() : "None");

        String[] options = {"Set Hourly Limit", "Set Weekly Limit", "Set Schedule Block", "Clear All Limits"};

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle(app.getDisplayLabel())
                .setMessage(message)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showSetMinutesDialog(app.getDisplayLabel(), "Hourly Limit (Minutes)", hourly, mins -> prefs.setHourlyLimitMinutes(pkg, mins));
                    } else if (which == 1) {
                        showSetMinutesDialog(app.getDisplayLabel(), "Weekly Limit (Minutes)", weekly, mins -> prefs.setWeeklyLimitMinutes(pkg, mins));
                    } else if (which == 2) {
                        showSetScheduleDialog(app);
                    } else if (which == 3) {
                        prefs.removeHourlyLimit(pkg);
                        prefs.removeWeeklyLimit(pkg);
                        prefs.removeScheduleBlock(pkg);
                        Toast.makeText(this, "Limits cleared for " + app.getDisplayLabel(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private interface OnMinutesSetListener {
        void onMinutesSet(int minutes);
    }

    private void showSetMinutesDialog(String title, String hint, int currentVal, OnMinutesSetListener listener) {
        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (currentVal > 0) input.setText(String.valueOf(currentVal));

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle(title)
                .setMessage(hint)
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        int val = Integer.parseInt(input.getText().toString().trim());
                        listener.onMinutesSet(val);
                        Toast.makeText(this, "Limit saved: " + val + "m", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSetScheduleDialog(AppItem app) {
        new TimePickerDialog(this, getDialogTheme(), (tp1, startH, startM) -> {
            new TimePickerDialog(this, getDialogTheme(), (tp2, endH, endM) -> {
                ScheduleBlock block = new ScheduleBlock(app.getPackageName(), startH, startM, endH, endM);
                prefs.setScheduleBlock(app.getPackageName(), block);
                Toast.makeText(this, "Schedule saved: " + block.getFormattedWindow(), Toast.LENGTH_SHORT).show();
            }, 17, 0, true).show();
        }, 9, 0, true).show();
    }

    // --- NOTIFICATION FILTER ---
    private void showManageNotifAppsDialog() {
        loadAppList();
        int mode = prefs.getNotificationFilterMode();
        boolean isBlocklist = mode == FocusPreferences.NOTIF_MODE_BLOCKLIST;

        Set<String> targetSet = isBlocklist ? prefs.getBlockedNotificationPackages() : prefs.getAllowedNotificationPackages();

        String[] labels = new String[installedApps.size()];
        boolean[] checked = new boolean[installedApps.size()];

        for (int i = 0; i < installedApps.size(); i++) {
            AppItem app = installedApps.get(i);
            labels[i] = app.getDisplayLabel();
            checked[i] = targetSet.contains(app.getPackageName());
        }

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle(isBlocklist ? "Manage Blocked Notifications (Noisy Apps)" : "Manage Allowed Notifications (Strict Allowlist)")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Save", (dialog, which) -> {
                    Set<String> newSet = new HashSet<>();
                    for (int i = 0; i < installedApps.size(); i++) {
                        if (checked[i]) {
                            newSet.add(installedApps.get(i).getPackageName());
                        }
                    }
                    if (isBlocklist) {
                        prefs.setBlockedNotificationPackages(newSet);
                    } else {
                        prefs.setAllowedNotificationPackages(newSet);
                    }
                    Toast.makeText(this, "Notification rules updated (" + newSet.size() + ")", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSafetyDigestDialog() {
        List<NotificationDigestItem> items = prefs.getDigestItems();
        if (items.isEmpty()) {
            Toast.makeText(this, "Safety Digest is empty (0 notifications dismissed)", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] lines = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            NotificationDigestItem it = items.get(i);
            lines[i] = "[" + it.getFormattedTime() + "] " + getAppLabel(it.getPackageName()) + ": " + it.getTitle() + " - " + it.getText();
        }

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle("Safety Digest (Last " + items.size() + " Dismissed)")
                .setItems(lines, null)
                .setPositiveButton("Clear Digest", (dialog, which) -> {
                    prefs.clearDigest();
                    Toast.makeText(this, "Digest cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // --- RULE INSPECTOR & JSON STORE ---
    private void showInspectorLogsDialog() {
        List<String> logs = prefs.getInspectorLogs();
        if (logs.isEmpty()) {
            Toast.makeText(this, "No inspector logs yet. Open YouTube/Instagram with Inspector Mode enabled to capture node signatures.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] items = logs.toArray(new String[0]);
        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle("Rule Inspector (Last " + items.length + " Nodes)")
                .setItems(items, null)
                .setPositiveButton("Clear Logs", (dialog, which) -> {
                    prefs.clearInspectorLogs();
                    Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showEditJsonRulesDialog() {
        EditText input = new EditText(this);
        input.setText(prefs.getRulesJson());
        input.setTextSize(13);

        new AlertDialog.Builder(this, getDialogTheme())
                .setTitle("JSON Rule Store Editor")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String raw = input.getText().toString().trim();
                    try {
                        org.json.JSONArray arr = new org.json.JSONArray(raw);
                        prefs.setRulesJson(arr.toString());
                        Toast.makeText(this, "Rules updated successfully", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid JSON: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNeutralButton("Reset to Defaults", (dialog, which) -> {
                    prefs.resetRulesToDefault();
                    Toast.makeText(this, "Rules reset to default", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
