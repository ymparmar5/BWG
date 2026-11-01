package com.example.focus.engine;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

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
    private long sessionEndTime = 0;
    private int completedCycles = 0;

    private String activeTaskId = null;
    private String activeTaskLabel = "Deep Focus Sprint";

    private final List<OnPomodoroTickListener> listeners = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;

            syncRemainingSecondsFromTimestamp();

            if (remainingSeconds > 0) {
                notifyTick();

                // Battery optimization: Only poll every 1000ms if a UI listener is actively registered.
                // If the user navigates away or turns off screen, avoid waking the CPU every second!
                if (!listeners.isEmpty()) {
                    handler.postDelayed(this, 1000);
                } else {
                    long msLeft = sessionEndTime - SystemClock.elapsedRealtime();
                    if (msLeft > 0) {
                        handler.postDelayed(this, msLeft);
                    } else {
                        onSessionFinished();
                    }
                }
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
            syncRemainingSecondsFromTimestamp();
            listener.onTick(remainingSeconds, currentState, activeTaskLabel);

            // If the timer is running and this is the first listener, resume 1s visual ticks
            if (isRunning && listeners.size() == 1) {
                handler.removeCallbacks(tickRunnable);
                handler.post(tickRunnable);
            }
        }
    }

    public synchronized void unregisterListener(OnPomodoroTickListener listener) {
        listeners.remove(listener);
    }

    private void syncRemainingSecondsFromTimestamp() {
        if (isRunning && sessionEndTime > 0) {
            long msLeft = sessionEndTime - SystemClock.elapsedRealtime();
            this.remainingSeconds = Math.max(0, msLeft / 1000L);
        }
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
        syncRemainingSecondsFromTimestamp();
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
        this.sessionEndTime = SystemClock.elapsedRealtime() + (totalDurationSeconds * 1000L);
        this.isRunning = true;
        notifyStateChanged();
        handler.post(tickRunnable);
    }

    public void startBreak(boolean isLongBreak, int minutes) {
        handler.removeCallbacks(tickRunnable);
        this.currentState = isLongBreak ? STATE_LONG_BREAK : STATE_SHORT_BREAK;
        this.totalDurationSeconds = minutes * 60L;
        this.remainingSeconds = totalDurationSeconds;
        this.sessionEndTime = SystemClock.elapsedRealtime() + (totalDurationSeconds * 1000L);
        this.isRunning = true;
        notifyStateChanged();
        handler.post(tickRunnable);
    }

    public void pause() {
        if (isRunning) {
            syncRemainingSecondsFromTimestamp();
            isRunning = false;
            handler.removeCallbacks(tickRunnable);
            notifyStateChanged();
        }
    }

    public void resume() {
        if (!isRunning && currentState != STATE_IDLE) {
            this.sessionEndTime = SystemClock.elapsedRealtime() + (remainingSeconds * 1000L);
            isRunning = true;
            handler.post(tickRunnable);
            notifyStateChanged();
        }
    }

    public void reset() {
        isRunning = false;
        sessionEndTime = 0;
        handler.removeCallbacks(tickRunnable);
        currentState = STATE_IDLE;
        remainingSeconds = 25 * 60;
        notifyStateChanged();
        notifyTick();
    }

    private void onSessionFinished() {
        isRunning = false;
        sessionEndTime = 0;
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
        for (OnPomodoroTickListener l : listeners) {
            try {
                l.onTick(remainingSeconds, currentState, activeTaskLabel);
            } catch (Exception ignored) {}
        }
    }

    private void notifyStateChanged() {
        for (OnPomodoroTickListener l : listeners) {
            try {
                l.onStateChanged(currentState, completedCycles);
            } catch (Exception ignored) {}
        }
    }
}
