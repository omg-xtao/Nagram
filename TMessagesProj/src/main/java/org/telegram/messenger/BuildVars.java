/*
 * This is the source code of Telegram for Android v. 7.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2020.
 */

package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

@SuppressWarnings("ConstantConditions")
public class BuildVars {

    public static final boolean IS_BILLING_UNAVAILABLE = false;
    public static boolean DEBUG_VERSION = BuildConfig.BUILD_TYPE.equals("debug");
    public static boolean DEBUG_PRIVATE_VERSION = DEBUG_VERSION;
    public static boolean LOGS_ENABLED = DEBUG_PRIVATE_VERSION;
    public static boolean USE_CLOUD_STRINGS = true;
    public static boolean NO_SCOPED_STORAGE = Build.VERSION.SDK_INT <= 29;
    public static String BUILD_VERSION_STRING = BuildConfig.BUILD_VERSION_STRING;

    public static int APP_ID = 4;
    public static String APP_HASH = "014b35b6184100b085b0d0572f9b5103";

    // SafetyNet key for Google Identity SDK, set it to empty to disable
    public static String SAFETYNET_KEY = "AIzaSyDqt8P-7F7CPCseMkOiVRgb1LY8RN1bvH8";
    public static int BUILD_VERSION; // generated
    public static String GITHUB_RELEASE_URL = "https://github.com/NextAlone/Nagram/releases";

    public static int OFFICAL_APP_ID = 4;
    public static String OFFICAL_APP_HASH = "014b35b6184100b085b0d0572f9b5103";

    public static int TGX_APP_ID = 21724;
    public static String TGX_APP_HASH = "3e0cb5efcd52300aec5994fdfc5bdc16";

    static {

        try {
            PackageInfo info = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            BUILD_VERSION = info.versionCode;
            BUILD_VERSION_STRING = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            BUILD_VERSION = BuildConfig.VERSION_CODE;
            BUILD_VERSION_STRING = BuildConfig.VERSION_NAME;
        }

        if (ApplicationLoader.applicationContext != null) {
            SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", Context.MODE_PRIVATE);
            LOGS_ENABLED = DEBUG_VERSION || sharedPreferences.getBoolean("logsEnabled", DEBUG_VERSION);
            if (LOGS_ENABLED) {
                final Thread.UncaughtExceptionHandler pastHandler = Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
                    FileLog.fatal(exception, false);
                    if (pastHandler != null) {
                        pastHandler.uncaughtException(thread, exception);
                    }
                });
            }
        }
    }

    public static boolean useInvoiceBilling() {
        return true;
    }

    public static String getSmsHash() {
        return ApplicationLoader.isStandaloneBuild() ? "w0lkcmTZkKh" : (DEBUG_VERSION ? "O2P2z+/jBpJ" : "oLeq9AcOZkT");
    }
}
