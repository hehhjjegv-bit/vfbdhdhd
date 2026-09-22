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
    public static final int CURRENT_VERSION_CODE = 121;
    public static final String CURRENT_VERSION_NAME = "1.2.1";

    private static final String UPDATE_PUBLIC_CERT =
            "-----BEGIN CERTIFICATE-----\\n"
            + "MIIEITCCAomgAwIBAgIICxABOcRgI4EwDQYJKoZIhvcNAQEMBQAwPzEaMBgGA1UEChMRUXVpY2sgRmlsZSBTdHVkaW8xITAfBgNVBAMTGFF1aWNrIEZpbGUgU3R1ZGlvIFVwZGF0ZTAeFw0yNjA5MjAxNzIzNTlaFw0zNjA5MTcxNzIzNTlaMD8xGjAYBgNVBAoTEVF1aWNrIEZpbGUgU3R1ZGlvMSEwHwYDVQQDExhRdWljayBGaWxlIFN0dWRpbyBVcGRhdGUwggGiMA0GCSqGSIb3DQEBAQUAA4IBjwAwggGKAoIBgQC0bWP8x0w921oNuIBaRR3ZV0Nmw5hLvSyiT02ceKTtNe3pmvBRnzJ/JpdaQncO7/giONAFpNWHI0WrT9+qU/EnhJarXRLanpzQHks8eR6JYR+MtfKB3sMaxF4Hv3EZy1xXFsK/KTWGgKkW8gzRqCvmQKpK2/+0EOHdN5M3lKwRAaVTENgmMrFK3L9i9ON9VHFF6F0Rq/ZP\\n"
            + "jmEPow79pRyj1XdXOo3ZOsYtwhknQ6XrTGMrdjkUlEe8j/tknOF0sJmcd6DFcLHYWd8NBfOaUwEq9x9+5BeF708+i87P43Jc014/qFbxQODIINpkDIBl/NGQMoDNbZWVUa9wpCj10ReVDpQb1ex3PNhwXDsS0s7shsrbOCotc9vNFtcWMjlWrFWeQjseF8UTZR5AaC5xNdXt2dzuSw9an+A6J66hZwruGLY6g10mqHSdpvcjj2IuiEetBKQAnGe/FvawNj20x+7KuKOYTOi2fIq/wvpdspiyfRWx54V8J2xYt15qHaJ/mvkCAwEAAaMhMB8wHQYDVR0OBBYEFII3VfIEkwZhau++1oQSuLEj1yHfMA0GCSqGSIb3DQEBDAUA\\n"
            + "A4IBgQAGWOjJWtJ50K7UmjwHyIiA+V+A7N8BkW+OcceQqOMOvFtMzghZTpvreTecZFQyRgsrVs7raZ/9LTT5vhvI4DFkcW2OCGGTQDqMCDAU4qKG8qH6TNreYX9RC4uDoV+gijzBqzmDMp1+vy5OOi87Mg0cK8agWOponUID8EohqXOkSKIJ8A+rGkh6KZpZ9Yvj14j8P5mxJ6h9yBefBZ6zFay6n9qCLtTcEQjy2vcNKNUMJ20lfp27eLdVZ8Ttc6rMzWm/Z9HwNi484Timts+T2nG14KY8UK1fheqPhMbIN1R2S+ccwCXZu9PyV/LTlvdKFa4wb6MlL+c8DWSP2dVGUDqjMnrtjxK8xEEPOzqN4m+UEmEwWg3iMZXVsb9+QSn1enT/gCR+cDiGtUILQ9lQJEZb8tHodBQUNA7z4VohKGmTSy1vCiIoj1JLcnDJlRMjZO0rFcny0bGmGtLYqwu4zdeBeFvooWAJgUwrB5HSxLJ1LqpaEaykQYzxFPmw31KYWVA=\\n"
            + "-----END CERTIFICATE-----";

    private final Context context;

    public UpdateManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public JSONObject appInfo() throws Exception {
        PackageManager pm = context.getPackageManager();

        int versionCode = CURRENT_VERSION_CODE;
        String versionName = CURRENT_VERSION_NAME;

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

            int currentCode = CURRENT_VERSION_CODE;
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
                        .put("currentVersionName", CURRENT_VERSION_NAME)
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
                    .put("currentVersionName", CURRENT_VERSION_NAME)
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
        return "QFS-UPDATE-V1\\n"
                + packageName + "\\n"
                + versionCode + "\\n"
                + versionName + "\\n"
                + apkUrl + "\\n"
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
