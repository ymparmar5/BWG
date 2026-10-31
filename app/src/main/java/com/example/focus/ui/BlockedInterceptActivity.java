package com.example.focus.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.focus.MainActivity;
import com.example.focus.R;

public class BlockedInterceptActivity extends AppCompatActivity {

    public static final String EXTRA_APP_LABEL = "extra_blocked_app_label";
    public static final String EXTRA_TITLE = "extra_blocked_title";
    public static final String EXTRA_DETAIL = "extra_blocked_detail";
    public static final String EXTRA_UNLOCK_TIME = "extra_blocked_unlock_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_intercept);

        String appLabel = getIntent().getStringExtra(EXTRA_APP_LABEL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String detail = getIntent().getStringExtra(EXTRA_DETAIL);
        String unlockTime = getIntent().getStringExtra(EXTRA_UNLOCK_TIME);

        TextView labelView = findViewById(R.id.blocked_app_label);
        TextView titleView = findViewById(R.id.blocked_title);
        TextView detailView = findViewById(R.id.blocked_detail);
        TextView unlockView = findViewById(R.id.blocked_unlock_time);
        Button btnHome = findViewById(R.id.btn_return_home);

        if (appLabel != null) labelView.setText(appLabel);
        if (title != null) titleView.setText(title);
        if (detail != null) detailView.setText(detail);
        if (unlockTime != null) unlockView.setText(unlockTime);

        btnHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(this, MainActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Return home
        Intent homeIntent = new Intent(this, MainActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
        finish();
    }
}

