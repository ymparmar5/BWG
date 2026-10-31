package com.example.focus;

import android.app.Notification;

import com.example.focus.data.FocusPreferences;
import com.example.focus.engine.NotificationFilterEngine;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class NotificationFilterEngineTest {

    private static final String OWN_PKG = "com.example.focus";

    @Test
    public void testBlocklistMode_BlockedPackageDismissed() {
        Set<String> blocked = new HashSet<>();
        blocked.add("com.distracting.game");

        boolean dismiss = NotificationFilterEngine.shouldDismiss(
                "com.distracting.game",
                Notification.CATEGORY_SOCIAL,
                0,
                true,
                FocusPreferences.NOTIF_MODE_BLOCKLIST,
                blocked,
                null,
                OWN_PKG
        );

        Assert.assertTrue(dismiss);
    }

    @Test
    public void testBlocklistMode_UnblockedPackageAllowed() {
        Set<String> blocked = new HashSet<>();
        blocked.add("com.distracting.game");

        boolean dismiss = NotificationFilterEngine.shouldDismiss(
                "com.important.work",
                Notification.CATEGORY_MESSAGE,
                0,
                true,
                FocusPreferences.NOTIF_MODE_BLOCKLIST,
                blocked,
                null,
                OWN_PKG
        );

        Assert.assertFalse(dismiss);
    }

    @Test
    public void testStrictAllowlistMode_UnlistedPackageDismissed() {
        Set<String> allowed = new HashSet<>();
        allowed.add("com.google.android.dialer");

        boolean dismiss = NotificationFilterEngine.shouldDismiss(
                "com.social.network",
                null,
                0,
                true,
                FocusPreferences.NOTIF_MODE_STRICT_ALLOWLIST,
                null,
                allowed,
                OWN_PKG
        );

        Assert.assertTrue(dismiss);
    }

    @Test
    public void testStrictAllowlistMode_AllowedPackageAllowed() {
        Set<String> allowed = new HashSet<>();
        allowed.add("com.google.android.apps.messaging");

        boolean dismiss = NotificationFilterEngine.shouldDismiss(
                "com.google.android.apps.messaging",
                Notification.CATEGORY_MESSAGE,
                0,
                true,
                FocusPreferences.NOTIF_MODE_STRICT_ALLOWLIST,
                null,
                allowed,
                OWN_PKG
        );

        Assert.assertFalse(dismiss);
    }

    @Test
    public void testIncomingCall_NeverDismissed() {
        Set<String> blocked = new HashSet<>();
        blocked.add("com.google.android.dialer");

        boolean dismiss = NotificationFilterEngine.shouldDismiss(
                "com.google.android.dialer",
                Notification.CATEGORY_CALL,
                0,
                true,
                FocusPreferences.NOTIF_MODE_BLOCKLIST,
                blocked,
                null,
                OWN_PKG
        );

        Assert.assertFalse("Incoming calls must NEVER be dismissed", dismiss);
    }

    @Test
    public void testAlarmNotification_NeverDismissed() {
        Set<String> blocked = new HashSet<>();
        blocked.add("com.google.android.deskclock");

        boolean dismiss = NotificationFilterEngine.shouldDismiss(
                "com.google.android.deskclock",
                Notification.CATEGORY_ALARM,
                0,
                true,
                FocusPreferences.NOTIF_MODE_BLOCKLIST,
                blocked,
                null,
                OWN_PKG
        );

        Assert.assertFalse("Alarms must NEVER be dismissed", dismiss);
    }

    @Test
    public void testOngoingForegroundEvent_NeverDismissed() {
        Set<String> blocked = new HashSet<>();
        blocked.add("com.spotify.music");

        boolean dismiss = NotificationFilterEngine.shouldDismiss(
                "com.spotify.music",
                Notification.CATEGORY_TRANSPORT,
                Notification.FLAG_ONGOING_EVENT,
                true,
                FocusPreferences.NOTIF_MODE_BLOCKLIST,
                blocked,
                null,
                OWN_PKG
        );

        Assert.assertFalse("Ongoing foreground events must NEVER be dismissed", dismiss);
    }
}

