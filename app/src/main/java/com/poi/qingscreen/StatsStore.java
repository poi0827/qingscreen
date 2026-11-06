package com.poi.qingscreen;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 按应用统计拦截次数，纯 SharedPreferences 本地存储，不联网不上传 */
public final class StatsStore {
    private static final String PREF = "qingscreen_stats";
    private static final String KEY_COUNTS = "counts";
    private static final String KEY_DAY = "day";
    private static final String KEY_TODAY = "today";

    public static class Entry {
        public final String pkg;
        public final int count;
        Entry(String p, int c) { pkg = p; count = c; }
    }

    private StatsStore() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public static synchronized void add(Context c, String pkg) {
        if (pkg == null || pkg.isEmpty()) return;
        SharedPreferences p = sp(c);
        String d = today();
        SharedPreferences.Editor e = p.edit();
        if (!d.equals(p.getString(KEY_DAY, ""))) {
            e.putString(KEY_DAY, d).putInt(KEY_TODAY, 0);
        }
        Map<String, Integer> map = parse(p.getString(KEY_COUNTS, ""));
        int n = map.containsKey(pkg) ? map.get(pkg) : 0;
        map.put(pkg, n + 1);
        e.putString(KEY_COUNTS, encode(map));
        e.putInt(KEY_TODAY, p.getInt(KEY_TODAY, 0) + 1);
        e.apply();
    }

    public static synchronized int todayTotal(Context c) {
        SharedPreferences p = sp(c);
        if (!today().equals(p.getString(KEY_DAY, ""))) return 0;
        return p.getInt(KEY_TODAY, 0);
    }

    public static synchronized int total(Context c) {
        int sum = 0;
        for (int v : parse(sp(c).getString(KEY_COUNTS, "")).values()) sum += v;
        return sum;
    }

    public static synchronized List<Entry> all(Context c) {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<String, Integer> e : parse(sp(c).getString(KEY_COUNTS, "")).entrySet()) {
            list.add(new Entry(e.getKey(), e.getValue()));
        }
        Collections.sort(list, (a, b) -> b.count - a.count);
        return list;
    }

    public static synchronized void clear(Context c) {
        sp(c).edit()
             .putString(KEY_COUNTS, "")
             .putString(KEY_DAY, today())
             .putInt(KEY_TODAY, 0)
             .apply();
    }

    private static Map<String, Integer> parse(String raw) {
        Map<String, Integer> m = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String pair : raw.split(",")) {
            int i = pair.lastIndexOf('=');
            if (i <= 0) continue;
            try {
                m.put(pair.substring(0, i), Integer.parseInt(pair.substring(i + 1)));
            } catch (Exception ignored) { }
        }
        return m;
    }

    private static String encode(Map<String, Integer> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }
}
