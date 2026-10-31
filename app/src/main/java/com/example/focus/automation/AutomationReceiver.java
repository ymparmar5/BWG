package com.example.focus.automation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutomationReceiver extends BroadcastReceiver {

    public static final String ACTION_ENABLE_DND = "com.example.focus.ACTION_ENABLE_DND";
    public static final String ACTION_DISABLE_DND = "com.example.focus.ACTION_DISABLE_DND";
    private static final String TAG = "AutomationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        Log.d(TAG, "Received automation action: " + action);

        switch (action) {
            case ACTION_ENABLE_DND:
                AutomationManager.applyDnd(context, true);
                AutomationManager.scheduleAlarms(context);
                break;
            case ACTION_DISABLE_DND:
                AutomationManager.applyDnd(context, false);
                AutomationManager.scheduleAlarms(context);
                break;
            case Intent.ACTION_BOOT_COMPLETED:
                AutomationManager.scheduleAlarms(context);
                break;
        }
    }
}

