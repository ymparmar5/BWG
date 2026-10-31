package com.example.focus.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.focus.MainActivity;
import com.example.focus.R;
import com.example.focus.data.FocusPreferences;
import com.example.focus.service.FocusAccessibilityService;
import com.example.focus.util.ThemeHelper;

public class BlockedInterceptActivity extends AppCompatActivity {

    public static final String EXTRA_APP_LABEL = "extra_blocked_app_label";
    public static final String EXTRA_TITLE = "extra_blocked_title";
    public static final String EXTRA_DETAIL = "extra_blocked_detail";
    public static final String EXTRA_USAGE_TODAY = "extra_blocked_usage_today";
    public static final String EXTRA_UNLOCK_TIME = "extra_blocked_unlock_time";
    public static final String EXTRA_IS_MINDFUL_ALERT = "extra_is_mindful_alert";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FocusPreferences prefs = new FocusPreferences(this);
        boolean isDarkMode = prefs.isDarkMode();
        ThemeHelper.applyTheme(this, isDarkMode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }

        setContentView(R.layout.activity_blocked_intercept);

        String appLabel = getIntent().getStringExtra(EXTRA_APP_LABEL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String detail = getIntent().getStringExtra(EXTRA_DETAIL);
        String usageToday = getIntent().getStringExtra(EXTRA_USAGE_TODAY);
        String unlockTime = getIntent().getStringExtra(EXTRA_UNLOCK_TIME);
        boolean isMindfulAlert = getIntent().getBooleanExtra(EXTRA_IS_MINDFUL_ALERT, false);

        TextView labelView = findViewById(R.id.blocked_app_label);
        TextView titleView = findViewById(R.id.blocked_title);
        TextView detailView = findViewById(R.id.blocked_detail);
        TextView usageView = findViewById(R.id.blocked_usage_today);
        TextView unlockView = findViewById(R.id.blocked_unlock_time);
        Button btnSecondary = findViewById(R.id.btn_secondary_action);
        Button btnHome = findViewById(R.id.btn_return_home);

        if (!TextUtils.isEmpty(appLabel)) labelView.setText(appLabel);
        if (!TextUtils.isEmpty(title)) titleView.setText(title);
        if (!TextUtils.isEmpty(detail)) detailView.setText(detail);

        if (!TextUtils.isEmpty(usageToday)) {
            usageView.setVisibility(View.VISIBLE);
            usageView.setText(usageToday);
        } else {
            usageView.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(unlockTime)) {
            unlockView.setVisibility(View.VISIBLE);
            unlockView.setText(unlockTime);
        } else {
            unlockView.setVisibility(View.GONE);
        }

        // Button colors based on dark/light mode
        int primaryBg = isDarkMode ? Color.WHITE : Color.BLACK;
        int primaryText = isDarkMode ? Color.BLACK : Color.WHITE;
        int secondaryBg = isDarkMode ? 0xFF222222 : 0xFFEAEAEA;
        int secondaryText = isDarkMode ? Color.WHITE : Color.BLACK;

        btnHome.setBackgroundTintList(ColorStateList.valueOf(primaryBg));
        btnHome.setTextColor(primaryText);

        btnSecondary.setBackgroundTintList(ColorStateList.valueOf(secondaryBg));
        btnSecondary.setTextColor(secondaryText);

        if (isMindfulAlert) {
            btnHome.setText("Take a Break (Return Home)");
            btnSecondary.setVisibility(View.VISIBLE);
            btnSecondary.setText("Lock Screen");
            btnSecondary.setOnClickListener(v -> {
                FocusAccessibilityService service = FocusAccessibilityService.getInstance();
                if (service != null) {
                    service.lockScreenDevice();
                }
                finish();
            });
        } else {
            btnHome.setText("Return Home");
            btnSecondary.setVisibility(View.VISIBLE);
            btnSecondary.setText("Lock Screen");
            btnSecondary.setOnClickListener(v -> {
                FocusAccessibilityService service = FocusAccessibilityService.getInstance();
                if (service != null) {
                    service.lockScreenDevice();
                }
                returnHome();
            });
        }

        btnHome.setOnClickListener(v -> returnHome());
    }

    private void returnHome() {
        Intent homeIntent = new Intent(this, MainActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        returnHome();
    }
}
