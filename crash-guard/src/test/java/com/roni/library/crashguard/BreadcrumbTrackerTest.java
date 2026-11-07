package com.roni.library.crashguard;

import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class BreadcrumbTrackerTest {

    @Test
    public void testRingBufferCapacityLimit() {
        BreadcrumbTracker tracker = new BreadcrumbTracker(5);

        for (int i = 1; i <= 10; i++) {
            tracker.add("Action " + i, Breadcrumb.Type.USER_ACTION, "test");
        }

        Assert.assertEquals(5, tracker.size());
        List<Breadcrumb> list = tracker.getAll();
        Assert.assertEquals(5, list.size());

        // First item should now be Action 6
        Assert.assertEquals("Action 6", list.get(0).getMessage());
        // Last item should be Action 10
        Assert.assertEquals("Action 10", list.get(4).getMessage());
    }

    @Test
    public void testClearTracker() {
        BreadcrumbTracker tracker = new BreadcrumbTracker(10);
        tracker.add("Test Breadcrumb", Breadcrumb.Type.NAVIGATION, "ui");
        Assert.assertEquals(1, tracker.size());

        tracker.clear();
        Assert.assertEquals(0, tracker.size());
    }

    @Test
    public void testMinimumCapacityIsEnforced() {
        BreadcrumbTracker tracker = new BreadcrumbTracker(1);
        for (int i = 0; i < 10; i++) {
            tracker.add("item " + i, Breadcrumb.Type.CUSTOM, "test");
        }
        Assert.assertEquals(5, tracker.size());
    }

    @Test
    public void testNullBreadcrumbIsIgnored() {
        BreadcrumbTracker tracker = new BreadcrumbTracker(10);
        tracker.add(null);
        Assert.assertEquals(0, tracker.size());
    }

    @Test
    public void testGetAllReturnsSnapshot() {
        BreadcrumbTracker tracker = new BreadcrumbTracker(10);
        tracker.add("one", Breadcrumb.Type.CUSTOM, "test");

        List<Breadcrumb> snapshot = tracker.getAll();
        tracker.add("two", Breadcrumb.Type.CUSTOM, "test");

        Assert.assertEquals(1, snapshot.size());
        Assert.assertEquals(2, tracker.size());
    }

    @Test
    public void testToJsonArray() {
        BreadcrumbTracker tracker = new BreadcrumbTracker(10);
        tracker.add("one", Breadcrumb.Type.CUSTOM, "test");
        tracker.add("two", Breadcrumb.Type.NAVIGATION, "ui");

        JSONArray array = tracker.toJsonArray();
        Assert.assertEquals(2, array.length());
    }
}
