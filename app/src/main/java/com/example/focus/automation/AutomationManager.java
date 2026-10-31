package com.example.focus.automation;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;

import com.example.focus.data.FocusPreferences;
import com.example.focus.receiver.AutomationReceiver;

import java.util.Calendar;

public class AutomationManager {

    public static boolean hasDndPermission(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }

    public static void openDndSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void scheduleAlarms(Context context) {
        FocusPreferences prefs = new FocusPreferences(context);
        if (!prefs.isDndAutomationEnabled()) {
            cancelAlarms(context);
            return;
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        long now = System.currentTimeMillis();
        long startTrigger = calculateNextTriggerMillis(prefs.getDndStartHour(), prefs.getDndStartMinute(), now);
        PendingIntent startPi = getPendingIntent(context, AutomationReceiver.ACTION_ENABLE_DND, 101);
        setExactAlarm(am, startTrigger, startPi);

        long endTrigger = calculateNextTriggerMillis(prefs.getDndEndHour(), prefs.getDndEndMinute(), now);
        PendingIntent endPi = getPendingIntent(context, AutomationReceiver.ACTION_DISABLE_DND, 102);
        setExactAlarm(am, endTrigger, endPi);
    }

    public static void cancelAlarms(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        am.cancel(getPendingIntent(context, AutomationReceiver.ACTION_ENABLE_DND, 101));
        am.cancel(getPendingIntent(context, AutomationReceiver.ACTION_DISABLE_DND, 102));
    }

    public static void applyDnd(Context context, boolean enable) {
        if (!hasDndPermission(context)) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (enable) {
            if (nm != null) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            }
            if (am != null) {
                am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        } else {
            if (nm != null) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            }
            if (am != null) {
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }
        }
    }

    public static long calculateNextTriggerMillis(int targetHour, int targetMinute, long currentMillis) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(currentMillis);
        target.set(Calendar.HOUR_OF_DAY, targetHour);
        target.set(Calendar.MINUTE, targetMinute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.getTimeInMillis() <= currentMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }
        return target.getTimeInMillis();
    }

    private static void setExactAlarm(AlarmManager am, long triggerMillis, PendingIntent pi) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
        }
    }

    private static PendingIntent getPendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, AutomationReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
