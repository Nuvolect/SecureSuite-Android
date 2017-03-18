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

package com.nuvolect.securesuite.data;//

import android.content.Context;

import com.google.gson.Gson;
import com.nuvolect.securesuite.webserver.Comm;
import com.nuvolect.securesuite.webserver.SyncRest;
import com.nuvolect.securesuite.webserver.WebUtil;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.WorkerCommand;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


/**
 * Collection of methods for syncing data over the LAN between two sqlite databases.
 * These methods run on the target device.
 */
public class SqlIncSyncTarget {

    private static SqlIncSyncTarget instance;
    private static final int SYNC_DATA_REQ_MAX = 200; // number of contacts in a single request
    private static int m_sync_data_req_size = SYNC_DATA_REQ_MAX;
    private SyncIncManifest m_source_manifest;
    private JSONObject m_syncIncrementDataObj;
    private int m_sync_data_retry;


    /**
     * Singletons are wonder things, because their the only one.
     * @return
     */
    public static synchronized SqlIncSyncTarget getInstance() {
        if(instance == null) {
            instance = new SqlIncSyncTarget();
        }
        return instance;
    }

    public JSONObject inspectSourceManifest(String jsonString) {

        /**
         * Expand the payload into a HashSet for each table and a JSONObject count summary obj.
         */
        m_source_manifest = new Gson().fromJson( jsonString, SyncIncManifest.class);
        String report =  "\ninspectSourceManifest report" +m_source_manifest.report();
        LogUtil.log(LogUtil.LogType.SQL_INC_SYNC_TARGET, report);

        //FUTURE respond with a manifest of changes.  When one device syncs, both devices sync
        return WebUtil.response( CConst.RESPONSE_CODE_SUCCESS_100 );
    }

    public void incSyncOptimizeAndStart(Context ctx) {

        /**
         * Nothing to optimize
         *
         * Kickoff the sync process.
         * First execute delete operations.
         * If anything remains in the source manifest, request source data,
         * otherwise cleanup and exit.
         */
        m_source_manifest = SqlCipher.incSyncPutIncrement(ctx,
                null, // no data object, only the manifest and only for contact and group deletes
                m_source_manifest);

        if( m_source_manifest.isEmpty())
            syncCleanup(ctx);
        else
            incSyncDataReq(ctx);
    }

    public void incSyncDataReq(Context ctx) {

        if( ! m_source_manifest.isEmpty()){

            String url = WebUtil.getCompanionServerUrl(CConst.SYNC);
            Map<String, String> parameters = new HashMap<String, String>();

            /**
             * Build the next increment of the plan.  Each pass the remaining manifest is sent
             * to the source server.  The source server will respond with data to a maximum size.
             * The source manifest is updated and returned as a requirement to the source server
             * until the entire manifest is satisfied.
             */
            String request = new Gson().toJson(m_source_manifest);
            parameters.put(SyncRest.COMM_KEYS.src_inc_sync_data_req.toString(), request);

            Comm.sendPost(ctx, url, parameters, new Comm.CommPostCallbacks() {
                @Override
                public void success(String response) {

                    LogUtil.log(LogUtil.LogType.SQL_INC_SYNC_TARGET, SyncRest.COMM_KEYS.src_inc_sync_data_req + " response: " + response);
                }

                @Override
                public void fail(String error) {

                    LogUtil.log(LogUtil.LogType.SQL_INC_SYNC_TARGET, SyncRest.COMM_KEYS.src_inc_sync_data_req + " error: " + error);
                }
            });
        }
        else
            LogUtil.log(LogUtil.LogType.SQL_INC_SYNC_TARGET, SyncRest.COMM_KEYS.src_inc_sync_data_req +" no data, all done");
    }

    public JSONObject incSyncInspectData(Context ctx, String payload, String md5_payload) {

        try {
            //FUTURE, figure out why test md5 on payload fails yet payload is valid
            m_syncIncrementDataObj = new JSONObject( payload );

            if( m_syncIncrementDataObj.length() == 0)
                return WebUtil.response( CConst.RESPONSE_CODE_PAYLOAD_EMPTY_206);

        } catch (JSONException e) {
            e.printStackTrace();
            LogUtil.logException(LogUtil.LogType.SQL_INC_SYNC_TARGET, e);
            return WebUtil.response( CConst.RESPONSE_CODE_JSON_ERROR_200 );
        } catch (Exception e){
            e.printStackTrace();
            LogUtil.logException(LogUtil.LogType.SQL_INC_SYNC_TARGET, e);
            return WebUtil.response( CConst.RESPONSE_CODE_EXCEPTION_202 );
        }
        m_sync_data_retry = 0;

        return WebUtil.response( CConst.RESPONSE_CODE_SUCCESS_100 );
    }

    public void incSyncProcessData(Context ctx) {
        /**
         * Process the next increment of data while updating the source manifest.
         * Each row that is process is removed from the manifest.
         */
        m_source_manifest = SqlCipher.incSyncPutIncrement(ctx, m_syncIncrementDataObj, m_source_manifest);

        LogUtil.log(LogUtil.LogType.SQL_INC_SYNC_TARGET, "processSyncData iteration complete, manifest remaining: "
                + m_source_manifest.report());

        if( m_source_manifest.isEmpty()){

            syncCleanup(ctx);
        }else{

            /**
             * The plan is not complete.  Make another request.
             */
            incSyncDataReq(ctx);
        }
    }

    private void syncCleanup(Context ctx) {

        String report = ""
                +"\n\n\n"
                +"\nIncremental synchronization complete (target)"
                +m_source_manifest.report()
                ;
        LogUtil.log(LogUtil.LogType.SQL_INC_SYNC_TARGET, report);
        LogUtil.log(LogUtil.LogType.SQL_INC_SYNC_TARGET, SqlCipher.dbCounts());

        m_source_manifest.init();
        m_syncIncrementDataObj = new JSONObject();

        sendIncSyncEnd(ctx);
    }

    private void sendIncSyncEnd(Context ctx) {

        // Save status for display in the settings page
        SqlIncSync.getInstance().setIncomingUpdate();

        WorkerCommand.refreshUserInterface(ctx, CConst.RECREATE);

        String url = WebUtil.getCompanionServerUrl(CConst.SYNC);
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put(SyncRest.COMM_KEYS.src_inc_sync_end.toString(), "no_errors");

        Comm.sendPost(ctx, url, parameters, new Comm.CommPostCallbacks() {
            @Override
            public void success(String response) {

                LogUtil.log(LogUtil.LogType.SQL_INC_SYNC_TARGET, SyncRest.COMM_KEYS.src_inc_sync_end + " response: " + response);
            }

            @Override
            public void fail(String error) {

                LogUtil.log(LogUtil.LogType.SQL_INC_SYNC_TARGET, SyncRest.COMM_KEYS.src_inc_sync_end + " error: " + error);
            }
        });
    }
}
