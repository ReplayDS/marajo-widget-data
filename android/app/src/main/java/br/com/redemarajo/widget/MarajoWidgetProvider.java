package br.com.redemarajo.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.SystemClock;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MarajoWidgetProvider extends AppWidgetProvider {
    private static final String URL_DATA = "https://raw.githubusercontent.com/ReplayDS/marajo-widget-data/main/dados.json";
    private static final String ACTION_REFRESH = "br.com.redemarajo.widget.REFRESH";
    private static final String ACTION_ALARM = "br.com.redemarajo.widget.ALARM";
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "BR"));
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", new Locale("pt", "BR"));

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        schedule(context);
        refresh(context);
    }

    @Override public void onEnabled(Context context) {
        schedule(context);
        refresh(context);
    }

    @Override public void onDisabled(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pending(context, ACTION_ALARM));
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action) || ACTION_ALARM.equals(action)) refresh(context);
    }

    private static PendingIntent pending(Context context, String action) {
        Intent intent = new Intent(context, MarajoWidgetProvider.class).setAction(action);
        return PendingIntent.getBroadcast(context, action.equals(ACTION_ALARM) ? 2 : 1,
            intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void schedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 15 * 60_000L,
                15 * 60_000L, pending(context, ACTION_ALARM));
        }
    }

    public static void refresh(Context context) {
        Context app = context.getApplicationContext();
        draw(app, false);
        new Thread(() -> {
            boolean ok = false;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(URL_DATA + "?t=" + (System.currentTimeMillis() / 60000)).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/json");
                if (conn.getResponseCode() != 200) throw new Exception("HTTP " + conn.getResponseCode());
                StringBuilder text = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) text.append(line);
                }
                JSONObject json = new JSONObject(text.toString());
                String date = json.getString("date");
                double value = json.getDouble("rawValue");
                String readAt = json.getString("readAt");
                Instant.parse(readAt);
                if (!Double.isFinite(value) || value < 0) throw new Exception("Valor inválido");
                app.getSharedPreferences("marajo", Context.MODE_PRIVATE).edit()
                    .putString("date", date).putString("readAt", readAt)
                    .putLong("valueBits", Double.doubleToRawLongBits(value))
                    .putBoolean("hasValue", true).apply();
                ok = true;
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
                draw(app, ok);
            }
        }).start();
    }

    private static void draw(Context context, boolean online) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, MarajoWidgetProvider.class));
        if (ids.length == 0) return;
        SharedPreferences prefs = context.getSharedPreferences("marajo", Context.MODE_PRIVATE);
        String today = LocalDate.now(ZONE).format(DATE);
        String date = prefs.getString("date", "");
        String readAt = prefs.getString("readAt", "");
        boolean valid = prefs.getBoolean("hasValue", false) && today.equals(date);
        String value = "--";
        String hour = "--:--";
        boolean stale = true;
        if (valid) {
            double liters = Double.longBitsToDouble(prefs.getLong("valueBits", 0));
            value = String.format(new Locale("pt", "BR"), "%,.1f", liters);
            try {
                Instant instant = Instant.parse(readAt);
                hour = instant.atZone(ZONE).format(TIME);
                long age = System.currentTimeMillis() - instant.toEpochMilli();
                stale = age < 0 || age > 30 * 60_000L;
            } catch (Exception ignored) {}
        }
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_marajo);
            views.setTextViewText(R.id.date, today);
            views.setTextViewText(R.id.liters, value);
            views.setTextViewText(R.id.time, hour);
            views.setTextViewText(R.id.status, !valid ? "AGUARDANDO DADOS DE HOJE" :
                !online ? "CACHE · TOQUE PARA ATUALIZAR" : stale ? "DADO ANTIGO" : "GITHUB · OK");
            views.setTextColor(R.id.status, stale || !online ? Color.rgb(240,198,116) : Color.rgb(169,215,176));
            views.setOnClickPendingIntent(R.id.liters, pending(context, ACTION_REFRESH));
            views.setOnClickPendingIntent(R.id.status, pending(context, ACTION_REFRESH));
            manager.updateAppWidget(id, views);
        }
    }
}
