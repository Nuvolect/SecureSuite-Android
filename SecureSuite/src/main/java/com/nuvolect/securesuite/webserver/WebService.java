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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Util;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.security.KeyStore;
import java.util.concurrent.Semaphore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import static com.nuvolect.securesuite.util.LogUtil.log;


/**
 * Long running Service that operates the LAN server.
 */
public class WebService extends Service {


    private Context m_ctx;
    private String mIpAddress;
    private Handler mHandler;
    public static File assetsDir;
    public static String assetsDirPath;

    private static SSLServerSocketFactory sslServerSocketFactory;
    private static SSLSocketFactory sslSocketFactory;
    private static SSLContext sslContext;
    private static OkHttpClient okHttpClient = null;

    private static String keyFile = "/assets/keystore.bks";
    private static char[] passPhrase = "27@NDMQu0cLY".toCharArray();//FIXME obscure or remove passphrase

    @Override
    public void onCreate() {
        super.onCreate();

        m_ctx = getApplicationContext();
        SqlCipher.getInstance(m_ctx);
        // Load group data into memory, used for group titles and people counts
        MyGroups.loadGroupMemory();

        /**
         * Android app assets are stored in the APK and are not part of the filesystem.
         * Copy all of the files from apk/assets to app private data
         */
        assetsDir = Util.copyAssets(m_ctx, "template");
        assetsDirPath = assetsDir.getAbsolutePath();

        WebServiceThread looper = new WebServiceThread();
        looper.start();
        try {
            looper.ready.acquire();
        } catch (InterruptedException e) {
            log(LogUtil.LogType.WEB_SERVICE,
                    "Interrupted during wait for the CommServiceThread to start, prepare for trouble!");
            LogUtil.logException(m_ctx, LogUtil.LogType.WEB_SERVICE, e);
        }

        CrypServer server = new CrypServer(m_ctx, WebUtil.getPort(m_ctx));

        try {
            okHttpClient = null;

            configureSSL(keyFile, passPhrase);

            server.makeSecure( sslServerSocketFactory);
            server.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
        mIpAddress = wifiIpAddress(m_ctx);
        log(LogUtil.LogType.WEB_SERVICE, "Server started: " + mIpAddress + ":" + WebUtil.getPort(m_ctx));
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
    protected String wifiIpAddress(Context context) {
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

    /**
     * Creates an SSLSocketFactory for HTTPS.
     *
     * Pass a KeyStore resource with your certificate and passphrase
     */
    public static void configureSSL(String keyAndTrustStoreClasspathPath, char[] passphrase) throws IOException {

        try {
            // Android does not have the default jks but uses bks
            KeyStore keystore = KeyStore.getInstance("BKS");
            InputStream keystoreStream = WebService.class.getResourceAsStream(keyAndTrustStoreClasspathPath);
            keystore.load(keystoreStream, passphrase);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            sslServerSocketFactory = sslContext.getServerSocketFactory();
            sslSocketFactory = sslContext.getSocketFactory();

        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
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
