package com.quickfilestudio.app.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class UpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (!Intent.ACTION_MY_PACKAGE_REPLACED.equals(
                intent.getAction()
        )) {
            return;
        }

        Intent launch =
                context.getPackageManager()
                        .getLaunchIntentForPackage(
                                context.getPackageName()
                        );

        if (launch != null) {

            launch.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            try {
                context.startActivity(launch);
            } catch (Exception ignored) {
            }
        }
    }
}
