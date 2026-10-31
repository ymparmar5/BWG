package com.example.focus.engine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RuleModel {
    private final String packageName;
    private final Set<String> blockedViewIds;
    private final Set<String> blockedDescriptions;
    private final Set<String> blockedTabKeywords;
    private final Set<String> whitelistViewIds;

    public RuleModel(String packageName,
                     Set<String> blockedViewIds,
                     Set<String> blockedDescriptions,
                     Set<String> blockedTabKeywords,
                     Set<String> whitelistViewIds) {
        this.packageName = packageName;
        this.blockedViewIds = blockedViewIds != null ? blockedViewIds : new HashSet<>();
        this.blockedDescriptions = blockedDescriptions != null ? blockedDescriptions : new HashSet<>();
        this.blockedTabKeywords = blockedTabKeywords != null ? blockedTabKeywords : new HashSet<>();
        this.whitelistViewIds = whitelistViewIds != null ? whitelistViewIds : new HashSet<>();
    }

    public String getPackageName() {
        return packageName;
    }

    public Set<String> getBlockedViewIds() {
        return blockedViewIds;
    }

    public Set<String> getBlockedDescriptions() {
        return blockedDescriptions;
    }

    public Set<String> getBlockedTabKeywords() {
        return blockedTabKeywords;
    }

    public Set<String> getWhitelistViewIds() {
        return whitelistViewIds;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("packageName", packageName);
        obj.put("blockedViewIds", new JSONArray(blockedViewIds));
        obj.put("blockedDescriptions", new JSONArray(blockedDescriptions));
        obj.put("blockedTabKeywords", new JSONArray(blockedTabKeywords));
        obj.put("whitelistViewIds", new JSONArray(whitelistViewIds));
        return obj;
    }

    public static RuleModel fromJsonObject(JSONObject obj) throws JSONException {
        String pkg = obj.getString("packageName");
        Set<String> viewIds = jsonArrayToSet(obj.optJSONArray("blockedViewIds"));
        Set<String> descriptions = jsonArrayToSet(obj.optJSONArray("blockedDescriptions"));
        Set<String> tabKeywords = jsonArrayToSet(obj.optJSONArray("blockedTabKeywords"));
        Set<String> whitelist = jsonArrayToSet(obj.optJSONArray("whitelistViewIds"));
        return new RuleModel(pkg, viewIds, descriptions, tabKeywords, whitelist);
    }

    private static Set<String> jsonArrayToSet(JSONArray array) {
        Set<String> set = new HashSet<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String val = array.optString(i);
                if (val != null && !val.trim().isEmpty()) {
                    set.add(val.trim());
                }
            }
        }
        return set;
    }
}

