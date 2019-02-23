/*
 * Copyright (c) 2018 Nuvolect LLC.
 * This software is offered for free under conditions of the GPLv3 open source software license.
 * Contact Nuvolect LLC for a less restrictive commercial license if you would like to use the software
 * without the GPLv3 restrictions.
 */

package com.nuvolect.securesuite.util;

import android.content.Context;

import com.nuvolect.securesuite.webserver.SSLUtil;
import com.nuvolect.securesuite.webserver.SelfSignedCertificate;

import org.junit.Test;

import java.io.File;

import javax.net.ssl.SSLServerSocketFactory;

import static com.nuvolect.securesuite.main.App.getContext;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test lifecycle of a selfsigned certificate and password
 * NOTE THIS IS A DESTRUCTIVE TEST. It will delete the existing certificate and password.
 */
public class SelfSignedKeystoreTest {

    @Test
    public void createCertTest(){

        LogUtil.log( KeystoreUtilTest.class, CrypUtilTest.class.getCanonicalName()+" test starting");
        Context ctx = getContext();

        // Create a self signed certificate and put it in a BKS keystore
        String keystoreFilename = "SelfSignedKeystoreTest.bks";

        File file = new File( ctx.getFilesDir(), keystoreFilename);
        file.delete();
        assertThat( file.exists(), is( false));

        Persist.deleteKey(ctx, Persist.SELFSIGNED_KS_KEY);
        assertThat( Persist.keyExists(ctx, Persist.SELFSIGNED_KS_KEY), is( false));

        String absolutePath = file.getAbsolutePath();
        SelfSignedCertificate.makeKeystore( ctx, absolutePath, false);
        assertThat( file.exists(), is( true));
        assertThat( Persist.keyExists(ctx, Persist.SELFSIGNED_KS_KEY), is( true));

        try {
            SSLServerSocketFactory sslServerSocketFactory = SSLUtil.configureSSL(ctx, absolutePath);
            String[] suites = sslServerSocketFactory.getSupportedCipherSuites();
            assertThat( suites.length > 0, is( true));

        } catch (Exception e) {
            LogUtil.logException(KeystoreUtilTest.class, e);
        }
        file.delete();
        assertThat( file.exists(), is( false));

        Persist.deleteKey(ctx, Persist.SELFSIGNED_KS_KEY);
        assertThat( Persist.keyExists(ctx, Persist.SELFSIGNED_KS_KEY), is( false));

        LogUtil.log( KeystoreUtilTest.class, CrypUtilTest.class.getCanonicalName()+" test ending, certificate deleted");
    }

}
