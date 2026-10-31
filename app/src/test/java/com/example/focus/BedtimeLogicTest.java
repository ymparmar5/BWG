package com.example.focus;

import org.junit.Assert;
import org.junit.Test;

public class BedtimeLogicTest {

    private boolean isBedtimeActive(int nowH, int nowM, int startH, int startM, int endH, int endM) {
        int nowTotal = nowH * 60 + nowM;
        int startTotal = startH * 60 + startM;
        int endTotal = endH * 60 + endM;

        if (startTotal == endTotal) return false;
        if (startTotal < endTotal) {
            return nowTotal >= startTotal && nowTotal < endTotal;
        } else {
            // Crossover midnight, e.g. 22:00 to 07:00
            return nowTotal >= startTotal || nowTotal < endTotal;
        }
    }

    @Test
    public void testBedtimeOverMidnight() {
        // Bedtime 22:00 to 07:00
        // 23:30 should be active
        Assert.assertTrue(isBedtimeActive(23, 30, 22, 0, 7, 0));
        // 01:15 should be active
        Assert.assertTrue(isBedtimeActive(1, 15, 22, 0, 7, 0));
        // 06:59 should be active
        Assert.assertTrue(isBedtimeActive(6, 59, 22, 0, 7, 0));
        // 07:00 should NOT be active
        Assert.assertFalse(isBedtimeActive(7, 0, 22, 0, 7, 0));
        // 14:00 should NOT be active
        Assert.assertFalse(isBedtimeActive(14, 0, 22, 0, 7, 0));
        // 21:59 should NOT be active
        Assert.assertFalse(isBedtimeActive(21, 59, 22, 0, 7, 0));
    }

    @Test
    public void testBedtimeSameDay() {
        // Bedtime 13:00 to 15:00
        Assert.assertTrue(isBedtimeActive(14, 0, 13, 0, 15, 0));
        Assert.assertFalse(isBedtimeActive(12, 59, 13, 0, 15, 0));
        Assert.assertFalse(isBedtimeActive(15, 0, 13, 0, 15, 0));
    }
}

