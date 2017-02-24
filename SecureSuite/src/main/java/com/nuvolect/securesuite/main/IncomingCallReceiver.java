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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.NotificationUtil;
import com.nuvolect.securesuite.util.TimeUtil;

import net.sqlcipher.Cursor;

/**
 *
 * Incoming number during a call in android?
 http://stackoverflow.com/questions/12659536/incoming-number-during-a-call-in-android?rq=1
 *
 * How to determine if second incoming call was answered, declined, or ignored on Android?
 http://stackoverflow.com/questions/33243248/how-to-determine-if-second-incoming-call-was-answered-declined-or-ignored-on-a?rq=1
 */

public class IncomingCallReceiver extends BroadcastReceiver {

    private Context mContext;
    private Intent mIntent;
    private boolean DEBUG = LogUtil.DEBUG;
    private boolean listenerNotSet;

    /**
     * The broadcast receiver is started with a Manifest definition.
     * These two states enable and disable it.
     */
    public IncomingCallReceiver(){

        LogUtil.log(LogUtil.LogType.CALL_RECEIVER, "IncomingCallReceiver() constructor");
        listenerNotSet = true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        LogUtil.log(LogUtil.LogType.CALL_RECEIVER, "onReceive, intent.getAction(): "+action);

        Bundle bundle = intent.getExtras();
        String phoneNr= bundle.getString("incoming_number");
        LogUtil.log(LogUtil.LogType.CALL_RECEIVER, "onReceive, incoming_number: "+phoneNr);

        mContext = context;
        mIntent = intent;

        if( listenerNotSet ) {

            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int events = PhoneStateListener.LISTEN_CALL_STATE;
            tm.listen(phoneStateListener, events);

            listenerNotSet = false;
        }

    }

    private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            String callState = "UNKNOWN";
            switch (state) {

                case TelephonyManager.CALL_STATE_IDLE:
                    callState = "IDLE";
                    break;

                case TelephonyManager.CALL_STATE_RINGING:
                    // -- check international call or not.
                    if (incomingNumber.startsWith("00")) {
                        Toast.makeText(mContext,"International Call- " + incomingNumber,Toast.LENGTH_LONG).show();
                        callState = "International - Ringing (" + incomingNumber+ ")";
                    } else {
                        callState = "Local - Ringing (" + incomingNumber + ")";

                        /**
                         * Get search for contacts matching incoming number.
                         * Incoming number will have format +1aaacccdddd
                         * aaa == area code
                         * cccdddd == phone number
                         *
                         * Format number to national number prior to number search
                         */
                        String country_code = SettingsActivity.getCountryCode(mContext);
                        PhoneNumberUtil m_phoneUtil = PhoneNumberUtil.getInstance();
                        PhoneNumberUtil.PhoneNumberFormat notation = PhoneNumberUtil.PhoneNumberFormat.NATIONAL;
                        if( SettingsActivity.getInternationalNotation(mContext))
                            notation = PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL;
                        try {
                            Phonenumber.PhoneNumber phoneNumber = m_phoneUtil.parse( incomingNumber, country_code);
                            String formattedNumber = m_phoneUtil.format( phoneNumber, notation);
                            LogUtil.log(LogUtil.LogType.CALL_RECEIVER, "localNumber: "+formattedNumber);

                            Cursor c = SqlCipher.getSearchDTabCursor( formattedNumber);

//                            if( DEBUG )
//                                Util.dumpCursorDescription("phone search",c);

                            String searchResult = "";

                            if( c != null && c.getCount() > 0){

                                c.moveToFirst();
                                String callerName = c.getString(SqlCipher.DTab.display_name.ordinal());
                                LogUtil.log(LogUtil.LogType.CALL_RECEIVER, "display_name: "+callerName);
                                if( callerName != null && ! callerName.isEmpty())
                                    searchResult = "Call < " + callerName;
                                else
                                    searchResult = "Call < "+formattedNumber;
                            }
                            else{
                                searchResult = "Call < "+formattedNumber;
                            }

                            LogUtil.log(LogUtil.LogType.CALL_RECEIVER, "Toast: "+searchResult);

                            Toast.makeText(mContext, searchResult, Toast.LENGTH_LONG).show();

                            if( SettingsActivity.getNotifyIncomingCall(mContext)){

                                NotificationUtil.pushNotification(mContext,
                                    searchResult +" "+ TimeUtil.friendlyTimeHrMinSec(System.currentTimeMillis()));
                            }

                            if( c != null)
                                c.close();

                        } catch (Exception e) {
                            LogUtil.logException(mContext, LogUtil.LogType.CALL_RECEIVER, e);
                        }
                    }
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:

//                    String dialingNumber = mIntent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
//                    if( dialingNumber == null || dialingNumber.isEmpty())
//                        break;
//
//                    if (dialingNumber.startsWith("00")) {
//                        Toast.makeText(mContext,"International - " + dialingNumber,Toast.LENGTH_LONG).show();
//                        callState = "International - Dialing (" + dialingNumber+ ")";
//                    } else {
//                        Toast.makeText(mContext, "Local - " + dialingNumber,Toast.LENGTH_LONG).show();
//                        callState = "Local - Dialing (" + dialingNumber + ")";
//                    }
                    break;
                default:
                    break;
            }
            LogUtil.log(LogUtil.LogType.CALL_RECEIVER, "onCallStateChanged: " + callState+ ", number: "+incomingNumber);
            super.onCallStateChanged(state, incomingNumber);
        }
    };
}
