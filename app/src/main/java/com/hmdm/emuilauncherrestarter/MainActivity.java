package com.hmdm.emuilauncherrestarter;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = "LauncherRestarter";
    private final String LEGACY_LAUNCHER_PACKAGE_ID = "ru.headwind.kiosk";
    private final String LAUNCHER_PACKAGE_ID = "com.hmdm.launcher";
    private int restartAttempts = 60;
    private final String LAUNCHER_OLD_VERSION = "oldVersion";
    private String oldLauncherVersion;
    private String packageId;

    Handler handler = new Handler();
    Handler guaranteedStartHandler = new Handler();

    Runnable callHome = new Runnable() {

        @Override
        public void run() {
            PackageManager packageManager = getPackageManager();
            packageId = LAUNCHER_PACKAGE_ID;
            Intent intent = packageManager.getLaunchIntentForPackage(LAUNCHER_PACKAGE_ID);
            if (intent == null) {
                packageId = LEGACY_LAUNCHER_PACKAGE_ID;
                intent = packageManager.getLaunchIntentForPackage(LEGACY_LAUNCHER_PACKAGE_ID);
            }
            if (intent == null) {
                // Not installed yet
                Log.i(LOG_TAG, "Launcher not yet found, attempts left: " + restartAttempts);
                retry();
                return;
            }

            // Check if it is already updated
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(packageId, 0);
                if (packageInfo != null && oldLauncherVersion != null && oldLauncherVersion.equals(packageInfo.versionName)) {
                    // Old version is not even terminated!
                    Log.i(LOG_TAG, "Old launcher found, version: " + packageInfo.versionName);
                    retry();
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Race condition? Let's retry
                Log.i(LOG_TAG, "Launcher not yet found, attempts left: " + restartAttempts);
                retry();
                return;
            }

            intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            guaranteedStart(intent);
        }
    };

    private void guaranteedStart(final Intent intent) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.i(LOG_TAG, "Starting Headwind MDM Launcher!");
                startActivity(intent);
            }
        };

        // Delay and re-send intent after 3, 10 and 30 sec to ensure launcher is running
        guaranteedStartHandler.postDelayed(runnable, 3000);
        guaranteedStartHandler.postDelayed(runnable, 10000);
        guaranteedStartHandler.postDelayed(runnable, 30000);
    }

    private void retry() {
        restartAttempts--;
        if (restartAttempts > 0) {
            restartHandler();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        oldLauncherVersion = getIntent().getStringExtra(LAUNCHER_OLD_VERSION);
        finish();
        restartHandler();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        oldLauncherVersion = intent.getStringExtra(LAUNCHER_OLD_VERSION);
        restartHandler();
    }

    private void restartHandler() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        Log.i(LOG_TAG, "Posting delayed launcher start within 1000 msec");
        handler.postDelayed(callHome, 1000);
    }

}
