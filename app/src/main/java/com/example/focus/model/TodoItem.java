package com.example.focus.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class TodoItem {

    // Eisenhower Matrix Quadrants
    public static final int QUADRANT_NONE = 0;
    public static final int QUADRANT_Q1_DO = 1;         // Urgent & Important (Do First)
    public static final int QUADRANT_Q2_SCHEDULE = 2;   // Not Urgent, Important (Deep Focus / Schedule)
    public static final int QUADRANT_Q3_DELEGATE = 3;   // Urgent, Not Important (Quick / Delegate)
    public static final int QUADRANT_Q4_ELIMINATE = 4;  // Not Urgent, Not Important (Eliminate / Drop)

    private final String id;
    private String text;
    private boolean isCompleted;
    private final long createdAt;

    // Productivity System Extensions
    private int eisenhowerQuadrant;
    private boolean isKaizen;                 // Micro-step / 1% improvement (< 5 mins)
    private int pomodorosCompleted;           // Completed 25m pomodoros
    private int pomodorosEstimated;           // Target estimated pomodoros (e.g. 1, 2, 4)

    public TodoItem(String text) {
        this(UUID.randomUUID().toString(), text, false, System.currentTimeMillis(), QUADRANT_NONE, false, 0, 1);
    }

    public TodoItem(String id, String text, boolean isCompleted, long createdAt) {
        this(id, text, isCompleted, createdAt, QUADRANT_NONE, false, 0, 1);
    }

    public TodoItem(String id, String text, boolean isCompleted, long createdAt,
                    int eisenhowerQuadrant, boolean isKaizen, int pomodorosCompleted, int pomodorosEstimated) {
        this.id = id;
        this.text = text;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
        this.eisenhowerQuadrant = eisenhowerQuadrant;
        this.isKaizen = isKaizen;
        this.pomodorosCompleted = pomodorosCompleted;
        this.pomodorosEstimated = Math.max(1, pomodorosEstimated);
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

    public String getTitle() {
        return text;
    }

    public void setTitle(String title) {
        this.text = title;
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

    public int getEisenhowerQuadrant() {
        return eisenhowerQuadrant;
    }

    public void setEisenhowerQuadrant(int quadrant) {
        this.eisenhowerQuadrant = quadrant;
    }

    public boolean isKaizen() {
        return isKaizen;
    }

    public void setKaizen(boolean kaizen) {
        isKaizen = kaizen;
    }

    public int getPomodorosCompleted() {
        return pomodorosCompleted;
    }

    public void setPomodorosCompleted(int completed) {
        this.pomodorosCompleted = completed;
    }

    public void incrementPomodoro() {
        this.pomodorosCompleted++;
    }

    public int getPomodorosEstimated() {
        return pomodorosEstimated;
    }

    public void setPomodorosEstimated(int estimated) {
        this.pomodorosEstimated = Math.max(1, estimated);
    }

    public String getQuadrantBadge() {
        switch (eisenhowerQuadrant) {
            case QUADRANT_Q1_DO:
                return "⚡ Q1: DO FIRST";
            case QUADRANT_Q2_SCHEDULE:
                return "💎 Q2: DEEP FOCUS";
            case QUADRANT_Q3_DELEGATE:
                return "⏳ Q3: QUICK";
            case QUADRANT_Q4_ELIMINATE:
                return "🚫 Q4: DROP";
            default:
                return "";
        }
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("text", text);
        obj.put("isCompleted", isCompleted);
        obj.put("createdAt", createdAt);
        obj.put("eisenhowerQuadrant", eisenhowerQuadrant);
        obj.put("isKaizen", isKaizen);
        obj.put("pomodorosCompleted", pomodorosCompleted);
        obj.put("pomodorosEstimated", pomodorosEstimated);
        return obj;
    }

    public static TodoItem fromJsonObject(JSONObject obj) {
        String id = obj.optString("id", UUID.randomUUID().toString());
        String text = obj.optString("text", "");
        boolean completed = obj.optBoolean("isCompleted", false);
        long time = obj.optLong("createdAt", System.currentTimeMillis());
        int quadrant = obj.optInt("eisenhowerQuadrant", QUADRANT_NONE);
        boolean kaizen = obj.optBoolean("isKaizen", false);
        int pCompleted = obj.optInt("pomodorosCompleted", 0);
        int pEstimated = obj.optInt("pomodorosEstimated", 1);
        return new TodoItem(id, text, completed, time, quadrant, kaizen, pCompleted, pEstimated);
    }
}
