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

package com.nuvolect.securesuite.data;//

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.nuvolect.securesuite.webserver.Comm;
import com.nuvolect.securesuite.webserver.RestfulHtm;
import com.nuvolect.securesuite.webserver.WebUtil;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.WorkerCommand;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


/**
 * Collection of methods for syncing data over the LAN between two sqlite databases.
 */
public class SqlSyncTest {

    private static SqlSyncTest instance;
    public static final Integer MAX_PING_PONG_TESTS = 40;
    int ping_counter = 0;
    int pong_counter = 0;
    static int MIN_PAYLOAD_SIZE =  50000;
    static int MAX_PAYLOAD_SIZE = 1000000;
    int payloadSize = 0;
    int payloadIncrement = MIN_PAYLOAD_SIZE;
    private Activity m_pingPongCallbacksAct;
    private PingPongCallbacks m_pingPongCallbacks = null;
    private boolean m_continueTest = true;

    private AlertDialog dialog_alert;
    private Activity m_act;
    private ProgressDialog m_pingPongProgressDialog;

    public static synchronized SqlSyncTest getInstance() {
        if(instance == null) {
            instance = new SqlSyncTest();
        }
        return instance;
    }

    public void init(){

        m_continueTest = true;
        ping_counter = 0;
        pong_counter = 0;
    }

    public void stopTest() {

        m_continueTest = false;
    }

