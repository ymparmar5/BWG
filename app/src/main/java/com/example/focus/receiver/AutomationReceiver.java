package com.example.focus.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.focus.automation.AutomationManager;

public class AutomationReceiver extends BroadcastReceiver {

    public static final String ACTION_ENABLE_DND = "com.example.focus.ACTION_ENABLE_DND";
    public static final String ACTION_DISABLE_DND = "com.example.focus.ACTION_DISABLE_DND";
    private static final String TAG = "AutomationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        Log.d(TAG, "Automation action received: " + action);

        if (ACTION_ENABLE_DND.equals(action)) {
            AutomationManager.applyDnd(context, true);
            AutomationManager.scheduleAlarms(context);
        } else if (ACTION_DISABLE_DND.equals(action)) {
            AutomationManager.applyDnd(context, false);
            AutomationManager.scheduleAlarms(context);
        }
    }
}

