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

package com.nuvolect.securesuite.webserver;//

import android.content.Context;

import com.nuvolect.securesuite.data.SqlFullSyncSource;
import com.nuvolect.securesuite.data.SqlFullSyncTarget;
import com.nuvolect.securesuite.data.SqlIncSyncSource;
import com.nuvolect.securesuite.data.SqlIncSyncTarget;
import com.nuvolect.securesuite.data.SqlSyncTest;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.WorkerCommand;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

/**
 * Generate and manage the utility page and RESTful services.
 */
public class RestfulHtm {

    private static Context m_ctx;
    private static String templateFile = "restful.htm";

    static String[] param_keys = {

            CConst.SEC_TOK,
            CConst.ACCOUNT_ID,
            CConst.CONTACT_ID,
            CConst.GROUP_ID,
            CConst.MD5_PAYLOAD,
            CConst.PAYLOAD,
            CConst.URI,
            CConst.QUERY_PARAMETER_STRINGS,
    };
    private static boolean isParameterKey(String key) {

        for( String param_key : param_keys){

            if( param_key.contentEquals( key ))
                return true;
        }
        return false;
    }

    public static enum COMM_KEYS {NIL,

        /**
         * Send this IP to self.  Success is when this IP matches the definition of self.
         */
        self_ip_test,
        /**
         * Send this IP to the companion.  Success is when this IP is defined as companion
         * on the companion device.
         */
        companion_ip_test,
        /**
         * Register companion device.  Used to associate a second SecureSuite device
         * to serve as a backup and to share contact/password data updates.
         * A security token SEC_TOK is passed that is used with all future communications.
         */
        register_companion_device,

//////////////////////////////////////////////////////////////////////////////////////
        /**
         * Internal request to start the full sync process.
         * This will initiate full_sync_versions to the target SecureSuite device
         */
        src_full_sync_start,

        /**
         * Receive _id of all records on the target SecureSuite device.
         * It will respond with a series of sync_data_req calls.
         */
        tgt_full_sync_source_manifest,

        /**
         * Request specific data from the source SecureSuite device.
         * Data is supplied as a JSONArray of contacts in simple form.
         */
        src_full_sync_data_req,

        /**
         * Send a JSONArray of contacts in simple form.
         */
        tgt_full_sync_data,

        /**
         * Sent when the sync plan is empty and sync is complete.
         * Time to clean up.
         */
        src_full_sync_end,
//////////////////////////////////////////////////////////////////////////////////////
        /**
         * Send a manifest describing updates to the target device.
         * It in turn will respond with requests for updates.
         */
        tgt_inc_sync_source_manifest,

        /**
         * Request incremental data from the source
         */
        src_inc_sync_data_req,

        /**
         * Send requested data
         */
        tgt_inc_sync_data,

        /**
         * Send when all data has been supplied and incremental sync is complete.
         */
        src_inc_sync_end,
//////////////////////////////////////////////////////////////////////////////////////

        /**
         * Communication test.  Bounce data back and forth to exercise communications.
         */
        ping_test,
        pong_test,

        /**
         * Basic parameters for routing and speciality query
         */
        uri,                    // Full uri for routing
        queryParameterStrings,  // Raw parameters
    }

    public static void init(Context ctx){

        m_ctx = ctx;
    }

    public static String render(Context ctx, String uniqueId, Map<String, String> params) {

        m_ctx = ctx;

        /**
         * Parse parameters and process any updates.
         * Return an Action indicating what to do next
         */
        String action = parse(uniqueId, params);// set mContactId & others based on params

        if( action.startsWith("download:"))  // Check for download, action string includes filename
            return action;

        if( action.startsWith("{"))  // Check for jsonObject
            return action;

        if( action.isEmpty())  // Check for empty string
            return "";

        /**
         * Generate the page html and return it as the session response
         */
        return generateHtml( uniqueId, params);
    }

    private static String generateHtml(String uniqueId, Map<String, String> params) {

        String generatedHtml = "";

        try {
            MiniTemplator t = new MiniTemplator(WebService.assetsDirPath + "/" + templateFile);

            t.setVariable("notify_js", CrypServer.getNotify(uniqueId));

            generatedHtml = t.generateOutput();

        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.logException(LogUtil.LogType.RESTFUL_HTM, e);
        }

        return generatedHtml;
    }

