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

package com.nuvolect.securesuite.util;//

import android.app.Activity;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.nuvolect.securesuite.webserver.WebUtil;


/**
 * Only allow the certificate to function if the request originates from
 * this server IP and port number
 */
public class MyWebViewClient extends WebViewClient {

    private final Activity m_act;

    public MyWebViewClient(Activity act) {

        m_act = act;
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {

        /**
         * Only allow the certificate to function if the request originates from
         * this server IP and port number
         *
         * This is necessary for internal WebView usage to avoid the error:
         * I/X509Util: Failed to validate the certificate chain,
         *      error: java.security.cert.CertPathValidatorException:
         *      Trust anchor for certification path not found.
         */
        String url = error.getUrl();

        if( LogUtil.DEBUG){

            String certificate = error.getCertificate().toString();
            LogUtil.log(LogUtil.LogType.MY_WEB_VIEW_CLIENT, "SSL certificate : "+certificate);

            LogUtil.log(LogUtil.LogType.MY_WEB_VIEW_CLIENT, "Url: "+url);
        }

        if( url.startsWith(WebUtil.getServerUrl(m_act)))
            handler.proceed();
    }
}
