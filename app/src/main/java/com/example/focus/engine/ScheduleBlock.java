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

    public ScheduleBlock(String packageName, int startHour, int startMinute, int endHour, int endMinute) {
        this(packageName, startHour, startMinute, endHour, endMinute, getAllDaysOfWeek());
    }

    public ScheduleBlock(String packageName, int startHour, int startMinute, int endHour, int endMinute, Set<Integer> daysOfWeek) {
        this.packageName = packageName;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
        this.daysOfWeek = daysOfWeek != null ? daysOfWeek : new HashSet<>();
    }

    public static Set<Integer> getAllDaysOfWeek() {
        Set<Integer> set = new HashSet<>();
        for (int i = 1; i <= 7; i++) set.add(i);
        return set;
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

    public boolean isActiveNow(Calendar now) {
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        if (!daysOfWeek.contains(dayOfWeek)) {
            return false;
        }

        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int startTotal = startHour * 60 + startMinute;
        int endTotal = endHour * 60 + endMinute;

        if (startTotal <= endTotal) {
            return currentMinutes >= startTotal && currentMinutes < endTotal;
        } else {
            // Over midnight (e.g. 22:00 to 07:00)
            return currentMinutes >= startTotal || currentMinutes < endTotal;
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
        JSONArray arr = new JSONArray();
        for (int d : daysOfWeek) arr.put(d);
        obj.put("daysOfWeek", arr);
        return obj;
    }

    public static ScheduleBlock fromJsonObject(JSONObject obj) {
        String pkg = obj.optString("packageName");
        int sh = obj.optInt("startHour");
        int sm = obj.optInt("startMinute");
        int eh = obj.optInt("endHour");
        int em = obj.optInt("endMinute");
        Set<Integer> days = new HashSet<>();
        JSONArray arr = obj.optJSONArray("daysOfWeek");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                days.add(arr.optInt(i));
            }
        }
        return new ScheduleBlock(pkg, sh, sm, eh, em, days);
    }
}
