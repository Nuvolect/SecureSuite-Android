package com.nuvolect.securesuite.main;

import android.app.Application;
import android.content.Context;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.util.LogUtil;

/**
 * Provide an ability to get a context without
 * having to use an Activity or Service context.
 */
public class App extends Application {

    private static Context mContext;
    public static String DEFAULT_IP_PORT = "0.0.0.0:0000";
    public static int DEFAULT_PORT = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;

        /**
         * Load build-dependent data into static variables that can be accessed without context.
         */
        LogUtil.setVerbose( Boolean.valueOf( mContext.getString(R.string.verbose_debug)));

        DEFAULT_IP_PORT = mContext.getString(R.string.default_ip_port);
        DEFAULT_PORT = Integer.valueOf(mContext.getString(R.string.default_port));
    }

    public static Context getContext(){
        return mContext;
    }
}
