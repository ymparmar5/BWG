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
import com.example.focus.engine.PomodoroEngine;
import com.example.focus.engine.RuleMatcherEngine;
import com.example.focus.engine.RuleModel;
import com.example.focus.engine.UsageTrackerEngine;
import com.example.focus.engine.WebBlockerEngine;
import com.example.focus.model.BlockProfile;
import com.example.focus.ui.BlockedInterceptActivity;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Map;

public class FocusAccessibilityService extends AccessibilityService {

    public static final String TAG = "FOCUS_INSPECTOR";
    private static final long ACTION_DEBOUNCE_MS = 1200;
    private static final long SCAN_THROTTLE_MS = 350; // Max ~3 scans/sec to preserve battery & CPU

    private static WeakReference<FocusAccessibilityService> instanceRef;
    private FocusPreferences prefs;
    private long lastActionTime = 0;
    private long lastContentScanTime = 0;
    private long continuousSessionStartTime = 0;
    private long lastMindfulAlertTime = 0;
    private volatile boolean isScreenOn = true;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ScreenStateReceiver screenReceiver;

    private class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                isScreenOn = false;
                continuousSessionStartTime = 0;
            } else if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
                isScreenOn = true;
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
        if (event == null || prefs == null || !isScreenOn) return;

        CharSequence pkgSeq = event.getPackageName();
        if (pkgSeq == null) return;
        String packageName = pkgSeq.toString();

        if (packageName.equals(getPackageName())) {
            return;
        }

        int eventType = event.getEventType();
        long now = SystemClock.elapsedRealtime();

        // FAST PATH: For window content changes (scrolls, text typing, animations),
        // completely bypass processing unless it is an active content-blocking target.
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (!isContentInspectionTarget(packageName)) {
                return; // Zero CPU/RAM overhead for 99% of apps on the phone
            }
            if (now - lastContentScanTime < SCAN_THROTTLE_MS) {
                return; // Throttle scans to max 3/sec to eliminate frame drops & battery drain
            }
        }

        if (continuousSessionStartTime == 0) {
            continuousSessionStartTime = now;
        }

        // 1. Continuous Screen Time Mindfulness Alert (Evaluated on window changes)
        if (prefs.isMindfulnessReminderEnabled() && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
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

        // 2-5. App Blocklist, Bedtime, Limits & Block Profiles (Only on window state changed)
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Named Block Profiles Check
            BlockProfile activeProfile = prefs.getActiveBlockProfileForPackage(this, packageName);
            if (activeProfile != null && activeProfile.isBlockAppLaunch()) {
                prefs.incrementBlockedAppLaunchesCount();
                long appUsage = UsageTrackerEngine.getTodayAppUsageSeconds(this, packageName);
                launchTakeoverScreen(
                        getAppDisplayName(packageName),
                        activeProfile.getName() + " Active",
                        "App is blocked by your '" + activeProfile.getName() + "' block rule.",
                        "Time spent today: " + UsageTrackerEngine.formatDuration(appUsage),
                        activeProfile.getActiveStatusBanner(this, packageName),
                        false
                );
                return;
            }

            // Strict App Blocklist Check
            if (prefs.isAppBlocked(packageName)) {
                prefs.incrementBlockedAppLaunchesCount();
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

            // Bedtime ("Bad Time") Schedule Check
            if (prefs.isBedtimeModeEnabled() && prefs.isBedtimeActiveNow()) {
                if (!isAllowedDuringBedtime(packageName)) {
                    prefs.incrementBlockedAppLaunchesCount();
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

            // Usage Limits & Focus Schedules
            UsageTrackerEngine.ViolationResult violation =
                    UsageTrackerEngine.checkLimitOrScheduleViolation(this, prefs, packageName);
            if (violation != null) {
                prefs.incrementBlockedAppLaunchesCount();
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

        // Record content scan timestamp to enforce rate-limiting
        lastContentScanTime = now;

        // 6. Adult & Misleading Site Blocker for Browsers (Chrome, Firefox, etc.)
        if (prefs.isAdultBlockerEnabled() && WebBlockerEngine.isBrowserPackage(packageName)) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                try {
                    String detectedUrl = WebBlockerEngine.extractBrowserUrl(root);
                    if (detectedUrl != null && WebBlockerEngine.isBlockedUrl(detectedUrl)) {
                        prefs.incrementBlockedAppLaunchesCount();
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        launchTakeoverScreen(
                                "Browser Protection",
                                "Site Blocked: " + WebBlockerEngine.getMatchedCategory(detectedUrl),
                                "Access to adult content, misleading ads, and adult dating sites is blocked.",
                                "Stay Focused & Mindful",
                                "Safe Browsing Active",
                                false
                        );
                        return;
                    }
                } finally {
                    root.recycle();
                }
            }
        }

        // 7. Content Blocker (YouTube Shorts, Instagram Reels, WhatsApp Status)
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

                boolean isAppInBlockPeriod = (prefs.getActiveBlockProfileForPackage(this, packageName) != null)
                        || (prefs.isBedtimeModeEnabled() && prefs.isBedtimeActiveNow())
                        || (UsageTrackerEngine.checkLimitOrScheduleViolation(this, prefs, packageName) != null)
                        || prefs.isAppBlocked(packageName);

                if (isAppInBlockPeriod) {
                    String featureName = "Short-form video";
                    if ("com.google.android.youtube".equals(packageName)) featureName = "YouTube Shorts";
                    else if ("com.instagram.android".equals(packageName)) featureName = "Instagram Reels";
                    else if ("com.whatsapp".equals(packageName)) featureName = "WhatsApp Status";

                    long appUsage = UsageTrackerEngine.getTodayAppUsageSeconds(this, packageName);
                    launchTakeoverScreen(
                            featureName,
                            "Distraction Blocked",
                            "Short-form video feeds are blocked during this block period.",
                            "Today's " + featureName + " app usage: " + UsageTrackerEngine.formatDuration(appUsage),
                            "Stay Mindful",
                            false
                    );
                } else {
                    // USER REQUIREMENT: Automatically close the reel or shorts on click without closing the app,
                    // allowing normal long-form video consumption!
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    lastActionTime = now;
                    mainHandler.post(() -> Toast.makeText(getApplicationContext(),
                            "Shorts/Reel closed • Long videos allowed", Toast.LENGTH_SHORT).show());
                }
            }
        } finally {
            root.recycle();
        }
    }

    private boolean isContentInspectionTarget(String packageName) {
        if ("com.google.android.youtube".equals(packageName) && prefs.isBlockYouTubeShortsEnabled()) return true;
        if ("com.instagram.android".equals(packageName) && prefs.isBlockInstagramReelsEnabled()) return true;
        if ("com.whatsapp".equals(packageName) && prefs.isBlockWhatsAppStatusEnabled()) return true;
        if (prefs.isAdultBlockerEnabled() && WebBlockerEngine.isBrowserPackage(packageName)) return true;
        return false;
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
