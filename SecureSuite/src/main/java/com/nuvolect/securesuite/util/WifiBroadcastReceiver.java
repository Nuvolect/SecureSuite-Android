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
