package com.example.focus.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.focus.R;
import com.example.focus.data.FocusPreferences;
import com.example.focus.engine.PomodoroEngine;
import com.example.focus.model.TodoItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FocusTasksActivity extends AppCompatActivity implements PomodoroEngine.OnPomodoroTickListener {

    private FocusPreferences prefs;
    private PomodoroEngine pomodoro;

    // Pomodoro Views
    private TextView tvPomodoroMode;
    private TextView tvPomodoroCycles;
    private TextView tvPomodoroTimer;
    private TextView tvPomodoroTask;
    private TextView btnPomodoroToggle;
    private TextView btnPomodoroBreak;
    private TextView btnPomodoroReset;

    // Kaizen Views
    private TextView tvKaizenStreak;
    private TextView btnAddKaizenQuick;

    // Eisenhower Tabs
    private TextView tabAll, tabQ1, tabQ2, tabQ3, tabQ4, tabKaizen;
    private String activeTabFilter = "ALL"; // ALL, Q1, Q2, Q3, Q4, KAIZEN

    // Tasks List
    private LinearLayout containerTaskItems;
    private TextView tvEmptyTasks;
    private List<TodoItem> allTasks = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new FocusPreferences(this);
        pomodoro = PomodoroEngine.getInstance();
        setContentView(R.layout.activity_focus_tasks);

        initViews();
        setupListeners();
        loadTasks();
        updateKaizenBanner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pomodoro.registerListener(this);
        updatePomodoroViews();
        updateKaizenBanner();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pomodoro.unregisterListener(this);
    }

    private void initViews() {
        tvPomodoroMode = findViewById(R.id.tv_pomodoro_mode);
        tvPomodoroCycles = findViewById(R.id.tv_pomodoro_cycles);
        tvPomodoroTimer = findViewById(R.id.tv_pomodoro_timer);
        tvPomodoroTask = findViewById(R.id.tv_pomodoro_task);
        btnPomodoroToggle = findViewById(R.id.btn_pomodoro_toggle);
        btnPomodoroBreak = findViewById(R.id.btn_pomodoro_break);
        btnPomodoroReset = findViewById(R.id.btn_pomodoro_reset);

        tvKaizenStreak = findViewById(R.id.tv_kaizen_streak);
        btnAddKaizenQuick = findViewById(R.id.btn_add_kaizen_quick);

        tabAll = findViewById(R.id.tab_all);
        tabQ1 = findViewById(R.id.tab_q1);
        tabQ2 = findViewById(R.id.tab_q2);
        tabQ3 = findViewById(R.id.tab_q3);
        tabQ4 = findViewById(R.id.tab_q4);
        tabKaizen = findViewById(R.id.tab_kaizen);

        containerTaskItems = findViewById(R.id.container_task_items);
        tvEmptyTasks = findViewById(R.id.tv_empty_tasks);

        findViewById(R.id.btn_tasks_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add_task).setOnClickListener(v -> showAddTaskDialog(false));
    }

    private void setupListeners() {
        btnPomodoroToggle.setOnClickListener(v -> {
            if (pomodoro.isRunning()) {
                pomodoro.pause();
            } else {
                if (pomodoro.getCurrentState() == PomodoroEngine.STATE_IDLE) {
                    pomodoro.startWorkSession(null, "Deep Focus Sprint", 25);
                } else {
                    pomodoro.resume();
                }
            }
            updatePomodoroViews();
        });

        btnPomodoroBreak.setOnClickListener(v -> {
            pomodoro.startBreak(false, 5);
            updatePomodoroViews();
            Toast.makeText(this, "5-Minute Break Started", Toast.LENGTH_SHORT).show();
        });

        btnPomodoroReset.setOnClickListener(v -> {
            pomodoro.reset();
            updatePomodoroViews();
        });

        btnAddKaizenQuick.setOnClickListener(v -> showAddTaskDialog(true));

        tabAll.setOnClickListener(v -> switchTab("ALL", tabAll));
        tabQ1.setOnClickListener(v -> switchTab("Q1", tabQ1));
        tabQ2.setOnClickListener(v -> switchTab("Q2", tabQ2));
        tabQ3.setOnClickListener(v -> switchTab("Q3", tabQ3));
        tabQ4.setOnClickListener(v -> switchTab("Q4", tabQ4));
        tabKaizen.setOnClickListener(v -> switchTab("KAIZEN", tabKaizen));
    }

    private void switchTab(String filter, TextView selectedTab) {
        activeTabFilter = filter;
        resetTabStyles();
        selectedTab.setTextColor(Color.parseColor("#A4E0B1"));
        renderTasks();
    }

    private void resetTabStyles() {
        int unselectedColor = Color.parseColor("#CAD5CD");
        tabAll.setTextColor(unselectedColor);
        tabQ1.setTextColor(unselectedColor);
        tabQ2.setTextColor(unselectedColor);
        tabQ3.setTextColor(unselectedColor);
        tabQ4.setTextColor(unselectedColor);
        tabKaizen.setTextColor(unselectedColor);
    }

    private void updateKaizenBanner() {
        int streak = prefs.getKaizenStreak();
        boolean todayRecorded = prefs.isKaizenRecordedToday();
        if (streak == 0) {
            tvKaizenStreak.setText("🌱 0-Day Kaizen Streak (Start today!)");
        } else {
            tvKaizenStreak.setText("🔥 " + streak + "-Day Kaizen Streak" + (todayRecorded ? " (Completed today ✓)" : ""));
        }
    }

    private void updatePomodoroViews() {
        int state = pomodoro.getCurrentState();
        long secs = pomodoro.getRemainingSeconds();
        long m = secs / 60;
        long s = secs % 60;
        tvPomodoroTimer.setText(String.format(Locale.US, "%02d:%02d", m, s));

        if (state == PomodoroEngine.STATE_WORK) {
            tvPomodoroMode.setText("🍅 Work Focus Sprint");
            tvPomodoroMode.setTextColor(Color.parseColor("#A4E0B1"));
        } else if (state == PomodoroEngine.STATE_SHORT_BREAK) {
            tvPomodoroMode.setText("☕ Short Break");
            tvPomodoroMode.setTextColor(Color.parseColor("#80D8FF"));
        } else if (state == PomodoroEngine.STATE_LONG_BREAK) {
            tvPomodoroMode.setText("🌴 Long Break");
            tvPomodoroMode.setTextColor(Color.parseColor("#B388FF"));
        } else {
            tvPomodoroMode.setText("🍅 Ready for Sprint (25m)");
            tvPomodoroMode.setTextColor(Color.parseColor("#CAD5CD"));
        }

        btnPomodoroToggle.setText(pomodoro.isRunning() ? "⏸ Pause" : "▶ Start");
        tvPomodoroCycles.setText("Cycles: " + pomodoro.getCompletedCycles());
        tvPomodoroTask.setText("Focusing on: " + pomodoro.getActiveTaskLabel());
    }

    @Override
    public void onTick(long remainingSeconds, int state, String taskLabel) {
        long m = remainingSeconds / 60;
        long s = remainingSeconds % 60;
        tvPomodoroTimer.setText(String.format(Locale.US, "%02d:%02d", m, s));
    }

    @Override
    public void onStateChanged(int newState, int completedCycles) {
        updatePomodoroViews();
    }

    private void loadTasks() {
        allTasks = prefs.getTodoList();
        renderTasks();
    }

    private void renderTasks() {
        containerTaskItems.removeAllViews();

        List<TodoItem> filtered = new ArrayList<>();
        for (TodoItem item : allTasks) {
            if ("ALL".equals(activeTabFilter)) {
                filtered.add(item);
            } else if ("KAIZEN".equals(activeTabFilter) && item.isKaizen()) {
                filtered.add(item);
            } else if (matchesQuadrantFilter(item, activeTabFilter)) {
                filtered.add(item);
            }
        }

        if (filtered.isEmpty()) {
            tvEmptyTasks.setVisibility(View.VISIBLE);
        } else {
            tvEmptyTasks.setVisibility(View.GONE);
            for (TodoItem item : filtered) {
                View taskRow = createTaskRow(item);
                containerTaskItems.addView(taskRow);
            }
        }
    }

    private View createTaskRow(TodoItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card_dark);
        row.setPadding(32, 24, 32, 24);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 14);
        row.setLayoutParams(lp);

        // Checkbox
        CheckBox cb = new CheckBox(this);
        cb.setChecked(item.isCompleted());
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#A4E0B1")));
        cb.setOnCheckedChangeListener((btn, isChecked) -> {
            item.setCompleted(isChecked);
            if (isChecked && item.isKaizen()) {
                prefs.recordKaizenWinToday();
                updateKaizenBanner();
                Toast.makeText(this, "🔥 Kaizen Win recorded! Streak kept alive!", Toast.LENGTH_SHORT).show();
            }
            prefs.saveTodoList(allTasks);
            renderTasks();
        });
        row.addView(cb);

        // Task content column
        LinearLayout contentCol = new LinearLayout(this);
        contentCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        contentLp.setMargins(16, 0, 16, 0);
        contentCol.setLayoutParams(contentLp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(item.getTitle());
        tvTitle.setTextColor(item.isCompleted() ? Color.parseColor("#6C7A70") : Color.WHITE);
        tvTitle.setTextSize(15);
        if (item.isCompleted()) {
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        contentCol.addView(tvTitle);

        // Badges row: Eisenhower Badge, Kaizen leaf, Pomodoro info
        LinearLayout badgesRow = new LinearLayout(this);
        badgesRow.setOrientation(LinearLayout.HORIZONTAL);
        badgesRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams badgesLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        badgesLp.setMargins(0, 8, 0, 0);
        badgesRow.setLayoutParams(badgesLp);

        // Eisenhower badge
        TextView tvQuadrant = new TextView(this);
        tvQuadrant.setText(getQuadrantDisplayName(item.getEisenhowerQuadrant()));
        tvQuadrant.setTextColor(getQuadrantColor(item.getEisenhowerQuadrant()));
        tvQuadrant.setTextSize(11);
        tvQuadrant.setBackgroundResource(R.drawable.bg_status_pill);
        tvQuadrant.setPadding(16, 4, 16, 4);
        badgesRow.addView(tvQuadrant);

        // Kaizen badge
        if (item.isKaizen()) {
            TextView tvKaizen = new TextView(this);
            tvKaizen.setText("🌱 Kaizen 2m");
            tvKaizen.setTextColor(Color.parseColor("#FFA726"));
            tvKaizen.setTextSize(11);
            tvKaizen.setBackgroundResource(R.drawable.bg_status_pill);
            tvKaizen.setPadding(16, 4, 16, 4);
            LinearLayout.LayoutParams kLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            kLp.setMargins(12, 0, 0, 0);
            tvKaizen.setLayoutParams(kLp);
            badgesRow.addView(tvKaizen);
        }

        contentCol.addView(badgesRow);
        row.addView(contentCol);

        // Pomodoro Start Button
        TextView btnStartPomo = new TextView(this);
        btnStartPomo.setText("🍅 Focus");
        btnStartPomo.setTextColor(Color.parseColor("#A4E0B1"));
        btnStartPomo.setTextSize(12);
        btnStartPomo.setBackgroundResource(R.drawable.bg_status_pill);
        btnStartPomo.setPadding(20, 10, 20, 10);
        btnStartPomo.setOnClickListener(v -> {
            pomodoro.startWorkSession(item.getId(), item.getTitle(), 25);
            updatePomodoroViews();
            Toast.makeText(this, "Pomodoro sprint started for: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        });
        row.addView(btnStartPomo);

        // Delete Button
        ImageView btnDelete = new ImageView(this);
        btnDelete.setImageResource(R.drawable.ic_close);
        btnDelete.setColorFilter(Color.parseColor("#6C7A70"));
        int delSize = (int) (28 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(delSize, delSize);
        delLp.setMargins(12, 0, 0, 0);
        btnDelete.setLayoutParams(delLp);
        btnDelete.setPadding(6, 6, 6, 6);
        btnDelete.setOnClickListener(v -> {
            allTasks.remove(item);
            prefs.saveTodoList(allTasks);
            renderTasks();
        });
        row.addView(btnDelete);

        return row;
    }

    private boolean matchesQuadrantFilter(TodoItem item, String filter) {
        if ("Q1".equalsIgnoreCase(filter)) return item.getEisenhowerQuadrant() == TodoItem.QUADRANT_Q1_DO;
        if ("Q2".equalsIgnoreCase(filter)) return item.getEisenhowerQuadrant() == TodoItem.QUADRANT_Q2_SCHEDULE;
        if ("Q3".equalsIgnoreCase(filter)) return item.getEisenhowerQuadrant() == TodoItem.QUADRANT_Q3_DELEGATE;
        if ("Q4".equalsIgnoreCase(filter)) return item.getEisenhowerQuadrant() == TodoItem.QUADRANT_Q4_ELIMINATE;
        return true;
    }

    private String getQuadrantDisplayName(int q) {
        if (q == TodoItem.QUADRANT_Q1_DO) return "⚡ Q1: Do First";
        if (q == TodoItem.QUADRANT_Q2_SCHEDULE) return "💎 Q2: Deep Focus";
        if (q == TodoItem.QUADRANT_Q3_DELEGATE) return "⏳ Q3: Quick";
        if (q == TodoItem.QUADRANT_Q4_ELIMINATE) return "🚫 Q4: Eliminate";
        return "Q2: Focus";
    }

    private int getQuadrantColor(int q) {
        if (q == TodoItem.QUADRANT_Q1_DO) return Color.parseColor("#EF5350"); // Red
        if (q == TodoItem.QUADRANT_Q2_SCHEDULE) return Color.parseColor("#A4E0B1"); // Green
        if (q == TodoItem.QUADRANT_Q3_DELEGATE) return Color.parseColor("#42A5F5"); // Blue
        if (q == TodoItem.QUADRANT_Q4_ELIMINATE) return Color.parseColor("#BDBDBD"); // Gray
        return Color.parseColor("#A4E0B1");
    }

    private void showAddTaskDialog(boolean defaultKaizen) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText editTitle = new EditText(this);
        editTitle.setHint(defaultKaizen ? "E.g., Read 1 page, 5 push-ups, drink water" : "Task description");
        editTitle.setTextColor(Color.WHITE);
        editTitle.setHintTextColor(Color.parseColor("#7A877E"));
        layout.addView(editTitle);

        // Quadrant selection text
        TextView tvQPrompt = new TextView(this);
        tvQPrompt.setText("\nEisenhower Matrix Priority:");
        tvQPrompt.setTextColor(Color.parseColor("#CAD5CD"));
        tvQPrompt.setTextSize(13);
        layout.addView(tvQPrompt);

        final RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);

        String[] options = {"⚡ Q1: Urgent & Important (Do First)", "💎 Q2: Important, Not Urgent (Deep Focus)", "⏳ Q3: Urgent, Not Important (Delegate)", "🚫 Q4: Not Urgent & Not Important (Drop)"};
        for (int i = 0; i < 4; i++) {
            android.widget.RadioButton rb = new android.widget.RadioButton(this);
            rb.setText(options[i]);
            rb.setTextColor(Color.WHITE);
            rb.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#A4E0B1")));
            rb.setId(i + 100);
            if (i == 1) rb.setChecked(true); // default Q2
            rg.addView(rb);
        }
        layout.addView(rg);

        // Kaizen checkbox
        final CheckBox cbKaizen = new CheckBox(this);
        cbKaizen.setText("🌱 Kaizen 2-minute micro-habit (< 5 min friction-free step)");
        cbKaizen.setTextColor(Color.parseColor("#FFA726"));
        cbKaizen.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFA726")));
        cbKaizen.setChecked(defaultKaizen);
        LinearLayout.LayoutParams kLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        kLp.setMargins(0, 16, 0, 0);
        cbKaizen.setLayoutParams(kLp);
        layout.addView(cbKaizen);

        new AlertDialog.Builder(this)
                .setTitle(defaultKaizen ? "Add Kaizen 1% Habit" : "Add Focus Task")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = editTitle.getText().toString().trim();
                    if (!title.isEmpty()) {
                        int checkedId = rg.getCheckedRadioButtonId();
                        int selectedQ = TodoItem.QUADRANT_Q2_SCHEDULE;
                        if (checkedId == 100) selectedQ = TodoItem.QUADRANT_Q1_DO;
                        else if (checkedId == 101) selectedQ = TodoItem.QUADRANT_Q2_SCHEDULE;
                        else if (checkedId == 102) selectedQ = TodoItem.QUADRANT_Q3_DELEGATE;
                        else if (checkedId == 103) selectedQ = TodoItem.QUADRANT_Q4_ELIMINATE;

                        TodoItem newItem = new TodoItem(title);
                        newItem.setEisenhowerQuadrant(selectedQ);
                        newItem.setKaizen(cbKaizen.isChecked());
                        newItem.setPomodorosEstimated(1);

                        allTasks.add(0, newItem);
                        prefs.saveTodoList(allTasks);
                        renderTasks();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
