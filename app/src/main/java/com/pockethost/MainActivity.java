package com.pockethost;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;

public class MainActivity extends Activity {
    private static final int PICK_FOLDER = 100;
    private TextView folderText, statusText, localUrlText, lanUrlText;
    private Button startButton, stopButton;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("pockethost", MODE_PRIVATE);
        buildUi();
        refreshState();
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD); t.setPadding(0, 8, 0, 8); return t;
    }
    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 34, 28, 24); root.setBackgroundColor(Color.rgb(246,248,247));
        TextView brand = text("PocketHost", 26, Color.rgb(24,54,43), true);
        root.addView(brand);
        root.addView(text("Your phone. Your server. Your domain.", 14, Color.rgb(90,105,99), false));

        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(20,20,20,20); card.setBackground(bg(Color.WHITE, 28));
        statusText = text("● Server stopped", 18, Color.rgb(170,55,55), true); card.addView(statusText);
        card.addView(text("LOCAL WEB SERVER", 12, Color.GRAY, true));
        localUrlText = text("http://127.0.0.1:8080", 16, Color.rgb(30,60,50), true); card.addView(localUrlText);
        lanUrlText = text("LAN address will appear here", 13, Color.DKGRAY, false); card.addView(lanUrlText);
        root.addView(card);

        root.addView(text("Website folder", 18, Color.rgb(30,50,45), true));
        folderText = text("No website folder selected", 14, Color.DKGRAY, false);
        folderText.setPadding(16,16,16,16); folderText.setBackground(bg(Color.WHITE,18)); root.addView(folderText);
        Button choose = button("Choose website folder"); root.addView(choose);
        choose.setOnClickListener(v -> { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); startActivityForResult(i,PICK_FOLDER); });

        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL); actions.setPadding(0,18,0,8);
        startButton = button("START SERVER"); stopButton = button("STOP SERVER");
        actions.addView(startButton, new LinearLayout.LayoutParams(0,120,1));
        LinearLayout.LayoutParams stopLp = new LinearLayout.LayoutParams(0,120,1); stopLp.setMargins(12,0,0,0); actions.addView(stopButton,stopLp); root.addView(actions);
        startButton.setOnClickListener(v -> startHosting()); stopButton.setOnClickListener(v -> stopHosting());
        root.addView(text("First milestone: host static HTML/CSS/JS files locally. Cloudflare Tunnel integration comes next.",13,Color.rgb(95,105,100),false));
        setContentView(root);
    }
    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(14); b.setTypeface(null,Typeface.BOLD); b.setAllCaps(false);
        b.setBackground(bg(Color.rgb(24,110,76),24)); return b;
    }
    private void startHosting() {
        String uri = prefs.getString("folder_uri", null);
        if(uri == null) { folderText.setText("Select a website folder first."); return; }
        Intent intent = new Intent(this, WebServerService.class); intent.setAction(WebServerService.ACTION_START);
        if(android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        refreshState();
    }
    private void stopHosting() {
        Intent i = new Intent(this, WebServerService.class); i.setAction(WebServerService.ACTION_STOP); startService(i); refreshState();
    }
    private void refreshState() {
        String uri = prefs.getString("folder_uri", null);
        folderText.setText(uri == null ? "No website folder selected" : Uri.parse(uri).getLastPathSegment());
        boolean running = WebServerService.isRunning;
        statusText.setText(running ? "● Server running" : "● Server stopped"); statusText.setTextColor(running ? Color.rgb(22,125,75) : Color.rgb(170,55,55));
        localUrlText.setText("http://127.0.0.1:8080");
        lanUrlText.setText("LAN URL: " + WebServerService.getLanUrl(this));
        startButton.setEnabled(!running); stopButton.setEnabled(running);
    }
    @Override protected void onResume(){ super.onResume(); refreshState(); }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){ super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_FOLDER && resultCode==RESULT_OK && data!=null){
            Uri uri=data.getData(); int flags=data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try { getContentResolver().takePersistableUriPermission(uri,flags); } catch(Exception ignored) {}
            prefs.edit().putString("folder_uri",uri.toString()).apply(); refreshState();
        }
    }
}
