package com.poi.qingscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/** 开机广播：优先拉起前台服务；服务被回收时由前台通知维持，系统不允许静默重开无障碍服务 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "QingScreen";

    @Override
    public void onReceive(Context c, Intent i) {
        String a = i == null ? null : i.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(a)
                && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(a)) return;
        try {
            Intent s = new Intent(c, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                c.startForegroundService(s);
            } else {
                c.startService(s);
            }
            Log.i(TAG, "boot -> keep-alive started");
        } catch (Exception e) {
            Log.w(TAG, "boot start failed", e);
        }
    }
}
