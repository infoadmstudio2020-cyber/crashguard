package com.roni.library.crashguard;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class BreadcrumbTest {

    @Test
    public void testJsonRoundTrip() {
        Breadcrumb breadcrumb = new Breadcrumb("tapped login", Breadcrumb.Type.USER_ACTION, "auth");
        JSONObject json = breadcrumb.toJson();

        Breadcrumb parsed = Breadcrumb.fromJson(json);
        Assert.assertNotNull(parsed);
        Assert.assertEquals("tapped login", parsed.getMessage());
        Assert.assertEquals(Breadcrumb.Type.USER_ACTION, parsed.getType());
        Assert.assertEquals("auth", parsed.getCategory());
        Assert.assertEquals(breadcrumb.getTimestamp(), parsed.getTimestamp());
    }

    @Test
    public void testNullSafety() {
        Breadcrumb breadcrumb = new Breadcrumb(null, null, null);
        Assert.assertEquals("", breadcrumb.getMessage());
        Assert.assertEquals(Breadcrumb.Type.CUSTOM, breadcrumb.getType());
        Assert.assertEquals("general", breadcrumb.getCategory());
    }

    @Test
    public void testUnknownTypeFallsBackToCustom() throws Exception {
        JSONObject json = new JSONObject();
        json.put("message", "x");
        json.put("type", "NOT_A_REAL_TYPE");
        json.put("category", "c");
        json.put("timestamp", 123L);

        Breadcrumb parsed = Breadcrumb.fromJson(json);
        Assert.assertNotNull(parsed);
        Assert.assertEquals(Breadcrumb.Type.CUSTOM, parsed.getType());
    }

    @Test
    public void testFromJsonHandlesNull() {
        Assert.assertNull(Breadcrumb.fromJson(null));
    }

    @Test
    public void testToStringContainsCategoryAndMessage() {
        Breadcrumb breadcrumb = new Breadcrumb("hello", Breadcrumb.Type.NETWORK, "api");
        String text = breadcrumb.toString();
        Assert.assertTrue(text.contains("api"));
        Assert.assertTrue(text.contains("hello"));
        Assert.assertTrue(text.contains("NETWORK"));
    }
}
