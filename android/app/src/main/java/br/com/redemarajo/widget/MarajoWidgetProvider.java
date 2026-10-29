package br.com.redemarajo.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MarajoWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_REFRESH = "br.com.redemarajo.widget.REFRESH";
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
        WorkManager.getInstance(context).cancelUniqueWork("marajo-periodic");
    }
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) refresh(context);
    }
    private static PendingIntent tap(Context context) {
        Intent intent = new Intent(context, MarajoWidgetProvider.class).setAction(ACTION_REFRESH);
        return PendingIntent.getBroadcast(context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private static Constraints network() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }
    private static void schedule(Context context) {
        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(
            MarajoUpdateWorker.class, 15, TimeUnit.MINUTES)
            .setConstraints(network()).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "marajo-periodic", ExistingPeriodicWorkPolicy.KEEP, periodic);
    }
    public static void refresh(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MarajoUpdateWorker.class)
            .setConstraints(network()).build();
        WorkManager.getInstance(context).enqueue(request);
    }
    static void draw(Context context, boolean online) {
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
            views.setOnClickPendingIntent(R.id.liters, tap(context));
            views.setOnClickPendingIntent(R.id.status, tap(context));
            manager.updateAppWidget(id, views);
        }
    }
}
