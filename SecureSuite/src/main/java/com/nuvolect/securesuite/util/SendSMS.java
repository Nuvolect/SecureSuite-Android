/*
 * Copyright (c) 2017. Nuvolect LLC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.nuvolect.securesuite.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;

import com.nuvolect.securesuite.util.LogUtil.LogType;

public class SendSMS {

    private static SmsCallbacks m_listener;
    private static String SENT = "SMS_SENT";
    private static int mUserInfoId;
    private static Intent mSentIntent;
    /**
     * A callback interface that all activities containing this class must implement.
     */
    public interface SmsCallbacks {

        public void sendStatus( int userInfoId, String status, int code);
    }

    public static void cancel(Context ctx){

        if( mSentIntent != null)
            PendingIntent.getBroadcast(ctx, 0, mSentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT).cancel();
    }

    public static void init(Context ctx){

        //---when the SMS has been sent---
        mSentIntent = ctx.registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {

                int resultCode = getResultCode();

                switch ( resultCode )
                {
                case Activity.RESULT_OK:
                    m_listener.sendStatus( mUserInfoId, "Message sent", resultCode);
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    m_listener.sendStatus( mUserInfoId, "Error", resultCode);
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    m_listener.sendStatus( mUserInfoId, "Error: No service", resultCode);
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    m_listener.sendStatus( mUserInfoId, "Error: Null PDU", resultCode);
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    m_listener.sendStatus( mUserInfoId, "Error: Radio off", resultCode);
                    break;
                default:
                    m_listener.sendStatus( mUserInfoId, "Error: Unmanaged", resultCode);
                    break;
                }
            }
        }, new IntentFilter(SENT));
    }

    /**
     * Send an SMS message to another device.
     * @param ctx
     * @param userInfoId - String passed back through callbacks to coordinate specific transactions
     * @param mobile - Target number
     * @param message - Body of the SMS
     * @param listener - Callback interface
     */
    public static void sendSMS(
            final Context ctx, final int userInfoId, String mobile, String message, SmsCallbacks listener) {

        try {
            m_listener = listener;
            mUserInfoId = userInfoId;

            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(mobile, null, message,
                    PendingIntent.getBroadcast( ctx, 0, new Intent(SENT), 0), null);

        } catch (Exception e) {
            LogUtil.logException(ctx, LogType.SEND_SMS, e);
            m_listener.sendStatus( mUserInfoId, "Error: Exception", 5);
        }
    }
}
