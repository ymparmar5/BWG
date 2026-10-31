package com.example.focus.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.example.focus.data.FocusPreferences;
import com.example.focus.engine.RuleMatcherEngine;
import com.example.focus.engine.RuleModel;
import com.example.focus.engine.UsageTrackerEngine;
import com.example.focus.ui.BlockedInterceptActivity;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Map;

public class FocusAccessibilityService extends AccessibilityService {

    public static final String TAG = "FOCUS_INSPECTOR";
    private static final long ACTION_DEBOUNCE_MS = 1200;

    private static WeakReference<FocusAccessibilityService> instanceRef;
    private FocusPreferences prefs;
    private long lastActionTime = 0;
    private long continuousSessionStartTime = 0;
    private long lastMindfulAlertTime = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ScreenStateReceiver screenReceiver;

    private class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                continuousSessionStartTime = 0;
            } else if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
                continuousSessionStartTime = SystemClock.elapsedRealtime();
                if (Intent.ACTION_USER_PRESENT.equals(action) && prefs != null) {
                    prefs.incrementTodayUnlockCount();
                }
            }
        }
    }

    public static FocusAccessibilityService getInstance() {
        return instanceRef != null ? instanceRef.get() : null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instanceRef = new WeakReference<>(this);
        prefs = new FocusPreferences(this);
        continuousSessionStartTime = SystemClock.elapsedRealtime();

        screenReceiver = new ScreenStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instanceRef = new WeakReference<>(this);
        updateServicePackageFilter();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (Exception ignored) {}
        }
        if (instanceRef != null && instanceRef.get() == this) {
            instanceRef = null;
        }
    }

    public void updateServicePackageFilter() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) return;

        // Monitor all packages so bed time, limits, and continuous mindfulness work across the OS
        info.packageNames = null;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || prefs == null) return;

        CharSequence pkgSeq = event.getPackageName();
        if (pkgSeq == null) return;
        String packageName = pkgSeq.toString();

        if (packageName.equals(getPackageName())) {
            return;
        }

        long now = SystemClock.elapsedRealtime();

        if (continuousSessionStartTime == 0) {
            continuousSessionStartTime = now;
        }

        // 1. Continuous Screen Time Mindfulness Alert (e.g. 30 Minutes Continuous Usage)
        if (prefs.isMindfulnessReminderEnabled()) {
            long continuousMs = now - continuousSessionStartTime;
            long reminderThresholdMs = prefs.getMindfulnessReminderMinutes() * 60 * 1000L;
            if (continuousMs >= reminderThresholdMs && (now - lastMindfulAlertTime) >= reminderThresholdMs) {
                lastMindfulAlertTime = now;
                long totalScreenTime = UsageTrackerEngine.getTodayTotalDeviceUsageSeconds(this);
                launchTakeoverScreen(
                        "Mindfulness Reminder",
                        prefs.getMindfulnessReminderMinutes() + "-Minute Screen Time Alert",
                        "You have been using your phone continuously for " + prefs.getMindfulnessReminderMinutes() + " minutes.\nTake a mindful breath, relax your posture, and rest your eyes.",
                        "Total device screen time today: " + UsageTrackerEngine.formatDuration(totalScreenTime),
                        "Continuous session: " + UsageTrackerEngine.formatDuration(continuousMs / 1000),
                        true
                );
                return;
            }
        }

        // 2. Strict App Blocklist Check
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (prefs.isAppBlocked(packageName)) {
                long appUsage = UsageTrackerEngine.getTodayAppUsageSeconds(this, packageName);
                launchTakeoverScreen(
                        getAppDisplayName(packageName),
                        "App Blocked",
                        "This app is blocked by your Focus Mode configuration.",
                        "Time spent on this app today: " + UsageTrackerEngine.formatDuration(appUsage),
                        "Blocked by Focus Mode",
                        false
                );
                return;
            }
        }

        // 3. Bedtime ("Bad Time" Downtime Schedule) Check
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (prefs.isBedtimeModeEnabled() && prefs.isBedtimeActiveNow()) {
                if (!isAllowedDuringBedtime(packageName)) {
                    long appUsage = UsageTrackerEngine.getTodayAppUsageSeconds(this, packageName);
                    launchTakeoverScreen(
                            getAppDisplayName(packageName),
                            "Bedtime Mode Active",
                            String.format(Locale.US, "It is bedtime (%02d:%02d - %02d:%02d). Time to sleep and recharge.",
                                    prefs.getBedtimeStartHour(), prefs.getBedtimeStartMinute(),
                                    prefs.getBedtimeEndHour(), prefs.getBedtimeEndMinute()),
                            "Time spent on this app today: " + UsageTrackerEngine.formatDuration(appUsage),
                            String.format(Locale.US, "Unlocks at %02d:%02d", prefs.getBedtimeEndHour(), prefs.getBedtimeEndMinute()),
                            false
                    );
                    return;
                }
            }
        }

        // 3. Check App Usage Limits & Schedule Blocks on Window State Changes
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            UsageTrackerEngine.ViolationResult violation =
                    UsageTrackerEngine.checkLimitOrScheduleViolation(this, prefs, packageName);

            if (violation != null) {
                long appUsage = UsageTrackerEngine.getTodayAppUsageSeconds(this, packageName);
                launchTakeoverScreen(
                        getAppDisplayName(packageName),
                        violation.title,
                        violation.detail,
                        "Time spent today: " + UsageTrackerEngine.formatDuration(appUsage),
                        violation.unlockTime,
                        false
                );
                return;
            }
        }

        // 4. Check Content Blocker (YouTube Shorts, Instagram Reels, WhatsApp Status)
        if (!prefs.isBlockerEnabled()) {
            return;
        }

        if ("com.google.android.youtube".equals(packageName) && !prefs.isBlockYouTubeShortsEnabled()) {
            return;
        }
        if ("com.instagram.android".equals(packageName) && !prefs.isBlockInstagramReelsEnabled()) {
            return;
        }
        if ("com.whatsapp".equals(packageName) && !prefs.isBlockWhatsAppStatusEnabled()) {
            return;
        }

        Map<String, RuleModel> rulesMap = prefs.getRulesMap();
        RuleModel rule = rulesMap.get(packageName);
        if (rule == null) {
            return;
        }

        if (now - lastActionTime < ACTION_DEBOUNCE_MS) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        boolean debugMode = prefs.isBlockerDebugMode();
        RuleMatcherEngine.InspectorCallback inspector = debugMode ? log -> {
            Log.d(TAG, "[" + packageName + "] " + log);
            prefs.addInspectorLog(packageName + " | " + log);
        } : null;

        try {
            RuleMatcherEngine.MatchResult result = RuleMatcherEngine.evaluateAccessibilityNode(root, rule, inspector);
            if (result.isBlocked) {
                Log.w(TAG, "Content blocked in " + packageName + " by rule: " + result.matchedPattern);

                String featureName = "Short-form video";
                if ("com.google.android.youtube".equals(packageName)) featureName = "YouTube Shorts";
                else if ("com.instagram.android".equals(packageName)) featureName = "Instagram Reels";
                else if ("com.whatsapp".equals(packageName)) featureName = "WhatsApp Status";

                long appUsage = UsageTrackerEngine.getTodayAppUsageSeconds(this, packageName);
                launchTakeoverScreen(
                        featureName,
                        "Distraction Blocked",
                        "Short-form video feeds are blocked to keep you focused.",
                        "Today's " + featureName + " app usage: " + UsageTrackerEngine.formatDuration(appUsage),
                        "Stay Mindful",
                        false
                );
            }
        } finally {
            root.recycle();
        }
    }

    private void launchTakeoverScreen(String appLabel, String title, String detail, String usageToday, String unlockTime, boolean isMindfulAlert) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastActionTime < ACTION_DEBOUNCE_MS) {
            return;
        }
        lastActionTime = now;

        Intent intercept = new Intent(this, BlockedInterceptActivity.class);
        intercept.putExtra(BlockedInterceptActivity.EXTRA_APP_LABEL, appLabel);
        intercept.putExtra(BlockedInterceptActivity.EXTRA_TITLE, title);
        intercept.putExtra(BlockedInterceptActivity.EXTRA_DETAIL, detail);
        intercept.putExtra(BlockedInterceptActivity.EXTRA_USAGE_TODAY, usageToday);
        intercept.putExtra(BlockedInterceptActivity.EXTRA_UNLOCK_TIME, unlockTime);
        intercept.putExtra(BlockedInterceptActivity.EXTRA_IS_MINDFUL_ALERT, isMindfulAlert);
        intercept.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        );
        startActivity(intercept);

        mainHandler.post(() -> Toast.makeText(getApplicationContext(), "Blocked by Focus Mode", Toast.LENGTH_SHORT).show());
    }

    private boolean isAllowedDuringBedtime(String packageName) {
        if (packageName == null) return true;
        if (packageName.equals(getPackageName())) return true;
        if ("com.android.systemui".equals(packageName)) return true;
        if ("android".equals(packageName)) return true;
        if (packageName.contains("dialer") || packageName.contains("telecom") || packageName.contains("incallui")) return true;
        if (packageName.contains("mms") || packageName.contains("messaging")) return true;
        if (packageName.contains("deskclock") || packageName.contains("alarmclock")) return true;
        if (packageName.contains("settings")) return true;
        return false;
    }

    private String getAppDisplayName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    public boolean expandNotificationShade() {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    }

    public boolean lockScreenDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
        return false;
    }

    @Override
    public void onInterrupt() {
    }
}
