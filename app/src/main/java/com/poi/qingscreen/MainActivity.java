package com.poi.qingscreen;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 主界面：权限引导 + 按 App 查看拦截统计（纯代码布局，零 XML 依赖） */
public class MainActivity extends Activity {

    private TextView statusText, todayText, totalText;
    private LinearLayout listBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotifPermission();
        buildUi();
        startKeepAlive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildUi() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("清屏 · 拦开屏广告");
        title.setTextSize(24);
        title.getPaint().setFakeBoldText(true);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("本机运行 · 不联网 · 统计只存在你手机里");
        sub.setTextSize(12);
        sub.setPadding(0, dp(4), 0, dp(16));
        root.addView(sub);

        statusText = new TextView(this);
        statusText.setTextSize(15);
        statusText.setPadding(0, 0, 0, dp(12));
        root.addView(statusText);

        root.addView(makeButton("① 开启「无障碍服务」（必开）", v ->
                safeStart(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));

        root.addView(makeButton("② 关闭电池优化 / 允许后台（重要）", v -> requestBattery()));

        root.addView(makeButton("③ 自启动与后台锁定（vivo i管家）", v -> openVivoAutostart()));

        root.addView(makeButton("④ 本应用详情页（手动加白名单）", v -> openAppDetails()));

        TextView h = new TextView(this);
        h.setText("拦截统计");
        h.setTextSize(18);
        h.getPaint().setFakeBoldText(true);
        h.setPadding(0, dp(20), 0, dp(8));
        root.addView(h);

        todayText = new TextView(this);
        todayText.setTextSize(15);
        root.addView(todayText);
        totalText = new TextView(this);
        totalText.setTextSize(15);
        totalText.setPadding(0, 0, 0, dp(12));
        root.addView(totalText);

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);

        Button clear = makeButton("清空统计", v -> {
            StatsStore.clear(this);
            refresh();
            toast("已清空");
        });
        clear.setPadding(0, dp(16), 0, 0);
        root.addView(clear);

        sv.addView(root);
        setContentView(sv);
    }

    private Button makeButton(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setOnClickListener(l);
        return b;
    }

    private void refresh() {
        boolean acc = Util.isAccessibilityEnabled(this);
        boolean batt = isIgnoringBattery();
        statusText.setText("无障碍服务：" + (acc ? "✅ 已开启" : "❌ 未开启（必须手动开）")
                + "\n电池优化：" + (batt ? "✅ 已忽略" : "⚠️ 未忽略（易被清理）"));
        todayText.setText("今日拦截：" + StatsStore.todayTotal(this) + " 次");
        totalText.setText("累计拦截：" + StatsStore.total(this) + " 次");

        listBox.removeAllViews();
        java.util.List<StatsStore.Entry> list = StatsStore.all(this);
        if (list.isEmpty()) {
            TextView t = new TextView(this);
            t.setText("还没有拦截记录。开好权限后，打开任意带开屏广告的 App 试试～");
            t.setTextSize(13);
            listBox.addView(t);
            return;
        }
        for (StatsStore.Entry e : list) {
            TextView t = new TextView(this);
            t.setText("· " + Util.appLabel(this, e.pkg) + "  ——  " + e.count + " 次");
            t.setTextSize(14);
            t.setPadding(0, dp(4), 0, dp(4));
            listBox.addView(t);
        }
    }

    private boolean isIgnoringBattery() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void requestBattery() {
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            safeStart(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    private void openVivoAutostart() {
        String[][] comps = {
                {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity"},
                {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity"},
                {"com.iqoo.powersaving", "com.iqoo.powersaving.PowerSavingManagerActivity"},
                {"com.vivo.abe", "com.vivo.abe.ABESettingsActivity"},
        };
        for (String[] c : comps) {
            Intent i = new Intent();
            i.setComponent(new ComponentName(c[0], c[1]));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (safeStart(i)) return;
        }
        toast("没找到 i管家入口，已打开应用详情，请手动开启「自启动 / 允许后台」");
        openAppDetails();
    }

    private void openAppDetails() {
        safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName())));
    }

    private boolean safeStart(Intent i) {
        try {
            PackageManager pm = getPackageManager();
            if (pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY) == null) return false;
            startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void startKeepAlive() {
        try {
            Intent s = new Intent(this, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(s);
            else startService(s);
        } catch (Exception ignored) { }
    }

    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
