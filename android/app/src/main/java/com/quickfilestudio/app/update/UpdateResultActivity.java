package com.quickfilestudio.app.update;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.widget.Toast;

public final class UpdateResultActivity extends Activity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        Intent intent = getIntent();

        int status = intent.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
        );

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {

            Intent confirmIntent =
                    intent.getParcelableExtra(Intent.EXTRA_INTENT);

            if (confirmIntent != null) {
                try {
                    startActivity(confirmIntent);
                } catch (Exception e) {
                    Toast.makeText(
                            this,
                            "تعذر فتح تأكيد التثبيت: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            } else {
                Toast.makeText(
                        this,
                        "يحتاج Android إلى تأكيد تثبيت التحديث",
                        Toast.LENGTH_LONG
                ).show();
            }

            finish();
            return;
        }

        if (status == PackageInstaller.STATUS_SUCCESS) {

            Intent launch = getPackageManager()
                    .getLaunchIntentForPackage(getPackageName());

            if (launch != null) {
                launch.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                );

                try {
                    startActivity(launch);
                } catch (Exception ignored) {
                }
            }

            finish();
            return;
        }

        String message = intent.getStringExtra(
                PackageInstaller.EXTRA_STATUS_MESSAGE
        );

        Toast.makeText(
                this,
                message == null || message.isEmpty()
                        ? "فشل تثبيت التحديث"
                        : "فشل التحديث: " + message,
                Toast.LENGTH_LONG
        ).show();

        finish();
    }
}
