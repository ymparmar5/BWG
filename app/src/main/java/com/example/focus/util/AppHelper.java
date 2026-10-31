package com.example.focus.util;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.Toast;

import com.example.focus.data.FocusPreferences;
import com.example.focus.model.AppItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppHelper {

    public static List<AppItem> getInstalledApps(Context context, FocusPreferences prefs) {
        List<AppItem> appList = new ArrayList<>();
        String ownPackage = context.getPackageName();

        try {
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);

            if (launcherApps != null && userManager != null) {
                List<UserHandle> userProfiles = userManager.getUserProfiles();
                for (UserHandle profile : userProfiles) {
                    List<LauncherActivityInfo> activities = launcherApps.getActivityList(null, profile);
                    if (activities != null) {
                        for (LauncherActivityInfo info : activities) {
                            String pkg = info.getApplicationInfo().packageName;
                            if (!pkg.equals(ownPackage)) {
                                String label = info.getLabel().toString();
                                String activityName = info.getComponentName().getClassName();
                                String alias = prefs != null ? prefs.getAppAlias(pkg) : null;
                                appList.add(new AppItem(pkg, activityName, label, alias, profile));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to standard PackageManager
            PackageManager pm = context.getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolvedList = pm.queryIntentActivities(mainIntent, 0);

            if (resolvedList != null) {
                for (ResolveInfo info : resolvedList) {
                    String pkg = info.activityInfo.packageName;
                    if (!pkg.equals(ownPackage)) {
                        String label = info.loadLabel(pm).toString();
                        String activityName = info.activityInfo.name;
                        String alias = prefs != null ? prefs.getAppAlias(pkg) : null;
                        appList.add(new AppItem(pkg, activityName, label, alias, Process.myUserHandle()));
                    }
                }
            }
        }

        Collections.sort(appList);
        return appList;
    }

    public static void launchApp(Context context, AppItem app) {
        if (app == null) return;

        try {
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (launcherApps != null && !app.getUserHandle().equals(Process.myUserHandle())) {
                launcherApps.startMainActivity(
                        new ComponentName(app.getPackageName(), app.getActivityName()),
                        app.getUserHandle(),
                        null,
                        null
                );
                return;
            }
        } catch (Exception ignored) {}

        // Standard launch intent fallback
        launchPackage(context, app.getPackageName());
    }

    public static void launchPackage(Context context, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return;
        }
        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            try {
                context.startActivity(launchIntent);
            } catch (Exception e) {
                Toast.makeText(context, "Could not open app", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "App not found", Toast.LENGTH_SHORT).show();
        }
    }

    public static void openDialer(Context context) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Dialer app not found", Toast.LENGTH_SHORT).show();
        }
    }

    public static void openMessages(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_MESSAGING);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"));
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(fallback);
            } catch (Exception ex) {
                Toast.makeText(context, "Messaging app not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void openAppInfo(Context context, String packageName) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open app info", Toast.LENGTH_SHORT).show();
        }
    }

    public static void uninstallApp(Context context, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Unable to start uninstall", Toast.LENGTH_SHORT).show();
        }
    }
}
