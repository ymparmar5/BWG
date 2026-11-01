package com.example.focus.engine;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class WebBlockerEngine {

    // Supported browser packages
    public static final Set<String> BROWSER_PACKAGES = new HashSet<>(Arrays.asList(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.canary",
            "com.chrome.dev",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.sec.android.app.sbrowser",
            "com.duckduckgo.mobile.android"
    ));

    // Known URL bar resource ID substrings across browsers
    private static final String[] URL_BAR_ID_SUBSTRINGS = {
            "url_bar",
            "search_box_text",
            "location_bar_edit_text",
            "mozac_browser_toolbar_url_view",
            "url_field",
            "toolbar_url",
            "address_bar"
    };

    // Adult, misleading ads, and adult romantic / hookup content keywords
    private static final String[] BLOCKED_KEYWORDS = {
            "porn", "xxx", "xvideos", "xnxx", "pornhub", "redtube", "youporn",
            "erotic", "onlyfans", "chaturbate", "cam4", "stripchat", "rule34",
            "nude", "sex", "hentai", "brazzers", "spankbang", "eporner",
            "hookup", "casualdating", "adultdating", "ashleymadison",
            "casino", "bet365", "1xbet", "gambling", "poker", "jackpot", "stake.com"
    };

    public static boolean isBrowserPackage(String packageName) {
        return packageName != null && BROWSER_PACKAGES.contains(packageName);
    }

    /**
     * Inspects active window hierarchy in a browser to find the current URL or search query.
     * Returns the detected URL or null if not found.
     */
    public static String extractBrowserUrl(AccessibilityNodeInfo root) {
        if (root == null) return null;

        for (String idPart : URL_BAR_ID_SUBSTRINGS) {
            String url = findTextByViewIdSubstring(root, idPart);
            if (url != null && !url.trim().isEmpty()) {
                return url.trim().toLowerCase(Locale.US);
            }
        }
        return null;
    }

    private static String findTextByViewIdSubstring(AccessibilityNodeInfo node, String substring) {
        if (node == null) return null;

        CharSequence resIdSeq = node.getViewIdResourceName();
        if (resIdSeq != null && resIdSeq.toString().toLowerCase(Locale.US).contains(substring)) {
            CharSequence text = node.getText();
            if (text != null && text.length() > 0) {
                return text.toString();
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                String found = findTextByViewIdSubstring(child, substring);
                child.recycle();
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Checks if the extracted URL/query contains adult, misleading ads, or blocked keywords.
     */
    public static boolean isBlockedUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String clean = url.toLowerCase(Locale.US);

        for (String kw : BLOCKED_KEYWORDS) {
            if (clean.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    public static String getMatchedCategory(String url) {
        if (url == null) return "Adult & Misleading Content";
        String clean = url.toLowerCase(Locale.US);

        if (clean.contains("casino") || clean.contains("bet365") || clean.contains("gambling") || clean.contains("poker")) {
            return "Gambling & Misleading Ads";
        }
        if (clean.contains("hookup") || clean.contains("dating") || clean.contains("ashleymadison")) {
            return "Adult Dating & Hookups";
        }
        return "Adult Content";
    }
}
