package br.com.redemarajo.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(36,36,36,36);
        layout.setBackgroundColor(Color.rgb(17,17,20));
        TextView title = new TextView(this);
        title.setText("MARAJÓ\n3001 · PORANGATU 01");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);
        TextView info = new TextView(this);
        info.setText("\nAdicione o widget Marajó pela tela inicial do Android.\nToque no widget para atualizar.");
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(15);
        info.setGravity(Gravity.CENTER);
        layout.addView(info);
        Button button = new Button(this);
        button.setText("Atualizar widget");
        button.setOnClickListener(v -> MarajoWidgetProvider.refresh(this));
        layout.addView(button);
        setContentView(layout);
        MarajoWidgetProvider.refresh(this);
    }
}
