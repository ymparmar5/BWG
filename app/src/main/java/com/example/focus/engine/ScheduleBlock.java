package com.example.focus.engine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class ScheduleBlock {
    private final String packageName;
    private final int startHour;
    private final int startMinute;
    private final int endHour;
    private final int endMinute;
    private final Set<Integer> daysOfWeek; // Calendar.SUNDAY (1) to Calendar.SATURDAY (7)

    public ScheduleBlock(String packageName, int startHour, int startMinute, int endHour, int endMinute, Set<Integer> daysOfWeek) {
        this.packageName = packageName;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
        this.daysOfWeek = daysOfWeek != null ? daysOfWeek : new HashSet<>();
    }

    public String getPackageName() {
        return packageName;
    }

    public int getStartHour() {
        return startHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public int getEndHour() {
        return endHour;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public Set<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }

    public boolean isActiveNow(Calendar cal) {
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            int currentDay = cal.get(Calendar.DAY_OF_WEEK);
            if (!daysOfWeek.contains(currentDay)) {
                return false;
            }
        }

        int currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        int startMinutes = startHour * 60 + startMinute;
        int endMinutes = endHour * 60 + endMinute;

        if (startMinutes <= endMinutes) {
            // Same day block, e.g. 09:00 to 17:00
            return currentMinutes >= startMinutes && currentMinutes < endMinutes;
        } else {
            // Overnight block, e.g. 22:00 to 07:00
            return currentMinutes >= startMinutes || currentMinutes < endMinutes;
        }
    }

    public String getFormattedWindow() {
        return String.format(java.util.Locale.US, "%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute);
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("packageName", packageName);
        obj.put("startHour", startHour);
        obj.put("startMinute", startMinute);
        obj.put("endHour", endHour);
        obj.put("endMinute", endMinute);
        JSONArray daysArr = new JSONArray();
        for (int d : daysOfWeek) {
            daysArr.put(d);
        }
        obj.put("daysOfWeek", daysArr);
        return obj;
    }

    public static ScheduleBlock fromJsonObject(JSONObject obj) throws JSONException {
        String pkg = obj.getString("packageName");
        int sh = obj.getInt("startHour");
        int sm = obj.getInt("startMinute");
        int eh = obj.getInt("endHour");
        int em = obj.getInt("endMinute");
        Set<Integer> days = new HashSet<>();
        JSONArray arr = obj.optJSONArray("daysOfWeek");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                days.add(arr.getInt(i));
            }
        }
        return new ScheduleBlock(pkg, sh, sm, eh, em, days);
    }
}

