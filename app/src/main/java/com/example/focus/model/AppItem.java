package com.example.focus.model;

import android.os.Process;
import android.os.UserHandle;

import androidx.annotation.NonNull;

public class AppItem implements Comparable<AppItem> {
    private final String packageName;
    private final String activityName;
    private final String originalLabel;
    private String customAlias;
    private final UserHandle userHandle;

    public AppItem(String packageName, String activityName, String originalLabel) {
        this(packageName, activityName, originalLabel, null, Process.myUserHandle());
    }

    public AppItem(String packageName, String activityName, String originalLabel, String customAlias, UserHandle userHandle) {
        this.packageName = packageName != null ? packageName : "";
        this.activityName = activityName != null ? activityName : "";
        this.originalLabel = originalLabel != null ? originalLabel : "";
        this.customAlias = customAlias;
        this.userHandle = userHandle != null ? userHandle : Process.myUserHandle();
    }

    public String getPackageName() {
        return packageName;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getOriginalLabel() {
        return originalLabel;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String alias) {
        this.customAlias = alias;
    }

    public UserHandle getUserHandle() {
        return userHandle;
    }

    public String getDisplayLabel() {
        if (customAlias != null && !customAlias.trim().isEmpty()) {
            return customAlias.trim();
        }
        return originalLabel;
    }

    @Override
    public int compareTo(@NonNull AppItem other) {
        return this.getDisplayLabel().compareToIgnoreCase(other.getDisplayLabel());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppItem)) return false;
        AppItem item = (AppItem) o;
        return packageName.equals(item.packageName) &&
                userHandle.equals(item.userHandle);
    }

    @Override
    public int hashCode() {
        return packageName.hashCode() * 31 + userHandle.hashCode();
    }
}
