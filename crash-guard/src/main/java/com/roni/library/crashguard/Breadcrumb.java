package com.roni.library.crashguard;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents a discrete user action, navigation step, or diagnostic event
 * recorded prior to an application crash.
 */
public class Breadcrumb implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        USER_ACTION,
        NAVIGATION,
        NETWORK,
        SYSTEM,
        CUSTOM
    }

    private final String message;
    private final Type type;
    private final String category;
    private final long timestamp;

    public Breadcrumb(String message, Type type, String category) {
        this.message = message != null ? message : "";
        this.type = type != null ? type : Type.CUSTOM;
        this.category = category != null ? category : "general";
        this.timestamp = System.currentTimeMillis();
    }

    public Breadcrumb(String message, Type type, String category, long timestamp) {
        this.message = message != null ? message : "";
        this.type = type != null ? type : Type.CUSTOM;
        this.category = category != null ? category : "general";
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public Type getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        return sdf.format(new Date(timestamp));
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("message", message);
            json.put("type", type.name());
            json.put("category", category);
            json.put("timestamp", timestamp);
            json.put("formattedTime", getFormattedTime());
        } catch (JSONException e) {
            // Handled safely
        }
        return json;
    }

    public static Breadcrumb fromJson(JSONObject json) {
        if (json == null) return null;
        String msg = json.optString("message", "");
        String typeStr = json.optString("type", Type.CUSTOM.name());
        Type type;
        try {
            type = Type.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            type = Type.CUSTOM;
        }
        String cat = json.optString("category", "general");
        long ts = json.optLong("timestamp", System.currentTimeMillis());
        return new Breadcrumb(msg, type, cat, ts);
    }

    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] [" + category + "] (" + type + "): " + message;
    }
}
