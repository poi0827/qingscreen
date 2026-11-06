package com.poi.qingscreen;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 核心：监听窗口变化，在当前窗口控件树里找「跳过/关闭广告」按钮并自动点掉。
 * 纯本机逻辑，不联网；按应用把拦截次数记进 StatsStore。
 */
public class ClearAccessibilityService extends AccessibilityService {

    private static final String TAG = "QingScreen";
    private static final long DEDUP_MS = 1500; // 同一 App 1.5 秒内只算一次，防重复计数

    private static final Set<String> SKIP_WORDS = new HashSet<>(Arrays.asList(
            "跳过", "跳过广告", "关闭广告", "点击跳过", "跳过 广告", "跳过广告 >",
            "skip", "skip ad", "skip ads", "跳过此广告", "关闭此广告",
            "广告剩余", "跳过广告 ", "跳过登入广告", "跳过该广告"
    ));

    private final Map<String, Long> lastHit = new HashMap<>();

    @Override
    public void onServiceConnected() {
        Log.i(TAG, "service connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return;

        CharSequence p = event.getPackageName();
        String pkg = p == null ? null : p.toString();
        if (pkg == null || pkg.isEmpty()) return;
        if (pkg.equals(getPackageName())) return;   // 不拦自己
        if (isLauncher(pkg)) return;                // 不拦桌面

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            AccessibilityNodeInfo target = findSkipNode(root);
            if (target == null) return;
            long now = System.currentTimeMillis();
            Long last = lastHit.get(pkg);
            if (last != null && now - last < DEDUP_MS) return;
            if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                lastHit.put(pkg, now);
                StatsStore.add(this, pkg);
                Log.i(TAG, "blocked splash ad in " + pkg);
            }
        } catch (Exception e) {
            Log.w(TAG, "scan error", e);
        }
    }

    /** 递归找含跳过词的控件，向上爬到最近的可点击节点 */
    private AccessibilityNodeInfo findSkipNode(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence t = node.getText();
        CharSequence d = node.getContentDescription();
        String s = (t == null ? "" : t.toString()) + "|" + (d == null ? "" : d.toString());
        String low = s.toLowerCase();
        for (String w : SKIP_WORDS) {
            if (low.contains(w.toLowerCase())) {
                AccessibilityNodeInfo c = climbToClickable(node);
                if (c != null) return c;
            }
        }
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            AccessibilityNodeInfo r = findSkipNode(child);
            if (r != null) return r;
        }
        return null;
    }

    private AccessibilityNodeInfo climbToClickable(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur = n;
        int hop = 0;
        while (cur != null && hop++ < 4) {
            if (cur.isClickable()) return cur;
            cur = cur.getParent();
        }
        return null;
    }

    private boolean isLauncher(String pkg) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo r = getPackageManager().resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
            return r != null && r.activityInfo != null && pkg.equals(r.activityInfo.packageName);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "service interrupted");
    }
}
