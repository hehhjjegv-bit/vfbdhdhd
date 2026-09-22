package com.quickfilestudio.app.storage;
import java.io.IOException;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class SafFileManager {

    private final Context context;
    private final ContentResolver resolver;

    public SafFileManager(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    public JSONArray list(Uri directory) throws Exception {
        if (directory == null) {
            throw new IllegalArgumentException("Directory URI is empty");
        }

        Uri children = DocumentsContract
            .buildChildDocumentsUriUsingTree(
                directory,
                DocumentsContract.getTreeDocumentId(directory)
            );

        JSONArray result = new JSONArray();
        Cursor cursor = null;

        try {
            cursor = resolver.query(
                children,
                new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_FLAGS
                },
                null,
                null,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC"
            );

            if (cursor == null) {
                return result;
            }

            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String name = cursor.getString(1);
                String mime = cursor.getString(2);

                long size = cursor.isNull(3)
                    ? 0
                    : cursor.getLong(3);

                long modified = cursor.isNull(4)
                    ? 0
                    : cursor.getLong(4);

                int flags = cursor.isNull(5)
                    ? 0
                    : cursor.getInt(5);

                Uri childUri = DocumentsContract.buildDocumentUriUsingTree(
                    directory,
                    id
                );

                boolean directoryItem =
                    DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);

                JSONObject item = new JSONObject();

                item.put("name", name == null ? "" : name);
                item.put("mime", mime == null ? "" : mime);
                item.put("size", size);
                item.put("modified", modified);
                item.put("dir", directoryItem);
                item.put("directory", directoryItem);
                item.put("documentId", id);
                item.put("uri", childUri.toString());
                item.put("flags", flags);

                item.put(
                    "canWrite",
                    (flags & DocumentsContract.Document.FLAG_SUPPORTS_WRITE) != 0
                );

                item.put(
                    "canDelete",
                    (flags & DocumentsContract.Document.FLAG_SUPPORTS_DELETE) != 0
                );

                item.put(
                    "canRename",
                    (flags & DocumentsContract.Document.FLAG_SUPPORTS_RENAME) != 0
                );

                item.put(
                    "canCopy",
                    (flags & DocumentsContract.Document.FLAG_SUPPORTS_COPY) != 0
                );

                item.put(
                    "canMove",
                    (flags & DocumentsContract.Document.FLAG_SUPPORTS_MOVE) != 0
                );

                result.put(item);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }

    public String read(Uri file) throws Exception {

        if (file == null) {
            throw new IllegalArgumentException("File URI is empty");
        }

        InputStream input = null;

        try {
            input = resolver.openInputStream(file);

            if (input == null) {
                throw new IllegalStateException(
                        "Unable to open file"
                );
            }

            return StreamUtils.readUtf8(input);

        } finally {
            StreamUtils.closeQuietly(input);
        }
    }

    public void write(Uri file, String content) throws Exception {

        if (file == null) {
            throw new IllegalArgumentException("File URI is empty");
        }

        OutputStream output = null;

        try {
            output = resolver.openOutputStream(file, "wt");

            if (output == null) {
                throw new IllegalStateException(
                        "Unable to open file for writing"
                );
            }

            StreamUtils.writeUtf8(output, content);

        } finally {
            StreamUtils.closeQuietly(output);
        }
    }

    public Uri createFile(
            Uri parent,
            String mime,
            String name
    ) throws Exception {

        if (parent == null) {
            throw new IllegalArgumentException("Parent URI is empty");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("File name is empty");
        }

        if (mime == null || mime.trim().isEmpty()) {
            mime = "text/plain";
        }

        return DocumentsContract.createDocument(
                resolver,
                parent,
                mime,
                name
        );
    }

    public Uri createFolder(
            Uri parent,
            String name
    ) throws Exception {

        if (parent == null) {
            throw new IllegalArgumentException("Parent URI is empty");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name is empty");
        }

        return DocumentsContract.createDocument(
                resolver,
                parent,
                DocumentsContract.Document.MIME_TYPE_DIR,
                name
        );
    }

    public Uri rename(
            Uri file,
            String newName
    ) throws Exception {

        if (file == null) {
            throw new IllegalArgumentException("File URI is empty");
        }

        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("New name is empty");
        }

        return DocumentsContract.renameDocument(
                resolver,
                file,
                newName
        );
    }

    public void delete(Uri file) throws Exception {

        if (file == null) {
            throw new IllegalArgumentException("File URI is empty");
        }

        if (!DocumentsContract.deleteDocument(
                resolver,
                file
        )) {
            throw new IllegalStateException(
                    "Delete operation failed"
            );
        }
    }

    public Uri copy(
            Uri source,
            Uri targetDirectory
    ) throws Exception {

        if (source == null) {
            throw new IllegalArgumentException("Source URI is empty");
        }

        if (targetDirectory == null) {
            throw new IllegalArgumentException(
                    "Target directory URI is empty"
            );
        }

        if (android.os.Build.VERSION.SDK_INT < 24) {
            throw new UnsupportedOperationException(
                    "Copy requires Android 7.0 or newer"
            );
        }

        return DocumentsContract.copyDocument(
                resolver,
                source,
                targetDirectory
        );
    }

    public Uri move(
            Uri source,
            Uri sourceParent,
            Uri targetParent
    ) throws Exception {

        if (android.os.Build.VERSION.SDK_INT < 24) {
            throw new UnsupportedOperationException(
                    "Move requires Android 7.0 or newer"
            );
        }

        return DocumentsContract.moveDocument(
                resolver,
                source,
                sourceParent,
                targetParent
        );
    }

    public Uri zip(Uri source) throws Exception {
        if (source == null) {
            throw new IllegalArgumentException("ZIP source is required");
        }

        String sourceName = getDisplayName(source);
        if (sourceName == null || sourceName.trim().isEmpty()) {
            sourceName = "archive";
        }

        String zipName = sourceName.endsWith(".zip")
                ? sourceName
                : sourceName + ".zip";

        Uri parent;
        String documentId;

        if (DocumentsContract.isTreeUri(source)) {
            documentId = DocumentsContract.getDocumentId(source);
            parent = DocumentsContract.buildDocumentUriUsingTree(
                    source,
                    documentId
            );
        } else {
            documentId = DocumentsContract.getDocumentId(source);

            Uri tree = DocumentsContract.buildTreeDocumentUri(
                    source.getAuthority(),
                    documentId
            );

            parent = DocumentsContract.buildDocumentUriUsingTree(
                    tree,
                    documentId
            );
        }

        Uri output = DocumentsContract.createDocument(
                resolver,
                parent,
                "application/zip",
                zipName
        );

        if (output == null) {
            throw new IOException("Unable to create ZIP file");
        }

        OutputStream raw = null;
        ZipOutputStream zip = null;

        try {
            raw = resolver.openOutputStream(output, "w");

            if (raw == null) {
                throw new IOException("Unable to open ZIP output");
            }

            zip = new ZipOutputStream(
                    new java.io.BufferedOutputStream(raw)
            );

            android.database.Cursor cursor = resolver.query(
                    source,
                    new String[]{
                            DocumentsContract.Document.COLUMN_MIME_TYPE
                    },
                    null,
                    null,
                    null
            );

            boolean directory = false;

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String mime = cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        DocumentsContract.Document.COLUMN_MIME_TYPE
                                )
                        );

                        directory =
                                DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);
                    }
                } finally {
                    cursor.close();
                }
            }

            if (directory) {
                addDirectoryToZip(source, sourceName, zip);
            } else {
                addFileToZip(source, sourceName, zip);
            }

            zip.finish();
            return output;

        } finally {
            StreamUtils.closeQuietly(zip);

            if (zip == null) {
                StreamUtils.closeQuietly(raw);
            }
        }
    }

    private void addFileToZip(
            Uri file,
            String entryName,
            ZipOutputStream zip) throws Exception {

        ZipEntry entry = new ZipEntry(entryName);
        zip.putNextEntry(entry);

        InputStream input = null;

        try {
            input = resolver.openInputStream(file);

            if (input == null) {
                throw new IOException("Unable to read: " + entryName);
            }

            byte[] buffer = new byte[8192];
            int count;

            while ((count = input.read(buffer)) != -1) {
                zip.write(buffer, 0, count);
            }

        } finally {
            StreamUtils.closeQuietly(input);
            zip.closeEntry();
        }
    }

    private void addDirectoryToZip(
            Uri directory,
            String entryName,
            ZipOutputStream zip) throws Exception {

        String normalized = entryName.endsWith("/")
                ? entryName
                : entryName + "/";

        zip.putNextEntry(new ZipEntry(normalized));
        zip.closeEntry();

        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(
                directory,
                DocumentsContract.getDocumentId(directory)
        );

        android.database.Cursor cursor = resolver.query(
                children,
                new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                },
                null,
                null,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
                        + " COLLATE NOCASE ASC"
        );

        if (cursor == null) {
            return;
        }

        try {
            int idIndex = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID
            );

            int nameIndex = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
            );

            int mimeIndex = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_MIME_TYPE
            );

            while (cursor.moveToNext()) {
                String childId = cursor.getString(idIndex);
                String childName = cursor.getString(nameIndex);
                String mime = cursor.getString(mimeIndex);

                Uri child = DocumentsContract.buildDocumentUriUsingTree(
                        directory,
                        childId
                );

                String childEntry = normalized + childName;

                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    addDirectoryToZip(child, childEntry, zip);
                } else {
                    addFileToZip(child, childEntry, zip);
                }
            }

        } finally {
            cursor.close();
        }
    }

    private String getDisplayName(Uri uri) throws Exception {
        android.database.Cursor cursor = resolver.query(
                uri,
                new String[]{
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                },
                null,
                null,
                null
        );

        if (cursor == null) {
            return null;
        }

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            return cursor.getString(
                    cursor.getColumnIndexOrThrow(
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    )
            );
        } finally {
            cursor.close();
        }
    }

    public String sha256(Uri file) throws Exception {

        InputStream input = null;

        try {
            input = resolver.openInputStream(file);

            if (input == null) {
                throw new IllegalStateException(
                        "Unable to open file"
                );
            }

            return StreamUtils.sha256(input);

        } finally {
            StreamUtils.closeQuietly(input);
        }
    }
}
