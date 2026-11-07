package com.roni.library.crashguard;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Thread-safe ring buffer for capturing a sliding window of recent breadcrumbs.
 */
public class BreadcrumbTracker {
    private final int maxCapacity;
    private final LinkedList<Breadcrumb> buffer = new LinkedList<>();
    private final Object lock = new Object();

    public BreadcrumbTracker(int maxCapacity) {
        this.maxCapacity = Math.max(5, maxCapacity);
    }

    public void add(Breadcrumb breadcrumb) {
        if (breadcrumb == null) return;
        synchronized (lock) {
            while (buffer.size() >= maxCapacity) {
                buffer.removeFirst();
            }
            buffer.addLast(breadcrumb);
        }
    }

    public void add(String message, Breadcrumb.Type type, String category) {
        add(new Breadcrumb(message, type, category));
    }

    public List<Breadcrumb> getAll() {
        synchronized (lock) {
            return new ArrayList<>(buffer);
        }
    }

    public void clear() {
        synchronized (lock) {
            buffer.clear();
        }
    }

    public int size() {
        synchronized (lock) {
            return buffer.size();
        }
    }

    public JSONArray toJsonArray() {
        JSONArray array = new JSONArray();
        List<Breadcrumb> list = getAll();
        for (Breadcrumb b : list) {
            array.put(b.toJson());
        }
        return array;
    }
}
