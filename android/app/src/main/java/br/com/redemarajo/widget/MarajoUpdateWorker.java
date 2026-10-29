package br.com.redemarajo.widget;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;

public class MarajoUpdateWorker extends Worker {
    private static final String DATA_URL =
        "https://raw.githubusercontent.com/ReplayDS/marajo-widget-data/main/dados.json";

    public MarajoUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(DATA_URL + "?t=" + (System.currentTimeMillis() / 60000)).openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(12000);
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
            boolean saved = context.getSharedPreferences("marajo", Context.MODE_PRIVATE).edit()
                .putString("date", date).putString("readAt", readAt)
                .putLong("valueBits", Double.doubleToRawLongBits(value))
                .putBoolean("hasValue", true).commit();
            if (!saved) throw new Exception("Falha ao salvar cache");
            MarajoWidgetProvider.draw(context, true);
            return Result.success();
        } catch (Exception e) {
            android.util.Log.w("MarajoWidget", "Falha ao atualizar", e);
            MarajoWidgetProvider.draw(context, false);
            return Result.retry();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
