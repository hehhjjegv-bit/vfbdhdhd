package com.quickfilestudio.app.update;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;

public final class UpdateResultActivity extends Activity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        Intent intent = getIntent();

        int status = intent.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
        );

        String message = intent.getStringExtra(
                PackageInstaller.EXTRA_STATUS_MESSAGE
        );

        if (status == PackageInstaller.STATUS_SUCCESS) {

            Intent launch = getPackageManager()
                    .getLaunchIntentForPackage(
                            getPackageName()
                    );

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

        } else {
            android.widget.Toast.makeText(
                    this,
                    message == null || message.isEmpty()
                            ? "فشل تثبيت التحديث"
                            : "فشل التحديث: " + message,
                    android.widget.Toast.LENGTH_LONG
            ).show();
        }

        finish();
    }
}
