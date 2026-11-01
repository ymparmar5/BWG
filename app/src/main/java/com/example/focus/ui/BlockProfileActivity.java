package com.example.focus.ui;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.focus.R;
import com.example.focus.data.FocusPreferences;
import com.example.focus.model.BlockProfile;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BlockProfileActivity extends AppCompatActivity {

    public static final String EXTRA_PROFILE_ID = "extra_profile_id";

    private FocusPreferences prefs;
    private BlockProfile profile;

    private EditText editBlockName;
    private View cardActiveStatus;
    private ImageView imgStatusIcon;
    private TextView tvActiveStatusText;
    private LinearLayout containerSelectedApps;
    private ImageView btnEditApps;

    // Daily limit
    private CheckBox chkEnableDailyLimit;
    private TextView tvLimitValM, tvLimitValT, tvLimitValW, tvLimitValTh, tvLimitValF, tvLimitValS, tvLimitValSu;
    private TextView btnLimit15m, btnLimit30m, btnLimit1h, btnLimit2h;

    // Weekly schedule
    private CheckBox chkEnableSchedule;
    private TextView tvScheduleStartTime, tvScheduleEndTime;
    private TextView chipDayMon, chipDayTue, chipDayWed, chipDayThu, chipDayFri, chipDaySat, chipDaySun;

    // Quick block
    private TextView btnQuick30m, btnQuick1h, btnQuick2h, btnQuickClear;

    // Pause options & What to block
    private SwitchCompat switchStrictMode;
    private CheckBox chkBlockAppLaunch;
    private CheckBox chkBlockNotifications;
    private CheckBox chkBlockAdultSites;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new FocusPreferences(this);
        setContentView(R.layout.activity_block_profile);

        initViews();
        loadOrCreateProfile();
        bindProfileData();
        setupListeners();
    }

    private void initViews() {
        editBlockName = findViewById(R.id.edit_block_name);
        cardActiveStatus = findViewById(R.id.card_active_status);
        imgStatusIcon = findViewById(R.id.img_status_icon);
        tvActiveStatusText = findViewById(R.id.tv_active_status_text);
        containerSelectedApps = findViewById(R.id.container_selected_apps);
        btnEditApps = findViewById(R.id.btn_edit_apps);

        chkEnableDailyLimit = findViewById(R.id.chk_enable_daily_limit);
        tvLimitValM = findViewById(R.id.tv_limit_val_m);
        tvLimitValT = findViewById(R.id.tv_limit_val_t);
        tvLimitValW = findViewById(R.id.tv_limit_val_w);
        tvLimitValTh = findViewById(R.id.tv_limit_val_th);
        tvLimitValF = findViewById(R.id.tv_limit_val_f);
        tvLimitValS = findViewById(R.id.tv_limit_val_s);
        tvLimitValSu = findViewById(R.id.tv_limit_val_su);

        btnLimit15m = findViewById(R.id.btn_limit_15m);
        btnLimit30m = findViewById(R.id.btn_limit_30m);
        btnLimit1h = findViewById(R.id.btn_limit_1h);
        btnLimit2h = findViewById(R.id.btn_limit_2h);

        chkEnableSchedule = findViewById(R.id.chk_enable_schedule);
        tvScheduleStartTime = findViewById(R.id.tv_schedule_start_time);
        tvScheduleEndTime = findViewById(R.id.tv_schedule_end_time);

        chipDayMon = findViewById(R.id.chip_day_mon);
        chipDayTue = findViewById(R.id.chip_day_tue);
        chipDayWed = findViewById(R.id.chip_day_wed);
        chipDayThu = findViewById(R.id.chip_day_thu);
        chipDayFri = findViewById(R.id.chip_day_fri);
        chipDaySat = findViewById(R.id.chip_day_sat);
        chipDaySun = findViewById(R.id.chip_day_sun);

        btnQuick30m = findViewById(R.id.btn_quick_30m);
        btnQuick1h = findViewById(R.id.btn_quick_1h);
        btnQuick2h = findViewById(R.id.btn_quick_2h);
        btnQuickClear = findViewById(R.id.btn_quick_clear);

        switchStrictMode = findViewById(R.id.switch_strict_mode);
        chkBlockAppLaunch = findViewById(R.id.chk_block_app_launch);
        chkBlockNotifications = findViewById(R.id.chk_block_notifications);
        chkBlockAdultSites = findViewById(R.id.chk_block_adult_sites);

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save_profile).setOnClickListener(v -> saveAndFinish());
    }

    private void loadOrCreateProfile() {
        String profileId = getIntent().getStringExtra(EXTRA_PROFILE_ID);
        List<BlockProfile> profiles = prefs.getBlockProfiles();

        if (profileId != null) {
            for (BlockProfile p : profiles) {
                if (p.getId().equals(profileId)) {
                    profile = p;
                    break;
                }
            }
        }

        if (profile == null) {
            if (!profiles.isEmpty()) {
                profile = profiles.get(0);
            } else {
                profile = new BlockProfile("Deep Focus");
                profile.setHasSchedule(true);
                profile.setScheduleTime(9, 0, 17, 0);
                profile.setHasDailyLimit(true);
                profile.setDailyLimitMinutes(30);
                profile.setBlockAppLaunch(true);
                profile.setBlockNotifications(true);
            }
        }
    }

    private void bindProfileData() {
        editBlockName.setText(profile.getName());

        // Update selected app icons
        updateSelectedAppIcons();

        // Daily limit
        chkEnableDailyLimit.setChecked(profile.hasDailyLimit());
        updateDailyLimitDisplays(profile.getDailyLimitMinutes());

        // Schedule
        chkEnableSchedule.setChecked(profile.hasSchedule());
        tvScheduleStartTime.setText(String.format(Locale.US, "From: %02d:%02d", profile.getScheduleStartHour(), profile.getScheduleStartMinute()));
        tvScheduleEndTime.setText(String.format(Locale.US, "Until: %02d:%02d", profile.getScheduleEndHour(), profile.getScheduleEndMinute()));
        updateDayChips();

        // Strict & what to block
        switchStrictMode.setChecked(profile.isStrictMode());
        chkBlockAppLaunch.setChecked(profile.isBlockAppLaunch());
        chkBlockNotifications.setChecked(profile.isBlockNotifications());
        chkBlockAdultSites.setChecked(prefs.isAdultBlockerEnabled());

        // Update active banner
        updateStatusBanner();
    }

    private void updateStatusBanner() {
        long now = System.currentTimeMillis();
        if (profile.getTemporaryBlockUntil() > now) {
            long remMins = Math.max(1, (profile.getTemporaryBlockUntil() - now) / 60000);
            tvActiveStatusText.setText(String.format(Locale.US, "⌛ %dm left today", remMins));
            imgStatusIcon.setImageResource(R.drawable.ic_hourglass);
            cardActiveStatus.setVisibility(View.VISIBLE);
        } else if (profile.hasSchedule()) {
            tvActiveStatusText.setText(String.format(Locale.US, "🔒 Schedule is active Until %02d:%02d",
                    profile.getScheduleEndHour(), profile.getScheduleEndMinute()));
            imgStatusIcon.setImageResource(R.drawable.ic_lock);
            cardActiveStatus.setVisibility(View.VISIBLE);
        } else if (profile.hasDailyLimit()) {
            tvActiveStatusText.setText(String.format(Locale.US, "⌛ %dm limit configured", profile.getDailyLimitMinutes()));
            imgStatusIcon.setImageResource(R.drawable.ic_hourglass);
            cardActiveStatus.setVisibility(View.VISIBLE);
        } else {
            cardActiveStatus.setVisibility(View.GONE);
        }
    }

    private void updateSelectedAppIcons() {
        containerSelectedApps.removeAllViews();
        PackageManager pm = getPackageManager();
        Set<String> pkgs = profile.getTargetPackages();

        if (pkgs.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No apps selected. Tap to add apps.");
            tvEmpty.setTextColor(Color.parseColor("#7A877E"));
            tvEmpty.setTextSize(13);
            containerSelectedApps.addView(tvEmpty);
            return;
        }

        for (String pkg : pkgs) {
            try {
                Drawable icon = pm.getApplicationIcon(pkg);
                ImageView iv = new ImageView(this);
                int sizePx = (int) (36 * getResources().getDisplayMetrics().density);
                int marginPx = (int) (4 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                lp.setMargins(0, 0, marginPx, 0);
                iv.setLayoutParams(lp);
                iv.setImageDrawable(icon);
                containerSelectedApps.addView(iv);
            } catch (Exception ignored) {}
        }
    }

    private void updateDailyLimitDisplays(int minutes) {
        String text = minutes >= 60 ? (minutes / 60) + "h" : minutes + "m";
        tvLimitValM.setText(text);
        tvLimitValT.setText(text);
        tvLimitValW.setText(text);
        tvLimitValTh.setText(text);
        tvLimitValF.setText(text);
        tvLimitValS.setText(text);
        tvLimitValSu.setText(text);
    }

    private void updateDayChips() {
        Set<Integer> days = profile.getScheduleDaysOfWeek();
        setDayChipState(chipDayMon, days.contains(Calendar.MONDAY));
        setDayChipState(chipDayTue, days.contains(Calendar.TUESDAY));
        setDayChipState(chipDayWed, days.contains(Calendar.WEDNESDAY));
        setDayChipState(chipDayThu, days.contains(Calendar.THURSDAY));
        setDayChipState(chipDayFri, days.contains(Calendar.FRIDAY));
        setDayChipState(chipDaySat, days.contains(Calendar.SATURDAY));
        setDayChipState(chipDaySun, days.contains(Calendar.SUNDAY));
    }

    private void setDayChipState(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(Color.parseColor("#101412"));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(Color.parseColor("#7A877E"));
        }
    }

    private void toggleScheduleDay(int calendarDay, TextView chip) {
        Set<Integer> days = profile.getScheduleDaysOfWeek();
        if (days.contains(calendarDay)) {
            days.remove(calendarDay);
        } else {
            days.add(calendarDay);
        }
        setDayChipState(chip, days.contains(calendarDay));
    }

    private void setupListeners() {
        editBlockName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                profile.setName(s.toString().trim());
            }
        });

        btnEditApps.setOnClickListener(v -> showAppSelectionDialog());
        containerSelectedApps.setOnClickListener(v -> showAppSelectionDialog());

        // Daily limit
        chkEnableDailyLimit.setOnCheckedChangeListener((btn, isChecked) -> profile.setHasDailyLimit(isChecked));
        btnLimit15m.setOnClickListener(v -> setDailyLimit(15));
        btnLimit30m.setOnClickListener(v -> setDailyLimit(30));
        btnLimit1h.setOnClickListener(v -> setDailyLimit(60));
        btnLimit2h.setOnClickListener(v -> setDailyLimit(120));

        // Weekly schedule
        chkEnableSchedule.setOnCheckedChangeListener((btn, isChecked) -> {
            profile.setHasSchedule(isChecked);
            updateStatusBanner();
        });

        tvScheduleStartTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                profile.setScheduleTime(hourOfDay, minute, profile.getScheduleEndHour(), profile.getScheduleEndMinute());
                tvScheduleStartTime.setText(String.format(Locale.US, "From: %02d:%02d", hourOfDay, minute));
                updateStatusBanner();
            }, profile.getScheduleStartHour(), profile.getScheduleStartMinute(), true).show();
        });

        tvScheduleEndTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                profile.setScheduleTime(profile.getScheduleStartHour(), profile.getScheduleStartMinute(), hourOfDay, minute);
                tvScheduleEndTime.setText(String.format(Locale.US, "Until: %02d:%02d", hourOfDay, minute));
                updateStatusBanner();
            }, profile.getScheduleEndHour(), profile.getScheduleEndMinute(), true).show();
        });

        chipDayMon.setOnClickListener(v -> toggleScheduleDay(Calendar.MONDAY, chipDayMon));
        chipDayTue.setOnClickListener(v -> toggleScheduleDay(Calendar.TUESDAY, chipDayTue));
        chipDayWed.setOnClickListener(v -> toggleScheduleDay(Calendar.WEDNESDAY, chipDayWed));
        chipDayThu.setOnClickListener(v -> toggleScheduleDay(Calendar.THURSDAY, chipDayThu));
        chipDayFri.setOnClickListener(v -> toggleScheduleDay(Calendar.FRIDAY, chipDayFri));
        chipDaySat.setOnClickListener(v -> toggleScheduleDay(Calendar.SATURDAY, chipDaySat));
        chipDaySun.setOnClickListener(v -> toggleScheduleDay(Calendar.SUNDAY, chipDaySun));

        // Quick Period Block
        btnQuick30m.setOnClickListener(v -> {
            profile.setTemporaryBlockDurationMinutes(30);
            updateStatusBanner();
            Toast.makeText(this, "Quick block enabled for 30 minutes", Toast.LENGTH_SHORT).show();
        });
        btnQuick1h.setOnClickListener(v -> {
            profile.setTemporaryBlockDurationMinutes(60);
            updateStatusBanner();
            Toast.makeText(this, "Quick block enabled for 1 hour", Toast.LENGTH_SHORT).show();
        });
        btnQuick2h.setOnClickListener(v -> {
            profile.setTemporaryBlockDurationMinutes(120);
            updateStatusBanner();
            Toast.makeText(this, "Quick block enabled for 2 hours", Toast.LENGTH_SHORT).show();
        });
        btnQuickClear.setOnClickListener(v -> {
            profile.clearTemporaryBlock();
            updateStatusBanner();
            Toast.makeText(this, "Quick block cleared", Toast.LENGTH_SHORT).show();
        });

        // Pause options & What to block
        switchStrictMode.setOnCheckedChangeListener((btn, isChecked) -> profile.setStrictMode(isChecked));
        chkBlockAppLaunch.setOnCheckedChangeListener((btn, isChecked) -> profile.setBlockAppLaunch(isChecked));
        chkBlockNotifications.setOnCheckedChangeListener((btn, isChecked) -> profile.setBlockNotifications(isChecked));
        chkBlockAdultSites.setOnCheckedChangeListener((btn, isChecked) -> prefs.setAdultBlockerEnabled(isChecked));
    }

    private void setDailyLimit(int minutes) {
        profile.setDailyLimitMinutes(minutes);
        profile.setHasDailyLimit(true);
        chkEnableDailyLimit.setChecked(true);
        updateDailyLimitDisplays(minutes);
        updateStatusBanner();
    }

    private void showAppSelectionDialog() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<AppSelectionItem> appItems = new ArrayList<>();
        Set<String> selectedPackages = new HashSet<>(profile.getTargetPackages());

        for (ApplicationInfo info : apps) {
            if (pm.getLaunchIntentForPackage(info.packageName) != null) {
                if (!info.packageName.equals(getPackageName())) {
                    String label = pm.getApplicationLabel(info).toString();
                    appItems.add(new AppSelectionItem(label, info.packageName, selectedPackages.contains(info.packageName)));
                }
            }
        }
        Collections.sort(appItems, (a, b) -> a.label.compareToIgnoreCase(b.label));

        String[] appLabels = new String[appItems.size()];
        boolean[] checkedItems = new boolean[appItems.size()];
        for (int i = 0; i < appItems.size(); i++) {
            appLabels[i] = appItems.get(i).label;
            checkedItems[i] = appItems.get(i).selected;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Blocked Apps")
                .setMultiChoiceItems(appLabels, checkedItems, (dialog, which, isChecked) -> {
                    appItems.get(which).selected = isChecked;
                })
                .setPositiveButton("Done", (dialog, which) -> {
                    Set<String> newSelected = new HashSet<>();
                    for (AppSelectionItem item : appItems) {
                        if (item.selected) {
                            newSelected.add(item.packageName);
                        }
                    }
                    profile.setTargetPackages(newSelected);
                    updateSelectedAppIcons();
                    updateStatusBanner();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveAndFinish() {
        String name = editBlockName.getText().toString().trim();
        if (name.isEmpty()) {
            name = "Focus Block";
        }
        profile.setName(name);

        prefs.saveBlockProfile(profile);
        Toast.makeText(this, "Block profile saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private static class AppSelectionItem {
        final String label;
        final String packageName;
        boolean selected;

        AppSelectionItem(String label, String packageName, boolean selected) {
            this.label = label;
            this.packageName = packageName;
            this.selected = selected;
        }
    }
}
