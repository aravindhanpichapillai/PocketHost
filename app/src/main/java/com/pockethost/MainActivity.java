package com.pockethost;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity {

    private Button startButton;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        startButton = button("START SERVER");
        stopButton = button("STOP SERVER");

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, 16, 0, 8);

        LinearLayout.LayoutParams startParams =
                new LinearLayout.LayoutParams(0, 112, 1);
        actions.addView(startButton, startParams);

        LinearLayout.LayoutParams stopParams =
                new LinearLayout.LayoutParams(0, 112, 1);
        stopParams.setMargins(10, 0, 0, 0);
        actions.addView(stopButton, stopParams);

        root.addView(actions);

        startButton.setOnClickListener(v -> startHosting());
        stopButton.setOnClickListener(v -> stopHosting());

        setContentView(root);
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        return button;
    }

    private void startHosting() {
        // Keep your existing startHosting() implementation here.
    }

    private void stopHosting() {
        // Keep your existing stopHosting() implementation here.
    }
}
