package com.poi.qingscreen;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.text.TextUtils;

public final class Util {
    private Util() {}

    /** 读系统设置里已启用的无障碍服务列表，判断本服务是否仍在位 */
    public static boolean isAccessibilityEnabled(Context c) {
        String enabled = Settings.Secure.getString(
                c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        String me = c.getPackageName();
        for (String part : enabled.split(":")) {
            if (part != null && part.startsWith(me + "/")) return true;
        }
        return false;
    }

    /** 包名换成应用名；查不到就退回包名 */
    public static String appLabel(Context c, String pkg) {
        try {
            PackageManager pm = c.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            return pkg;
        }
    }
}
