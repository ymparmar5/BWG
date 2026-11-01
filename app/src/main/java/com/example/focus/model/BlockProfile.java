package com.example.focus.model;

import android.content.Context;

import com.example.focus.data.FocusPreferences;
import com.example.focus.engine.UsageTrackerEngine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class BlockProfile {

    private final String id;
    private String name;
    private final Set<String> targetPackages;

    // Strict Mode: When true, block cannot be paused or edited while active
    private boolean isStrictMode;

    // What to block
    private boolean blockAppLaunch;
    private boolean blockNotifications;

    // Restriction 1: Schedule Window
    private boolean hasSchedule;
    private int scheduleStartHour;
    private int scheduleStartMinute;
    private int scheduleEndHour;
    private int scheduleEndMinute;
    private final Set<Integer> scheduleDaysOfWeek; // Calendar.SUNDAY (1) to Calendar.SATURDAY (7)

    // Restriction 2: Daily Usage Limit (minutes)
    private boolean hasDailyLimit;
    private int dailyLimitMinutes;

    // Restriction 3: Temporary Quick Block for a certain period of time (timestamp until which blocked)
    private long temporaryBlockUntil;

    public BlockProfile(String name) {
        this(UUID.randomUUID().toString(), name);
    }

    public BlockProfile(String id, String name) {
        this.id = id;
        this.name = name;
        this.targetPackages = new HashSet<>();
        this.isStrictMode = false;
        this.blockAppLaunch = true;
        this.blockNotifications = false;

        this.hasSchedule = false;
        this.scheduleStartHour = 9;
        this.scheduleStartMinute = 0;
        this.scheduleEndHour = 17;
        this.scheduleEndMinute = 0;
        this.scheduleDaysOfWeek = getAllDaysOfWeek();

        this.hasDailyLimit = false;
        this.dailyLimitMinutes = 30;

        this.temporaryBlockUntil = 0;
    }

    public static Set<Integer> getAllDaysOfWeek() {
        Set<Integer> set = new HashSet<>();
        for (int i = 1; i <= 7; i++) set.add(i);
        return set;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getTargetPackages() {
        return targetPackages;
    }

    public void setTargetPackages(Set<String> packages) {
        this.targetPackages.clear();
        if (packages != null) {
            this.targetPackages.addAll(packages);
        }
    }

    public boolean isStrictMode() {
        return isStrictMode;
    }

    public void setStrictMode(boolean strictMode) {
        isStrictMode = strictMode;
    }

    public boolean isBlockAppLaunch() {
        return blockAppLaunch;
    }

    public void setBlockAppLaunch(boolean blockAppLaunch) {
        this.blockAppLaunch = blockAppLaunch;
    }

    public boolean isBlockNotifications() {
        return blockNotifications;
    }

    public void setBlockNotifications(boolean blockNotifications) {
        this.blockNotifications = blockNotifications;
    }

    public boolean hasSchedule() {
        return hasSchedule;
    }

    public void setHasSchedule(boolean hasSchedule) {
        this.hasSchedule = hasSchedule;
    }

    public int getScheduleStartHour() {
        return scheduleStartHour;
    }

    public int getScheduleStartMinute() {
        return scheduleStartMinute;
    }

    public int getScheduleEndHour() {
        return scheduleEndHour;
    }

    public int getScheduleEndMinute() {
        return scheduleEndMinute;
    }

    public void setScheduleTime(int startH, int startM, int endH, int endM) {
        this.scheduleStartHour = startH;
        this.scheduleStartMinute = startM;
        this.scheduleEndHour = endH;
        this.scheduleEndMinute = endM;
    }

    public Set<Integer> getScheduleDaysOfWeek() {
        return scheduleDaysOfWeek;
    }

    public void setScheduleDaysOfWeek(Set<Integer> days) {
        this.scheduleDaysOfWeek.clear();
        if (days != null) {
            this.scheduleDaysOfWeek.addAll(days);
        }
    }

    public boolean hasDailyLimit() {
        return hasDailyLimit;
    }

    public void setHasDailyLimit(boolean hasDailyLimit) {
        this.hasDailyLimit = hasDailyLimit;
    }

    public int getDailyLimitMinutes() {
        return dailyLimitMinutes;
    }

    public void setDailyLimitMinutes(int dailyLimitMinutes) {
        this.dailyLimitMinutes = dailyLimitMinutes;
    }

    public long getTemporaryBlockUntil() {
        return temporaryBlockUntil;
    }

    public void setTemporaryBlockDurationMinutes(int minutes) {
        if (minutes > 0) {
            this.temporaryBlockUntil = System.currentTimeMillis() + (minutes * 60 * 1000L);
        } else {
            this.temporaryBlockUntil = 0;
        }
    }

    public void clearTemporaryBlock() {
        this.temporaryBlockUntil = 0;
    }

    /**
     * Checks if this block profile is currently active and restricting apps.
     */
    public boolean isActiveNow(Context context, String packageName) {
        if (packageName == null || !targetPackages.contains(packageName)) {
            return false;
        }

        long now = System.currentTimeMillis();

        // 1. Temporary Quick Block (e.g. Block for 2 hours)
        if (temporaryBlockUntil > now) {
            return true;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);

        // 2. Weekly Schedule Window
        if (hasSchedule) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            if (scheduleDaysOfWeek.contains(dayOfWeek)) {
                int currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                int startTotal = scheduleStartHour * 60 + scheduleStartMinute;
                int endTotal = scheduleEndHour * 60 + scheduleEndMinute;

                boolean inWindow;
                if (startTotal <= endTotal) {
                    inWindow = currentMins >= startTotal && currentMins < endTotal;
                } else {
                    // Overnight crossover
                    inWindow = currentMins >= startTotal || currentMins < endTotal;
                }
                if (inWindow) {
                    return true;
                }
            }
        }

        // 3. Daily Usage Limit
        if (hasDailyLimit && dailyLimitMinutes > 0 && context != null) {
            long usedSeconds = UsageTrackerEngine.getTodayAppUsageSeconds(context, packageName);
            long usedMinutes = usedSeconds / 60;
            if (usedMinutes >= dailyLimitMinutes) {
                return true;
            }
        }

        return false;
    }

    public String getActiveStatusBanner(Context context, String packageName) {
        long now = System.currentTimeMillis();

        if (temporaryBlockUntil > now) {
            long remSecs = (temporaryBlockUntil - now) / 1000;
            long remMins = remSecs / 60;
            if (remMins > 60) {
                return String.format(Locale.US, "🔒 Block is active for next %dh %dm", remMins / 60, remMins % 60);
            } else {
                return String.format(Locale.US, "🔒 Block is active for next %d minutes", Math.max(1, remMins));
            }
        }

        if (hasSchedule) {
            return String.format(Locale.US, "🔒 Schedule is active until %02d:%02d", scheduleEndHour, scheduleEndMinute);
        }

        if (hasDailyLimit && context != null && packageName != null) {
            long usedSeconds = UsageTrackerEngine.getTodayAppUsageSeconds(context, packageName);
            long usedMins = usedSeconds / 60;
            long leftMins = Math.max(0, dailyLimitMinutes - usedMins);
            return String.format(Locale.US, "⌛ %dm left today", leftMins);
        }

        return "Active";
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);

        JSONArray pkgs = new JSONArray();
        for (String p : targetPackages) pkgs.put(p);
        obj.put("targetPackages", pkgs);

        obj.put("isStrictMode", isStrictMode);
        obj.put("blockAppLaunch", blockAppLaunch);
        obj.put("blockNotifications", blockNotifications);

        obj.put("hasSchedule", hasSchedule);
        obj.put("scheduleStartHour", scheduleStartHour);
        obj.put("scheduleStartMinute", scheduleStartMinute);
        obj.put("scheduleEndHour", scheduleEndHour);
        obj.put("scheduleEndMinute", scheduleEndMinute);

        JSONArray days = new JSONArray();
        for (int d : scheduleDaysOfWeek) days.put(d);
        obj.put("scheduleDaysOfWeek", days);

        obj.put("hasDailyLimit", hasDailyLimit);
        obj.put("dailyLimitMinutes", dailyLimitMinutes);
        obj.put("temporaryBlockUntil", temporaryBlockUntil);

        return obj;
    }

    public static BlockProfile fromJsonObject(JSONObject obj) {
        String id = obj.optString("id", UUID.randomUUID().toString());
        String name = obj.optString("name", "Focus Block");
        BlockProfile profile = new BlockProfile(id, name);

        JSONArray pkgs = obj.optJSONArray("targetPackages");
        if (pkgs != null) {
            Set<String> set = new HashSet<>();
            for (int i = 0; i < pkgs.length(); i++) {
                set.add(pkgs.optString(i));
            }
            profile.setTargetPackages(set);
        }

        profile.setStrictMode(obj.optBoolean("isStrictMode", false));
        profile.setBlockAppLaunch(obj.optBoolean("blockAppLaunch", true));
        profile.setBlockNotifications(obj.optBoolean("blockNotifications", false));

        profile.setHasSchedule(obj.optBoolean("hasSchedule", false));
        profile.scheduleStartHour = obj.optInt("scheduleStartHour", 9);
        profile.scheduleStartMinute = obj.optInt("scheduleStartMinute", 0);
        profile.scheduleEndHour = obj.optInt("scheduleEndHour", 17);
        profile.scheduleEndMinute = obj.optInt("scheduleEndMinute", 0);

        JSONArray days = obj.optJSONArray("scheduleDaysOfWeek");
        if (days != null) {
            Set<Integer> set = new HashSet<>();
            for (int i = 0; i < days.length(); i++) {
                set.add(days.optInt(i));
            }
            profile.setScheduleDaysOfWeek(set);
        }

        profile.setHasDailyLimit(obj.optBoolean("hasDailyLimit", false));
        profile.dailyLimitMinutes = obj.optInt("dailyLimitMinutes", 30);
        profile.temporaryBlockUntil = obj.optLong("temporaryBlockUntil", 0);

        return profile;
    }
}
