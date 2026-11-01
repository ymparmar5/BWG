package com.example.focus;

import com.example.focus.engine.WebBlockerEngine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WebBlockerEngineTest {

    @Test
    public void testBrowserPackageRecognition() {
        assertTrue(WebBlockerEngine.isBrowserPackage("com.android.chrome"));
        assertTrue(WebBlockerEngine.isBrowserPackage("org.mozilla.firefox"));
        assertTrue(WebBlockerEngine.isBrowserPackage("com.brave.browser"));
        assertTrue(WebBlockerEngine.isBrowserPackage("com.sec.android.app.sbrowser"));

        assertFalse(WebBlockerEngine.isBrowserPackage("com.google.android.youtube"));
        assertFalse(WebBlockerEngine.isBrowserPackage("com.whatsapp"));
        assertFalse(WebBlockerEngine.isBrowserPackage(null));
    }

    @Test
    public void testBlockedUrlsAndKeywords() {
        // Adult
        assertTrue(WebBlockerEngine.isBlockedUrl("https://www.pornhub.com/video/123"));
        assertTrue(WebBlockerEngine.isBlockedUrl("https://xxx-adult-site.org"));
        assertTrue(WebBlockerEngine.isBlockedUrl("https://xnxx.com"));
        assertTrue(WebBlockerEngine.isBlockedUrl("https://onlyfans.com/profile"));
        assertTrue(WebBlockerEngine.isBlockedUrl("https://brazzers.com"));

        // Misleading ads / gambling
        assertTrue(WebBlockerEngine.isBlockedUrl("https://bet365.com/live"));
        assertTrue(WebBlockerEngine.isBlockedUrl("https://casino-jackpot-winner.net"));
        assertTrue(WebBlockerEngine.isBlockedUrl("https://1xbet.com"));

        // Adult dating / hookup
        assertTrue(WebBlockerEngine.isBlockedUrl("https://hookup-tonight.com"));
        assertTrue(WebBlockerEngine.isBlockedUrl("https://ashleymadison.com"));

        // Whitelisted normal websites
        assertFalse(WebBlockerEngine.isBlockedUrl("https://en.wikipedia.org/wiki/Android"));
        assertFalse(WebBlockerEngine.isBlockedUrl("https://github.com/ymparmar5/BWG"));
        assertFalse(WebBlockerEngine.isBlockedUrl("https://developer.android.com"));
        assertFalse(WebBlockerEngine.isBlockedUrl("https://google.com/search?q=weather"));
    }

    @Test
    public void testCategorization() {
        assertEquals("Gambling & Misleading Ads", WebBlockerEngine.getMatchedCategory("https://bet365.com"));
        assertEquals("Adult Dating & Hookups", WebBlockerEngine.getMatchedCategory("https://hookup-club.com"));
        assertEquals("Adult Content", WebBlockerEngine.getMatchedCategory("https://pornhub.com"));
    }
}
