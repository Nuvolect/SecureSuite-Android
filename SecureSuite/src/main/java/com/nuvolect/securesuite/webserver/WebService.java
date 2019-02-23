/*
 * Copyright (c) 2017. Nuvolect LLC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Contact legal@nuvolect.com for a less restrictive commercial license if you would like to use the
 * software without the GPLv3 restrictions.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not,
 * see <http://www.gnu.org/licenses/>.
 *
 */

package com.nuvolect.securesuite.webserver;//

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.license.AppSpecific;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Util;
import com.nuvolect.securesuite.webserver.connector.ServerInit;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.concurrent.Semaphore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import androidx.core.app.NotificationCompat;

import static com.nuvolect.securesuite.util.LogUtil.log;


/**
 * Long running Service that operates the LAN server.
 */
public class WebService extends Service {


    private String mIpAddress;
    private Handler mHandler;
    public static String assetsDirPath;

    private static SSLServerSocketFactory sslServerSocketFactory;
    private static SSLSocketFactory sslSocketFactory;
    private static SSLContext sslContext;
    private static OkHttpClient okHttpClient = null;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1,new Notification());

        Context ctx = getApplicationContext();
        /**
         * Initialize web service command data
         */
        ServerInit.init( ctx);

        SqlCipher.getInstance(ctx);
        // Load group data into memory, used for group titles and people counts
        MyGroups.loadGroupMemory();

        /**
         * Android app assets are stored in the APK and are not part of the filesystem.
         * Copy all of the files from apk/assets to app private data
         */
        File assetsDir = Util.copyAssets(ctx, "template");
        assetsDirPath = assetsDir.getAbsolutePath();

        WebServiceThread looper = new WebServiceThread();
        looper.start();
        try {
            looper.ready.acquire();
        } catch (InterruptedException e) {
            log(LogUtil.LogType.WEB_SERVICE,
                    "Interrupted during wait for the CommServiceThread to start, prepare for trouble!");
            LogUtil.logException(ctx, LogUtil.LogType.WEB_SERVICE, e);
        }

        CrypServer server = new CrypServer(ctx, WebUtil.getPort(ctx));

        try {
            okHttpClient = null;

            // Create a self signed certificate and put it in a BKS keystore
            String keystoreFilename = CConst.SELF_SIGNED_NAME;
            File file = new File( ctx.getFilesDir(), keystoreFilename);
            String absolutePath = file.getAbsolutePath();

            // Create the certificate in a keystore, recreate: true means do it every time
            // A random password is secured with Android keystore
            SelfSignedCertificate.makeKeystore( ctx, absolutePath, false);

            // Configure SSL layer 
            sslServerSocketFactory = SSLUtil.configureSSL( ctx, absolutePath);
            
            /**
             * Save the context and socket factory used to create secure connections
             * @see #getOkHttpClient() 
             */
            sslContext = SSLUtil.sslContext;
            sslSocketFactory = sslContext.getSocketFactory();
            
            getOkHttpClient();// Build client
            
            server.makeSecure( sslServerSocketFactory, null);
            server.start();

        } catch (Exception e) {
            LogUtil.logException(ctx, LogUtil.LogType.WEB_SERVICE, e);
        }
        mIpAddress = wifiIpAddress(ctx);
        log(LogUtil.LogType.WEB_SERVICE, "Server started: " + mIpAddress + ":" + WebUtil.getPort(ctx));
    }

    private void startMyOwnForeground(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            String NOTIFICATION_CHANNEL_ID = "com.nuvolect.securesuite";
            String channelName = "My Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(AppSpecific.SMALL_ICON)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(2, notification);
        }
    }

    private class WebServiceThread extends Thread {
        public Semaphore ready = new Semaphore(0);

        WebServiceThread() {
            this.setName("webServiceThread");
        }

        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    _handleMessage(msg);
                }
            };
            ready.release(); // Signal the looper and handler are created
            Looper.loop();
        }
    }

    private void _handleMessage(Message msg) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log(LogUtil.LogType.WEB_SERVICE, "onDestroy()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        log(LogUtil.LogType.WEB_SERVICE, "onBind()");
        return null;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        log(LogUtil.LogType.WEB_SERVICE, "onReBind()");
    }

    /**
     * Return the IP in 4 number 3 dot format, or null if unable to get host address.
     * @param context
     * @return
     */
    public static String wifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endian if needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }

    public static OkHttpClient getOkHttpClient() {

        if( okHttpClient == null) {

            okHttpClient = new OkHttpClient();
            okHttpClient.setHostnameVerifier(WebUtil.NullHostNameVerifier.getInstance());
            okHttpClient.setSslSocketFactory(sslSocketFactory);
        }
        return okHttpClient;
    }
}
