package com.example.focus;

import com.example.focus.automation.AutomationManager;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;

public class AutomationScheduleTest {

    @Test
    public void testNextTrigger_LaterToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.OCTOBER, 31, 14, 0, 0);
        long now = cal.getTimeInMillis();

        long nextTrigger = AutomationManager.calculateNextTriggerMillis(22, 0, now);

        Calendar res = Calendar.getInstance();
        res.setTimeInMillis(nextTrigger);

        Assert.assertEquals(2026, res.get(Calendar.YEAR));
        Assert.assertEquals(Calendar.OCTOBER, res.get(Calendar.MONTH));
        Assert.assertEquals(31, res.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(22, res.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, res.get(Calendar.MINUTE));
    }

    @Test
    public void testNextTrigger_EarlierToday_RollsToTomorrow() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.OCTOBER, 31, 14, 0, 0);
        long now = cal.getTimeInMillis();

        long nextTrigger = AutomationManager.calculateNextTriggerMillis(7, 30, now);

        Calendar res = Calendar.getInstance();
        res.setTimeInMillis(nextTrigger);

        Assert.assertEquals(Calendar.NOVEMBER, res.get(Calendar.MONTH));
        Assert.assertEquals(1, res.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(7, res.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(30, res.get(Calendar.MINUTE));
    }
}

