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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nuvolect.securesuite.webserver.WebUtil;

/**
 * The broadcast receiver is started with a Manifest definition.
 * These two states enable and disable it.
 */
public class WifiBroadcastReceiver extends BroadcastReceiver {

    public WifiBroadcastReceiver(){
        LogUtil.log(LogUtil.LogType.WIFI_BROADCAST_RECEIVER, "WifiBroadcastReceiver() constructor");
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        LogUtil.log(LogUtil.LogType.WIFI_BROADCAST_RECEIVER, "onReceive, intent.getAction(): " + action);

        if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {

            if( WebUtil.wifiEnabled( context)) {
                /**
                 * Wifi has been restored.  Disable the broadcast receiver.  The receiver will
                 * be enabled again if a pending update discovers that wifi is not available.
                 */
                WorkerCommand.stopWifiBroadcastReceiver( context);

                LogUtil.log(LogUtil.LogType.WIFI_BROADCAST_RECEIVER, "Wifi discovered, incSync starting");
                WorkerCommand.queStartIncSync( context);

            } else {
                LogUtil.log(LogUtil.LogType.WIFI_BROADCAST_RECEIVER, "Wifi connection lost");
            }
        }
    }
}
