package com.pockethost;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Bitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import java.util.EnumMap;
import java.util.Map;
import android.content.SharedPreferences;

public class MainActivity extends Activity {
    private static final int PICK_FOLDER = 100;
    private TextView folderText, statusText, localUrlText, lanUrlText, requestText, uptimeText, lastRequestText;
    private ImageView qrImage;
    private String lastQrValue = "";
    private Button startButton, stopButton;
    private SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable(){@Override public void run(){refreshState();handler.postDelayed(this,1000);}};
    private int green=Color.rgb(22,120,80), dark=Color.rgb(20,50,40), bgColor=Color.rgb(244,247,245);

    @Override public void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);prefs=getSharedPreferences("pockethost",MODE_PRIVATE);buildUi();refreshState();}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(0,7,0,7);return t;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setTypeface(null,Typeface.BOLD);b.setAllCaps(false);b.setBackground(bg(green,22));return b;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(18,16,18,16);c.setBackground(bg(Color.WHITE,24));return c;}
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,28,24,22);root.setBackgroundColor(bgColor);scroll.addView(root);
        root.addView(text("PocketHost",28,dark,true));root.addView(text("Your phone. Your server. Your domain.",15,Color.rgb(85,105,96),false));
        LinearLayout server=card();statusText=text("● Server stopped",18,Color.rgb(170,55,55),true);server.addView(statusText);server.addView(text("LOCAL WEB SERVER",12,Color.GRAY,true));localUrlText=text("http://127.0.0.1:8080",16,dark,true);server.addView(localUrlText);lanUrlText=text("LAN: —",14,Color.DKGRAY,false);server.addView(lanUrlText);
        Button copyLan=button("COPY LAN URL");copyLan.setOnClickListener(v->copy(WebServerService.getLanUrl(this)));server.addView(copyLan);
        Button copyLocal=button("COPY LOCAL URL");copyLocal.setOnClickListener(v->copy("http://127.0.0.1:8080"));server.addView(copyLocal);root.addView(server);
        root.addView(text("Share on your Wi-Fi",18,dark,true));LinearLayout qrCard=card();qrImage=new ImageView(this);qrImage.setAdjustViewBounds(true);qrImage.setScaleType(ImageView.ScaleType.FIT_CENTER);qrImage.setMinimumHeight(240);qrCard.addView(qrImage);qrCard.addView(text("Scan this QR code from another device connected to the same network.",13,Color.DKGRAY,false));root.addView(qrCard);
        root.addView(text("Website folder",18,dark,true));folderText=text("No website folder selected",14,Color.DKGRAY,false);folderText.setPadding(16,14,16,14);folderText.setBackground(bg(Color.WHITE,18));root.addView(folderText);
        Button choose=button("Choose website folder");root.addView(choose);choose.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK_FOLDER);});
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setPadding(0,16,0,8);startButton=button("START SERVER");stopButton=button("STOP SERVER");actions.addView(startButton,new LinearLayout.LayoutParams(0,112,1));LinearLayout slp=new LinearLayout.LayoutParams(0,112,1);slp.setMargins(10,0,0,0);actions.addView(stopButton,slp);root.addView(actions);startButton.setOnClickListener(v->startHosting());stopButton.setOnClickListener(v->stopHosting());
        root.addView(text("Server statistics",18,dark,true));LinearLayout stats=card();requestText=text("Requests served: 0",16,dark,true);uptimeText=text("Uptime: —",14,Color.DKGRAY,false);lastRequestText=text("Last request: —",13,Color.DKGRAY,false);stats.addView(requestText);stats.addView(uptimeText);stats.addView(lastRequestText);root.addView(stats);
        root.addView(text("PocketHost hosts static website files from your selected folder. Keep the app's server running while the site is needed.",13,Color.rgb(95,105,100),false));setContentView(scroll);
    }
    private void copy(String value){if(value==null||value.startsWith("Not connected"))return;ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("PocketHost URL",value));}
    private void startHosting(){if(prefs.getString("folder_uri",null)==null){folderText.setText("Select a website folder first.");return;}Intent intent=new Intent(this,WebServerService.class);intent.setAction(WebServerService.ACTION_START);if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(intent);else startService(intent);handler.postDelayed(()->refreshState(),300);}
    private void stopHosting(){Intent i=new Intent(this,WebServerService.class);i.setAction(WebServerService.ACTION_STOP);if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);handler.postDelayed(()->refreshState(),300);}
    private String formatUptime(long millis){long s=Math.max(0,millis/1000);return String.format(java.util.Locale.US,"%02d:%02d:%02d",s/3600,(s%3600)/60,s%60);}
    private void refreshState(){String uri=prefs.getString("folder_uri",null);folderText.setText(uri==null?"No website folder selected":Uri.parse(uri).getLastPathSegment());boolean running=WebServerService.isRunning;statusText.setText(running?"● Server running":"● Server stopped");statusText.setTextColor(running?green:Color.rgb(170,55,55));localUrlText.setText("http://127.0.0.1:8080");String lan=WebServerService.getLanUrl(this);lanUrlText.setText("LAN URL: "+lan);updateQr(lan);startButton.setEnabled(!running);stopButton.setEnabled(running);requestText.setText("Requests served: "+WebServerService.requestCount);uptimeText.setText("Uptime: "+(running&&WebServerService.startedAt>0?formatUptime(System.currentTimeMillis()-WebServerService.startedAt):"—"));lastRequestText.setText("Last request: "+WebServerService.lastRequest);}
    private void updateQr(String value){
        if(qrImage==null||value==null||value.startsWith("Not connected")||value.equals(lastQrValue))return;
        lastQrValue=value;
        try{
            Map<EncodeHintType,Object> hints=new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN,1);
            BitMatrix matrix=new MultiFormatWriter().encode(value,BarcodeFormat.QR_CODE,420,420,hints);
            Bitmap bitmap=Bitmap.createBitmap(420,420,Bitmap.Config.ARGB_8888);
            for(int x=0;x<420;x++)for(int y=0;y<420;y++)bitmap.setPixel(x,y,matrix.get(x,y)?Color.BLACK:Color.WHITE);
            qrImage.setImageBitmap(bitmap);
        }catch(Exception e){qrImage.setImageBitmap(null);lastQrValue="";}
    }
    @Override protected void onResume(){super.onResume();handler.post(refreshRunnable);}
    @Override protected void onPause(){handler.removeCallbacks(refreshRunnable);super.onPause();}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==PICK_FOLDER&&resultCode==RESULT_OK&&data!=null){Uri uri=data.getData();int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);try{getContentResolver().takePersistableUriPermission(uri,flags);}catch(Exception ignored){}prefs.edit().putString("folder_uri",uri.toString()).apply();refreshState();}}
}
