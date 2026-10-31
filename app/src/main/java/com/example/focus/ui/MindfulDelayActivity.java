package com.example.focus.ui;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.focus.R;
import com.example.focus.util.AppHelper;

public class MindfulDelayActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE = "extra_target_package";
    public static final String EXTRA_LABEL = "extra_target_label";
    public static final String EXTRA_SECONDS = "extra_delay_seconds";

    private String targetPackage;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mindful_delay);

        targetPackage = getIntent().getStringExtra(EXTRA_PACKAGE);
        String targetLabel = getIntent().getStringExtra(EXTRA_LABEL);
        int delaySeconds = getIntent().getIntExtra(EXTRA_SECONDS, 10);

        TextView labelView = findViewById(R.id.mindful_target_app);
        TextView countdownView = findViewById(R.id.mindful_countdown);
        Button btnCancel = findViewById(R.id.btn_cancel_focus);
        Button btnProceed = findViewById(R.id.btn_proceed);

        labelView.setText(targetLabel != null ? targetLabel : targetPackage);
        countdownView.setText(String.valueOf(delaySeconds));

        btnCancel.setOnClickListener(v -> finish());

        countDownTimer = new CountDownTimer(delaySeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secs = (int) Math.ceil(millisUntilFinished / 1000.0);
                countdownView.setText(String.valueOf(secs));
            }

            @Override
            public void onFinish() {
                countdownView.setText("0");
                btnProceed.setEnabled(true);
                btnProceed.setTextColor(getResources().getColor(R.color.white));
                btnProceed.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.gray_border)));
            }
        }.start();

        btnProceed.setOnClickListener(v -> {
            AppHelper.launchPackage(MindfulDelayActivity.this, targetPackage);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}

