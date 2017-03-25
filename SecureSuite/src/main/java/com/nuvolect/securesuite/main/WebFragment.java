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

package com.nuvolect.securesuite.main;//

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.nuvolect.securesuite.util.ActionBarUtil;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.MyWebViewClient;
import com.nuvolect.securesuite.util.MyWebViewFragment;

import org.nanohttpd.protocols.http.content.Cookie;

import java.util.HashMap;
import java.util.Map;

public class WebFragment extends MyWebViewFragment {

    private static final String KEY_FILE="file";

    public static WebFragment newInstance(String file){

        WebFragment f = new WebFragment();

        Bundle args = new Bundle();

        args.putString(KEY_FILE, file);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

    }

    @Override
    public void onResume() {
        super.onResume();

        ActionBarUtil.hide(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View result=super.onCreateView(inflater, container, savedInstanceState);

        WebView webView = getWebView();
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);

        webView.setWebViewClient(new MyWebViewClient(getActivity()));
        webView.setWebChromeClient(new WebChromeClient());
        webView.clearCache( true );
        webView.clearHistory();

        clearCookies(getActivity());

        CookieManager.getInstance().setAcceptThirdPartyCookies( webView, true );
        CookieManager.getInstance().flush();

        String cookie = CConst.UNIQUE_ID+"="+ CConst.EMBEDDED_USER
                +" ; path=/;"
                +" expires="+ Cookie.getHTTPTime( 7 )+";";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cookie", cookie);
        webView.loadUrl(getPage(), headers);

        return(result);
    }

      private String getPage() {
        return(getArguments().getString(KEY_FILE));
    }

    @SuppressWarnings("deprecation")
    public static void clearCookies(Context context)
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            LogUtil.log(LogUtil.LogType.WEB_FRAGMENT, "Using clearCookies code for API >=" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else
        {
            LogUtil.log(LogUtil.LogType.WEB_FRAGMENT, "Using clearCookies code for API <" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieSyncManager cookieSyncMngr= CookieSyncManager.createInstance(context);
            cookieSyncMngr.startSync();
            CookieManager cookieManager= CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }
}
