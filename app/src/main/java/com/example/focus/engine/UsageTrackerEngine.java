package com.example.focus.engine;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import com.example.focus.data.FocusPreferences;
import com.example.focus.model.AppUsageStat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UsageTrackerEngine {

    public static class RawUsageEvent {
        public static final int TYPE_RESUMED = 1;      // ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
        public static final int TYPE_PAUSED = 2;       // ACTIVITY_PAUSED / MOVE_TO_BACKGROUND
        public static final int TYPE_STOPPED = 23;     // ACTIVITY_STOPPED

        public final String packageName;
        public final int eventType;
        public final long timestamp;

        public RawUsageEvent(String packageName, int eventType, long timestamp) {
            this.packageName = packageName;
            this.eventType = eventType;
            this.timestamp = timestamp;
        }
    }

    public static class ViolationResult {
        public final String title;
        public final String detail;
        public final String unlockTime;

        public ViolationResult(String title, String detail, String unlockTime) {
            this.title = title;
            this.detail = detail;
            this.unlockTime = unlockTime;
        }
    }

    public static boolean hasUsageAccessPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) return false;
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.getPackageName()
        );
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static long getStartOfClockHour(long currentTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentTime);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long getStartOfToday(long currentTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentTime);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long getStartOfWeek(long currentTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentTime);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() > currentTime) {
            cal.add(Calendar.WEEK_OF_YEAR, -1);
        }
        return cal.getTimeInMillis();
    }

    /**
     * Pure testable calculation engine for foreground duration in milliseconds.
     */
    public static long calculateForegroundTimeMillis(List<RawUsageEvent> events, String targetPackage, long startTime, long endTime) {
        if (events == null || targetPackage == null || startTime >= endTime) {
            return 0;
        }

        List<RawUsageEvent> sorted = new ArrayList<>(events);
        Collections.sort(sorted, Comparator.comparingLong(e -> e.timestamp));

        long totalForegroundMs = 0;
        long activeSessionStart = -1;

        for (RawUsageEvent ev : sorted) {
            if (!targetPackage.equals(ev.packageName)) {
                continue;
            }

            if (ev.eventType == RawUsageEvent.TYPE_RESUMED) {
                if (ev.timestamp <= endTime) {
                    activeSessionStart = Math.max(startTime, ev.timestamp);
                }
            } else if (ev.eventType == RawUsageEvent.TYPE_PAUSED || ev.eventType == RawUsageEvent.TYPE_STOPPED) {
                if (activeSessionStart != -1) {
                    long sessionEnd = Math.min(endTime, ev.timestamp);
                    if (sessionEnd > activeSessionStart) {
                        totalForegroundMs += (sessionEnd - activeSessionStart);
                    }
                    activeSessionStart = -1;
                }
            }
        }

        // Handle case where app is currently still in the foreground at endTime
        if (activeSessionStart != -1 && activeSessionStart < endTime) {
            totalForegroundMs += (endTime - activeSessionStart);
        }

        return totalForegroundMs;
    }

    public static long getForegroundTimeSeconds(Context context, String packageName, long startTime, long endTime) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return 0;

        UsageEvents events = usm.queryEvents(startTime, endTime);
        if (events == null) return 0;

        List<RawUsageEvent> rawList = new ArrayList<>();
        UsageEvents.Event ev = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(ev);
            if (packageName.equals(ev.getPackageName())) {
                int type = ev.getEventType();
                if (type == UsageEvents.Event.ACTIVITY_RESUMED || type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    rawList.add(new RawUsageEvent(ev.getPackageName(), RawUsageEvent.TYPE_RESUMED, ev.getTimeStamp()));
                } else if (type == UsageEvents.Event.ACTIVITY_PAUSED || type == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    rawList.add(new RawUsageEvent(ev.getPackageName(), RawUsageEvent.TYPE_PAUSED, ev.getTimeStamp()));
                } else if (type == 23) { // ACTIVITY_STOPPED on API 28+
                    rawList.add(new RawUsageEvent(ev.getPackageName(), RawUsageEvent.TYPE_STOPPED, ev.getTimeStamp()));
                }
            }
        }

        long totalMs = calculateForegroundTimeMillis(rawList, packageName, startTime, endTime);
        long seconds = totalMs / 1000;

        // Fallback to queryUsageStats if queryEvents had empty results on certain OEM implementations
        if (seconds == 0) {
            Map<String, UsageStats> statsMap = usm.queryAndAggregateUsageStats(startTime, endTime);
            if (statsMap != null && statsMap.containsKey(packageName)) {
                UsageStats stats = statsMap.get(packageName);
                if (stats != null) {
                    seconds = stats.getTotalTimeInForeground() / 1000;
                }
            }
        }

        return seconds;
    }

    /**
     * Returns total seconds an app has been used today (since midnight).
     */
    public static long getTodayAppUsageSeconds(Context context, String packageName) {
        long now = System.currentTimeMillis();
        long startOfToday = getStartOfToday(now);
        return getForegroundTimeSeconds(context, packageName, startOfToday, now);
    }

    /**
     * Returns total screen time across all apps today.
     */
    public static long getTodayTotalDeviceUsageSeconds(Context context) {
        if (!hasUsageAccessPermission(context)) return 0;
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return 0;

        long now = System.currentTimeMillis();
        long startOfToday = getStartOfToday(now);

        Map<String, UsageStats> statsMap = usm.queryAndAggregateUsageStats(startOfToday, now);
        if (statsMap == null) return 0;

        long totalMs = 0;
        for (UsageStats stats : statsMap.values()) {
            totalMs += stats.getTotalTimeInForeground();
        }
        return totalMs / 1000;
    }

    /**
     * Calculates device unlock count today.
     * Uses UsageEvents (KEYGUARD_HIDDEN = 18, SCREEN_INTERACTIVE = 15) and FocusPreferences counter.
     */
    public static int getTodayUnlockCount(Context context) {
        FocusPreferences prefs = new FocusPreferences(context);
        int localCount = prefs.getTodayUnlockCount();

        if (!hasUsageAccessPermission(context)) {
            return Math.max(1, localCount);
        }

        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return Math.max(1, localCount);

        long now = System.currentTimeMillis();
        long startOfToday = getStartOfToday(now);

        UsageEvents events = usm.queryEvents(startOfToday, now);
        if (events == null) return Math.max(1, localCount);

        int eventUnlocks = 0;
        UsageEvents.Event ev = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(ev);
            int type = ev.getEventType();
            // 18 = KEYGUARD_HIDDEN (Unlock on API 28+)
            // 15 = SCREEN_INTERACTIVE
            if (type == 18) {
                eventUnlocks++;
            }
        }

        return Math.max(localCount, eventUnlocks);
    }

    /**
     * Returns list of AppUsageStat for today, sorted either low-to-high or high-to-low.
     */
    public static List<AppUsageStat> getTodayAppUsageStats(Context context, boolean lowToHigh) {
        List<AppUsageStat> list = new ArrayList<>();
        if (!hasUsageAccessPermission(context)) {
            return list;
        }

        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return list;

        long now = System.currentTimeMillis();
        long startOfToday = getStartOfToday(now);

        Map<String, UsageStats> statsMap = usm.queryAndAggregateUsageStats(startOfToday, now);
        if (statsMap == null || statsMap.isEmpty()) return list;

        long totalUsageSecs = getTodayTotalDeviceUsageSeconds(context);
        PackageManager pm = context.getPackageManager();

        for (Map.Entry<String, UsageStats> entry : statsMap.entrySet()) {
            String pkg = entry.getKey();
            UsageStats stats = entry.getValue();
            long sec = stats.getTotalTimeInForeground() / 1000;
            if (sec <= 0) continue;

            // Skip own package or system background services
            if (context.getPackageName().equals(pkg)) continue;

            String label = pkg;
            try {
                label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
            } catch (Exception ignored) {}

            int pct = totalUsageSecs > 0 ? (int) ((sec * 100) / totalUsageSecs) : 0;
            list.add(new AppUsageStat(pkg, label, sec, pct));
        }

        // Sort by usage seconds
        if (lowToHigh) {
            list.sort(Comparator.comparingLong(AppUsageStat::getUsageSeconds));
        } else {
            list.sort((a, b) -> Long.compare(b.getUsageSeconds(), a.getUsageSeconds()));
        }

        return list;
    }

    /**
     * Formats seconds into human-readable duration like "45m", "1h 20m", or "< 1m".
     */
    public static String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds > 0 ? "< 1m" : "0m";
        }
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remMinutes = minutes % 60;
        if (hours > 0) {
            return hours + "h " + remMinutes + "m";
        } else {
            return minutes + "m";
        }
    }

    /**
     * Checks if hourly limit, weekly limit, or active schedule block is violated.
     * Returns ViolationResult if blocked, or null if allowed.
     */
    public static ViolationResult checkLimitOrScheduleViolation(Context context, FocusPreferences prefs, String packageName) {
        if (!hasUsageAccessPermission(context)) {
            return null;
        }

        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);

        // 1. Check Schedule Block (Focus Window)
        ScheduleBlock schedule = prefs.getScheduleBlock(packageName);
        if (schedule != null && schedule.isActiveNow(cal)) {
            String window = schedule.getFormattedWindow();
            return new ViolationResult(
                    "Focus Window Active",
                    "Blocked during focus schedule (" + window + ")",
                    String.format(Locale.US, "Unlocks at %02d:%02d", schedule.getEndHour(), schedule.getEndMinute())
            );
        }

        // 2. Check Hourly Limit
        int hourlyLimitMin = prefs.getHourlyLimitMinutes(packageName);
        if (hourlyLimitMin > 0) {
            long startHour = getStartOfClockHour(now);
            long usedSeconds = getForegroundTimeSeconds(context, packageName, startHour, now);
            long usedMinutes = usedSeconds / 60;
            if (usedMinutes >= hourlyLimitMin) {
                cal.add(Calendar.HOUR_OF_DAY, 1);
                int nextHour = cal.get(Calendar.HOUR_OF_DAY);
                return new ViolationResult(
                        "Hourly Limit Reached",
                        usedMinutes + "m / " + hourlyLimitMin + "m used this hour",
                        String.format(Locale.US, "Unlocks at %02d:00", nextHour)
                );
            }
        }

        // 3. Check Weekly Limit
        int weeklyLimitMin = prefs.getWeeklyLimitMinutes(packageName);
        if (weeklyLimitMin > 0) {
            long startWeek = getStartOfWeek(now);
            long usedSeconds = getForegroundTimeSeconds(context, packageName, startWeek, now);
            long usedMinutes = usedSeconds / 60;
            if (usedMinutes >= weeklyLimitMin) {
                return new ViolationResult(
                        "Weekly Limit Reached",
                        usedMinutes + "m / " + weeklyLimitMin + "m used this week",
                        "Unlocks next Monday at 00:00"
                );
            }
        }

        return null;
    }
}
