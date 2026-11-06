package com.poi.qingscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/** 前台常驻服务：让 OriginOS 尽量别把进程收掉。无障碍服务是系统托管的，回调由系统拉起，无需它常驻 */
public class KeepAliveService extends Service {
    private static final String CH = "qingscreen_keep";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel c = new NotificationChannel(CH, "清屏常驻", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("保持拦截服务存活");
            nm.createNotificationChannel(c);
        }
        Intent i = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 1, i, flags);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CH)
                : new Notification.Builder(this);
        Notification n = b.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("清屏正在守护")
                .setContentText("点此查看拦截统计与权限状态")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        startForeground(1, n);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // 被系统回收后尽量重建
    }

    @Override
    public IBinder onBind(Intent i) { return null; }
}
