package com.example.focus.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.focus.automation.AutomationManager;
import com.example.focus.service.FocusAccessibilityService;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        Log.d(TAG, "Device rebooted / package replaced: " + intent.getAction());

        // Reschedule DND automation alarms
        AutomationManager.scheduleAlarms(context);

        // Update accessibility monitored packages
        FocusAccessibilityService service = FocusAccessibilityService.getInstance();
        if (service != null) {
            service.updateServicePackageFilter();
        }
    }
}

