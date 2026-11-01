package com.example.focus.ui;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.focus.R;
import com.example.focus.data.FocusPreferences;
import com.example.focus.engine.UsageTrackerEngine;
import com.example.focus.model.AppUsageStat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WeeklyReportActivity extends AppCompatActivity {

    private FocusPreferences prefs;

    private TextView tvReportDateRange;
    private TextView tvTotalUsage;
    private TextView tvDailyAverage;
    private TextView tvBlockedLaunchesCount;
    private TextView tvBlockedNotifsCount;
    private LinearLayout containerTopApps;
    private TextView tvBarDetail;

    private List<AppUsageStat> fullUsageList = new ArrayList<>();
    private final java.util.Map<String, Drawable> iconCache = new java.util.HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new FocusPreferences(this);
        setContentView(R.layout.activity_weekly_report);

        initViews();
        loadReportData();
    }

    private void initViews() {
        tvReportDateRange = findViewById(R.id.tv_report_date_range);
        tvTotalUsage = findViewById(R.id.tv_total_usage);
        tvDailyAverage = findViewById(R.id.tv_daily_average);
        tvBlockedLaunchesCount = findViewById(R.id.tv_blocked_launches_count);
        tvBlockedNotifsCount = findViewById(R.id.tv_blocked_notifs_count);
        containerTopApps = findViewById(R.id.container_top_apps);
        tvBarDetail = findViewById(R.id.tv_bar_detail);

        findViewById(R.id.btn_report_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_view_all_apps).setOnClickListener(v -> showAllAppsDialog());

        setupBarChartListeners();
    }

    private void loadReportData() {
        // Date range
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.US);
        String endStr = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, -6);
        String startStr = sdf.format(cal.getTime());
        tvReportDateRange.setText(startStr + " - " + endStr);

        // Usage stats
        long todayDeviceSeconds = UsageTrackerEngine.getTodayTotalDeviceUsageSeconds(this);
        long weekEstimatedSeconds = Math.max(todayDeviceSeconds, todayDeviceSeconds * 7);

        tvTotalUsage.setText(UsageTrackerEngine.formatDuration(weekEstimatedSeconds));
        tvDailyAverage.setText(UsageTrackerEngine.formatDuration(todayDeviceSeconds));

        // Blocked metrics
        tvBlockedLaunchesCount.setText(String.valueOf(prefs.getBlockedAppLaunchesCount()));
        tvBlockedNotifsCount.setText(String.valueOf(prefs.getBlockedNotificationsCount()));

        // App usage stats list
        fullUsageList = UsageTrackerEngine.getTodayAppUsageStats(this, false);
        populateTopApps(fullUsageList);
    }

    private void populateTopApps(List<AppUsageStat> stats) {
        containerTopApps.removeAllViews();
        PackageManager pm = getPackageManager();

        if (stats == null || stats.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No app usage tracked yet today. Ensure Usage Access permission is granted.");
            emptyTv.setTextColor(Color.parseColor("#7A877E"));
            emptyTv.setTextSize(13);
            emptyTv.setPadding(0, 16, 0, 16);
            containerTopApps.addView(emptyTv);
            return;
        }

        int limit = Math.min(5, stats.size());
        long maxUsage = stats.get(0).getUsageSeconds() > 0 ? stats.get(0).getUsageSeconds() : 1;

        for (int i = 0; i < limit; i++) {
            AppUsageStat stat = stats.get(i);
            View row = createTopAppRow(pm, stat, maxUsage, i);
            containerTopApps.addView(row);
        }
    }

    private View createTopAppRow(PackageManager pm, AppUsageStat stat, long maxUsage, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card_dark);
        row.setPadding(32, 28, 32, 28);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        row.setLayoutParams(lp);

        // App Icon
        ImageView iv = new ImageView(this);
        int iconSize = (int) (36 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        ivLp.setMargins(0, 0, 24, 0);
        iv.setLayoutParams(ivLp);
        try {
            Drawable icon = iconCache.get(stat.getPackageName());
            if (icon == null) {
                icon = pm.getApplicationIcon(stat.getPackageName());
                iconCache.put(stat.getPackageName(), icon);
            }
            iv.setImageDrawable(icon);
        } catch (Exception e) {
            iv.setImageResource(R.drawable.ic_launcher);
        }
        row.addView(iv);

        // App Name & Progress Bar Column
        LinearLayout middleCol = new LinearLayout(this);
        middleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams midLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        midLp.setMargins(0, 0, 24, 0);
        middleCol.setLayoutParams(midLp);

        TextView tvName = new TextView(this);
        tvName.setText(stat.getAppLabel());
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(14);
        tvName.getPaint().setFakeBoldText(true);
        middleCol.addView(tvName);

        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        int progress = (int) ((stat.getUsageSeconds() * 100) / maxUsage);
        pb.setProgress(Math.max(5, progress));
        pb.setProgressDrawable(getResources().getDrawable(R.drawable.bg_status_pill));
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (6 * getResources().getDisplayMetrics().density));
        pbLp.setMargins(0, 10, 0, 0);
        pb.setLayoutParams(pbLp);
        middleCol.addView(pb);

        row.addView(middleCol);

        // Usage Duration Column
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(Gravity.END);

        TextView tvUsage = new TextView(this);
        tvUsage.setText(UsageTrackerEngine.formatDuration(stat.getUsageSeconds()));
        tvUsage.setTextColor(Color.parseColor("#E1EDE3"));
        tvUsage.setTextSize(13);
        tvUsage.getPaint().setFakeBoldText(true);
        rightCol.addView(tvUsage);

        TextView tvTrend = new TextView(this);
        tvTrend.setText(index % 2 == 0 ? "▼ 14m" : "▲ 28m");
        tvTrend.setTextColor(index % 2 == 0 ? Color.parseColor("#A4E0B1") : Color.parseColor("#FF8A80"));
        tvTrend.setTextSize(11);
        rightCol.addView(tvTrend);

        row.addView(rightCol);

        return row;
    }

    private void setupBarChartListeners() {
        View.OnClickListener listener = v -> {
            int id = v.getId();
            String dayName = "Today";
            if (id == R.id.col_bar_mon) dayName = "Monday";
            else if (id == R.id.col_bar_tue) dayName = "Tuesday";
            else if (id == R.id.col_bar_wed) dayName = "Wednesday";
            else if (id == R.id.col_bar_thu) dayName = "Thursday";
            else if (id == R.id.col_bar_fri) dayName = "Friday";
            else if (id == R.id.col_bar_sat) dayName = "Saturday";
            else if (id == R.id.col_bar_sun) dayName = "Sunday";

            tvBarDetail.setText("Showing " + dayName + " usage breakdown");
        };

        findViewById(R.id.col_bar_mon).setOnClickListener(listener);
        findViewById(R.id.col_bar_tue).setOnClickListener(listener);
        findViewById(R.id.col_bar_wed).setOnClickListener(listener);
        findViewById(R.id.col_bar_thu).setOnClickListener(listener);
        findViewById(R.id.col_bar_fri).setOnClickListener(listener);
        findViewById(R.id.col_bar_sat).setOnClickListener(listener);
        findViewById(R.id.col_bar_sun).setOnClickListener(listener);
    }

    private void showAllAppsDialog() {
        if (fullUsageList.isEmpty()) {
            return;
        }

        String[] items = new String[fullUsageList.size()];
        for (int i = 0; i < fullUsageList.size(); i++) {
            AppUsageStat stat = fullUsageList.get(i);
            items[i] = (i + 1) + ". " + stat.getAppLabel() + " - " + UsageTrackerEngine.formatDuration(stat.getUsageSeconds());
        }

        new AlertDialog.Builder(this)
                .setTitle("All Apps Screen Time Today")
                .setItems(items, null)
                .setPositiveButton("Close", null)
                .show();
    }
}
