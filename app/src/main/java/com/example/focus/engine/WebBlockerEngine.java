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

    // Fast memory cache for the most recently evaluated URL to avoid repeated string matching
    private static String lastEvaluatedUrl = null;
    private static boolean lastEvaluationResult = false;

    public static boolean isBrowserPackage(String packageName) {
        return packageName != null && BROWSER_PACKAGES.contains(packageName);
    }

    /**
     * Efficient, single-pass depth-limited search for URL bar in the active window hierarchy.
     * Prevents deep DOM recursion into arbitrary web content, keeping battery & CPU usage near zero.
     */
    public static String extractBrowserUrl(AccessibilityNodeInfo root) {
        if (root == null) return null;
        return searchUrlInHierarchy(root, 0);
    }

    private static String searchUrlInHierarchy(AccessibilityNodeInfo node, int depth) {
        // Browser address bars are always in the top toolbars (depth <= 6). Skip deeper web content.
        if (node == null || depth > 6) return null;

        CharSequence resIdSeq = node.getViewIdResourceName();
        if (resIdSeq != null) {
            String resId = resIdSeq.toString().toLowerCase(Locale.US);
            for (String sub : URL_BAR_ID_SUBSTRINGS) {
                if (resId.contains(sub)) {
                    CharSequence text = node.getText();
                    if (text != null && text.length() > 0) {
                        return text.toString().trim().toLowerCase(Locale.US);
                    }
                }
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                String found = searchUrlInHierarchy(child, depth + 1);
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
     * Includes fast memoization for identical consecutive URLs.
     */
    public static synchronized boolean isBlockedUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String clean = url.toLowerCase(Locale.US);

        if (clean.equals(lastEvaluatedUrl)) {
            return lastEvaluationResult;
        }

        boolean blocked = false;
        for (String kw : BLOCKED_KEYWORDS) {
            if (clean.contains(kw)) {
                blocked = true;
                break;
            }
        }

        lastEvaluatedUrl = clean;
        lastEvaluationResult = blocked;
        return blocked;
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
