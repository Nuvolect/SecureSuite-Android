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
import com.nuvolect.securesuite.webserver.RestfulHtm;
import com.nuvolect.securesuite.webserver.WebUtil;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.WorkerCommand;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/**
 * Collection of methods for syncing data over the LAN between two sqlite databases.
 * These methods run on the target device.
 *
 * Data synchronization is accomplished through a series server to server communications.
 * Each pass the remaining manifest is sent to the source server.
 * The source server will respond with data to a maximum size.
 * The source manifest is updated and returned as a requirement to the source server
 * until the entire manifest is satisfied.
 */
public class SqlFullSyncTarget {

    private static SqlFullSyncTarget instance;
    private static final int SYNC_DATA_REQ_MAX = 200; // number of contacts in a single request
    private static int m_sync_data_req_size = SYNC_DATA_REQ_MAX;
    /**
     * Plan keyed with contact_id, value is version.
     */
    private ArrayList<Long> m_failedContacts = new ArrayList<Long>();

    private JSONObject m_syncIncrementDataObj;
    private SourceManifest m_source_manifest;
    private int m_sync_data_retry;

    /**
     * Singletons are wonder things, because their the only one.
     * @return
     */
    public static synchronized SqlFullSyncTarget getInstance() {
        if(instance == null) {
            instance = new SqlFullSyncTarget();
        }
        return instance;
    }

    /** Data structure used to track data synchronization requirements */
    public class SourceManifest{
        HashSet<Integer> detail_table;       // _id for each record
        HashSet<Integer> account_table;      // _id for each record
        HashSet<Integer> account_data_table; // _id for each record
        HashSet<Integer> group_title_table;  // _id for each record
        HashSet<Integer> account_cryp_table; // _id for each record
        JSONObject counts;

