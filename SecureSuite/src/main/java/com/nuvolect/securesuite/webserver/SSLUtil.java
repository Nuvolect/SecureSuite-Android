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
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.util.OmniUtil;
import com.nuvolect.securesuite.util.Persist;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Configure SSL
 */
public class SSLUtil {

    /**
     * Creates an SSLSocketFactory for HTTPS loading certificate from absolutePath.
     *
     * @param ctx
     * @param absolutePath
     * @return
     * @throws IOException
     */
    public static SSLServerSocketFactory configureSSLPath(Context ctx, String absolutePath) {

        SSLServerSocketFactory sslServerSocketFactory = null;
        try {
            // Android does not have the default jks but uses bks
            KeyStore keystore = KeyStore.getInstance("BKS");

            char[] passphrase = Persist.getSelfsignedKsKey( ctx);

            File loadFile = new File(absolutePath);
//            assert loadFile != null;
//            assert loadFile.exists();
//            assert loadFile.canRead();
//            assert loadFile.length() > 0;
//            LogUtil.log( SSLUtil.class, "Certificate length: "+loadFile.length());

            InputStream keystoreStream = new java.io.FileInputStream( loadFile );
            keystore.load(keystoreStream, passphrase);
            keystoreStream.close();

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            sslServerSocketFactory = sslContext.getServerSocketFactory();

        } catch (Exception e) {
            LogUtil.logException(LogUtil.LogType.SSL_UTIL, e);
        }

        return sslServerSocketFactory;
    }
    /**
     * Creates an SSLSocketFactory for HTTPS loading certificate from assets.
     *
     * Pass a KeyStore resource with your certificate and passphrase
     */
    public static SSLServerSocketFactory configureSSLAsset(String assetCertPath, char[] passphrase) throws IOException {

        SSLServerSocketFactory sslServerSocketFactory = null;
        try {
            // Android does not have the default jks but uses bks
            KeyStore keystore = KeyStore.getInstance("BKS");
            InputStream keystoreStream = WebService.class.getResourceAsStream(assetCertPath);
            keystore.load(keystoreStream, passphrase);
            keystoreStream.close();
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            sslServerSocketFactory = sslContext.getServerSocketFactory();

        } catch (Exception e) {
            LogUtil.logException(LogUtil.LogType.SSL_UTIL, e);
            throw new IOException(e.getMessage());
        }

        return sslServerSocketFactory;
    }

    /**
     * Store certificate to a keystore file.
     * @param cert
     * @param passcode
     * @param outFile
     * @return
     */
    public static boolean storeCertInKeystore( byte [] cert, char [] passcode, OmniFile outFile){

        try {
            FileOutputStream fos = new FileOutputStream( outFile.getStdFile());

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream certstream = new ByteArrayInputStream(cert);
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(certstream);

            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load( null, passcode);// Initialize it
            keyStore.setCertificateEntry("mycert", certificate);
            keyStore.store( fos, passcode);
            fos.close();

            return true;

        } catch(Exception e) {
            LogUtil.logException(LogUtil.LogType.SSL_UTIL, e);
        }
        return false;
    }

    public static void probeCert(String certPath, char[] password) throws IOException {

        try {
            // Android does not have the default jks but uses bks
            KeyStore keystore = KeyStore.getInstance("BKS");
            OmniFile certFile = new OmniFile("u0", certPath);
            InputStream keystoreStream = certFile.getFileInputStream();
            keystore.load(keystoreStream, password);

            String log = "Certificate filename: "+ certFile.getName();
            log += "\ncert path: "+ certPath;
            log += "\ncert password: "+ password;

            String alias = "";
            Enumeration<String> aliases = keystore.aliases();
            for (; aliases.hasMoreElements(); ) {
                String s = aliases.nextElement();
                log += "\nAlias: "+s;
                if (alias.isEmpty())
                    alias = s;
            }
            log += probeKeystore( keystore, alias, password);
            log += probeCert( keystore.getCertificate(alias));

            OmniUtil.writeFile( new OmniFile("u0", certFile.getName()+"_probe.txt"), log);

        } catch (Exception e) {
            LogUtil.logException(LogUtil.LogType.SSL_UTIL, e);
            throw new IOException(e.getMessage());
        }
    }

    private static String probeKeystore(KeyStore keystore, String alias, char[] password) {

        String log = "\n";
        try {
            log += "\nCreation date: " + keystore.getCreationDate(alias).toString();
            log += "\nKeystore type: " + keystore.getType();

            Provider provider = keystore.getProvider();
            log += "\nProvider name: " + provider.getName();
            log += "\nProvider info: " + provider.getInfo();

            Key key = keystore.getKey(alias, password);
            if( key != null){
                log += "\nKey algorithm: " + key.getAlgorithm();
                log += "\nKey format: " + key.getFormat();
                log += "\nKey toString: " + key.toString();
            }else
                log += "\nKey  is null";

        } catch (KeyStoreException e) {
            LogUtil.logException(LogUtil.LogType.SSL_UTIL, e);
            e.printStackTrace();
            log += e.toString();
        } catch (NoSuchAlgorithmException e) {
            LogUtil.logException(LogUtil.LogType.SSL_UTIL, e);
            e.printStackTrace();
            log += e.toString();
        } catch (UnrecoverableKeyException e) {
            LogUtil.logException(LogUtil.LogType.SSL_UTIL, e);
            e.printStackTrace();
            log += e.toString();
        }

        return log;
    }

    private static String probeCert(Certificate cert) {

        String log = "\n";

        PublicKey pubKey = cert.getPublicKey();

        try {
            log += "\nPublic key algorithm: " + pubKey.getAlgorithm();
            log += "\nPublic key format: " + pubKey.getFormat();
            log += "\nPublic key hashcode: " + String.valueOf(pubKey.hashCode());
            log += "\nPublic key toString: " + pubKey.toString();

            log += "\ncert type: " + cert.getType();
            log += "\ncert hashcode: " + String.valueOf(cert.hashCode());
            log += "\ncert toString: " + cert.toString();
        } catch (Exception e) {
            LogUtil.logException(LogUtil.LogType.SSL_UTIL, e);
            e.printStackTrace();
            log += e.toString();
        }

        return log;
    }
}
