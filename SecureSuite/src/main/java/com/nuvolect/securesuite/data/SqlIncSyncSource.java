package com.nuvolect.securesuite.data;//

import android.content.Context;

import com.google.gson.Gson;
import com.nuvolect.securesuite.webserver.Comm;
import com.nuvolect.securesuite.webserver.RestfulHtm;
import com.nuvolect.securesuite.webserver.WebUtil;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.WorkerCommand;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static com.nuvolect.securesuite.util.LogUtil.LogType.SQL_INC_SYNC_SRC;


/**
 * Collection of methods for syncing data over the LAN between two sqlite databases.
 * These methods run on the source device.
 */
public class SqlIncSyncSource {

    private static SqlIncSyncSource instance;

    private static final int MAX_PAYLOAD_SIZE = 750000;// plus encoding will be less than 1 MB
    private boolean m_continue_sync = true;
    private long m_time_last_sync = 0;
    private SyncIncManifest m_request_manifest;

    public static synchronized SqlIncSyncSource getInstance() {
        if(instance == null) {
            instance = new SqlIncSyncSource();
        }
        return instance;
    }

    /**
     * FIXME, while syncing a user may try to import more data.  Manage the process to be seamless.
     *
     * A test fails when 128 contacts have been imported and while they sync a second batch
     * of 128 is imported.
     */
    /**
     * Start the sync process.  Run through startup test to confirm a sync can take place.
     * Build a manifest of updates and send it to the companion device.  The companion device
     * will use the manifest to request data from the source.
     * @param ctx
     * @return
     */
    public JSONObject startIncSyncSource(Context ctx) {

        if( getSyncInProgress()){

            LogUtil.log(SQL_INC_SYNC_SRC, "startIncSyncSource, sync already in progress");
            return WebUtil.response(CConst.RESPONSE_CODE_SYNC_IN_PROCESS_201);
        }

        if( ! WebUtil.companionServerAssigned()) {
            LogUtil.log(SQL_INC_SYNC_SRC, "startIncSyncSource, no backup server ");
            return WebUtil.response(CConst.RESPONSE_CODE_NO_BACKUP_SERVER_205);
        }

        if( ! WebUtil.wifiEnabled( ctx )){

            LogUtil.log(SQL_INC_SYNC_SRC, "startIncSyncSource, Wifi not enabled, enabling wifi broadcast receiver ");
            /**
             * Start WiFi broadcast receiver.  When WiFi is restored the receiver will fire
             * and the sync process will be restarted.
             */
            WorkerCommand.startWifiBroadcastReceiver(ctx);

            return WebUtil.response(CConst.RESPONSE_CODE_WIFI_NOT_ENABLED_208);
        }

        try {
            /**
             * Test to see if the companion server can be reached.
             */
            String ip = WebUtil.getCompanionServerIp();
            if( ! InetAddress.getByName(ip).isReachable(CConst.IP_TEST_TIMEOUT_MS)) {

                LogUtil.log(SQL_INC_SYNC_SRC, "startIncSyncSource, companion device not reachable: "+ip);

                return WebUtil.response(CConst.RESPONSE_CODE_IP_NOT_REACHABLE_207);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
         * All the entry tests have passed.  Set in progress to true.
         * Later, set it to false on exit
         */
        setSyncInProgress(true );
        LogUtil.log(SQL_INC_SYNC_SRC, "startIncSyncSource sync tests passed, starting...");

        /**
         * Build and optimize the manifest of updates to share with the companion device.
         */
        SyncIncManifest syncIncManifest = new SyncIncManifest();
        syncIncManifest.loadDbIncSync();
        syncIncManifest.optimize();

        /**
         * If there is no work to be done make an early exit.
         */
        if( syncIncManifest.isEmpty()) {
            LogUtil.log(SQL_INC_SYNC_SRC, "startIncSyncSource, manifest is empty, nothing to sync");
            return WebUtil.response(CConst.RESPONSE_CODE_EMPTY_MANIFEST_103);
        }

        String jsonString = new Gson().toJson(syncIncManifest);
        LogUtil.log(SQL_INC_SYNC_SRC, "startIncSyncSource starting, manifest report: \n"+syncIncManifest.report());
        LogUtil.log(SQL_INC_SYNC_SRC, "startIncSyncSource starting, jsonString: \n"+jsonString);
//        m_continue_sync = true;

        String url = WebUtil.getCompanionServerUrl(CConst.RESTFUL_HTM);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(RestfulHtm.COMM_KEYS.tgt_inc_sync_source_manifest.toString(), jsonString);

        Comm.sendPost(url, parameters, new Comm.CommPostCallbacks() {
            @Override
            public void success(String response) {

                /**
                 * The request was successful.  The companion device will respond with a request for data.
                 */
                LogUtil.log(SQL_INC_SYNC_SRC, RestfulHtm.COMM_KEYS.tgt_inc_sync_source_manifest + " response: " + response);
            }

            @Override
            public void fail(String error) {

//                CrypServer.notify(uniqueId, "Co-device not found. Is WiFi enabled?", "warn");
                LogUtil.log(SQL_INC_SYNC_SRC, RestfulHtm.COMM_KEYS.tgt_inc_sync_source_manifest + " error: " + error);
                setSyncInProgress( false );
            }
        });
        return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100);
    }

    /**
     * Set the state of syncing.  When sync is starting the current sync time is captured.
     * When sync has stopped the sync time is set to zero.
     * @param newSyncInProgress
     */
    private void setSyncInProgress(boolean newSyncInProgress) {

        if( newSyncInProgress)
            m_time_last_sync = System.currentTimeMillis();
        else
            m_time_last_sync = 0;
    }

    /**
     * Check if a sync is in progress.  If the time since a sync was last started is within
     * a TIMEOUT period, assume a sync is in process.  When a sync completes the
     * time of the last sync is set to zero allowing for an immediate sync.
     * This solves problems when networks go up and down and there are multiple attempts to sync.
     * @return
     */
    private boolean getSyncInProgress() {

        long ms_since_last_sync = System.currentTimeMillis() - m_time_last_sync;

        if( ms_since_last_sync < CConst.FAILSAFE_SYNC_TIMEOUT){

            return true;
        }else{

            m_time_last_sync = 0;  // long long time ago
            return false;
        }
    }

    public JSONObject inspectSyncDataRequest(String jsonString) {

        m_request_manifest = new Gson().fromJson(jsonString, SyncIncManifest.class);

        LogUtil.log(SQL_INC_SYNC_SRC, "inspectSyncDataRequest: " + m_request_manifest.report());

        if( m_continue_sync && ! m_request_manifest.isEmpty())
            return WebUtil.response( CConst.RESPONSE_CODE_SUCCESS_100 );
        else
            return WebUtil.response( CConst.RESPONSE_CODE_USER_CANCEL_102);
    }

    public void incSyncDataSend(Context ctx) {

        if( ! m_continue_sync)
            return;
        /**
         * Build the increment
         */
        JSONObject jsonObject = SqlCipher.incSyncGetIncrement(ctx, m_request_manifest, MAX_PAYLOAD_SIZE);

        /**
         * Communicate the batch of contact data to the companion server
         */
        String url = WebUtil.getCompanionServerUrl(CConst.RESTFUL_HTM);

        String payload = jsonObject.toString();
        String md5_payload = com.squareup.okhttp.internal.Util.md5Hex(payload);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(RestfulHtm.COMM_KEYS.tgt_inc_sync_data.toString(), payload );
        parameters.put(CConst.MD5_PAYLOAD, md5_payload);

        LogUtil.log(SQL_INC_SYNC_SRC, "incSyncDataSend, json package length: "+jsonObject.length());

        Comm.sendPost(ctx, url, parameters, new Comm.CommPostCallbacks() {
            @Override
            public void success(String response) {

                /**
                 * The request was successful.  The companion device will process the data
                 * and respond with the next step.
                 */
                LogUtil.log(SQL_INC_SYNC_SRC, RestfulHtm.COMM_KEYS.tgt_inc_sync_data + " response: " + response);
            }

            @Override
            public void fail(String error) {

                setSyncInProgress( false );
                LogUtil.log(SQL_INC_SYNC_SRC, RestfulHtm.COMM_KEYS.tgt_inc_sync_data + " error: " + error);
            }
        });
    }

    public void incSyncEnd(Context ctx) {

        LogUtil.log(SQL_INC_SYNC_SRC, "incSyncEnd");

        /**
         * When initiated from Android, it will refresh itself when necessary.
         * Other cases requiring filtering:
         * Remote Android: refresh contact list and group detail
         * Web apps: refresh listHtm. If active, post notice, "Data updated, please refresh"
         *
         * Also consider DetailHtm, ContactDetailFragment and ContactDetail. The user may have
         * this on their screen when the contact is deleted on a remote device.
         */

        SqlCipher.dropIncSyncTable();
        LogUtil.log(SQL_INC_SYNC_SRC, "syncCleanup\n" + SqlCipher.dbCounts());

        // Save status for display in the settings page
        SqlIncSync.getInstance().setOutgoingUpdate();

        /**
         * Finally set sync status to not in progress
         */
        setSyncInProgress( false );
    }
}
