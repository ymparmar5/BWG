package com.example.focus.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.Window;

import androidx.core.view.WindowInsetsControllerCompat;

import com.example.focus.R;

public class ThemeHelper {

    public static void applyTheme(Activity activity, boolean isDarkMode) {
        activity.setTheme(isDarkMode ? R.style.Theme_FocusLauncher : R.style.Theme_FocusLauncher_Light);

        Window window = activity.getWindow();
        if (window != null) {
            int bg = isDarkMode ? Color.BLACK : Color.WHITE;
            window.getDecorView().setBackgroundColor(bg);

            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
            controller.setAppearanceLightStatusBars(!isDarkMode);
            controller.setAppearanceLightNavigationBars(!isDarkMode);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setStatusBarColor(bg);
                window.setNavigationBarColor(bg);
            }
        }
    }
}

