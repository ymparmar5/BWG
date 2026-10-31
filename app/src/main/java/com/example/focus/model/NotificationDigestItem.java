package com.example.focus.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationDigestItem {
    private final String packageName;
    private final String title;
    private final String text;
    private final long timestamp;

    public NotificationDigestItem(String packageName, String title, String text, long timestamp) {
        this.packageName = packageName != null ? packageName : "";
        this.title = title != null ? title : "";
        this.text = text != null ? text : "";
        this.timestamp = timestamp;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("packageName", packageName);
        obj.put("title", title);
        obj.put("text", text);
        obj.put("timestamp", timestamp);
        return obj;
    }

    public static NotificationDigestItem fromJsonObject(JSONObject obj) {
        String pkg = obj.optString("packageName", "");
        String title = obj.optString("title", "");
        String text = obj.optString("text", "");
        long timestamp = obj.optLong("timestamp", System.currentTimeMillis());
        return new NotificationDigestItem(pkg, title, text, timestamp);
    }
}

