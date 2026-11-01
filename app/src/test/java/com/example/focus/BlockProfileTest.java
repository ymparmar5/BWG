package com.example.focus;

import com.example.focus.model.BlockProfile;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockProfileTest {

    @Test
    public void testProfileDefaultsAndModifications() {
        BlockProfile profile = new BlockProfile("Deep Focus");
        assertEquals("Deep Focus", profile.getName());
        assertTrue(profile.isBlockAppLaunch());
        assertFalse(profile.isStrictMode());

        profile.setTargetPackages(new HashSet<>(Arrays.asList("com.instagram.android", "com.google.android.youtube")));
        assertTrue(profile.getTargetPackages().contains("com.instagram.android"));
        assertTrue(profile.getTargetPackages().contains("com.google.android.youtube"));
        assertFalse(profile.getTargetPackages().contains("com.whatsapp"));
    }

    @Test
    public void testTemporaryQuickBlock() {
        BlockProfile profile = new BlockProfile("QuickSprint");
        profile.setTargetPackages(new HashSet<>(Arrays.asList("com.facebook.katana")));

        assertFalse(profile.isActiveNow(null, "com.facebook.katana"));

        profile.setTemporaryBlockDurationMinutes(30);
        assertTrue(profile.isActiveNow(null, "com.facebook.katana"));
        assertFalse(profile.isActiveNow(null, "com.other.app"));

        profile.clearTemporaryBlock();
        assertFalse(profile.isActiveNow(null, "com.facebook.katana"));
    }

    @Test
    public void testJsonSerializationRoundTrip() throws Exception {
        BlockProfile original = new BlockProfile("Gaming Quarantine");
        original.setTargetPackages(new HashSet<>(Arrays.asList("com.pubg.imobile", "com.dts.freefireth")));
        original.setStrictMode(true);
        original.setBlockAppLaunch(true);
        original.setBlockNotifications(true);
        original.setHasSchedule(true);
        original.setScheduleTime(8, 30, 18, 0);
        original.setHasDailyLimit(true);
        original.setDailyLimitMinutes(45);

        JSONObject json = original.toJsonObject();
        BlockProfile deserialized = BlockProfile.fromJsonObject(json);

        assertEquals(original.getId(), deserialized.getId());
        assertEquals("Gaming Quarantine", deserialized.getName());
        assertTrue(deserialized.isStrictMode());
        assertTrue(deserialized.isBlockAppLaunch());
        assertTrue(deserialized.isBlockNotifications());
        assertTrue(deserialized.hasSchedule());
        assertEquals(8, deserialized.getScheduleStartHour());
        assertEquals(30, deserialized.getScheduleStartMinute());
        assertEquals(18, deserialized.getScheduleEndHour());
        assertEquals(0, deserialized.getScheduleEndMinute());
        assertTrue(deserialized.hasDailyLimit());
        assertEquals(45, deserialized.getDailyLimitMinutes());
        assertTrue(deserialized.getTargetPackages().contains("com.pubg.imobile"));
    }
}
