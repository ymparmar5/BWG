package com.example.focus.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.focus.engine.RuleModel;
import com.example.focus.engine.ScheduleBlock;
import com.example.focus.model.NotificationDigestItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FocusPreferences {
    private static final String PREF_NAME = "focus_production_prefs";

    public static final int NOTIF_MODE_BLOCKLIST = 0;
    public static final int NOTIF_MODE_STRICT_ALLOWLIST = 1;

    // Keys
    private static final String KEY_FAVORITES = "fav_apps_list";
    private static final String KEY_HIDDEN = "hidden_apps_set";
    private static final String KEY_ALIASES = "app_aliases_json";
    private static final String KEY_MINDFUL_DELAYS = "mindful_delays_json";
    private static final String KEY_HOURLY_LIMITS = "hourly_limits_json";
    private static final String KEY_WEEKLY_LIMITS = "weekly_limits_json";
    private static final String KEY_SCHEDULE_BLOCKS = "schedule_blocks_json";

    private static final String KEY_NOTIF_FILTER_ENABLED = "notif_filter_enabled";
    private static final String KEY_NOTIF_MODE = "notif_filter_mode";
    private static final String KEY_NOTIF_BLOCKED_PKGS = "notif_blocked_pkgs";
    private static final String KEY_NOTIF_ALLOWED_PKGS = "notif_allowed_pkgs";
    private static final String KEY_NOTIF_DIGEST = "notif_digest_ring_buffer";

    private static final String KEY_BLOCKER_ENABLED = "content_blocker_enabled";
    private static final String KEY_BLOCKER_DEBUG = "content_blocker_debug";
    private static final String KEY_RULES_JSON = "blocker_rules_json";
    private static final String KEY_INSPECTOR_BUFFER = "inspector_node_buffer";

    private static final String KEY_AUTO_LAUNCH_SINGLE = "auto_launch_single_result";
    private static final String KEY_SWIPE_LEFT = "swipe_left_pkg";
    private static final String KEY_SWIPE_RIGHT = "swipe_right_pkg";

    private static final String KEY_DND_ENABLED = "dnd_automation_enabled";
    private static final String KEY_DND_START_H = "dnd_start_hour";
    private static final String KEY_DND_START_M = "dnd_start_minute";
    private static final String KEY_DND_END_H = "dnd_end_hour";
    private static final String KEY_DND_END_M = "dnd_end_minute";

    private final SharedPreferences prefs;

    public FocusPreferences(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ensureDefaultRules();
    }

    // --- FAVORITES (Max 7) ---
    public List<String> getFavorites() {
        String raw = prefs.getString(KEY_FAVORITES, "");
        if (TextUtils.isEmpty(raw)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(raw.split(",")));
    }

    public void setFavorites(List<String> list) {
        if (list == null) list = new ArrayList<>();
        if (list.size() > 7) {
            list = list.subList(0, 7);
        }
        prefs.edit().putString(KEY_FAVORITES, TextUtils.join(",", list)).apply();
    }

    public boolean addFavorite(String packageName) {
        List<String> favs = getFavorites();
        if (favs.contains(packageName)) return true;
        if (favs.size() >= 7) return false;
        favs.add(packageName);
        setFavorites(favs);
        return true;
    }

    public void removeFavorite(String packageName) {
        List<String> favs = getFavorites();
        if (favs.remove(packageName)) {
            setFavorites(favs);
        }
    }

    public boolean isFavorite(String packageName) {
        return getFavorites().contains(packageName);
    }

    // --- APP ALIASES / RENAMES ---
    public String getAppAlias(String packageName) {
        try {
            JSONObject obj = new JSONObject(prefs.getString(KEY_ALIASES, "{}"));
            return obj.optString(packageName, null);
        } catch (JSONException e) {
            return null;
        }
    }

    public void setAppAlias(String packageName, String alias) {
        try {
            JSONObject obj = new JSONObject(prefs.getString(KEY_ALIASES, "{}"));
            if (alias == null || alias.trim().isEmpty()) {
                obj.remove(packageName);
            } else {
                obj.put(packageName, alias.trim());
            }
            prefs.edit().putString(KEY_ALIASES, obj.toString()).apply();
        } catch (JSONException ignored) {}
    }

    // --- HIDDEN APPS ---
    public Set<String> getHiddenApps() {
        return new HashSet<>(prefs.getStringSet(KEY_HIDDEN, new HashSet<>()));
    }

    public void hideApp(String packageName) {
        Set<String> set = getHiddenApps();
        set.add(packageName);
        prefs.edit().putStringSet(KEY_HIDDEN, set).apply();
    }

    public void unhideApp(String packageName) {
        Set<String> set = getHiddenApps();
        set.remove(packageName);
        prefs.edit().putStringSet(KEY_HIDDEN, set).apply();
    }

    public boolean isHidden(String packageName) {
        return getHiddenApps().contains(packageName);
    }

    // --- MINDFUL DELAYS (5, 10, 15 seconds) ---
    public int getMindfulDelaySeconds(String packageName) {
        try {
            JSONObject obj = new JSONObject(prefs.getString(KEY_MINDFUL_DELAYS, "{}"));
            return obj.optInt(packageName, 0);
        } catch (JSONException e) {
            return 0;
        }
    }

    public void setMindfulDelaySeconds(String packageName, int seconds) {
        try {
            JSONObject obj = new JSONObject(prefs.getString(KEY_MINDFUL_DELAYS, "{}"));
            if (seconds <= 0) {
                obj.remove(packageName);
            } else {
                obj.put(packageName, seconds);
            }
            prefs.edit().putString(KEY_MINDFUL_DELAYS, obj.toString()).apply();
        } catch (JSONException ignored) {}
    }

    // --- HOURLY LIMITS (Minutes) ---
    public int getHourlyLimitMinutes(String packageName) {
        try {
            JSONObject obj = new JSONObject(prefs.getString(KEY_HOURLY_LIMITS, "{}"));
            return obj.optInt(packageName, 0);
        } catch (JSONException e) {
            return 0;
        }
    }

    public void setHourlyLimitMinutes(String packageName, int minutes) {
        try {
            JSONObject obj = new JSONObject(prefs.getString(KEY_HOURLY_LIMITS, "{}"));
            if (minutes <= 0) {
                obj.remove(packageName);
            } else {
                obj.put(packageName, minutes);
            }
            prefs.edit().putString(KEY_HOURLY_LIMITS, obj.toString()).apply();
        } catch (JSONException ignored) {}
    }

    // --- WEEKLY LIMITS (Minutes) ---
    public int getWeeklyLimitMinutes(String packageName) {
        try {
            JSONObject obj = new JSONObject(prefs.getString(KEY_WEEKLY_LIMITS, "{}"));
            return obj.optInt(packageName, 0);
        } catch (JSONException e) {
            return 0;
        }
    }

    public void setWeeklyLimitMinutes(String packageName, int minutes) {
        try {
            JSONObject obj = new JSONObject(prefs.getString(KEY_WEEKLY_LIMITS, "{}"));
            if (minutes <= 0) {
                obj.remove(packageName);
            } else {
                obj.put(packageName, minutes);
            }
            prefs.edit().putString(KEY_WEEKLY_LIMITS, obj.toString()).apply();
        } catch (JSONException ignored) {}
    }

    // --- SCHEDULE BLOCKS ---
    public ScheduleBlock getScheduleBlock(String packageName) {
        try {
            JSONObject obj = new JSONObject(prefs.getString(KEY_SCHEDULE_BLOCKS, "{}"));
            JSONObject blockObj = obj.optJSONObject(packageName);
            return blockObj != null ? ScheduleBlock.fromJsonObject(blockObj) : null;
        } catch (JSONException e) {
            return null;
        }
    }

    public void setScheduleBlock(String packageName, ScheduleBlock block) {
        try {
            JSONObject obj = new JSONObject(prefs.getString(KEY_SCHEDULE_BLOCKS, "{}"));
            if (block == null) {
                obj.remove(packageName);
            } else {
                obj.put(packageName, block.toJsonObject());
            }
            prefs.edit().putString(KEY_SCHEDULE_BLOCKS, obj.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public Set<String> getAllMonitoredPackages() {
        Set<String> set = new HashSet<>();
        try {
            JSONObject hObj = new JSONObject(prefs.getString(KEY_HOURLY_LIMITS, "{}"));
            Iterator<String> hKeys = hObj.keys();
            while (hKeys.hasNext()) set.add(hKeys.next());

            JSONObject wObj = new JSONObject(prefs.getString(KEY_WEEKLY_LIMITS, "{}"));
            Iterator<String> wKeys = wObj.keys();
            while (wKeys.hasNext()) set.add(wKeys.next());

            JSONObject sObj = new JSONObject(prefs.getString(KEY_SCHEDULE_BLOCKS, "{}"));
            Iterator<String> sKeys = sObj.keys();
            while (sKeys.hasNext()) set.add(sKeys.next());
        } catch (JSONException ignored) {}
        return set;
    }

    // --- NOTIFICATION FILTER ---
    public boolean isNotificationFilterEnabled() {
        return prefs.getBoolean(KEY_NOTIF_FILTER_ENABLED, true);
    }

    public void setNotificationFilterEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_FILTER_ENABLED, enabled).apply();
    }

    public int getNotificationFilterMode() {
        return prefs.getInt(KEY_NOTIF_MODE, NOTIF_MODE_BLOCKLIST);
    }

    public void setNotificationFilterMode(int mode) {
        prefs.edit().putInt(KEY_NOTIF_MODE, mode).apply();
    }

    public Set<String> getBlockedNotificationPackages() {
        return new HashSet<>(prefs.getStringSet(KEY_NOTIF_BLOCKED_PKGS, new HashSet<>()));
    }

    public void setBlockedNotificationPackages(Set<String> pkgs) {
        prefs.edit().putStringSet(KEY_NOTIF_BLOCKED_PKGS, pkgs).apply();
    }

    public void blockNotification(String packageName) {
        Set<String> set = getBlockedNotificationPackages();
        set.add(packageName);
        setBlockedNotificationPackages(set);
    }

    public void unblockNotification(String packageName) {
        Set<String> set = getBlockedNotificationPackages();
        set.remove(packageName);
        setBlockedNotificationPackages(set);
    }

    public Set<String> getAllowedNotificationPackages() {
        return new HashSet<>(prefs.getStringSet(KEY_NOTIF_ALLOWED_PKGS, new HashSet<>()));
    }

    public void setAllowedNotificationPackages(Set<String> pkgs) {
        prefs.edit().putStringSet(KEY_NOTIF_ALLOWED_PKGS, pkgs).apply();
    }

    // --- SAFETY DIGEST (Ring Buffer, Max 50) ---
    public synchronized List<NotificationDigestItem> getDigestItems() {
        List<NotificationDigestItem> list = new ArrayList<>();
        String raw = prefs.getString(KEY_NOTIF_DIGEST, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                list.add(NotificationDigestItem.fromJsonObject(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {}
        return list;
    }

    public synchronized void addDigestItem(NotificationDigestItem item) {
        List<NotificationDigestItem> list = getDigestItems();
        list.add(0, item);
        while (list.size() > 50) {
            list.remove(list.size() - 1);
        }
        JSONArray arr = new JSONArray();
        for (NotificationDigestItem it : list) {
            try {
                arr.put(it.toJsonObject());
            } catch (JSONException ignored) {}
        }
        prefs.edit().putString(KEY_NOTIF_DIGEST, arr.toString()).apply();
    }

    public void clearDigest() {
        prefs.edit().putString(KEY_NOTIF_DIGEST, "[]").apply();
    }

    // --- CONTENT BLOCKER & JSON RULES ---
    public boolean isBlockerEnabled() {
        return prefs.getBoolean(KEY_BLOCKER_ENABLED, true);
    }

    public void setBlockerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BLOCKER_ENABLED, enabled).apply();
    }

    public boolean isBlockerDebugMode() {
        return prefs.getBoolean(KEY_BLOCKER_DEBUG, false);
    }

    public void setBlockerDebugMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_BLOCKER_DEBUG, enabled).apply();
    }

    public String getRulesJson() {
        return prefs.getString(KEY_RULES_JSON, "");
    }

    public void setRulesJson(String json) {
        prefs.edit().putString(KEY_RULES_JSON, json).apply();
    }

    public Map<String, RuleModel> getRulesMap() {
        Map<String, RuleModel> map = new HashMap<>();
        try {
            JSONArray arr = new JSONArray(getRulesJson());
            for (int i = 0; i < arr.length(); i++) {
                RuleModel model = RuleModel.fromJsonObject(arr.getJSONObject(i));
                map.put(model.getPackageName(), model);
            }
        } catch (JSONException e) {
            return getDefaultRulesMap();
        }
        return map;
    }

    public void resetRulesToDefault() {
        JSONArray arr = new JSONArray();
        for (RuleModel m : getDefaultRulesMap().values()) {
            try {
                arr.put(m.toJsonObject());
            } catch (JSONException ignored) {}
        }
        setRulesJson(arr.toString());
    }

    private void ensureDefaultRules() {
        if (TextUtils.isEmpty(getRulesJson())) {
            resetRulesToDefault();
        }
    }

    public static Map<String, RuleModel> getDefaultRulesMap() {
        Map<String, RuleModel> map = new HashMap<>();

        // 1. YouTube Shorts Rules
        Set<String> ytBlockedIds = new HashSet<>(Arrays.asList(
                "reel_player_page_view", "reel_recycler", "shorts_container",
                "reel_player_fragment_container", "reel_watch_fragment_container",
                "reel_player_view", "shorts_video_player"
        ));
        Set<String> ytBlockedDescs = new HashSet<>(Arrays.asList("Shorts", "Shorts player"));
        Set<String> ytTabKeywords = new HashSet<>(Arrays.asList("Shorts"));
        Set<String> ytWhitelist = new HashSet<>(Arrays.asList(
                "watch_player", "watch_while_layout", "player_fragment_container",
                "player_container", "movie_player", "video_title"
        ));
        map.put("com.google.android.youtube", new RuleModel("com.google.android.youtube",
                ytBlockedIds, ytBlockedDescs, ytTabKeywords, ytWhitelist));

        // 2. Instagram Reels Rules
        Set<String> igBlockedIds = new HashSet<>(Arrays.asList(
                "clips_video_container", "clips_viewer_view_pager", "reel_viewer_root",
                "clips_swipe_refresh_layout", "clips_viewer_root", "clips_viewer_fragment",
                "reel_viewer_image_view"
        ));
        Set<String> igBlockedDescs = new HashSet<>(Arrays.asList("Reels", "Reel viewer"));
        Set<String> igTabKeywords = new HashSet<>(Arrays.asList("Reels", "Reel"));
        Set<String> igWhitelist = new HashSet<>(Arrays.asList(
                "feed_tab", "direct_inbox", "thread_container", "action_bar_title"
        ));
        map.put("com.instagram.android", new RuleModel("com.instagram.android",
                igBlockedIds, igBlockedDescs, igTabKeywords, igWhitelist));

        // 3. WhatsApp Status / Updates Rules
        Set<String> waBlockedIds = new HashSet<>(Arrays.asList(
                "status_playback_fragment_container", "status_view_pager", "pager_holder",
                "status_playback_container", "status_playback_fragment", "status_page_container"
        ));
        Set<String> waBlockedDescs = new HashSet<>(Arrays.asList("Status playback", "Viewing status", "Status tab"));
        Set<String> waTabKeywords = new HashSet<>(Arrays.asList("Status", "Updates"));
        Set<String> waWhitelist = new HashSet<>(Arrays.asList(
                "conversation_layout", "chat_list", "entry", "send", "chat_title"
        ));
        map.put("com.whatsapp", new RuleModel("com.whatsapp",
                waBlockedIds, waBlockedDescs, waTabKeywords, waWhitelist));

        return map;
    }

    // --- LIVE INSPECTOR NODE BUFFER (Max 30) ---
    public synchronized List<String> getInspectorBuffer() {
        List<String> list = new ArrayList<>();
        String raw = prefs.getString(KEY_INSPECTOR_BUFFER, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }
        } catch (JSONException ignored) {}
        return list;
    }

    public synchronized void addInspectorLog(String logEntry) {
        List<String> list = getInspectorBuffer();
        list.add(0, logEntry);
        while (list.size() > 30) {
            list.remove(list.size() - 1);
        }
        JSONArray arr = new JSONArray();
        for (String entry : list) {
            arr.put(entry);
        }
        prefs.edit().putString(KEY_INSPECTOR_BUFFER, arr.toString()).apply();
    }

    public void clearInspectorBuffer() {
        prefs.edit().putString(KEY_INSPECTOR_BUFFER, "[]").apply();
    }

    // --- LAUNCHER UI PREFERENCES ---
    public boolean isAutoLaunchSingle() {
        return prefs.getBoolean(KEY_AUTO_LAUNCH_SINGLE, false);
    }

    public void setAutoLaunchSingle(boolean autoLaunch) {
        prefs.edit().putBoolean(KEY_AUTO_LAUNCH_SINGLE, autoLaunch).apply();
    }

    public String getSwipeLeftPackage() {
        return prefs.getString(KEY_SWIPE_LEFT, null);
    }

    public void setSwipeLeftPackage(String pkg) {
        prefs.edit().putString(KEY_SWIPE_LEFT, pkg).apply();
    }

    public String getSwipeRightPackage() {
        return prefs.getString(KEY_SWIPE_RIGHT, null);
    }

    public void setSwipeRightPackage(String pkg) {
        prefs.edit().putString(KEY_SWIPE_RIGHT, pkg).apply();
    }

    // --- DND AUTOMATION ---
    public boolean isDndAutomationEnabled() {
        return prefs.getBoolean(KEY_DND_ENABLED, false);
    }

    public void setDndAutomationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DND_ENABLED, enabled).apply();
    }

    public int getDndStartHour() {
        return prefs.getInt(KEY_DND_START_H, 22);
    }

    public int getDndStartMinute() {
        return prefs.getInt(KEY_DND_START_M, 0);
    }

    public void setDndStartTime(int h, int m) {
        prefs.edit().putInt(KEY_DND_START_H, h).putInt(KEY_DND_START_M, m).apply();
    }

    public int getDndEndHour() {
        return prefs.getInt(KEY_DND_END_H, 7);
    }

    public int getDndEndMinute() {
        return prefs.getInt(KEY_DND_END_M, 0);
    }

    public void setDndEndTime(int h, int m) {
        prefs.edit().putInt(KEY_DND_END_H, h).putInt(KEY_DND_END_M, m).apply();
    }
}

