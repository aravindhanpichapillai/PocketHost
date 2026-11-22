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

import org.nanohttpd.NanoHTTPD;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class WebServerService extends Service {
    public static final String ACTION_START="com.pockethost.START", ACTION_STOP="com.pockethost.STOP";
    public static volatile boolean isRunning=false;
    private static final int PORT=8080; private SiteServer server;
    @Override public void onCreate(){ super.onCreate(); createChannel(); }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?ACTION_START:intent.getAction();
        if(ACTION_STOP.equals(action)){ stopServer(); stopForegroundCompat(); stopSelf(); }
        else startServer(); return START_STICKY;
    }
    private void startServer(){
        if(server!=null){return;}
        startForeground(7,notification("Starting website server…"));
        SharedPreferences p=getSharedPreferences("pockethost",MODE_PRIVATE); String tree=p.getString("folder_uri",null);
        if(tree==null){stopSelf();return;}
        try{ server=new SiteServer(this,Uri.parse(tree)); server.start(NanoHTTPD.SOCKET_READ_TIMEOUT,false); isRunning=true; }
        catch(Exception e){server=null;isRunning=false; stopSelf();}
    }
    private void stopServer(){ try{if(server!=null)server.stop();}catch(Exception ignored){} server=null;isRunning=false; }
    private void createChannel(){ if(Build.VERSION.SDK_INT>=26){ NotificationChannel c=new NotificationChannel("pockethost_server","PocketHost Server",NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager.class).createNotificationChannel(c); } }
    private Notification notification(String msg){
        if(Build.VERSION.SDK_INT>=26) return new Notification.Builder(this,"pockethost_server").setContentTitle("PocketHost").setContentText(msg).setSmallIcon(android.R.drawable.stat_sys_upload_done).setOngoing(true).build();
        return new Notification.Builder(this).setContentTitle("PocketHost").setContentText(msg).setSmallIcon(android.R.drawable.stat_sys_upload_done).setOngoing(true).build();
    }
    private void stopForegroundCompat(){ if(Build.VERSION.SDK_INT>=24)stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true); }
    public static String getLanUrl(Context ctx){
        try{ for(NetworkInterface ni:Collections.list(NetworkInterface.getNetworkInterfaces())) for(InetAddress a:Collections.list(ni.getInetAddresses())) if(!a.isLoopbackAddress() && a.getHostAddress().indexOf(':')<0) return "http://"+a.getHostAddress()+":"+PORT; }catch(Exception ignored){} return "Not connected to Wi-Fi";
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){stopServer();stopForegroundCompat();super.onDestroy();}

    static class SiteServer extends NanoHTTPD {
        private final Context context; private final Uri treeUri;
        SiteServer(Context c,Uri u){super(PORT);context=c;treeUri=u;}
        @Override public Response serve(IHTTPSession session){
            String path=session.getUri(); if(path==null||path.isEmpty()||path.equals("/")) path="/index.html";
            if(path.contains("..")) return newFixedLengthResponse(Response.Status.FORBIDDEN,"text/plain","Forbidden");
            String[] segments=path.split("/"); Uri target=treeUri;
            for(String s:segments){ if(s.isEmpty())continue; target=findChild(target,s); if(target==null)return newFixedLengthResponse(Response.Status.NOT_FOUND,"text/plain","File not found"); }
            if(target==null)return newFixedLengthResponse(Response.Status.NOT_FOUND,"text/plain","File not found");
            try{ InputStream stream=context.getContentResolver().openInputStream(target); if(stream==null)return newFixedLengthResponse(Response.Status.NOT_FOUND,"text/plain","File not found"); String mime=mime(path); return newChunkedResponse(Response.Status.OK,mime,stream); }
            catch(Exception e){return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,"text/plain","Unable to read file");}
        }
        private Uri findChild(Uri parent,String name){
            String id=DocumentsContract.getTreeDocumentId(parent); Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,id);
            Cursor c=null; try{ c=context.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null); if(c!=null)while(c.moveToNext()) if(name.equals(c.getString(1))){String childId=c.getString(0); return DocumentsContract.buildDocumentUriUsingTree(treeUri,childId);} }
            finally{if(c!=null)c.close();} return null;
        }
        private String mime(String path){String ext=MimeTypeMap.getFileExtensionFromUrl(path); String m=MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext); return m==null?"application/octet-stream":m;}
    }
}
