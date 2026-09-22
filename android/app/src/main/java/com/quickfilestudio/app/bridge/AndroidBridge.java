package com.quickfilestudio.app.bridge;
import android.content.Intent;

import android.content.Context;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import android.net.Uri;
import android.util.Base64;
import android.webkit.JavascriptInterface;

import com.quickfilestudio.app.storage.SafFileManager;
import com.quickfilestudio.app.storage.SafPermissionManager;
import com.quickfilestudio.app.update.UpdateManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AndroidBridge {

    public interface Callback {
        void success(String id, Object payload);
        void error(String id, String message);
    }

    private final Context context;
    private final SafPermissionManager permissions;
    private final SafFileManager files;
    private final Callback callback;
    private final UpdateManager updates;

    private final ExecutorService executor =
            Executors.newCachedThreadPool();

    public AndroidBridge(
            Context context,
            Callback callback
    ) {
        this.context = context;
        this.callback = callback;

        this.permissions =
                new SafPermissionManager(context);

        this.files =
                new SafFileManager(context);
        this.updates = new UpdateManager(context);
    }

    @JavascriptInterface
    public String version() {
        return "3.0-STORAGE-CORE";
    }

    @JavascriptInterface
    public void call(String json) {

        if (json == null || json.trim().isEmpty()) {
            return;
        }

        try {
            JSONObject request =
                    new JSONObject(json);

            final String id =
                    request.optString("id", "");

            final String command =
                    request.optString("command", "");

            final JSONObject args =
                    request.optJSONObject("args");

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    handle(id, command, args);
                }
            });

        } catch (Exception e) {
            if (callback != null) {
                callback.error(
                        "",
                        safeMessage(e)
                );
            }
        }
    }

    private void handle(
            String id,
            String command,
            JSONObject args
    ) {

        try {

            if ("readBase64".equals(command)) {
                try {
                    Uri source = parseUri(args, "uri");

                    if (source == null) {
                        error(
                                id,
                                "مسار الملف غير صالح"
                        );
                        return;
                    }

                    InputStream input =
                            context.getContentResolver()
                                    .openInputStream(source);

                    if (input == null) {
                        error(
                                id,
                                "تعذر فتح الملف"
                        );
                        return;
                    }

                    ByteArrayOutputStream output =
                            new ByteArrayOutputStream();

                    byte[] buffer = new byte[8192];
                    int count;

                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }

                    input.close();

                    String mime =
                            context.getContentResolver()
                                    .getType(source);

                    if (mime == null || mime.trim().isEmpty()) {
                        mime = "application/octet-stream";
                    }

                    JSONObject result =
                            new JSONObject()
                                    .put(
                                            "data",
                                            Base64.encodeToString(
                                                    output.toByteArray(),
                                                    Base64.NO_WRAP
                                            )
                                    )
                                    .put("mime", mime);

                    success(
                            id,
                            result
                    );

                } catch (Exception e) {
                    error(
                            id,
                            "تعذر قراءة الملف: " + e.getMessage()
                    );
                }

                return;
            }

            if ("readAsset".equals(command)) {
            String path = args == null ? "" : args.optString("path", "");
            if (path.trim().isEmpty()) {
                throw new IllegalArgumentException("readAsset requires path");
            }

            path = path.replace("\\\\", "/");
            while (path.startsWith("/")) {
                path = path.substring(1);
            }

            if (path.contains("..") || path.startsWith("www/")) {
                throw new SecurityException("Invalid asset path");
            }

            String assetPath = "www/" + path;
            StringBuilder content = new StringBuilder();

            try (InputStream input = context.getAssets().open(assetPath);
                 java.io.InputStreamReader reader =
                         new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8);
                 java.io.BufferedReader buffered =
                         new java.io.BufferedReader(reader)) {

                String line;
                while ((line = buffered.readLine()) != null) {
                    content.append(line).append('\n');
                }
            }

            success(id, new JSONObject().put("content", content.toString()));
            return;
        }

        if ("version".equals(command)) {

                success(
                        id,
                        new JSONObject()
                                .put("version", version())
                );

                return;
            }

            if ("chooseFolder".equals(command)) {
                if (context instanceof com.quickfilestudio.app.MainActivity) {
                    ((com.quickfilestudio.app.MainActivity) context).chooseFolderFromBridge(id);
                    return;
                }
                throw new IllegalStateException("MainActivity is not available");
            }

            if ("chooseUserFilesFolder".equals(command)) {
                if (context instanceof com.quickfilestudio.app.MainActivity) {
                    ((com.quickfilestudio.app.MainActivity) context).chooseUserFilesFolderFromBridge(id);
                    return;
                }
                throw new IllegalStateException("MainActivity is not available");
            }

            if ("chooseFile".equals(command)) {
                if (context instanceof com.quickfilestudio.app.MainActivity) {
                    ((com.quickfilestudio.app.MainActivity) context).chooseFileFromBridge(id);
                    return;
                }
                throw new IllegalStateException("MainActivity is not available");
            }

            if ("openExternal".equals(command)) {
            String uriValue = args == null ? "" : args.optString("uri", "");
            if (uriValue.trim().isEmpty()) {
                throw new IllegalArgumentException("openExternal requires uri");
            }

            Uri uri = Uri.parse(uriValue);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).startActivity(intent);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }

            success(id, new JSONObject().put("opened", true));
            return;
        }

        if ("zip".equals(command)) {
            Uri source = parseUri(args, "uri");
            Uri output = files.zip(source);

            JSONObject result = new JSONObject();
            result.put("uri", output.toString());

            String name = "";
            android.database.Cursor cursor = context.getContentResolver().query(
                    output,
                    new String[]{android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                    null,
                    null,
                    null
            );

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(0);
                    }
                } finally {
                    cursor.close();
                }
            }

            result.put("name", name);
            success(id, result);
            return;
        }

        if ("exec".equals(command)) {
            String shellCommand = args == null ? "" : args.optString("command", "");
            if (shellCommand.trim().isEmpty()) {
                throw new IllegalArgumentException("الأمر فارغ");
            }

            int timeout = args == null ? 10000 : args.optInt("timeout", 10000);
            if (timeout < 1000) timeout = 1000;
            if (timeout > 30000) timeout = 30000;

            if (context instanceof com.quickfilestudio.app.MainActivity) {
                ((com.quickfilestudio.app.MainActivity) context)
                        .execFromBridge(id, shellCommand, timeout);
                return;
            }

            throw new IllegalStateException("MainActivity is not available");
        }

        if ("getWorkspace".equals(command)) {

                JSONObject result =
                        new JSONObject();

                result.put(
                        "treeUri",
                        permissions.getTreeUriString()
                );

                result.put(
                        "storagePermission",
                        permissions.hasTree()
                );

                android.content.SharedPreferences qfsPrefs =
                        context.getSharedPreferences(
                                "qfs_storage",
                                android.content.Context.MODE_PRIVATE
                        );

                String userFilesTree =
                        qfsPrefs.getString(
                                "user_files_tree_uri",
                                ""
                        );

                result.put(
                        "userFilesTreeUri",
                        userFilesTree
                );

                result.put(
                        "userFilesStoragePermission",
                        userFilesTree != null &&
                        !userFilesTree.trim().isEmpty()
                );

                success(id, result);
                return;
            }

            if ("list".equals(command)) {

                Uri uri =
                        parseUri(
                                args,
                                "uri"
                        );

                JSONArray result =
                        files.list(uri);

                success(id, result);
                return;
            }

            if ("read".equals(command)) {

                Uri uri =
                        parseUri(
                                args,
                                "uri"
                        );

                String content =
                        files.read(uri);

                JSONObject result =
                        new JSONObject();

                result.put(
                        "content",
                        content
                );

                success(id, result);
                return;
            }

            if ("write".equals(command)) {

                Uri uri =
                        parseUri(
                                args,
                                "uri"
                        );

                String content =
                        args == null
                                ? ""
                                : args.optString(
                                        "content",
                                        ""
                                );

                files.write(
                        uri,
                        content
                );

                success(
                        id,
                        new JSONObject()
                                .put("saved", true)
                );

                return;
            }

            if ("createFile".equals(command)) {

                Uri parent =
                        parseUri(
                                args,
                                "parent"
                        );

                String name =
                        args.optString(
                                "name",
                                ""
                        );

                String mime =
                        args.optString(
                                "mime",
                                "text/plain"
                        );

                Uri created =
                        files.createFile(
                                parent,
                                mime,
                                name
                        );

                success(
                        id,
                        new JSONObject()
                                .put(
                                        "uri",
                                        created == null
                                                ? ""
                                                : created.toString()
                                )
                );

                return;
            }

            if ("createFolder".equals(command)) {

                Uri parent =
                        parseUri(
                                args,
                                "parent"
                        );

                String name =
                        args.optString(
                                "name",
                                ""
                        );

                Uri created =
                        files.createFolder(
                                parent,
                                name
                        );

                success(
                        id,
                        new JSONObject()
                                .put(
                                        "uri",
                                        created == null
                                                ? ""
                                                : created.toString()
                                )
                );

                return;
            }

            if ("rename".equals(command)) {

                Uri uri =
                        parseUri(
                                args,
                                "uri"
                        );

                String name =
                        args.optString(
                                "name",
                                ""
                        );

                Uri renamed =
                        files.rename(
                                uri,
                                name
                        );

                success(
                        id,
                        new JSONObject()
                                .put(
                                        "uri",
                                        renamed == null
                                                ? ""
                                                : renamed.toString()
                                )
                );

                return;
            }

            if ("delete".equals(command)) {

                Uri uri =
                        parseUri(
                                args,
                                "uri"
                        );

                files.delete(uri);

                success(
                        id,
                        new JSONObject()
                                .put("deleted", true)
                );

                return;
            }

            if ("copy".equals(command)) {

                Uri source =
                        parseUri(
                                args,
                                "sourceUri"
                        );

                Uri target =
                        parseUri(
                                args,
                                "targetParentUri"
                        );

                Uri copied =
                        files.copy(
                                source,
                                target
                        );

                success(
                        id,
                        new JSONObject()
                                .put(
                                        "uri",
                                        copied == null
                                                ? ""
                                                : copied.toString()
                                )
                );

                return;
            }

            if ("move".equals(command)) {

                Uri source =
                        parseUri(
                                args,
                                "sourceUri"
                        );

                String sourceParentValue =
                        args == null
                                ? ""
                                : args.optString(
                                        "sourceParentUri",
                                        ""
                                );

                Uri sourceParent;

                if (sourceParentValue.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Move requires sourceParentUri"
                    );
                }

                sourceParent =
                        Uri.parse(sourceParentValue);

                Uri targetParent =
                        parseUri(
                                args,
                                "targetParentUri"
                        );

                Uri moved =
                        files.move(
                                source,
                                sourceParent,
                                targetParent
                        );

                success(
                        id,
                        new JSONObject()
                                .put(
                                        "uri",
                                        moved == null
                                                ? ""
                                                : moved.toString()
                                )
                );

                return;
            }

            if ("duplicate".equals(command)) {

                Uri source =
                        parseUri(
                                args,
                                "sourceUri"
                        );

                Uri target =
                        parseUri(
                                args,
                                "targetParentUri"
                        );

                Uri copied =
                        files.copy(
                                source,
                                target
                        );

                success(
                        id,
                        new JSONObject()
                                .put(
                                        "uri",
                                        copied == null
                                                ? ""
                                                : copied.toString()
                                )
                                .put("duplicated", true)
                );

                return;
            }

            if ("sha256".equals(command)) {

                Uri uri =
                        parseUri(
                                args,
                                "uri"
                        );

                String hash =
                        files.sha256(uri);

                success(
                        id,
                        new JSONObject()
                                .put(
                                        "sha256",
                                        hash
                                )
                );

                return;
            }


            if ("updateAppInfo".equals(command)) {
                success(id, updates.appInfo());
                return;
            }

            if ("updateCheck".equals(command)) {
                String url = args == null
                        ? ""
                        : args.optString("url", "");

                success(id, updates.check(url));
                return;
            }

            if ("updateInstall".equals(command)) {
                String apkUrl = args == null
                        ? ""
                        : args.optString("apkUrl", "");

                String sha256 = args == null
                        ? ""
                        : args.optString("sha256", "");

                success(
                        id,
                        updates.install(apkUrl, sha256)
                );
                return;
            }

            if ("openInstallSettings".equals(command)) {
                updates.openInstallSettings();

                success(
                        id,
                        new JSONObject().put("opened", true)
                );
                return;
            }

            error(
                    id,
                    "Unsupported command: " + command
            );

        } catch (Exception e) {

            error(
                    id,
                    safeMessage(e)
            );
        }
    }

    private Uri parseUri(
            JSONObject args,
            String key
    ) {

        if (args == null) {
            throw new IllegalArgumentException(
                    "Missing arguments"
            );
        }

        String value =
                args.optString(
                        key,
                        ""
                );

        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing URI: " + key
            );
        }

        return Uri.parse(value);
    }

    private void success(
            String id,
            Object payload
    ) {

        if (callback != null) {
            callback.success(
                    id,
                    payload
            );
        }
    }

    private void error(
            String id,
            String message
    ) {

        if (callback != null) {
            callback.error(
                    id,
                    message
            );
        }
    }

    private String safeMessage(Exception e) {

        String message =
                e.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            return e.getClass()
                    .getSimpleName();
        }

        return message;
    }

    public void shutdown() {

        executor.shutdownNow();
    }
}