    //TODO complete ping pong test.  Show running data rate, success and failure.  Is it a pocket test or between two clients?
    public JSONObject ping_test(final Context ctx) {

        managePayloadSize();
        Map<String, String> parameters = makeParameters();
        parameters.put(RestfulHtm.COMM_KEYS.ping_test.toString(), String.valueOf(++ping_counter));
        String url = WebUtil.getCompanionServerUrl(CConst.RESTFUL_HTM);

        Comm.sendPost(ctx, url, parameters, new Comm.CommPostCallbacks() {
            @Override
            public void success(String response) {

                LogUtil.log(LogUtil.LogType.SQL_SYNC_TEST, RestfulHtm.COMM_KEYS.ping_test + " response: " + response);

                if (WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)) {

                    if (m_pingPongCallbacks != null) {

                        Handler handler = new Handler(Looper.getMainLooper());
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {

                                m_pingPongCallbacks.progressUpdate(m_pingPongCallbacksAct, ping_counter, pong_counter, payloadSize);

                                if (!m_continueTest)
                                    m_pingPongCallbacks.cancelDialog(m_pingPongCallbacksAct);
                            }
                        };
                        handler.post(r);
                    }
                    /**
                     * To alternate volleys, pong is kicked off by the response sender
                     * versus the machine receiving the response
                     *
                     * if( m_continueTest) WorkerCommand.quePongTest(ctx);
                     */
                }
            }

            @Override
            public void fail(String error) {

                LogUtil.log(LogUtil.LogType.SQL_SYNC_TEST, RestfulHtm.COMM_KEYS.ping_test + " error: " + error);
            }
        });
        if( m_continueTest)
            return WebUtil.response( CConst.RESPONSE_CODE_SUCCESS_100 );
        else
            return WebUtil.response( CConst.RESPONSE_CODE_USER_CANCEL_102);
    }

    public JSONObject pong_test(final Context ctx) {

        managePayloadSize();
        Map<String, String> parameters = makeParameters();
        parameters.put(RestfulHtm.COMM_KEYS.pong_test.toString(), String.valueOf( ++pong_counter));
        String url = WebUtil.getCompanionServerUrl(CConst.RESTFUL_HTM);

        Comm.sendPost(ctx, url, parameters, new Comm.CommPostCallbacks() {
            @Override
            public void success(String response) {

                LogUtil.log(LogUtil.LogType.SQL_SYNC_TEST, RestfulHtm.COMM_KEYS.pong_test + " response: " + response);

                if (WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)) {

                    if (m_pingPongCallbacks != null) {

                        Handler handler = new Handler(Looper.getMainLooper());
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {

                                m_pingPongCallbacks.progressUpdate(m_pingPongCallbacksAct, ping_counter, pong_counter, payloadSize);

                                if (!m_continueTest)
                                    m_pingPongCallbacks.cancelDialog(m_pingPongCallbacksAct);
                            }
                        };
                        handler.post(r);
                    }
                    if (m_continueTest) WorkerCommand.quePingTest(ctx);
                }
            }

            @Override
            public void fail(String error) {
                LogUtil.log(LogUtil.LogType.SQL_SYNC_TEST, RestfulHtm.COMM_KEYS.pong_test + " error: " + error);
            }
        });
        if( m_continueTest)
            return WebUtil.response( CConst.RESPONSE_CODE_SUCCESS_100 );
        else
            return WebUtil.response( CConst.RESPONSE_CODE_USER_CANCEL_102);
    }

    public void managePayloadSize(){

        payloadSize += payloadIncrement;

        if( payloadSize > MAX_PAYLOAD_SIZE ){

            payloadIncrement = -MIN_PAYLOAD_SIZE;// negative number, coming down
            payloadSize = MAX_PAYLOAD_SIZE;
        }
        else if( payloadSize < MIN_PAYLOAD_SIZE){

            payloadIncrement = MIN_PAYLOAD_SIZE;// positive number, going up
            payloadSize = MIN_PAYLOAD_SIZE;
        }
    }

    public Map<String, String> makeParameters(){

        StringBuilder builder = new StringBuilder();
        for( int i=0; i<payloadSize; i++){

            builder.append( i % 10 );
        }
        String payload = builder.toString();

        byte[]  bytesEncoded = Base64.encodeBase64(payload.getBytes());
        String payloadEncoded = new String( bytesEncoded );

        String md5_payload = com.squareup.okhttp.internal.Util.md5Hex(payload);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(CConst.MD5_PAYLOAD, md5_payload);
        parameters.put(CConst.PAYLOAD, payloadEncoded);

        return parameters;
    }

    public boolean payloadValid(String encodedPayload, String md5_payload) throws UnsupportedEncodingException {

        boolean md5_payload_test = false;
        byte[] decoded = Base64.decodeBase64( encodedPayload.getBytes("utf-8"));
        String payload = new String( decoded);
        String md5_payload_i = com.squareup.okhttp.internal.Util.md5Hex( payload );

        if( md5_payload_i.contentEquals(md5_payload))
            md5_payload_test = true;

        LogUtil.log(LogUtil.LogType.SQL_SYNC_TEST, "payload md5_test: " + md5_payload_test);

        return md5_payload_test ;
    }

    public void setProgressCallback(Activity act, PingPongCallbacks pingPongCallbacksCallbacks) {

        m_pingPongCallbacksAct = act;
        m_pingPongCallbacks = pingPongCallbacksCallbacks;
    }

    public JSONObject checkResult(String label, String numberOfTests, String md5Payload, String encodedPayload) {

        JSONObject response = null;

        if( m_continueTest){

            if( Integer.valueOf( numberOfTests ) <= MAX_PING_PONG_TESTS/2){

                try {
                    if(SqlSyncTest.getInstance().payloadValid(encodedPayload, md5Payload))
                        response = WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100);
                    else
                        response = WebUtil.response(CConst.RESPONSE_CODE_MD5_FAIL_203);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    LogUtil.logException(LogUtil.LogType.RESTFUL_HTM, e);
                }
                LogUtil.log(LogUtil.LogType.RESTFUL_HTM, label
                        +"ping test size: "+ encodedPayload.length()+", continue: " + response.toString());
            }else{
                response = WebUtil.response(CConst.RESPONSE_CODE_DONE_101); // done
                LogUtil.log(LogUtil.LogType.RESTFUL_HTM, label+" done: " + response.toString());
            }
            return response;
        }
        else
            return WebUtil.response(CConst.RESPONSE_CODE_USER_CANCEL_102);
    }

    public interface PingPongCallbacks {

        void progressUpdate(Activity act, int ping_counter, int pong_counter, int payload_size);
        void cancelDialog(Activity m_pingPongCallbacksAct);
    }

    public void pingPongConfirmDiag(final Activity act) {

        m_act = act;

        String title = "Ping pong progress?" ;
        String message = "Shall we?" ;

        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(title);
        builder.setMessage( Html.fromHtml(message));
        builder.setIcon(CConst.SMALL_ICON);
        builder.setCancelable(true);

        builder.setPositiveButton("Go for it!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                setProgressCallback(m_act, pingPongCallbacks);
                pingPongProgress(act);
                SqlSyncTest.getInstance().init();// Small amount of init on UI thread
                WorkerCommand.quePingTest(m_act);// Heavy lifting on non-UI thread
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                dialog_alert.cancel();
            }
        });
        dialog_alert = builder.create();
        dialog_alert.show();

        // Activate the HTML
        TextView tv = ((TextView) dialog_alert.findViewById(android.R.id.message));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    PingPongCallbacks pingPongCallbacks = new PingPongCallbacks() {
        @Override
        public void progressUpdate(Activity act, int ping_counter, int pong_counter, int payload_size) {

            if( m_pingPongProgressDialog != null && m_pingPongProgressDialog.isShowing()) {
                m_pingPongProgressDialog.setProgress(pong_counter + ping_counter);
                m_pingPongProgressDialog.setMessage("Payload size last test: " + payload_size);
            }
        }
        @Override
        public void cancelDialog(Activity m_pingPongCallbacksAct) {
            if( m_pingPongProgressDialog != null && m_pingPongProgressDialog.isShowing())
                m_pingPongProgressDialog.cancel();
        }
    };

    private void pingPongProgress(Activity act){

        m_pingPongProgressDialog = new ProgressDialog(m_act);
        m_pingPongProgressDialog.setTitle("Ping Pong In Progress");
        m_pingPongProgressDialog.setMessage("And away we go...");
        m_pingPongProgressDialog.setMax(SqlSyncTest.MAX_PING_PONG_TESTS);
        m_pingPongProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        m_pingPongProgressDialog.setIndeterminate(false);
        m_pingPongProgressDialog.setCancelable(false);

        m_pingPongProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        SqlSyncTest.getInstance().stopTest();
                        m_pingPongProgressDialog.cancel();
                        return;
                    }
                });
        m_pingPongProgressDialog.setProgress(0);
        m_pingPongProgressDialog.show();
    }

}
