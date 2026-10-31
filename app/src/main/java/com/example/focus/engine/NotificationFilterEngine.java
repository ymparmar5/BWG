package com.example.focus.engine;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import com.example.focus.data.FocusPreferences;
import com.example.focus.model.NotificationDigestItem;

import java.util.Set;

public class NotificationFilterEngine {

    public static boolean shouldDismiss(String packageName,
                                         String category,
                                         int flags,
                                         boolean isFilterEnabled,
                                         int filterMode,
                                         Set<String> blockedPackages,
                                         Set<String> allowedPackages,
                                         String ownPackageName) {
        if (!isFilterEnabled || packageName == null) {
            return false;
        }

        // Never filter own launcher notifications
        if (packageName.equals(ownPackageName)) {
            return false;
        }

        // Exemption 1: Active incoming or ongoing calls
        if (Notification.CATEGORY_CALL.equals(category)) {
            return false;
        }

        // Exemption 2: System alarms
        if (Notification.CATEGORY_ALARM.equals(category)) {
            return false;
        }

        // Exemption 3: Ongoing foreground events (media players, navigation, calls)
        if ((flags & Notification.FLAG_ONGOING_EVENT) != 0) {
            return false;
        }

        if (filterMode == FocusPreferences.NOTIF_MODE_BLOCKLIST) {
            // Blocklist mode: only dismiss apps explicitly marked in blockedPackages
            return blockedPackages != null && blockedPackages.contains(packageName);
        } else {
            // Strict Allowlist mode: dismiss everything EXCEPT explicitly allowed packages
            if (allowedPackages == null || !allowedPackages.contains(packageName)) {
                return true;
            }
            return false;
        }
    }

    public static boolean evaluateStatusBarNotification(StatusBarNotification sbn,
                                                         FocusPreferences prefs,
                                                         String ownPackageName) {
        if (sbn == null || prefs == null) {
            return false;
        }

        Notification n = sbn.getNotification();
        if (n == null) {
            return false;
        }

        String pkg = sbn.getPackageName();
        String cat = n.category;
        int flags = n.flags;

        boolean dismiss = shouldDismiss(
                pkg,
                cat,
                flags,
                prefs.isNotificationFilterEnabled(),
                prefs.getNotificationFilterMode(),
                prefs.getBlockedNotificationPackages(),
                prefs.getAllowedNotificationPackages(),
                ownPackageName
        );

        if (dismiss) {
            // Record in safety digest
            CharSequence titleSeq = n.extras != null ? n.extras.getCharSequence(Notification.EXTRA_TITLE) : null;
            CharSequence textSeq = n.extras != null ? n.extras.getCharSequence(Notification.EXTRA_TEXT) : null;
            String title = titleSeq != null ? titleSeq.toString() : "";
            String text = textSeq != null ? textSeq.toString() : "";

            NotificationDigestItem item = new NotificationDigestItem(pkg, title, text, System.currentTimeMillis());
            prefs.addDigestItem(item);
        }

        return dismiss;
    }
}

