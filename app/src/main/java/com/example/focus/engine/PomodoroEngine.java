package com.example.focus.engine;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class PomodoroEngine {

    public static final int STATE_IDLE = 0;
    public static final int STATE_WORK = 1;         // 25 mins
    public static final int STATE_SHORT_BREAK = 2;  // 5 mins
    public static final int STATE_LONG_BREAK = 3;   // 15 mins

    public interface OnPomodoroTickListener {
        void onTick(long remainingSeconds, int state, String taskLabel);
        void onStateChanged(int newState, int completedCycles);
    }

    private static PomodoroEngine instance;

    private int currentState = STATE_IDLE;
    private boolean isRunning = false;
    private long remainingSeconds = 25 * 60;
    private long totalDurationSeconds = 25 * 60;
    private int completedCycles = 0;

    private String activeTaskId = null;
    private String activeTaskLabel = "Deep Focus Sprint";

    private final List<OnPomodoroTickListener> listeners = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;

            if (remainingSeconds > 0) {
                remainingSeconds--;
                notifyTick();
                handler.postDelayed(this, 1000);
            } else {
                onSessionFinished();
            }
        }
    };

    private PomodoroEngine() {}

    public static synchronized PomodoroEngine getInstance() {
        if (instance == null) {
            instance = new PomodoroEngine();
        }
        return instance;
    }

    public synchronized void registerListener(OnPomodoroTickListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            listener.onTick(remainingSeconds, currentState, activeTaskLabel);
        }
    }

    public synchronized void unregisterListener(OnPomodoroTickListener listener) {
        listeners.remove(listener);
    }

    public boolean isWorkSessionActive() {
        return isRunning && currentState == STATE_WORK;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getCurrentState() {
        return currentState;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public int getCompletedCycles() {
        return completedCycles;
    }

    public String getActiveTaskLabel() {
        return activeTaskLabel;
    }

    public String getActiveTaskId() {
        return activeTaskId;
    }

    public void startWorkSession(String taskId, String taskLabel, int minutes) {
        handler.removeCallbacks(tickRunnable);
        this.activeTaskId = taskId;
        this.activeTaskLabel = taskLabel != null && !taskLabel.isEmpty() ? taskLabel : "Deep Focus Sprint";
        this.currentState = STATE_WORK;
        this.totalDurationSeconds = minutes * 60L;
        this.remainingSeconds = totalDurationSeconds;
        this.isRunning = true;
        notifyStateChanged();
        handler.post(tickRunnable);
    }

    public void startBreak(boolean isLongBreak, int minutes) {
        handler.removeCallbacks(tickRunnable);
        this.currentState = isLongBreak ? STATE_LONG_BREAK : STATE_SHORT_BREAK;
        this.totalDurationSeconds = minutes * 60L;
        this.remainingSeconds = totalDurationSeconds;
        this.isRunning = true;
        notifyStateChanged();
        handler.post(tickRunnable);
    }

    public void pause() {
        if (isRunning) {
            isRunning = false;
            handler.removeCallbacks(tickRunnable);
            notifyStateChanged();
        }
    }

    public void resume() {
        if (!isRunning && currentState != STATE_IDLE) {
            isRunning = true;
            handler.post(tickRunnable);
            notifyStateChanged();
        }
    }

    public void reset() {
        isRunning = false;
        handler.removeCallbacks(tickRunnable);
        currentState = STATE_IDLE;
        remainingSeconds = 25 * 60;
        notifyStateChanged();
        notifyTick();
    }

    private void onSessionFinished() {
        isRunning = false;
        if (currentState == STATE_WORK) {
            completedCycles++;
            // After 4 work sprints, take a long break (15 mins), otherwise short break (5 mins)
            boolean isLongBreak = (completedCycles % 4 == 0);
            int breakMins = isLongBreak ? 15 : 5;
            startBreak(isLongBreak, breakMins);
        } else {
            // Break finished, ready for next work session
            currentState = STATE_IDLE;
            remainingSeconds = 25 * 60;
            notifyStateChanged();
            notifyTick();
        }
    }

    private void notifyTick() {
        for (OnPomodoroTickListener l : new ArrayList<>(listeners)) {
            l.onTick(remainingSeconds, currentState, activeTaskLabel);
        }
    }

    private void notifyStateChanged() {
        for (OnPomodoroTickListener l : new ArrayList<>(listeners)) {
            l.onStateChanged(currentState, completedCycles);
        }
    }
}
