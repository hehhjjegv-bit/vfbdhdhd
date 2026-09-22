package com.quickfilestudio.app.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public final class SafPermissionManager {

    private static final String PREFS = "qfs_storage";
    private static final String TREE_URI = "tree_uri";

    private final Context context;
    private final SharedPreferences prefs;

    public SafPermissionManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );
    }

    public void saveTreeUri(Uri uri) {
        if (uri == null) return;

        prefs.edit()
                .putString(TREE_URI, uri.toString())
                .apply();
    }

    public Uri getTreeUri() {
        String value = prefs.getString(TREE_URI, null);

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Uri.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    public String getTreeUriString() {
        Uri uri = getTreeUri();
        return uri == null ? "" : uri.toString();
    }

    public boolean hasTree() {
        return getTreeUri() != null;
    }

    public void clearTree() {
        prefs.edit()
                .remove(TREE_URI)
                .apply();
    }
}
