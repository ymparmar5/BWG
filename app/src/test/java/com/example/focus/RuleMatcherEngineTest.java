package com.example.focus;

import com.example.focus.data.FocusPreferences;
import com.example.focus.engine.RuleMatcherEngine;
import com.example.focus.engine.RuleMatcherEngine.InspectableNode;
import com.example.focus.engine.RuleMatcherEngine.MatchResult;
import com.example.focus.engine.RuleModel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class RuleMatcherEngineTest {

    private Map<String, RuleModel> defaultRules;

    @Before
    public void setup() {
        defaultRules = FocusPreferences.getDefaultRulesMap();
    }

    @Test
    public void testYouTubeShorts_BlockedByViewId() {
        RuleModel ytRule = defaultRules.get("com.google.android.youtube");
        Assert.assertNotNull(ytRule);

        InspectableNode root = new InspectableNode("com.google.android.youtube:id/main_layout", null, null, "FrameLayout", false);
        InspectableNode shortsContainer = new InspectableNode("com.google.android.youtube:id/reel_recycler", "Reels", null, "RecyclerView", false);
        root.addChild(shortsContainer);

        MatchResult res = RuleMatcherEngine.evaluateNode(root, ytRule, null);
        Assert.assertTrue("YouTube Shorts should be blocked", res.isBlocked);
    }

    @Test
    public void testYouTubeShorts_BlockedBySelectedTab() {
        RuleModel ytRule = defaultRules.get("com.google.android.youtube");

        InspectableNode root = new InspectableNode("com.google.android.youtube:id/bottom_bar", null, null, "LinearLayout", false);
        InspectableNode shortsTab = new InspectableNode("com.google.android.youtube:id/tab_item", "Shorts", "Shorts", "Button", true);
        root.addChild(shortsTab);

        MatchResult res = RuleMatcherEngine.evaluateNode(root, ytRule, null);
        Assert.assertTrue("Selected Shorts tab should be blocked", res.isBlocked);
    }

    @Test
    public void testYouTubeNormalWatchScreen_NotBlockedDueToWhitelist() {
        RuleModel ytRule = defaultRules.get("com.google.android.youtube");

        InspectableNode root = new InspectableNode("com.google.android.youtube:id/watch_while_layout", null, null, "ViewGroup", false);
        InspectableNode player = new InspectableNode("com.google.android.youtube:id/watch_player", null, null, "View", false);
        InspectableNode title = new InspectableNode("com.google.android.youtube:id/video_title", null, "Awesome Docu", "TextView", false);

        root.addChild(player);
        root.addChild(title);

        MatchResult res = RuleMatcherEngine.evaluateNode(root, ytRule, null);
        Assert.assertFalse("Regular YouTube video watch player must NOT be blocked", res.isBlocked);
    }

    @Test
    public void testInstagramReels_Blocked() {
        RuleModel igRule = defaultRules.get("com.instagram.android");
        Assert.assertNotNull(igRule);

        InspectableNode root = new InspectableNode("com.instagram.android:id/root", null, null, "FrameLayout", false);
        InspectableNode reelPager = new InspectableNode("com.instagram.android:id/clips_viewer_view_pager", "Reel viewer", null, "ViewPager", false);
        root.addChild(reelPager);

        MatchResult res = RuleMatcherEngine.evaluateNode(root, igRule, null);
        Assert.assertTrue("Instagram Reels viewer must be blocked", res.isBlocked);
    }

    @Test
    public void testInstagramFeed_NotBlocked() {
        RuleModel igRule = defaultRules.get("com.instagram.android");

        InspectableNode root = new InspectableNode("com.instagram.android:id/root", null, null, "FrameLayout", false);
        InspectableNode feedTab = new InspectableNode("com.instagram.android:id/feed_tab", "Home", null, "View", true);
        root.addChild(feedTab);

        MatchResult res = RuleMatcherEngine.evaluateNode(root, igRule, null);
        Assert.assertFalse("Instagram standard feed must NOT be blocked", res.isBlocked);
    }

    @Test
    public void testWhatsAppStatus_Blocked() {
        RuleModel waRule = defaultRules.get("com.whatsapp");
        Assert.assertNotNull(waRule);

        InspectableNode root = new InspectableNode("com.whatsapp:id/main_layout", null, null, "FrameLayout", false);
        InspectableNode statusPlayback = new InspectableNode("com.whatsapp:id/status_playback_fragment_container", "Status playback", null, "FrameLayout", false);
        root.addChild(statusPlayback);

        MatchResult res = RuleMatcherEngine.evaluateNode(root, waRule, null);
        Assert.assertTrue("WhatsApp status playback must be blocked", res.isBlocked);
    }

    @Test
    public void testWhatsAppNormalChat_NotBlocked() {
        RuleModel waRule = defaultRules.get("com.whatsapp");

        InspectableNode root = new InspectableNode("com.whatsapp:id/main_layout", null, null, "FrameLayout", false);
        InspectableNode chat = new InspectableNode("com.whatsapp:id/conversation_layout", null, null, "ViewGroup", false);
        InspectableNode entry = new InspectableNode("com.whatsapp:id/entry", null, "Hello!", "EditText", false);

        root.addChild(chat);
        root.addChild(entry);

        MatchResult res = RuleMatcherEngine.evaluateNode(root, waRule, null);
        Assert.assertFalse("WhatsApp chat screen must NOT be blocked", res.isBlocked);
    }
}

