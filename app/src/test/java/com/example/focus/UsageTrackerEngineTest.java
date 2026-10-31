package com.example.focus;

import com.example.focus.engine.ScheduleBlock;
import com.example.focus.engine.UsageTrackerEngine;
import com.example.focus.engine.UsageTrackerEngine.RawUsageEvent;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UsageTrackerEngineTest {

    @Test
    public void testClockHourCalculation() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.OCTOBER, 31, 14, 35, 42);
        cal.set(Calendar.MILLISECOND, 500);

        long startOfHour = UsageTrackerEngine.getStartOfClockHour(cal.getTimeInMillis());

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTimeInMillis(startOfHour);

        Assert.assertEquals(14, resultCal.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, resultCal.get(Calendar.MINUTE));
        Assert.assertEquals(0, resultCal.get(Calendar.SECOND));
        Assert.assertEquals(0, resultCal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testStartOfWeekCalculation() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.OCTOBER, 31, 10, 0, 0); // Saturday

        long startOfWeek = UsageTrackerEngine.getStartOfWeek(cal.getTimeInMillis());

        Calendar res = Calendar.getInstance();
        res.setTimeInMillis(startOfWeek);

        Assert.assertEquals(Calendar.MONDAY, res.get(Calendar.DAY_OF_WEEK));
        Assert.assertEquals(0, res.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, res.get(Calendar.MINUTE));
        Assert.assertTrue(startOfWeek <= cal.getTimeInMillis());
    }

    @Test
    public void testForegroundCalculation_SingleCleanSession() {
        String target = "com.test.app";
        List<RawUsageEvent> events = new ArrayList<>();
        events.add(new RawUsageEvent(target, RawUsageEvent.TYPE_RESUMED, 1000L));
        events.add(new RawUsageEvent(target, RawUsageEvent.TYPE_PAUSED, 6000L)); // 5000ms

        long ms = UsageTrackerEngine.calculateForegroundTimeMillis(events, target, 0L, 10000L);
        Assert.assertEquals(5000L, ms);
    }

    @Test
    public void testForegroundCalculation_MultipleSessionsWithOtherApp() {
        String target = "com.target.app";
        String other = "com.other.app";

        List<RawUsageEvent> events = new ArrayList<>();
        events.add(new RawUsageEvent(target, RawUsageEvent.TYPE_RESUMED, 1000L));
        events.add(new RawUsageEvent(target, RawUsageEvent.TYPE_PAUSED, 3000L)); // 2000ms

        events.add(new RawUsageEvent(other, RawUsageEvent.TYPE_RESUMED, 3500L));
        events.add(new RawUsageEvent(other, RawUsageEvent.TYPE_PAUSED, 5000L));

        events.add(new RawUsageEvent(target, RawUsageEvent.TYPE_RESUMED, 6000L));
        events.add(new RawUsageEvent(target, RawUsageEvent.TYPE_STOPPED, 9000L)); // 3000ms

        long ms = UsageTrackerEngine.calculateForegroundTimeMillis(events, target, 0L, 10000L);
        Assert.assertEquals(5000L, ms);
    }

    @Test
    public void testForegroundCalculation_UnfinishedSessionAtEnd() {
        String target = "com.target.app";

        List<RawUsageEvent> events = new ArrayList<>();
        events.add(new RawUsageEvent(target, RawUsageEvent.TYPE_RESUMED, 5000L));
        // App is still open when we evaluate at 8000L

        long ms = UsageTrackerEngine.calculateForegroundTimeMillis(events, target, 0L, 8000L);
        Assert.assertEquals(3000L, ms);
    }

    @Test
    public void testForegroundCalculation_SessionAcrossStartTimeBoundary() {
        String target = "com.target.app";

        List<RawUsageEvent> events = new ArrayList<>();
        events.add(new RawUsageEvent(target, RawUsageEvent.TYPE_RESUMED, 1000L));
        events.add(new RawUsageEvent(target, RawUsageEvent.TYPE_PAUSED, 8000L));

        // We only evaluate from 4000L to 10000L
        long ms = UsageTrackerEngine.calculateForegroundTimeMillis(events, target, 4000L, 10000L);
        Assert.assertEquals(4000L, ms); // 8000 - 4000
    }

    @Test
    public void testScheduleBlock_SameDay() {
        Set<Integer> days = new HashSet<>();
        days.add(Calendar.MONDAY);
        days.add(Calendar.TUESDAY);

        ScheduleBlock block = new ScheduleBlock("com.block.app", 9, 0, 17, 0, days);

        Calendar testCal = Calendar.getInstance();
        testCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        testCal.set(Calendar.HOUR_OF_DAY, 14);
        testCal.set(Calendar.MINUTE, 30);
        Assert.assertTrue(block.isActiveNow(testCal));

        testCal.set(Calendar.HOUR_OF_DAY, 18);
        Assert.assertFalse(block.isActiveNow(testCal));

        // Different day
        testCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        testCal.set(Calendar.HOUR_OF_DAY, 14);
        Assert.assertFalse(block.isActiveNow(testCal));
    }

    @Test
    public void testScheduleBlock_Overnight() {
        Set<Integer> allDays = new HashSet<>();
        for (int i = 1; i <= 7; i++) allDays.add(i);

        ScheduleBlock overnight = new ScheduleBlock("com.night.app", 22, 0, 7, 0, allDays);

        Calendar lateNight = Calendar.getInstance();
        lateNight.set(Calendar.HOUR_OF_DAY, 23);
        lateNight.set(Calendar.MINUTE, 30);
        Assert.assertTrue(overnight.isActiveNow(lateNight));

        Calendar earlyMorning = Calendar.getInstance();
        earlyMorning.set(Calendar.HOUR_OF_DAY, 4);
        earlyMorning.set(Calendar.MINUTE, 0);
        Assert.assertTrue(overnight.isActiveNow(earlyMorning));

        Calendar daytime = Calendar.getInstance();
        daytime.set(Calendar.HOUR_OF_DAY, 12);
        daytime.set(Calendar.MINUTE, 0);
        Assert.assertFalse(overnight.isActiveNow(daytime));
    }
}

