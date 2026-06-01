package io.github.parsecbubbleinput;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.provider.Settings;

final class PermissionHelper {
    private PermissionHelper() {
    }

    static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        if (Settings.canDrawOverlays(context)) return true;

        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            ApplicationInfo appInfo = context.getApplicationInfo();
            if (appOps == null || appInfo == null) return false;

            int mode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mode = appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                        appInfo.uid,
                        context.getPackageName()
                );
            } else {
                mode = appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                        appInfo.uid,
                        context.getPackageName()
                );
            }
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception ignored) {
            return false;
        }
    }
}