        /**
         * For somereason Gson is creating an empty counts object, when it is passed complete data.
         * This hack recreates the counts object.
         * //FUTURE fix counts Gson conversion
         */
        public void setCounts(){

            counts = new JSONObject();
            try {
                counts.put(SqlCipher.DETAIL_TABLE, detail_table.size());
                counts.put(SqlCipher.ACCOUNT_TABLE, account_table.size());
                counts.put(SqlCipher.ACCOUNT_DATA_TABLE, account_data_table.size());
                counts.put(SqlCipher.GROUP_TITLE_TABLE, group_title_table.size());
                counts.put(SqlCipher.ACCOUNT_CRYP_TABLE, account_cryp_table.size());
                counts.put(CConst.TOTAL, size());

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        /** True when the number of elements remaining is zero */
        public boolean isEmpty(){

            if( detail_table.size() > 0)
                return false;
            if( account_table.size() > 0)
                return false;
            if( account_data_table.size() > 0)
                return false;
            if( group_title_table.size() > 0)
                return false;
            if( account_cryp_table.size() > 0)
                return false;

            return true;
        }

        /** Number of manifest items remaining */
        public int size(){

            int size = detail_table.size();
            size += account_table.size();
            size += account_data_table.size();
            size += group_title_table.size();
            size += account_cryp_table.size();

            return size;
        }

        /** Total size of the manifest as originally requested */
        public int total(){

            int total = 0;
            try {
                total = counts.getJSONObject(CConst.COUNTS).getInt(CConst.TOTAL);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return total;
        }
        /** Simple string report of the manifest contents */
        public String report(){

            String report = "";
            try {
                report = ""
                        +"\ndetail_table      : "+detail_table.size()
                        +"\naccount_table     : "+account_table.size()
                        +"\naccount_data_table: "+account_data_table.size()
                        +"\ngroup_title_table : "+group_title_table.size()
                        +"\naccount_cryp_table: "+account_cryp_table.size()
                        +"\ncounts            : "+counts.toString(4);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return report;
        }
    }

    /**
     * Save the version data. Return a response with success or fail code.
     * @param jsonString
     * @return
     */
    public JSONObject inspectSourceManifest(String jsonString) {

        /**
         * Expand the payload into a HashSet for each table and a JSONObject count summary obj.
         */
        m_source_manifest = new Gson().fromJson( jsonString, SourceManifest.class);
        m_source_manifest.setCounts();
        String report =  "\ninspectSourceManifest report" +m_source_manifest.report();
        LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_TARGET, report);

        return WebUtil.response( CConst.RESPONSE_CODE_SUCCESS_100 );
    }

    /**
     * Clear the existing database and kickoff the data request process.
     */
    public void fullSyncStart(Context ctx){

        LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_TARGET, "startFullSync: clearing database");
        SqlCipher.dropTables();

        /**
         * Kickoff the sync process
         */
        fullSyncDataReq(ctx);
    }

    /**
     * Make the next data request to the source server.
     * @param ctx
     */
    public void fullSyncDataReq(Context ctx) {

        if( ! m_source_manifest.isEmpty()){

            String url = WebUtil.getCompanionServerUrl(CConst.RESTFUL_HTM);
            Map<String, String> parameters = new HashMap<String, String>();

            /**
             * Build the next increment of the plan.  Each pass the remaining manifest is sent
             * to the source server.  The source server will respond with data to a maximum size.
             * The source manifest is updated and returned as a requirement to the source server
             * until the entire manifest is satisfied.
             */
            String request = new Gson().toJson(m_source_manifest);
            parameters.put(RestfulHtm.COMM_KEYS.src_full_sync_data_req.toString(), request);

            Comm.sendPost(ctx, url, parameters, new Comm.CommPostCallbacks() {
                @Override
                public void success(String response) {

                    LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_TARGET, RestfulHtm.COMM_KEYS.src_full_sync_data_req + " response: " + response);
                }

                @Override
                public void fail(String error) {

                    LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_TARGET, RestfulHtm.COMM_KEYS.src_full_sync_data_req + " error: " + error);
                }
            });
        }
        else
            LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_TARGET, RestfulHtm.COMM_KEYS.src_full_sync_data_req +" no data, all done");
    }

    /**
     * Inspect the array of incoming sync data. Respond with error when the MD5 check or JSON check fails,
     * otherwise respond with a successful response.
     * @param payload
     * @param md5_payload
     * @return response success/fail
     * output: m_syncIncrementDataObject
     */
    public JSONObject inspectSyncData( Context ctx, String payload, String md5_payload) {

        try {
            m_syncIncrementDataObj = new JSONObject( payload );

        } catch (JSONException e) {
            e.printStackTrace();
            LogUtil.logException(LogUtil.LogType.SQL_FULL_SYNC_TARGET, e);
            return WebUtil.response( CConst.RESPONSE_CODE_JSON_ERROR_200 );
        } catch (Exception e){
            e.printStackTrace();
            LogUtil.logException(LogUtil.LogType.SQL_FULL_SYNC_TARGET, e);
            return WebUtil.response( CConst.RESPONSE_CODE_EXCEPTION_202 );
        }
        m_sync_data_retry = 0;

        return WebUtil.response( CConst.RESPONSE_CODE_SUCCESS_100 );
    }

    /**
     * Process an increment of sync data and write it to the database.
     * @param ctx
     * Input: m_syncIncrementDataObject
     * Output: sqlite database
     * Output: m_optimizedSyncPlan : synced contacts are removed
     */
    public void fullSyncDataProcess(Context ctx) {

        /**
         * Process the next increment of data while updating the source manifest.
         * Each row that is process is removed from the manifest.
         */
        m_source_manifest = SqlCipher.putNextSyncIncrement( m_syncIncrementDataObj, m_source_manifest );

        LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_TARGET, "processSyncData iteration complete, manifest remaining: "
                + m_source_manifest.report());

        if( m_source_manifest.isEmpty()){

            syncCleanup(ctx);
        }else{

            /**
             * The plan is not complete.  Make another request.
             */
            fullSyncDataReq(ctx);
        }
    }

    /** Cleanup from sync, release large memory structures and advance to sendFullSyncEnd. */
    private void syncCleanup(Context ctx){

        String report = ""
                +"\n\n\n"
                +"\nFull synchronization complete"
                +m_source_manifest.report()
                +"\nFailed : "+m_failedContacts.size()//FUTURE implement or remove
                ;
        LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_TARGET, report);
        LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_TARGET, SqlCipher.dbCounts());

        // Release memory
        m_failedContacts = new ArrayList<Long>();

        sendFullSyncEnd(ctx);
    }

    /**
     * Send a message to the source machine indicating the full sync is complete.
     * @param ctx
     */
    private void sendFullSyncEnd(Context ctx) {

        WorkerCommand.refreshUserInterface(ctx, CConst.RECREATE);

        // Save status for display in the settings page
        SqlIncSync.getInstance().setIncomingUpdate();

        String url = WebUtil.getCompanionServerUrl(CConst.RESTFUL_HTM);
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put(RestfulHtm.COMM_KEYS.src_full_sync_end.toString(), "no_errors");

        Comm.sendPost(ctx, url, parameters, new Comm.CommPostCallbacks() {
            @Override
            public void success(String response) {

                LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_TARGET, RestfulHtm.COMM_KEYS.src_full_sync_end + " response: " + response);
            }

            @Override
            public void fail(String error) {

                LogUtil.log(LogUtil.LogType.SQL_FULL_SYNC_TARGET, RestfulHtm.COMM_KEYS.src_full_sync_end + " error: " + error);
            }
        });
    }

    /**
     * Check to see if there are any contacts remaining to sync.
     * @return true/false
     */
    public boolean syncInProcess() {

        return ! m_source_manifest.isEmpty();
    }

}
