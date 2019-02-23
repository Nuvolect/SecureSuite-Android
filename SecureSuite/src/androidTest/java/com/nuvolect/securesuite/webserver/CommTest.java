/*
 * Copyright (c) 2018 Nuvolect LLC.
 * This software is offered for free under conditions of the GPLv3 open source software license.
 * Contact Nuvolect LLC for a less restrictive commercial license if you would like to use the software
 * without the GPLv3 restrictions.
 */

package com.nuvolect.securesuite.webserver;

import android.content.Context;

import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Util;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;

import static com.nuvolect.securesuite.main.App.getContext;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CommTest {

    @Test
    public void loopbackTest() {

        Context ctx = getContext();

        InetAddress inetAddress = InetAddress.getLoopbackAddress();
        try {
            inetAddress.isReachable(1000);
            assertThat(true, is(true));
        } catch (IOException e) {
            LogUtil.logException(CommTest.class, e);
            assertThat(false, is(true));
        }
    }

    @Test
    public void wikipediaTest() {

        try {

            URL url = new URL("https://wikipedia.org");
            URLConnection urlConnection = null;
            urlConnection = url.openConnection();
            InputStream in = urlConnection.getInputStream();
            String s = Util.copyFile( in);
            in.close();

            assertThat(s.contains("Wikipedia"), is(true));

        } catch (IOException e) {
            LogUtil.logException(CommTest.class, e);
            assertThat(false, is(true));
        }
    }
}
