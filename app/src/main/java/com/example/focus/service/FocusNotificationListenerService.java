package com.example.focus.service;

import android.content.ComponentName;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.example.focus.data.FocusPreferences;
import com.example.focus.engine.NotificationFilterEngine;

public class FocusNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "FocusNotifListener";
    private FocusPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new FocusPreferences(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || prefs == null) return;

        boolean shouldDismiss = NotificationFilterEngine.evaluateStatusBarNotification(sbn, prefs, getPackageName());
        if (shouldDismiss) {
            Log.d(TAG, "Dismissing filtered notification from: " + sbn.getPackageName());
            cancelNotification(sbn.getKey());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(new ComponentName(this, FocusNotificationListenerService.class));
            } catch (Exception e) {
                Log.e(TAG, "Failed to request rebind", e);
            }
        }
    }
}