    private static String parse(final String uniqueId, Map<String, String> params) {

        String key = "";
        String value = "";

        for (Map.Entry<String, String> entry : params.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            if( value == null)
                value = "";

            if( LogUtil.DEBUG){
                String str = value.length() > 30 ? value.substring(0, 30) : value;
                LogUtil.log(LogUtil.LogType.RESTFUL_HTM, "key, value: " + key+", "+str);
            }

            if( isParameterKey(key)) {// Used to key a parameter, not a command key
                continue;
            }

            COMM_KEYS key_enum = COMM_KEYS.NIL;
            try {
                key_enum = COMM_KEYS.valueOf(key);
            } catch (Exception e) {
                LogUtil.log(LogUtil.LogType.RESTFUL_HTM, "Error unknown key: " + key);
                LogUtil.logException(m_ctx, LogUtil.LogType.RESTFUL_HTM, e);
            }

            switch (key_enum) {

                case NIL:

                case self_ip_test:{

                    if( value.contentEquals(WebUtil.getServerIpPort(m_ctx)))
                        return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100).toString();
                    else
                        return WebUtil.response(CConst.RESPONSE_CODE_SELF_TEST_FAIL_211).toString();
                }

                case companion_ip_test:{

                    if( value.contentEquals(WebUtil.getCompanionServerIpPort()))
                        return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100).toString();
                    else
                        return WebUtil.response(CConst.RESPONSE_CODE_COMPANION_TEST_FAIL_210).toString();
                }

                case register_companion_device:{

                    /**
                     * Only allow registration when device is in pairing mode, this is the
                     * only time host verification is disabled.
                     */
                    if( ! WebUtil.NullHostNameVerifier.getInstance().m_hostVerifierEnabled ){

                        String sec_tok = params.get(CConst.SEC_TOK);
                        CrypServer.setSecTok( sec_tok );

                        WebUtil.setCompanionServerIpPort( value );

                        LogUtil.log(LogUtil.LogType.RESTFUL_HTM, key_enum
                                +" registered: "+value+", sec_tok: "+sec_tok);

                        return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100).toString();
                    }else{

                        LogUtil.log(LogUtil.LogType.RESTFUL_HTM, key_enum
                                +" ERROR registration attempt and not in pairing mode");

                        return WebUtil.response(CConst.RESPONSE_CODE_REGISTRATION_ERROR_209).toString();
                    }
                }

                case src_full_sync_start:{

                    if( SqlFullSyncTarget.getInstance().syncInProcess())
                        return WebUtil.response(CConst.RESPONSE_CODE_SYNC_IN_PROCESS_201).toString();
                    else {
                        WorkerCommand.fullSyncSendSourceManifest(m_ctx);

                        return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100).toString();
                    }
                }
                case tgt_full_sync_source_manifest:{

                    JSONObject response = SqlFullSyncTarget.getInstance().inspectSourceManifest(value);

                    if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)){

                        WorkerCommand.queOptimizeFullSyncPlan(m_ctx);// and make first sync request
                    }
                    return response.toString();
                }
                case src_full_sync_data_req:{

                    JSONObject response = SqlFullSyncSource.getInstance().inspectSyncDataRequest(value);
                    LogUtil.log(LogUtil.LogType.RESTFUL_HTM, key_enum+" response: " + response.toString());

                    if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)){

                        WorkerCommand.queFullSyncSendData(m_ctx);
                    }
                    return response.toString();
                }
                case tgt_full_sync_data: {

                    String md5_payload = params.get(CConst.MD5_PAYLOAD);

                    JSONObject response = SqlFullSyncTarget.getInstance().inspectSyncData(
                            m_ctx, value, md5_payload);

                    LogUtil.log(LogUtil.LogType.RESTFUL_HTM, key_enum+" response: " + response.toString());

                    if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)){

                        WorkerCommand.queFullSyncProcessData(m_ctx);
                    }
                    return response.toString();
                }
                case src_full_sync_end:{

                    SqlFullSyncSource.getInstance().fullSyncEnd(m_ctx);
                    return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100).toString();
                }

                case tgt_inc_sync_source_manifest:{

                    JSONObject response = SqlIncSyncTarget.getInstance().inspectSourceManifest(value);

                    if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)){

                        WorkerCommand.queOptimizeIncSyncPlan(m_ctx);// and make first sync request
                    }
                    return response.toString();
                }
                case src_inc_sync_data_req:{

                    JSONObject response = SqlIncSyncSource.getInstance().inspectSyncDataRequest(value);
                    LogUtil.log(LogUtil.LogType.RESTFUL_HTM, key_enum+" response: " + response.toString());

                    if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)){

                        WorkerCommand.queIncSyncSendData(m_ctx);
                    }
                    return response.toString();
                }
                case tgt_inc_sync_data:{

                    String md5_payload = params.get(CConst.MD5_PAYLOAD);

                    JSONObject response = SqlIncSyncTarget.getInstance().incSyncInspectData(
                            m_ctx, value, md5_payload);

                    LogUtil.log(LogUtil.LogType.RESTFUL_HTM, key_enum+" response: " + response.toString());

                    if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)){

                        WorkerCommand.queIncSyncProcessData(m_ctx);
                    }
                    return response.toString();
                }
                case src_inc_sync_end: {

                    SqlIncSyncSource.getInstance().incSyncEnd(m_ctx);
                    return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100).toString();
                }

                case ping_test:{

                    String md5_payload = params.get(CConst.MD5_PAYLOAD);
                    String encodedPayload = params.get(CConst.PAYLOAD);
                    String numberOfTests = value;

                    JSONObject response = SqlSyncTest.getInstance()
                            .checkResult("ping", numberOfTests, md5_payload, encodedPayload);

                    if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100))
                        WorkerCommand.quePongTest(m_ctx);

                    return response.toString();
                }
                case pong_test:{

                    String md5_payload = params.get(CConst.MD5_PAYLOAD);
                    String encodedPayload = params.get(CConst.PAYLOAD);
                    String numberOfTests = value;

                    JSONObject response = SqlSyncTest.getInstance()
                            .checkResult("pong", numberOfTests, md5_payload, encodedPayload);
                    return response.toString();
                }

                // Ignore
                case uri:
                case queryParameterStrings:
                    break;

                default:
            }
        }
        return CConst.GENERATE_HTML ;// Default to generate html as next step
    }
}
