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
