package com.quickfilestudio.app.update;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;
import android.util.Base64;

public final class UpdateManager {

    public static final String PACKAGE_NAME = "com.quickfilestudio.app";
    private int getCurrentVersionCode() {
        try {
            android.content.pm.PackageInfo info =
                    context.getPackageManager().getPackageInfo(
                            context.getPackageName(),
                            0
                    );

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                return (int) info.getLongVersionCode();
            }

            return info.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getCurrentVersionName() {
        try {
            android.content.pm.PackageInfo info =
                    context.getPackageManager().getPackageInfo(
                            context.getPackageName(),
                            0
                    );

            return info.versionName == null ? "" : info.versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private static final String UPDATE_PUBLIC_CERT =
            "-----BEGIN CERTIFICATE-----\n"
            + "MIIEIjCCAoqgAwIBAgIJAPFdqEWfUUL7MA0GCSqGSIb3DQEBCwUAMD8xGjAYBgNV\n"
            + "BAoTEVF1aWNrIEZpbGUgU3R1ZGlvMSEwHwYDVQQDExhRdWljayBGaWxlIFN0dWRp\n"
            + "byBVcGRhdGUwHhcNMjYwOTIyMDIyMjU3WhcNMzYwOTE5MDIyMjU3WjA/MRowGAYD\n"
            + "VQQKExFRdWljayBGaWxlIFN0dWRpbzEhMB8GA1UEAxMYUXVpY2sgRmlsZSBTdHVk\n"
            + "aW8gVXBkYXRlMIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAmBH/Hrqy\n"
            + "Bjmh/mpI1+YJy5Rn6GAnuZcD/6pgYSrBG2VVThSferf8EouQ26N8BAf5oAkFHCBK\n"
            + "lbY+SxQ9JKs1qwf5G+WqPAL6mk3CmeEjbFYP/qR3Vn8fZ1sz2PBFYK1aHSmrk3he\n"
            + "6d/+Z/cOgdnpAKX7A8mPhA/KiKYSry1iQIqisKzSmKaC/r3P7sD1zWF9cdhOul3w\n"
            + "OeFpBJCbjof1tsbV/nBzPffwPWATVxFP3Mco48exmPT2U0NNbNx3Mn6GwQcfiBIX\n"
            + "Jqpp+UM3HPFEMO3wX6CM+/PBkTyTWuQuDDzP0OY2uGv1IZDCSFLs8WTLZurDFJhR\n"
            + "I48TMqgad273LabG1jpwD+LvKb+r+SWBfR7ntyjdsIIDiF+VWgu86iyjkl842+Cg\n"
            + "853pZ6Oe5SsS790FwzayP3RJq6P5bFn9M7cBYaK48j8XnDntN/8Jbwa+EfIpQuEi\n"
            + "J7JBxsLSTRzQlSTQ9eeBrrQhr6ioKO0+6+NrIbCsRYpBrqzvyHO93wA9AgMBAAGj\n"
            + "ITAfMB0GA1UdDgQWBBQcnS9qwvxKmqzTc7Mf4CqA1zleXjANBgkqhkiG9w0BAQsF\n"
            + "AAOCAYEAdNNbfBCoIIOKBe6epvZ2EEr6IDdQBwjrIe4Q4RnTp1UbY9NFp4z+x0O7\n"
            + "aYgpfrUpr9WWSqu61C9foKzMchF95DNnjSFi0N2egKvB+2+wx6yamjhfROCLHBZu\n"
            + "us7lKe8Xn83KEZgBoljSw0s5gXBKFT+jCT+i7wLgSM6DAxIQTA/IcNN8NvdIIIHK\n"
            + "TFc9t9nbQGj1ucAdaMdS5SvbaQQVqyCH+fevqmMUx1ae/inoIsj3oOzO5Uey6Q7m\n"
            + "qCJpD4UgPZvyv7iKxkZcQHKkpGJRIaM1INL0g1uFAhBg+cU1HOUQoHhL3LfZ43fj\n"
            + "8pUj9aopcosMAX8g4XSokVD/pWo9IniU2ysDXqzDuvwXm8e7QXjXadbwaGKrIyb9\n"
            + "LYbpnmjzubtMvemafzpYJbrtH0SuRGOCKFEHTlZjX5hYY19mVhMJnZJ+0dQTzBK9\n"
            + "W0d6+RGgyrvpxtXw2LtXTUjseWTq+UhErxCrDhjpOATTh13lyODnOmuT834KZzx0\n"
            + "YV/I0x5x\n"
            + "-----END CERTIFICATE-----";

    private final Context context;

    public UpdateManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public JSONObject appInfo() throws Exception {
        PackageManager pm = context.getPackageManager();

        int versionCode = getCurrentVersionCode();
        String versionName = getCurrentVersionName();

        try {
            android.content.pm.PackageInfo info =
                    pm.getPackageInfo(PACKAGE_NAME, 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = (int) info.getLongVersionCode();
            } else {
                versionCode = info.versionCode;
            }

            if (info.versionName != null) {
                versionName = info.versionName;
            }
        } catch (Exception ignored) {
        }

        return new JSONObject()
                .put("packageName", PACKAGE_NAME)
                .put("versionCode", versionCode)
                .put("versionName", versionName);
    }

    public JSONObject check(String updateUrl) throws Exception {
        if (updateUrl == null || updateUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("رابط التحديث فارغ");
        }

        HttpURLConnection connection = null;

        try {
            String separator = updateUrl.contains("?") ? "&" : "?";

            URL url = new URL(
                    updateUrl + separator + "t=" + System.currentTimeMillis()
            );

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty(
                    "User-Agent",
                    "QuickFileStudio-Updater/1.0"
            );

            int responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode >= 300) {
                throw new Exception(
                        "خادم التحديث غير متاح: HTTP " + responseCode
                );
            }

            StringBuilder jsonText = new StringBuilder();

            try (InputStream in =
                         new BufferedInputStream(connection.getInputStream())) {

                byte[] buffer = new byte[8192];
                int count;

                while ((count = in.read(buffer)) != -1) {
                    jsonText.append(
                            new String(
                                    buffer,
                                    0,
                                    count,
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    );
                }
            }

            JSONObject remote = new JSONObject(jsonText.toString());

            int currentCode = getCurrentVersionCode();
            int remoteCode = remote.optInt("versionCode", 0);

            String packageName = remote.optString(
                    "packageName",
                    PACKAGE_NAME
            );

            if (!PACKAGE_NAME.equals(packageName)) {
                throw new Exception(
                        "حزمة التحديث لا تطابق Quick File Studio"
                );
            }

            if (remoteCode <= currentCode) {
                return new JSONObject()
                        .put("available", false)
                        .put("currentVersionCode", currentCode)
                        .put("currentVersionName", getCurrentVersionName())
                        .put("latest", remote);
            }

            String versionName = remote.optString("versionName", "").trim();
            String apkUrl = remote.optString("apkUrl", "").trim();
            String sha256Value = remote.optString("sha256", "").trim();
            String signature = remote.optString("signature", "").trim();

            if (versionName.isEmpty()) {
                throw new Exception("بيانات التحديث ناقصة: versionName");
            }

            if (apkUrl.isEmpty()) {
                throw new Exception("بيانات التحديث ناقصة: apkUrl");
            }

            if (sha256Value.isEmpty()) {
                throw new Exception("بيانات التحديث ناقصة: sha256");
            }

            if (signature.isEmpty()) {
                throw new Exception("التحديث غير موقع رقمياً");
            }

            String signedData = buildSignedData(
                    packageName,
                    remoteCode,
                    versionName,
                    apkUrl,
                    sha256Value
            );

            if (!verifyUpdateSignature(signedData, signature)) {
                throw new Exception("فشل التحقق من توقيع التحديث");
            }

            return new JSONObject()
                    .put("available", true)
                    .put("currentVersionCode", currentCode)
                    .put("currentVersionName", getCurrentVersionName())
                    .put("latest", remote);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public JSONObject install(
            String apkUrl,
            String expectedSha256
    ) throws Exception {

        if (apkUrl == null || apkUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("رابط APK فارغ");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.getPackageManager().canRequestPackageInstalls()) {

            return new JSONObject()
                    .put("ok", false)
                    .put("needsInstallPermission", true);
        }

        File apk = new File(
                context.getCacheDir(),
                "qfs-update.apk"
        );

        if (apk.exists()) {
            // لا نستخدم ملفًا قديمًا إذا فشل تنزيل جديد.
            // سيتم استبداله أثناء التنزيل.
            apk.delete();
        }

        download(apkUrl, apk);

        if (!apk.exists() || apk.length() < 1024) {
            throw new Exception("ملف APK غير صالح أو ناقص");
        }

        if (expectedSha256 != null &&
                !expectedSha256.trim().isEmpty()) {

            String actual = sha256(apk);

            if (!actual.equalsIgnoreCase(expectedSha256.trim())) {
                apk.delete();
                throw new Exception(
                        "فشل التحقق من SHA-256 للتحديث"
                );
            }
        }

        installPackage(apk);

        return new JSONObject()
                .put("ok", true)
                .put("installing", true);
    }

    private void download(
            String apkUrl,
            File destination
    ) throws Exception {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(apkUrl);

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setRequestProperty(
                    "User-Agent",
                    "QuickFileStudio-Updater/1.0"
            );

            int responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode >= 300) {
                throw new Exception(
                        "فشل تنزيل APK: HTTP " + responseCode
                );
            }

            try (
                    InputStream in =
                            new BufferedInputStream(
                                    connection.getInputStream()
                            );
                    FileOutputStream out =
                            new FileOutputStream(destination)
            ) {
                byte[] buffer = new byte[64 * 1024];
                int count;

                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }

                out.flush();
            }

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void installPackage(File apk) throws Exception {

        PackageInstaller installer =
                context.getPackageManager().getPackageInstaller();

        PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL
                );

        int sessionId = installer.createSession(params);

        PackageInstaller.Session session =
                installer.openSession(sessionId);

        try {
            try (
                    InputStream in =
                            new BufferedInputStream(
                                    new FileInputStream(apk)
                            );
                    java.io.OutputStream out =
                            session.openWrite(
                                    "base.apk",
                                    0,
                                    apk.length()
                            )
            ) {
                byte[] buffer = new byte[64 * 1024];
                int count;

                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }

                out.flush();
            }

            Intent resultIntent =
                    new Intent(
                            context,
                            UpdateResultActivity.class
                    );

            resultIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent =
                    PendingIntent.getActivity(
                            context,
                            sessionId,
                            resultIntent,
                            flags
                    );

            session.commit(
                    pendingIntent.getIntentSender()
            );

        } catch (Exception e) {
            try {
                session.abandon();
            } catch (Exception ignored) {
            }

            throw e;

        } finally {
            session.close();
        }
    }

    private String buildSignedData(
            String packageName,
            int versionCode,
            String versionName,
            String apkUrl,
            String sha256
    ) {
        return "QFS-UPDATE-V1\n"
                + packageName + "\n"
                + versionCode + "\n"
                + versionName + "\n"
                + apkUrl + "\n"
                + sha256.toLowerCase(java.util.Locale.US);
    }

    private boolean verifyUpdateSignature(
            String signedData,
            String signatureBase64
    ) throws Exception {
        CertificateFactory factory =
                CertificateFactory.getInstance("X.509");

        X509Certificate certificate =
                (X509Certificate) factory.generateCertificate(
                        new ByteArrayInputStream(
                                UPDATE_PUBLIC_CERT.getBytes(
                                        java.nio.charset.StandardCharsets.US_ASCII
                                )
                        )
                );

        PublicKey publicKey = certificate.getPublicKey();

        Signature verifier =
                Signature.getInstance("SHA256withRSA");

        verifier.initVerify(publicKey);

        verifier.update(
                signedData.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                )
        );

        byte[] signatureBytes =
                Base64.decode(
                        signatureBase64,
                        Base64.DEFAULT
                );

        return verifier.verify(signatureBytes);
    }

    private String sha256(File file) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        try (
                InputStream in =
                        new BufferedInputStream(
                                new FileInputStream(file)
                        )
        ) {
            byte[] buffer = new byte[8192];
            int count;

            while ((count = in.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }

        StringBuilder result = new StringBuilder();

        for (byte b : digest.digest()) {
            result.append(
                    String.format("%02x", b)
            );
        }

        return result.toString();
    }

    public void openInstallSettings() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(
                            "package:" +
                            context.getPackageName()
                    )
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);
        }
    }
}
