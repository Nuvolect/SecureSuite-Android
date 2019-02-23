/*
 * Copyright (c) 2018. Nuvolect LLC
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

package com.nuvolect.securesuite.webserver;

import android.content.Context;

import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Persist;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Configure SSL
 */
public class SSLUtil {

    public static SSLContext sslContext = null;
    private static X509TrustManager origTrustmanager = null;

    /**
     * Creates an SSLSocketFactory for HTTPS loading certificate from absolutePath.
     *
     * @param ctx
     * @param absolutePath
     * @return
     * @throws IOException
     */
    public static SSLServerSocketFactory configureSSL(Context ctx, String absolutePath) {

        SSLServerSocketFactory sslServerSocketFactory = null;
        try {
            // Android does not have the default jks but uses bks
            KeyStore keystore = KeyStore.getInstance("BKS");

            char[] passphrase = Persist.getSelfsignedKsKey( ctx);

            File loadFile = new File(absolutePath);

            InputStream keystoreStream = new java.io.FileInputStream( loadFile );
            keystore.load(keystoreStream, passphrase);
            keystoreStream.close();

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), getTrustManagers( keystore), null);
            sslServerSocketFactory = sslContext.getServerSocketFactory();

        } catch (Exception e) {
            LogUtil.logException(LogUtil.LogType.SSL_UTIL, e);
        }

        return sslServerSocketFactory;
    }

    private static TrustManager[] getTrustManagers(KeyStore keyStore){

        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init( keyStore);
            TrustManager[] trustManagers = tmf.getTrustManagers();
            origTrustmanager = (X509TrustManager)trustManagers[0];

        } catch (KeyStoreException e) {
            LogUtil.logException(LogUtil.LogType.SSL_UTIL, e);
        } catch (NoSuchAlgorithmException e) {
            LogUtil.logException(LogUtil.LogType.SSL_UTIL, e);
        }

        TrustManager[] wrappedTrustManagers = new TrustManager[]{

                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return origTrustmanager.getAcceptedIssuers();
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        origTrustmanager.checkClientTrusted(certs, authType);
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        try {
                            //FIXME Handle case where an exception occurs, the certificate should NOT be trusted
                            origTrustmanager.checkServerTrusted(certs, authType);
                        } catch (CertificateExpiredException e) {} catch (CertificateException e) {
                            LogUtil.logException(LogUtil.LogType.SSL_UTIL, e);
                        }
                    }
                }
        };

        return wrappedTrustManagers;
    }
}
