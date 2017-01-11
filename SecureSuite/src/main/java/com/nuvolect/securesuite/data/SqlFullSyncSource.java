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
import android.widget.Toast;

import com.google.gson.Gson;
import com.nuvolect.securesuite.main.App;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.main.DialogUtil;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.WorkerCommand;
import com.nuvolect.securesuite.webserver.Comm;
import com.nuvolect.securesuite.webserver.RestfulHtm;
import com.nuvolect.securesuite.webserver.WebUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;


/**
 * Collection of methods for syncing data over the LAN between two sqlite databases.
 * These methods run on the source device.
 */
public class SqlFullSyncSource {

    private static SqlFullSyncSource instance;

    private static final int MAX_PAYLOAD_SIZE = 250000;// With encoding it will be slightly larger
    private AlertDialog dialog_alert;
    private ProgressDialog m_progressDialog;
    private boolean m_continue_sync = true;
    private SqlFullSyncTarget.SourceManifest m_request_manifest;
    private int m_manifest_total;

    public static synchronized SqlFullSyncSource getInstance() {
        if(instance == null) {
            instance = new SqlFullSyncSource();
        }
        return instance;
    }

    /**
     * Start the full sync process.
     * Assemble and send a manifest of source data to the target SecureSuite device.
     * The target will use this to query data until everything from the manifest is sent.
     * @param m_ctx
     */
    public JSONObject fullSyncSourceManifest(Context m_ctx) {

        LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_SRC, "fullSync starting...");
        m_continue_sync = true;

        try {
            String ip = WebUtil.getCompanionServerIpPort();
            int timeout = 2000;
            if( ! InetAddress.getByName(ip).isReachable( timeout)) {
                Toast.makeText(m_ctx, "Device "+ip+" not reachable",Toast.LENGTH_SHORT).show();
                return WebUtil.response(CConst.RESPONSE_CODE_IP_NOT_REACHABLE_207);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        String url = WebUtil.getCompanionServerUrl(CConst.RESTFUL_HTM);
        Map<String, String> parameters = new HashMap<String, String>();

        JSONObject manifest = SqlCipher.getSourceManifest();
        try {
            m_manifest_total = manifest.getJSONObject(CConst.COUNTS).getInt(CConst.TOTAL);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        parameters.put(RestfulHtm.COMM_KEYS.tgt_full_sync_source_manifest.toString(), manifest.toString());

        Comm.sendPost(m_ctx, url, parameters, new Comm.CommPostCallbacks() {
            @Override
            public void success(String response) {

                updateProgressDialog(2);// Push along a bit so user knows connectivity is working
                LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_SRC, RestfulHtm.COMM_KEYS.tgt_full_sync_source_manifest + " response: " + response);
            }

            @Override
            public void fail(String error) {

//                CrypServer.notify(uniqueId, "Co-device not found. Is WiFi enabled?", "warn");
                LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_SRC, RestfulHtm.COMM_KEYS.tgt_full_sync_source_manifest + " error: " + error);
            }
        });
        return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100);
    }

    /**
     * Inspect the data request and if the data appears valid send success response.
     * @param jsonString
     * @return
     * Output: m_sync_data_request : JSONArray of long, contact_id to sync
     */
    public JSONObject inspectSyncDataRequest(String jsonString) {

        m_request_manifest = new Gson().fromJson(jsonString, SqlFullSyncTarget.SourceManifest.class);

        LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_SRC, "inspectSyncDataRequest: " + m_request_manifest.report());

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable r = new Runnable() {
            @Override
            public void run() {

                m_backupCallbacks.progressUpdate( m_request_manifest.size(), m_manifest_total);
            }
        };
        handler.post(r);

        if( m_continue_sync)
            return WebUtil.response( CConst.RESPONSE_CODE_SUCCESS_100 );
        else
            return WebUtil.response( CConst.RESPONSE_CODE_USER_CANCEL_102);
    }

    /**
     * Build a sync data increment object and send it back.
     * @param ctx
     * Input: m_sync_data_request : JSONArray of long, contact_id to sync
     */
    public void fullSyncDataSend(Context ctx) {

        if( ! m_continue_sync)
            return;
        /**
         * Build the increment
         */
        JSONObject jsonObject = SqlCipher.getNextFullSyncIncement(m_request_manifest, MAX_PAYLOAD_SIZE);

        /**
         * Communicate the batch of contact data to the companion server
         */
        String url = WebUtil.getCompanionServerUrl(CConst.RESTFUL_HTM);

        String payload = jsonObject.toString();
        String md5_payload = com.squareup.okhttp.internal.Util.md5Hex(payload);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(RestfulHtm.COMM_KEYS.tgt_full_sync_data.toString(), payload );
        parameters.put(CConst.MD5_PAYLOAD, md5_payload);

        Comm.sendPost(ctx, url, parameters, new Comm.CommPostCallbacks() {
            @Override
            public void success(String response) {

                /**
                 * The request was successful.  The companion device will process the data
                 * and respond with the next step.
                 */
                LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_SRC, RestfulHtm.COMM_KEYS.tgt_full_sync_data + " response: " + response);
            }

            @Override
            public void fail(String error) {

                LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_SRC, RestfulHtm.COMM_KEYS.tgt_full_sync_data + " error: " + error);
            }
        });
    }

    public void stopSync() {

        m_continue_sync = false;
    }

    public void cancelDialog(){

        m_backupCallbacks.cancelDialog();
    }

    public void fullSyncEnd(Context m_ctx) {

        SqlFullSyncSource.getInstance().cancelDialog();
        LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_SRC, "fullSyncEnd\n" + SqlCipher.dbCounts());

        LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_SRC, "fullSyncEnd, purge IncSyncTable");
        SqlCipher.dropIncSyncTable();

        // Save status for display in the settings page
        SqlIncSync.getInstance().setOutgoingUpdate();

