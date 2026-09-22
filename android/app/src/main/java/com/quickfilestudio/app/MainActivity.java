package com.quickfilestudio.app;
import android.os.Handler;
import android.os.Looper;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;
import android.widget.Toast;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.quickfilestudio.app.bridge.AndroidBridge;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class MainActivity extends Activity {

    private static final int REQUEST_STORAGE = 9101;
    private static final int REQUEST_TREE = 9102;
    private static final int REQUEST_FILE = 9103;
    private static final int REQUEST_USER_FILES_TREE = 9104;

    private static final String PREFS = "qfs_storage";
    private static final String TREE_URI = "tree_uri";
    private static final String USER_FILES_TREE_URI = "user_files_tree_uri";

    private WebView webView;
    private SharedPreferences prefs;

    private String pendingFolderCallback = "";
    private String pendingUserFilesFolderCallback = "";
    private String pendingFileCallback = "";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        setupWebView();

        setContentView(webView);

        // Android 10 and below: request legacy storage permission.
        // Real file/folder access is still granted through SAF.
        webView.postDelayed(() -> {
            requestStoragePermissionIfNeeded();
        }, 600);
    }

    private void setupWebView() {

        webView = new WebView(this);

        WebSettings s = webView.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);

        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        try {
            s.setAllowFileAccessFromFileURLs(true);
            s.setAllowUniversalAccessFromFileURLs(true);
        } catch (Throwable t) { /* بعض الإصدارات لا تدعمها */ }

        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        s.setJavaScriptCanOpenWindowsAutomatically(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                try {
                    String msg = cm.message() + "  [" + cm.sourceId() + ":" + cm.lineNumber() + "]";
                    android.util.Log.d("QFS_CONSOLE", msg);
                    if (cm.messageLevel() == ConsoleMessage.MessageLevel.ERROR
                        || cm.messageLevel() == ConsoleMessage.MessageLevel.WARNING) {
                        final String show = msg.length() > 300 ? msg.substring(0, 300) + "..." : msg;
                        runOnUiThread(new Runnable() {
                            @Override public void run() {
                                Toast.makeText(MainActivity.this, show, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (Throwable t) { /* تجاهل */ }
                return true;
            }
        });

        AndroidBridge bridge = new AndroidBridge(
                this,
                new AndroidBridge.Callback() {
                    @Override
                    public void success(String id, Object payload) {
                        callbackSuccess(id, payload);
                    }

                    @Override
                    public void error(String id, String message) {
                        callbackError(id, message);
                    }
                }
        );

        webView.addJavascriptInterface(bridge, "AndroidBridge");
        webView.addJavascriptInterface(bridge, "QFS");

        webView.loadUrl("file:///android_asset/www/index.html");
    }

    private void requestStoragePermissionIfNeeded() {

        if (Build.VERSION.SDK_INT >= 23 &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {

            boolean read = checkSelfPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED;

            boolean write = checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED;

            if (!read || !write) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        REQUEST_STORAGE
                );

                return;
            }
        }

        notifyPermissionResult(true);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] results) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                results
        );

        if (requestCode == REQUEST_STORAGE) {

            boolean granted = true;

            for (int result : results) {

                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            notifyPermissionResult(granted);
        }
    }

    private void notifyPermissionResult(boolean granted) {

        if (webView == null) return;

        String js =
                "window.dispatchEvent(new CustomEvent('storagePermission'," +
                "{detail:" + granted + "}));";

        runOnUiThread(() ->
                webView.evaluateJavascript(js, null)
        );
    }

    public void chooseFolderFromBridge(String callbackId) {
        chooseFolder(callbackId);
    }

    public void chooseUserFilesFolderFromBridge(String callbackId) {
        chooseUserFilesFolder(callbackId);
    }

    public void chooseFileFromBridge(String callbackId) {
        chooseFile(callbackId);
    }

    private void chooseFolder(String callbackId) {

        pendingFolderCallback = callbackId;

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );

        startActivityForResult(intent, REQUEST_TREE);
    }

    private void chooseUserFilesFolder(String callbackId) {
        pendingUserFilesFolderCallback = callbackId;

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                REQUEST_USER_FILES_TREE
        );
    }

    private void chooseFile(String callbackId) {

        pendingFileCallback = callbackId;

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType("*/*");

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(intent, REQUEST_FILE);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null) {

            if (requestCode == REQUEST_TREE &&
                    !pendingFolderCallback.isEmpty()) {

                callbackError(
                        pendingFolderCallback,
                        "تم إلغاء اختيار المجلد"
                );

                pendingFolderCallback = "";
            }

            if (requestCode == REQUEST_FILE &&
                    !pendingFileCallback.isEmpty()) {

                callbackError(
                        pendingFileCallback,
                        "تم إلغاء اختيار الملف"
                );

                pendingFileCallback = "";
            }

            if (requestCode == REQUEST_USER_FILES_TREE &&
                    !pendingUserFilesFolderCallback.isEmpty()) {
                callbackError(
                        pendingUserFilesFolderCallback,
                        "تم إلغاء اختيار مجلد ملفاتك"
                );

                pendingUserFilesFolderCallback = "";
            }

            return;
        }

        Uri uri = data.getData();

        try {

            int flags =
                    data.getFlags()
                            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            if (flags != 0) {
                getContentResolver().takePersistableUriPermission(
                        uri,
                        flags
                );
            }

        } catch (Exception e) {
            android.util.Log.w(
                    "QuickFileStudio",
                    "Persistable URI permission failed",
                    e
            );
        }

        if (requestCode == REQUEST_TREE) {

            prefs.edit()
                    .putString(TREE_URI, uri.toString())
                    .apply();

            JSONObject folderResult = new JSONObject();
                    try {
                        folderResult.put("uri", uri.toString());
                        folderResult.put("name", getDocumentName(uri));
                    } catch (JSONException ignored) {}
                    callbackSuccess(pendingFolderCallback, folderResult);

            pendingFolderCallback = "";

            notifyFolderSelected(uri.toString());

        } else if (requestCode == REQUEST_USER_FILES_TREE) {
            prefs.edit()
                    .putString(
                            USER_FILES_TREE_URI,
                            uri.toString()
                    )
                    .apply();

            JSONObject folderResult = new JSONObject();

            try {
                folderResult.put(
                        "uri",
                        uri.toString()
                );

                folderResult.put(
                        "name",
                        getDocumentName(uri)
                );
            } catch (JSONException ignored) {}

            callbackSuccess(
                    pendingUserFilesFolderCallback,
                    folderResult
            );

            pendingUserFilesFolderCallback = "";

            notifyFolderSelected(
                    uri.toString()
            );

        } else if (requestCode == REQUEST_FILE) {

            JSONObject fileResult = new JSONObject();
                    try {
                        fileResult.put("uri", uri.toString());
                        fileResult.put("name", getDocumentName(uri));
                    } catch (JSONException ignored) {}
                    callbackSuccess(pendingFileCallback, fileResult);

            pendingFileCallback = "";

            notifyFileSelected(uri.toString());
        }
    }

    private void notifyFolderSelected(String uri) {

        if (webView == null) return;

        String js =
                "window.dispatchEvent(new CustomEvent(" +
                "'folderSelected',{detail:" +
                JSONObject.quote(uri) +
                "}));";

        runOnUiThread(() ->
                webView.evaluateJavascript(js, null)
        );
    }

    private void notifyFileSelected(String uri) {

        if (webView == null) return;

        String js =
                "window.dispatchEvent(new CustomEvent(" +
                "'fileSelected',{detail:" +
                JSONObject.quote(uri) +
                "}));";

        runOnUiThread(() ->
                webView.evaluateJavascript(js, null)
        );
    }

    private String savedTree() {

        return prefs.getString(TREE_URI, "");
    }

    private Uri uri(String value) {

        if (value == null || value.isEmpty()) {
            return null;
        }

        return Uri.parse(value);
    }

    private String getDocumentName(Uri uri) {

        Cursor c = null;

        try {

            c = getContentResolver().query(
                    uri,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    },
                    null,
                    null,
                    null
            );

            if (c != null && c.moveToFirst()) {

                return c.getString(0);
            }

        } catch (Exception ignored) {

        } finally {

            if (c != null) c.close();
        }

        return "ملف";
    }

    private JSONObject success() throws JSONException {

        return new JSONObject()
                .put("ok", true);
    }

    private JSONObject error(String message) throws JSONException {

        return new JSONObject()
                .put("ok", false)
                .put("error", message == null ? "خطأ غير معروف" : message);
    }

    private void callbackSuccess(
            String id,
            Object payload) {

        if (id == null || id.isEmpty()) return;

        String js =
                "window.qfsResult(" +
                JSONObject.quote(id) +
                ",true," +
                JSONObject.quote(payload.toString()) +
                ");";

        runOnUiThread(() ->
                webView.evaluateJavascript(js, null)
        );
    }

    private void callbackError(
            String id,
            String message) {

        if (id == null || id.isEmpty()) return;

        String js =
                "window.qfsResult(" +
                JSONObject.quote(id) +
                ",false," +
                JSONObject.quote(message) +
                ");";

        runOnUiThread(() ->
                webView.evaluateJavascript(js, null)
        );
    }

    private String mime(Cursor c) {

        int index =
                c.getColumnIndex(
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                );

        if (index >= 0) {

            String m = c.getString(index);

            if (m != null) return m;
        }

        return "application/octet-stream";
    }

    private boolean isDirectory(String mime) {

        return DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);
    }

    private long longColumn(
            Cursor c,
            String column) {

        int index = c.getColumnIndex(column);

        if (index < 0) return 0;

        try {
            return c.getLong(index);
        } catch (Exception e) {
            return 0;
        }
    }

    private JSONObject itemFromCursor(Cursor c) throws JSONException {

        String name =
                c.getString(
                        c.getColumnIndex(
                                DocumentsContract.Document.COLUMN_DISPLAY_NAME
                        )
                );

        String mime = mime(c);

        long size =
                longColumn(
                        c,
                        DocumentsContract.Document.COLUMN_SIZE
                );

        long modified =
                longColumn(
                        c,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED
                );

        String docId =
                c.getString(
                        c.getColumnIndex(
                                DocumentsContract.Document.COLUMN_DOCUMENT_ID
                        )
                );

        return new JSONObject()
                .put("name", name)
                .put("mime", mime)
                .put("size", size)
                .put("modified", modified)
                .put("dir", isDirectory(mime))
                .put("directory", isDirectory(mime))
                .put("documentId", docId);
    }

    public class LegacyAndroidBridge {

        private final Context context;

        LegacyAndroidBridge(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public String version() {
            return "2.0-SAF-STORAGE";
        }

        @JavascriptInterface
        public void call(String json) {

            try {

                JSONObject req =
                        new JSONObject(json);

                String command =
                        req.optString("command", "");

                String id =
                        req.optString("id", "");

                JSONObject args =
                        req.optJSONObject("args");

                if (args == null) {
                    args = new JSONObject();
                }

                handle(command, id, args);

            } catch (Exception e) {

                try {

                    JSONObject req =
                            new JSONObject(json);

                    callbackError(
                            req.optString("id", ""),
                            e.getMessage()
                    );

                } catch (Exception ignored) {}
            }
        }

        private void handle(
                String command,
                String id,
                JSONObject args) {

            new Thread(() -> {

                try {

                    switch (command) {

                        case "getWorkspace": {

                            JSONObject result =
                                    new JSONObject()
                                            .put("treeUri", savedTree())
                                            .put(
                                                    "userFilesTreeUri",
                                                    prefs.getString(
                                                            USER_FILES_TREE_URI,
                                                            ""
                                                    )
                                            )
                                            .put(
                                                    "userFilesStoragePermission",
                                                    hasUserFilesStoragePermission()
                                            )
                                            .put("storagePermission",
                                                    hasStoragePermission());

                            callbackSuccess(id, result);

                            break;
                        }

                        case "chooseFolder": {

                            runOnUiThread(() ->
                                    chooseFolder(id)
                            );

                            break;
                        }

                        case "chooseUserFilesFolder": {
                            runOnUiThread(() ->
                                    chooseUserFilesFolder(id)
                            );

                            break;
                        }

                        case "chooseFile": {

                            runOnUiThread(() ->
                                    chooseFile(id)
                            );

                            break;
                        }

                        case "list": {

                            list(id, args.optString("uri", ""));

                            break;
                        }

                        case "read": {

                            read(id, args.optString("uri", ""));

                            break;
                        }

                        case "write": {

                            write(
                                    id,
                                    args.optString("uri", ""),
                                    args.optString("content", "")
                            );

                            break;
                        }

                        case "createFile": {
                            String parent =
                                    args.optString("parent", "");

                            if (parent.isEmpty()) {
                                parent =
                                        args.optString(
                                                "parentUri",
                                                ""
                                        );
                            }

                            createFile(
                                    id,
                                    parent,
                                    args.optString("name", ""),
                                    args.optString(
                                            "mime",
                                            "text/plain"
                                    )
                            );

                            break;
                        }

                        case "createFolder": {
                            String parent =
                                    args.optString("parent", "");

                            if (parent.isEmpty()) {
                                parent =
                                        args.optString(
                                                "parentUri",
                                                ""
                                        );
                            }

                            createFolder(
                                    id,
                                    parent,
                                    args.optString("name", "")
                            );

                            break;
                        }

                        case "rename": {

                            rename(
                                    id,
                                    args.optString("uri", ""),
                                    args.optString("name", "")
                            );

                            break;
                        }

                        case "delete": {

                            delete(
                                    id,
                                    args.optString("uri", "")
                            );

                            break;
                        }

                        case "copy": {

                            copy(
                                    id,
                                    args.optString("sourceUri", ""),
                                    args.optString("targetParentUri", "")
                            );

                            break;
                        }

                        case "move": {

                            move(
                                    id,
                                    args.optString("sourceUri", ""),
                                    args.optString("targetParentUri", "")
                            );

                            break;
                        }

                        case "duplicate": {

                            copy(
                                    id,
                                    args.optString("sourceUri", ""),
                                    args.optString("targetParentUri", "")
                            );

                            break;
                        }

                        case "sha256": {

                            sha256(
                                    id,
                                    args.optString("uri", "")
                            );

                            break;
                        }

                        case "openExternal": {

                            openExternal(
                                    id,
                                    args.optString("uri", "")
                            );

                            break;
                        }

                        case "exec": {

                            exec(
                                    id,
                                    args.optString("command", "")
                            );

                            break;
                        }

                        default:

                            callbackError(
                                    id,
                                    "الأمر غير مدعوم: " + command
                            );
                    }

                } catch (Exception e) {

                    callbackError(
                            id,
                            e.getMessage()
                    );
                }

            }).start();
        }

        private boolean hasUserFilesStoragePermission() {
        String saved =
                prefs.getString(
                        USER_FILES_TREE_URI,
                        ""
                );

        if (saved == null || saved.trim().isEmpty()) {
            return false;
        }

        Uri wanted;

        try {
            wanted = Uri.parse(saved);
        } catch (Exception e) {
            return false;
        }

        for (android.content.UriPermission permission :
                getContentResolver()
                        .getPersistedUriPermissions()) {

            if (wanted.equals(permission.getUri()) &&
                    permission.isReadPermission()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasStoragePermission() {

            if (Build.VERSION.SDK_INT < 23) {
                return true;
            }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                return true;
            }

            return checkSelfPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED;
        }

        private void list(
                String id,
                String uriString) {

            Uri parent =
                    uri(uriString);

            if (parent == null) {

                parent = uri(savedTree());
            }

            if (parent == null) {

                callbackError(
                        id,
                        "لم يتم اختيار مجلد. اضغط اختيار المجلد أولاً."
                );

                return;
            }

            Cursor cursor = null;

            try {

                Uri childrenUri =
                        DocumentsContract.buildChildDocumentsUriUsingTree(
                                parent,
                                DocumentsContract.getTreeDocumentId(parent)
                        );

                cursor =
                        getContentResolver().query(
                                childrenUri,
                                new String[]{
                                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                                        DocumentsContract.Document.COLUMN_SIZE,
                                        DocumentsContract.Document.COLUMN_LAST_MODIFIED
                                },
                                null,
                                null,
                                DocumentsContract.Document.COLUMN_DISPLAY_NAME + " ASC"
                        );

                JSONArray array =
                        new JSONArray();

                if (cursor != null) {

                    while (cursor.moveToNext()) {

                        array.put(
                                itemFromCursor(cursor)
                        );
                    }
                }

                callbackSuccess(id, array);

            } catch (Exception e) {

                callbackError(
                        id,
                        "تعذر قراءة المجلد: " + e.getMessage()
                );

            } finally {

                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        private void read(
                String id,
                String uriString) {

            Uri u = uri(uriString);

            if (u == null) {

                callbackError(
                        id,
                        "مسار الملف غير صالح"
                );

                return;
            }

            try {

                InputStream in =
                        getContentResolver()
                                .openInputStream(u);

                if (in == null) {

                    callbackError(
                            id,
                            "تعذر فتح الملف"
                    );

                    return;
                }

                ByteArrayOutputStream out =
                        new ByteArrayOutputStream();

                byte[] buffer =
                        new byte[8192];

                int n;

                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }

                in.close();

                String content =
                        out.toString("UTF-8");

                callbackSuccess(
                        id,
                        content
                );

            } catch (Exception e) {

                callbackError(
                        id,
                        "تعذر قراءة الملف: " + e.getMessage()
                );
            }
        }

        private void write(
                String id,
                String uriString,
                String content) {

            Uri u = uri(uriString);

            if (u == null) {

                callbackError(
                        id,
                        "مسار الملف غير صالح"
                );

                return;
            }

            try {

                OutputStream out =
                        getContentResolver()
                                .openOutputStream(u, "wt");

                if (out == null) {

                    callbackError(
                            id,
                            "تعذر الكتابة إلى الملف"
                    );

                    return;
                }

                out.write(
                        content.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

                out.flush();
                out.close();

                callbackSuccess(
                        id,
                        success()
                );

            } catch (Exception e) {

                callbackError(
                        id,
                        "تعذر حفظ الملف: " + e.getMessage()
                );
            }
        }

        private void createFile(
                String id,
                String parentString,
                String name,
                String mimeType) {

            Uri parent = uri(parentString);

            if (parent == null) {
                parent = uri(savedTree());
            }

            if (parent == null) {

                callbackError(
                        id,
                        "اختر مجلداً أولاً"
                );

                return;
            }

            try {

                Uri result =
                        DocumentsContract.createDocument(
                                getContentResolver(),
                                parent,
                                mimeType,
                                name
                        );

                if (result == null) {

                    callbackError(
                            id,
                            "لم يتم إنشاء الملف"
                    );

                    return;
                }

                callbackSuccess(
                        id,
                        new JSONObject()
                                .put("uri", result.toString())
                                .put("name", name)
                );

            } catch (Exception e) {

                callbackError(
                        id,
                        "فشل إنشاء الملف: " + e.getMessage()
                );
            }
        }

        private void createFolder(
                String id,
                String parentString,
                String name) {

            Uri parent = uri(parentString);

            if (parent == null) {
                parent = uri(savedTree());
            }

            if (parent == null) {

                callbackError(
                        id,
                        "اختر مجلداً أولاً"
                );

                return;
            }

            try {

                Uri result =
                        DocumentsContract.createDocument(
                                getContentResolver(),
                                parent,
                                DocumentsContract.Document.MIME_TYPE_DIR,
                                name
                        );

                if (result == null) {

                    callbackError(
                            id,
                            "لم يتم إنشاء المجلد"
                    );

                    return;
                }

                callbackSuccess(
                        id,
                        new JSONObject()
                                .put("uri", result.toString())
                                .put("name", name)
                );

            } catch (Exception e) {

                callbackError(
                        id,
                        "فشل إنشاء المجلد: " + e.getMessage()
                );
            }
        }

        private void rename(
                String id,
                String uriString,
                String newName) {

            Uri u = uri(uriString);

            if (u == null) {

                callbackError(
                        id,
                        "URI غير صالح"
                );

                return;
            }

            try {

                Uri result =
                        DocumentsContract.renameDocument(
                                getContentResolver(),
                                u,
                                newName
                        );

                if (result == null) {

                    callbackError(
                            id,
                            "فشلت إعادة التسمية"
                    );

                    return;
                }

                callbackSuccess(
                        id,
                        new JSONObject()
                                .put("uri", result.toString())
                                .put("name", newName)
                );

            } catch (Exception e) {

                callbackError(
                        id,
                        "فشلت إعادة التسمية: " + e.getMessage()
                );
            }
        }

        private void delete(
                String id,
                String uriString) {

            Uri u = uri(uriString);

            if (u == null) {

                callbackError(
                        id,
                        "URI غير صالح"
                );

                return;
            }

            try {

                boolean result =
                        DocumentsContract.deleteDocument(
                                getContentResolver(),
                                u
                        );

                if (!result) {

                    callbackError(
                            id,
                            "لم يتم حذف العنصر"
                    );

                    return;
                }

                callbackSuccess(
                        id,
                        success()
                );

            } catch (Exception e) {

                callbackError(
                        id,
                        "فشل الحذف: " + e.getMessage()
                );
            }
        }

        private void copy(
                String id,
                String sourceString,
                String targetString) {

            Uri source = uri(sourceString);
            Uri target = uri(targetString);

            if (source == null || target == null) {

                callbackError(
                        id,
                        "المصدر أو الوجهة غير صالحة"
                );

                return;
            }

            try {

                Uri result =
                        DocumentsContract.copyDocument(
                                getContentResolver(),
                                source,
                                target
                        );

                if (result == null) {

                    callbackError(
                            id,
                            "النظام لم يسمح بالنسخ"
                    );

                    return;
                }

                callbackSuccess(
                        id,
                        new JSONObject()
                                .put("uri", result.toString())
                );

            } catch (Exception e) {

                callbackError(
                        id,
                        "فشل النسخ: " + e.getMessage()
                );
            }
        }

        private void move(
                String id,
                String sourceString,
                String targetString) {

            Uri source = uri(sourceString);
            Uri target = uri(targetString);

            if (source == null || target == null) {

                callbackError(
                        id,
                        "المصدر أو الوجهة غير صالحة"
                );

                return;
            }

            try {

                Uri result =
                        DocumentsContract.moveDocument(
                                getContentResolver(),
                                source,
                                null,
                                target
                        );

                if (result == null) {

                    callbackError(
                            id,
                            "النظام لم يسمح بالنقل"
                    );

                    return;
                }

                callbackSuccess(
                        id,
                        new JSONObject()
                                .put("uri", result.toString())
                );

            } catch (Exception e) {

                callbackError(
                        id,
                        "فشل النقل: " + e.getMessage()
                );
            }
        }

        private void sha256(
                String id,
                String uriString) {

            Uri u = uri(uriString);

            if (u == null) {

                callbackError(
                        id,
                        "URI غير صالح"
                );

                return;
            }

            try {

                InputStream in =
                        getContentResolver()
                                .openInputStream(u);

                if (in == null) {

                    callbackError(
                            id,
                            "تعذر فتح الملف"
                    );

                    return;
                }

                MessageDigest digest =
                        MessageDigest.getInstance("SHA-256");

                byte[] buffer =
                        new byte[8192];

                int n;

                while ((n = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, n);
                }

                in.close();

                byte[] hash =
                        digest.digest();

                StringBuilder result =
                        new StringBuilder();

                for (byte b : hash) {

                    result.append(
                            String.format(
                                    "%02x",
                                    b
                            )
                    );
                }

                callbackSuccess(
                        id,
                        result.toString()
                );

            } catch (Exception e) {

                callbackError(
                        id,
                        "فشل حساب SHA-256: " + e.getMessage()
                );
            }
        }

        private void openExternal(
                String id,
                String uriString) {

            Uri u = uri(uriString);

            if (u == null) {

                callbackError(
                        id,
                        "URI غير صالح"
                );

                return;
            }

            try {

                String type =
                        getContentResolver()
                                .getType(u);

                if (type == null) {
                    type = "*/*";
                }

                Intent intent =
                        new Intent(
                                Intent.ACTION_VIEW,
                                u
                        );

                intent.setDataAndType(u, type);

                intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

                startActivity(intent);

                callbackSuccess(
                        id,
                        success()
                );

            } catch (ActivityNotFoundException e) {

                callbackError(
                        id,
                        "لا يوجد تطبيق يمكنه فتح هذا الملف"
                );

            } catch (Exception e) {

                callbackError(
                        id,
                        "تعذر فتح الملف: " + e.getMessage()
                );
            }
        }

        public void execFromBridge(String id, String command, int timeoutMs) {
        if (command == null || command.trim().isEmpty()) {
            callbackError(id, "الأمر فارغ");
            return;
        }

        final int safeTimeout = Math.max(1000, Math.min(timeoutMs, 30000));

        new Thread(() -> {
            Process process = null;

            try {
                process = new ProcessBuilder(
                        "/system/bin/sh",
                        "-c",
                        command
                ).redirectErrorStream(false).start();

                InputStream stdout = process.getInputStream();
                InputStream stderr = process.getErrorStream();

                java.util.concurrent.Future<String> outFuture =
                        java.util.concurrent.Executors.newSingleThreadExecutor()
                                .submit(() -> readStream(stdout));

                java.util.concurrent.Future<String> errFuture =
                        java.util.concurrent.Executors.newSingleThreadExecutor()
                                .submit(() -> readStream(stderr));

                boolean finished = process.waitFor(
                        safeTimeout,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                );

                if (!finished) {
                    process.destroy();
                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }

                    JSONObject timeoutResult = new JSONObject()
                            .put("output", "")
                            .put("error", "انتهت مهلة تنفيذ الأمر")
                            .put("exitCode", -1);

                    runOnUiThread(() -> callbackSuccess(id, timeoutResult));
                    return;
                }

                String out = outFuture.get();
                String err = errFuture.get();

                JSONObject result = new JSONObject()
                        .put("output", out)
                        .put("error", err)
                        .put("exitCode", process.exitValue());

                runOnUiThread(() -> callbackSuccess(id, result));

            } catch (Exception e) {
                runOnUiThread(() ->
                        callbackError(id, "فشل تنفيذ الأمر: " + e.getMessage())
                );
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }, "QFS-Exec").start();
    }

    private void exec(
                String id,
                String command) {

            if (command == null ||
                    command.trim().isEmpty()) {

                callbackError(
                        id,
                        "الأمر فارغ"
                );

                return;
            }

            try {

                Process process =
                        new ProcessBuilder(
                                "/system/bin/sh",
                                "-c",
                                command
                        )
                        .redirectErrorStream(false)
                        .start();

                InputStream stdout =
                        process.getInputStream();

                InputStream stderr =
                        process.getErrorStream();

                String out =
                        readStream(stdout);

                String err =
                        readStream(stderr);

                int code =
                        process.waitFor();

                JSONObject result =
                        new JSONObject()
                                .put("stdout", out)
                                .put("stderr", err)
                                .put("exitCode", code);

                callbackSuccess(
                        id,
                        result
                );

            } catch (Exception e) {

                callbackError(
                        id,
                        "فشل تنفيذ الأمر: " + e.getMessage()
                );
            }
        }

        private String readStream(
                InputStream in)
                throws Exception {

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            byte[] buffer =
                    new byte[8192];

            int n;

            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }

            return out.toString("UTF-8");
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.removeJavascriptInterface(
                    "AndroidBridge"
            );

            webView.removeJavascriptInterface(
                    "QFS"
            );

            webView.destroy();
        }

        super.onDestroy();
    }

    public void execFromBridge(final String id, final String command, final int timeout) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                int exitCode = -1;
                String stdout = "";
                String stderr = "";
                try {
                    ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
                    final Process process = pb.start();
                    final StringBuilder outBuf = new StringBuilder();
                    final StringBuilder errBuf = new StringBuilder();

                    Thread outReader = new Thread(() -> {
                        try (java.io.BufferedReader r = new java.io.BufferedReader(
                                new java.io.InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = r.readLine()) != null) outBuf.append(line).append('\n');
                        } catch (Exception ignored) {}
                    });
                    Thread errReader = new Thread(() -> {
                        try (java.io.BufferedReader r = new java.io.BufferedReader(
                                new java.io.InputStreamReader(process.getErrorStream()))) {
                            String line;
                            while ((line = r.readLine()) != null) errBuf.append(line).append('\n');
                        } catch (Exception ignored) {}
                    });
                    outReader.start();
                    errReader.start();

                    final long deadline = System.currentTimeMillis() + timeout;
                    while (System.currentTimeMillis() < deadline) {
                        try {
                            exitCode = process.exitValue();
                            break;
                        } catch (IllegalThreadStateException stillRunning) {
                            Thread.sleep(50);
                        }
                    }
                    if (exitCode == -1 && process.isAlive()) {
                        process.destroy();
                        stderr = "Command timed out after " + timeout + " ms";
                    }
                    outReader.join(500);
                    errReader.join(500);
                    stdout = outBuf.toString();
                    stderr = errBuf.toString().isEmpty() ? stderr : errBuf.toString();
                } catch (Exception e) {
                    stderr = e.getMessage() == null ? e.toString() : e.getMessage();
                }
                final int finalExit = exitCode;
                final String finalOut = stdout;
                final String finalErr = stderr;
                mainHandler.post(() -> {
                    try {
                            org.json.JSONObject res = new org.json.JSONObject();
                            res.put("exitCode", finalExit);
                            res.put("stdout", finalOut);
                            res.put("stderr", finalErr);
                            if (finalExit == 0) {
                                callbackSuccess(id, res);
                            } else {
                                callbackError(id,
                                    (finalErr == null || finalErr.isEmpty())
                                        ? ("exit code " + finalExit)
                                        : finalErr);
                            }
                        } catch (Exception ex) {
                            callbackError(id, ex.toString());
                        }
                });
            }
        }).start();
    }
}
