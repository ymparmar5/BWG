package com.example.focus.engine;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RuleMatcherEngine {

    /**
     * Testable node abstraction for unit tests.
     */
    public static class InspectableNode {
        public final String viewId;
        public final String contentDescription;
        public final String text;
        public final String className;
        public final boolean isSelected;
        public final List<InspectableNode> children = new ArrayList<>();

        public InspectableNode(String viewId, String contentDescription, String text, String className, boolean isSelected) {
            this.viewId = viewId;
            this.contentDescription = contentDescription;
            this.text = text;
            this.className = className;
            this.isSelected = isSelected;
        }

        public void addChild(InspectableNode child) {
            if (child != null) {
                children.add(child);
            }
        }
    }

    public static class MatchResult {
        public final boolean isBlocked;
        public final String matchedPattern;

        public MatchResult(boolean isBlocked, String matchedPattern) {
            this.isBlocked = isBlocked;
            this.matchedPattern = matchedPattern;
        }

        public static MatchResult pass() {
            return new MatchResult(false, null);
        }

        public static MatchResult block(String pattern) {
            return new MatchResult(true, pattern);
        }
    }

    public interface InspectorCallback {
        void onNodeInspected(String log);
    }

    /**
     * Pure testable evaluation method on InspectableNode tree.
     */
    public static MatchResult evaluateNode(InspectableNode root, RuleModel rule, InspectorCallback inspector) {
        if (root == null || rule == null) {
            return MatchResult.pass();
        }

        // Pre-check whitelist across tree to avoid false positives (e.g. standard YouTube watch screen)
        if (hasWhitelistedNode(root, rule.getWhitelistViewIds())) {
            return MatchResult.pass();
        }

        return scanInspectableNode(root, rule, 0, inspector);
    }

    private static boolean hasWhitelistedNode(InspectableNode node, Set<String> whitelist) {
        if (node == null || whitelist == null || whitelist.isEmpty()) return false;
        if (matchesAnyId(node.viewId, whitelist)) {
            return true;
        }
        for (InspectableNode child : node.children) {
            if (hasWhitelistedNode(child, whitelist)) return true;
        }
        return false;
    }

    private static MatchResult scanInspectableNode(InspectableNode node, RuleModel rule, int depth, InspectorCallback inspector) {
        if (node == null || depth > 18) return MatchResult.pass();

        if (inspector != null) {
            inspector.onNodeInspected(String.format(
                    "d:%d | id:%s | desc:%s | txt:%s | sel:%b",
                    depth, node.viewId, node.contentDescription, node.text, node.isSelected
            ));
        }

        // 1. Layer 1: Check View ID
        if (matchesAnyId(node.viewId, rule.getBlockedViewIds())) {
            return MatchResult.block("ViewId: " + node.viewId);
        }

        // 2. Layer 2: Check Content Description
        if (node.contentDescription != null && !node.contentDescription.trim().isEmpty()) {
            for (String targetDesc : rule.getBlockedDescriptions()) {
                if (node.contentDescription.equalsIgnoreCase(targetDesc)) {
                    return MatchResult.block("Description: " + node.contentDescription);
                }
            }
        }

        // 3. Layer 2: Check Selected Tab Keywords (e.g. selected "Shorts" or "Reels" tab)
        if (node.isSelected) {
            String combined = ((node.contentDescription != null ? node.contentDescription : "") + " " +
                    (node.text != null ? node.text : "")).trim();
            for (String kw : rule.getBlockedTabKeywords()) {
                if (!combined.isEmpty() && combined.toLowerCase().contains(kw.toLowerCase())) {
                    return MatchResult.block("Active Tab: " + combined);
                }
            }
        }

        // Scan children
        for (InspectableNode child : node.children) {
            MatchResult childResult = scanInspectableNode(child, rule, depth + 1, inspector);
            if (childResult.isBlocked) {
                return childResult;
            }
        }

        return MatchResult.pass();
    }

    /**
     * Android AccessibilityNodeInfo evaluation wrapper.
     */
    public static MatchResult evaluateAccessibilityNode(AccessibilityNodeInfo root, RuleModel rule, InspectorCallback inspector) {
        if (root == null || rule == null) {
            return MatchResult.pass();
        }

        // Fast whitelist check
        if (hasAccessibilityWhitelist(root, rule.getWhitelistViewIds())) {
            return MatchResult.pass();
        }

        return scanAccessibilityNode(root, rule, 0, inspector);
    }

    private static boolean hasAccessibilityWhitelist(AccessibilityNodeInfo node, Set<String> whitelist) {
        if (node == null || whitelist == null || whitelist.isEmpty()) return false;
        String viewId = node.getViewIdResourceName();
        if (matchesAnyId(viewId, whitelist)) {
            return true;
        }

        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean hit = hasAccessibilityWhitelist(child, whitelist);
                child.recycle();
                if (hit) return true;
            }
        }
        return false;
    }

    private static MatchResult scanAccessibilityNode(AccessibilityNodeInfo node, RuleModel rule, int depth, InspectorCallback inspector) {
        if (node == null || depth > 18) return MatchResult.pass();

        String viewId = node.getViewIdResourceName();
        CharSequence descSeq = node.getContentDescription();
        String desc = descSeq != null ? descSeq.toString().trim() : null;
        CharSequence txtSeq = node.getText();
        String text = txtSeq != null ? txtSeq.toString().trim() : null;
        boolean isSelected = node.isSelected();

        if (inspector != null) {
            inspector.onNodeInspected(String.format(
                    "d:%d | id:%s | desc:%s | txt:%s | sel:%b",
                    depth, viewId, desc, text, isSelected
            ));
        }

        if (matchesAnyId(viewId, rule.getBlockedViewIds())) {
            return MatchResult.block("ViewId: " + viewId);
        }

        if (desc != null && !desc.isEmpty()) {
            for (String targetDesc : rule.getBlockedDescriptions()) {
                if (desc.equalsIgnoreCase(targetDesc)) {
                    return MatchResult.block("Description: " + desc);
                }
            }
        }

        if (isSelected) {
            String combined = ((desc != null ? desc : "") + " " + (text != null ? text : "")).trim();
            for (String kw : rule.getBlockedTabKeywords()) {
                if (!combined.isEmpty() && combined.toLowerCase().contains(kw.toLowerCase())) {
                    return MatchResult.block("Active Tab: " + combined);
                }
            }
        }

        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                MatchResult res = scanAccessibilityNode(child, rule, depth + 1, inspector);
                child.recycle();
                if (res.isBlocked) {
                    return res;
                }
            }
        }

        return MatchResult.pass();
    }

    public static boolean matchesAnyId(String fullViewId, Set<String> targetIds) {
        if (fullViewId == null || fullViewId.isEmpty() || targetIds == null) {
            return false;
        }
        if (targetIds.contains(fullViewId)) {
            return true;
        }

        int colon = fullViewId.indexOf(":id/");
        String shortId = colon != -1 ? fullViewId.substring(colon + 4) : fullViewId;
        return targetIds.contains(shortId);
    }
}

