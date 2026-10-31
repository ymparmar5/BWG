package com.example.focus.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FocusAccessibilityService extends AccessibilityService {

    public static final String TAG = "FOCUS_INSPECTOR";
    private static final long ACTION_DEBOUNCE_MS = 1200;

    private static WeakReference<FocusAccessibilityService> instanceRef;
    private FocusPreferences prefs;
    private long lastActionTime = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static FocusAccessibilityService getInstance() {
        return instanceRef != null ? instanceRef.get() : null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instanceRef = new WeakReference<>(this);
        prefs = new FocusPreferences(this);
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
        if (instanceRef != null && instanceRef.get() == this) {
            instanceRef = null;
        }
    }

    public void updateServicePackageFilter() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) return;

        Set<String> targetPackages = new HashSet<>();
        targetPackages.add("com.google.android.youtube");
        targetPackages.add("com.instagram.android");
        targetPackages.add("com.whatsapp");

        if (prefs != null) {
            targetPackages.addAll(prefs.getAllMonitoredPackages());
        }

        info.packageNames = targetPackages.toArray(new String[0]);
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

        // 1. Check App Usage Limits & Schedule Blocks on Window State Changes
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            UsageTrackerEngine.ViolationResult violation =
                    UsageTrackerEngine.checkLimitOrScheduleViolation(this, prefs, packageName);

            if (violation != null) {
                if (now - lastActionTime >= ACTION_DEBOUNCE_MS) {
                    lastActionTime = now;
                    performGlobalAction(GLOBAL_ACTION_HOME);

                    Intent intercept = new Intent(this, BlockedInterceptActivity.class);
                    intercept.putExtra(BlockedInterceptActivity.EXTRA_APP_LABEL, packageName);
                    intercept.putExtra(BlockedInterceptActivity.EXTRA_TITLE, violation.title);
                    intercept.putExtra(BlockedInterceptActivity.EXTRA_DETAIL, violation.detail);
                    intercept.putExtra(BlockedInterceptActivity.EXTRA_UNLOCK_TIME, violation.unlockTime);
                    intercept.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intercept);
                }
                return;
            }
        }

        // 2. Check Content Blocker (YouTube Shorts, Instagram Reels, WhatsApp Status)
        if (!prefs.isBlockerEnabled()) {
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
                lastActionTime = now;
                Log.w(TAG, "Content blocked in " + packageName + " by rule: " + result.matchedPattern);
                performGlobalAction(GLOBAL_ACTION_HOME);
                mainHandler.post(() -> Toast.makeText(getApplicationContext(), "Blocked by Focus Mode", Toast.LENGTH_SHORT).show());
            }
        } finally {
            root.recycle();
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

