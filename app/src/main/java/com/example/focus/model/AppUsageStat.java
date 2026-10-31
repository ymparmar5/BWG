package com.example.focus.model;

public class AppUsageStat {
    private final String packageName;
    private final String appLabel;
    private final long usageSeconds;
    private final int percentage;

    public AppUsageStat(String packageName, String appLabel, long usageSeconds, int percentage) {
        this.packageName = packageName;
        this.appLabel = appLabel;
        this.usageSeconds = usageSeconds;
        this.percentage = percentage;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppLabel() {
        return appLabel;
    }

    public long getUsageSeconds() {
        return usageSeconds;
    }

    public int getPercentage() {
        return percentage;
    }
}