//        WorkerCommand.refreshUserInterface(m_ctx);
    }

    public interface BackupCallbacks {

        void progressUpdate(int remaining, int total);
        void cancelDialog();
    }

    private void updateProgressDialog(int progress){

        if( m_progressDialog != null && m_progressDialog.isShowing()) {
            m_progressDialog.setProgress( progress );
        }
    }

    BackupCallbacks m_backupCallbacks = new BackupCallbacks() {
        @Override
        public void progressUpdate(int remaining, int total) {

            if( m_progressDialog != null && m_progressDialog.isShowing()) {
                int progress = (100*(total - remaining)) / total;
                /**
                 * Hardwired a bit.  After manifest is set, progress is set to 2%.
                 * The first time called here, it resets to zero.
                 */
                if( progress > 2)
                    m_progressDialog.setProgress((100*(total - remaining)) / total);
            }
        }

        @Override
        public void cancelDialog() {

            if( m_progressDialog != null && m_progressDialog.isShowing())
                m_progressDialog.cancel();
        }
    };

    /**
     * UI for backing up to a remote device over the LAN.
     * First confirm user wants to take this action, it will wipe the remote device.
     * Pass the progressCallBacks link to the SqlSyncSource class.
     * Start the backup process.
     * Provide a cancel button to stop the process.
     * //TODO show a dialog on the remote device with backup progress.
     * @param act
     * @return
     */
    public void backupDiag(final Activity act) {

        if( WebUtil.getCompanionServerIpPort().contentEquals(App.DEFAULT_IP_PORT)){

            DialogUtil.dismissDialog( act,
                    "The Companion SecureSuite Device is undefined",
                    "Select Menu, Settings, Companion SecureSuite IP, and enter the remote \"My SecureSuite IP\" address.");
            return;
        }
        String title = "Backup to SecureSuite over the LAN?" ;
        String message = "The remote SecureSuite will be wiped and replaced.  You can't undo this action." ;

        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(title);
        builder.setMessage( Html.fromHtml(message));
        builder.setIcon(CConst.SMALL_ICON);
        builder.setCancelable(true);

        builder.setPositiveButton("Backup", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                backupProgress(act);
                WorkerCommand.fullSyncSendSourceManifest(act);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                dialog_alert.cancel();
                stopSync();
            }
        });
        dialog_alert = builder.create();
        dialog_alert.show();

        // Activate the HTML
        TextView tv = ((TextView) dialog_alert.findViewById(android.R.id.message));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void backupProgress(Activity act){

        m_progressDialog = new ProgressDialog(act);
        m_progressDialog.setTitle("SecureSuite Backup In Progress");
        m_progressDialog.setMessage("This may take a few moments...");
        m_progressDialog.setMax(100);
        m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        m_progressDialog.setIndeterminate(false);
        m_progressDialog.setCancelable(false);

        m_progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        SqlFullSyncSource.getInstance().stopSync();
                        m_progressDialog.cancel();
                        return;
                    }
                });
        m_progressDialog.setProgress(0);
        m_progressDialog.show();
    }

}
