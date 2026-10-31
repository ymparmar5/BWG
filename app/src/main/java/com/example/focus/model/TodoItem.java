package com.example.focus.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class TodoItem {
    private final String id;
    private String text;
    private boolean isCompleted;
    private final long createdAt;

    public TodoItem(String text) {
        this(UUID.randomUUID().toString(), text, false, System.currentTimeMillis());
    }

    public TodoItem(String id, String text, boolean isCompleted, long createdAt) {
        this.id = id;
        this.text = text;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("text", text);
        obj.put("isCompleted", isCompleted);
        obj.put("createdAt", createdAt);
        return obj;
    }

    public static TodoItem fromJsonObject(JSONObject obj) {
        String id = obj.optString("id", UUID.randomUUID().toString());
        String text = obj.optString("text", "");
        boolean completed = obj.optBoolean("isCompleted", false);
        long time = obj.optLong("createdAt", System.currentTimeMillis());
        return new TodoItem(id, text, completed, time);
    }
}

