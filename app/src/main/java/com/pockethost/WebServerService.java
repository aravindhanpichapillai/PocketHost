package com.pockethost;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.content.Context;
import android.database.Cursor;
import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;

import fi.iki.elonen.NanoHTTPD;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class WebServerService extends Service {
    public static final String ACTION_START="com.pockethost.START", ACTION_STOP="com.pockethost.STOP";
    public static volatile boolean isRunning=false;
    public static volatile long requestCount=0;
    public static volatile String lastRequest="No requests yet";
    public static volatile long startedAt=0L;
    private static final int PORT=8080; private SiteServer server;

    @Override public void onCreate(){ super.onCreate(); createChannel(); }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?ACTION_START:intent.getAction();
        if(ACTION_STOP.equals(action)){ stopServer(); stopForegroundCompat(); stopSelf(); }
        else startServer();
        return START_STICKY;
    }
    private void startServer(){
        if(server!=null)return;
        startForeground(7,notification("Starting website server…"));
        SharedPreferences p=getSharedPreferences("pockethost",MODE_PRIVATE); String tree=p.getString("folder_uri",null);
        if(tree==null){stopForegroundCompat();stopSelf();return;}
        try{ server=new SiteServer(this,Uri.parse(tree)); server.start(NanoHTTPD.SOCKET_READ_TIMEOUT,false); isRunning=true; startedAt=System.currentTimeMillis(); }
        catch(Exception e){server=null;isRunning=false;startedAt=0L;stopForegroundCompat();stopSelf();}
    }
    private void stopServer(){try{if(server!=null)server.stop();}catch(Exception ignored){}server=null;isRunning=false;startedAt=0L;}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel("pockethost_server","PocketHost Server",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(c);}}
    private Notification notification(String msg){
        if(Build.VERSION.SDK_INT>=26)return new Notification.Builder(this,"pockethost_server").setContentTitle("PocketHost").setContentText(msg).setSmallIcon(android.R.drawable.stat_sys_upload_done).setOngoing(true).build();
        return new Notification.Builder(this).setContentTitle("PocketHost").setContentText(msg).setSmallIcon(android.R.drawable.stat_sys_upload_done).setOngoing(true).build();
    }
    private void stopForegroundCompat(){if(Build.VERSION.SDK_INT>=24)stopForeground(STOP_FOREGROUND_REMOVE);else stopForeground(true);}
    public static String getLanUrl(Context ctx){
        try{for(NetworkInterface ni:Collections.list(NetworkInterface.getNetworkInterfaces()))for(InetAddress a:Collections.list(ni.getInetAddresses()))if(!a.isLoopbackAddress()&&a.getHostAddress().indexOf(':')<0)return "http://"+a.getHostAddress()+":"+PORT;}catch(Exception ignored){}return "Not connected to Wi-Fi";
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){stopServer();stopForegroundCompat();super.onDestroy();}

    static class SiteServer extends NanoHTTPD {
        private final Context context; private final Uri rootTree;
        SiteServer(Context c,Uri u){super(PORT);context=c;rootTree=u;}
        @Override public Response serve(IHTTPSession session){
            requestCount++;
            lastRequest=session.getMethod().name()+" "+session.getUri();
            String raw=session.getUri();
            if(raw==null||raw.isEmpty()||raw.equals("/"))raw="/index.html";
            if(raw.indexOf('\\')>=0||raw.contains("../")||raw.equals(".."))return text(Response.Status.FORBIDDEN,"Forbidden");
            // NanoHTTPD supplies the URI path; avoid a second decoding pass that could turn encoded traversal into real traversal.
            String path=raw;
            String[] segments=path.split("/"); Uri target=rootTree;
            for(String segment:segments){
                if(segment.isEmpty()||segment.equals("."))continue;
                if(segment.equals(".."))return text(Response.Status.FORBIDDEN,"Forbidden");
                target=findChild(target,segment);
                if(target==null)return text(Response.Status.NOT_FOUND,"File not found: "+segment);
            }
            if(target==null)return text(Response.Status.NOT_FOUND,"File not found");
            try{
                InputStream stream=context.getContentResolver().openInputStream(target);
                if(stream==null)return text(Response.Status.NOT_FOUND,"File not found");
                String mime=mime(path);
                Response response=newChunkedResponse(Response.Status.OK,mime,stream);
                response.addHeader("Cache-Control","no-cache");
                response.addHeader("X-Content-Type-Options","nosniff");
                return response;
            }catch(Exception e){return text(Response.Status.INTERNAL_ERROR,"Unable to read file");}
        }
        private Response text(Response.Status status,String message){return newFixedLengthResponse(status,"text/plain; charset=utf-8",message);}
        private Uri findChild(Uri parent,String name){
            String id;
            try{id=DocumentsContract.getDocumentId(parent);}catch(Exception e){return null;}
            Uri children;
            try{children=DocumentsContract.buildChildDocumentsUriUsingTree(rootTree,id);}catch(Exception e){return null;}
            Cursor c=null;
            try{
                c=context.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null);
                if(c!=null)while(c.moveToNext()){
                    String display=c.getString(1);
                    if(display!=null&&name.equalsIgnoreCase(display))return DocumentsContract.buildDocumentUriUsingTree(rootTree,c.getString(0));
                }
            }catch(Exception ignored){}finally{if(c!=null)c.close();}
            return null;
        }
        private String mime(String path){
            String clean=path;
            int q=clean.indexOf('?');if(q>=0)clean=clean.substring(0,q);
            int dot=clean.lastIndexOf('.');String ext=dot>=0?clean.substring(dot+1).toLowerCase():"";
            if(ext.equals("js"))return "application/javascript";
            if(ext.equals("json"))return "application/json";
            if(ext.equals("svg"))return "image/svg+xml";
            if(ext.equals("wasm"))return "application/wasm";
            String m=MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            return m==null?"application/octet-stream":m;
        }
    }
}
