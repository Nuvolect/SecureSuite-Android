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
import com.nuvolect.securesuite.data.SqlIncSync;
import com.nuvolect.securesuite.data.SqlIncSyncSource;
import com.nuvolect.securesuite.data.SqlIncSyncTarget;
import com.nuvolect.securesuite.data.SqlSyncTest;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.WorkerCommand;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.nuvolect.securesuite.webserver.WebUtil.getCompanionServerIpPort;

/**
 * Support for data synchronization RESTful services.
 */
public class SyncRest {

    /**
     * Commands supported by this REST interface.
     * Use key {@link CConst#CMD} and a value from the enum table.
     */
    public static enum CMD {

        NIL,

        /**
         * Send this IP to self.  Success is when this IP matches the definition of self.
         * Parameter: {@link CConst#IP_PORT} : 1.2.3.4:3210
         */
        self_ip_test,
        /**
         * Send this IP to the companion.  Success is when this IP is defined as companion
         * on the companion device.
         * Parameter: {@link CConst#IP_PORT} : 1.2.3.4:3210
         */
        companion_ip_test,
        /**
         * Register companion device.  Used to associate a second SecureSuite device
         * to serve as a backup and to share contact/password data updates.
         * A security token SEC_TOK is passed that is used with all future communications.
         * Parameter: {@link CConst#SEC_TOK} : 32 char token
         * Parameter: {@link CConst#IP_PORT} : 1.2.3.4:3210
         */
        register_companion_device,

//////////////////////////////////////////////////////////////////////////////////////
        /**
         * Internal request to start the full sync process.
         * This will initiate full_sync_versions to the target SecureSuite device
         * Parameter: none
         */
        src_full_sync_start,

        /**
         * Receive _id of all records on the target SecureSuite device.
         * It will respond with a series of sync_data_req calls.
         * Parameter: {@link CConst#SOURCE_MANIFEST} : json string
         */
        tgt_full_sync_source_manifest,

        /**
         * Request specific data from the source SecureSuite device.
         * Data is supplied as a JSONArray of contacts in simple form.
         * Parameter: {@link CConst#DATA_REQUEST} : json string
         */
        src_full_sync_data_req,

        /**
         * Send a JSONArray of contacts in simple form.
         * Parameter: {@link CConst#SYNC_DATA} : json string
         * Parameter: {@link CConst#MD5_PAYLOAD} : 32char hex
         */
        tgt_full_sync_data,

        /**
         * Sent when the sync plan is empty and sync is complete.
         * Time to clean up.
         * Parameter: none
         */
        src_full_sync_end,
//////////////////////////////////////////////////////////////////////////////////////
        /**
         * Send a manifest describing updates to the target device.
         * It in turn will respond with requests for updates.
         * Parameter: {@link CConst#SOURCE_MANIFEST} : json string
         */
        tgt_inc_sync_source_manifest,

        /**
         * Request incremental data from the source
         * Parameter: {@link CConst#SYNC_DATA_REQUEST} : json string
         */
        src_inc_sync_data_req,

        /**
         * Send requested data
         * Parameter: {@link CConst#MD5_PAYLOAD} : 32char
         * Parameter: {@link CConst#SYNC_DATA} : json string
         */
        tgt_inc_sync_data,

        /**
         * Send when all data has been supplied and incremental sync is complete.
         * Parameter: none
         */
        src_inc_sync_end,
//////////////////////////////////////////////////////////////////////////////////////

        /**
         * Current synchronization state.
         * Parameter: none
         */
        sync_state,
        /**
         * Communication test.  Bounce data back and forth to exercise communications.
         * Parameter: {@link CConst#MD5_PAYLOAD} : 32char
         * Parameter: {@link CConst#PAYLOAD} : json string
         * Parameter: {@link CConst#COUNTER} : integer string
         */
        ping_test,
        /**
         * Communication test.  Bounce data back and forth to exercise communications.
         * Parameter: {@link CConst#MD5_PAYLOAD} : 32char
         * Parameter: {@link CConst#PAYLOAD} : json string
         * Parameter: {@link CConst#COUNTER} : integer string
         */
        pong_test,
    }

    /**
     * Parse parameters and process any updates.
     *
     * @param ctx
     * @param params
     * @return
     */
    public static String render(Context ctx, Map<String, String> params) {

        String jsonString = parse(ctx, params);// set mContactId & others based on params

        return jsonString;
    }

    private static String parse(Context ctx, Map<String, String> params) {

        CMD cmd = CMD.NIL;
        try {
            cmd = CMD.valueOf(params.get(CConst.CMD));
        } catch (Exception e) {
            LogUtil.log(LogUtil.LogType.SYNC_REST, "Error invalid command: " + params);
            LogUtil.logException(ctx, LogUtil.LogType.SYNC_REST, e);
        }
        LogUtil.log(LogUtil.LogType.SYNC_REST, "cmd=" + cmd +" params: "+params);

        switch (cmd) {

            case NIL:
                break;

            case self_ip_test:{

                if( params.get(CConst.IP_PORT).contentEquals(WebUtil.getServerIpPort(ctx)))
                    return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100).toString();
                else
                    return WebUtil.response(CConst.RESPONSE_CODE_SELF_TEST_FAIL_211).toString();
            }

            case companion_ip_test:{

                if( params.get(CConst.IP_PORT).contentEquals(getCompanionServerIpPort()))
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

                    WebUtil.setCompanionServerIpPort( params.get(CConst.IP_PORT) );

                    LogUtil.log(LogUtil.LogType.SYNC_REST, cmd
                            +" registered: "+params.get(CConst.IP_PORT)+", sec_tok: "+sec_tok);

                    return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100).toString();
                }else{

                    LogUtil.log(LogUtil.LogType.SYNC_REST, cmd
                            +" ERROR registration attempt and not in pairing mode");

                    return WebUtil.response(CConst.RESPONSE_CODE_REGISTRATION_ERROR_209).toString();
                }
            }

            case src_full_sync_start:{

                if( SqlFullSyncTarget.getInstance().syncInProcess())
                    return WebUtil.response(CConst.RESPONSE_CODE_SYNC_IN_PROCESS_201).toString();
                else {
                    WorkerCommand.fullSyncSendSourceManifest(ctx);

                    return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100).toString();
                }
            }
            case tgt_full_sync_source_manifest:{

                JSONObject response = SqlFullSyncTarget.getInstance()
                        .inspectSourceManifest(params.get(CConst.SOURCE_MANIFEST));

                if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)){

                    WorkerCommand.queOptimizeFullSyncPlan(ctx);// and make first sync request
                }
                return response.toString();
            }
            case src_full_sync_data_req:{

                JSONObject response = SqlFullSyncSource.getInstance()
                        .inspectSyncDataRequest(params.get(CConst.DATA_REQUEST));
                LogUtil.log(LogUtil.LogType.SYNC_REST, cmd +" response: " + response.toString());

                if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)){

                    WorkerCommand.queFullSyncSendData(ctx);
                }
                return response.toString();
            }
            case tgt_full_sync_data: {

                String md5_payload = params.get(CConst.MD5_PAYLOAD);

                JSONObject response = SqlFullSyncTarget.getInstance().inspectSyncData(
                        ctx, params.get(CConst.SYNC_DATA), md5_payload);

                LogUtil.log(LogUtil.LogType.SYNC_REST, cmd +" response: " + response.toString());

                if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)){

                    WorkerCommand.queFullSyncProcessData(ctx);
                }
                return response.toString();
            }
            case src_full_sync_end:{

                SqlFullSyncSource.getInstance().fullSyncEnd(ctx);
                return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100).toString();
            }

            case tgt_inc_sync_source_manifest:{

                JSONObject response = SqlIncSyncTarget.getInstance()
                        .inspectSourceManifest(params.get(CConst.SOURCE_MANIFEST));

                if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)){

                    WorkerCommand.queOptimizeIncSyncPlan(ctx);// and make first sync request
                }
                return response.toString();
            }
            case src_inc_sync_data_req:{

                JSONObject response = SqlIncSyncSource.getInstance()
                        .inspectSyncDataRequest(params.get(CConst.SYNC_DATA_REQUEST));
                LogUtil.log(LogUtil.LogType.SYNC_REST, cmd +" response: " + response.toString());

                if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)){

                    WorkerCommand.queIncSyncSendData(ctx);
                }
                return response.toString();
            }
            case tgt_inc_sync_data:{

                JSONObject response = SqlIncSyncTarget.getInstance().incSyncInspectData(
                        ctx, params.get(CConst.SYNC_DATA), params.get(CConst.MD5_PAYLOAD));

                LogUtil.log(LogUtil.LogType.SYNC_REST, cmd +" response: " + response.toString());

                if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100)){

                    WorkerCommand.queIncSyncProcessData(ctx);
                }
                return response.toString();
            }
            case src_inc_sync_end: {

                SqlIncSyncSource.getInstance().incSyncEnd(ctx);
                return WebUtil.response(CConst.RESPONSE_CODE_SUCCESS_100).toString();
            }

            case sync_state:{

                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put( "companion_ip_port", getCompanionServerIpPort());
                    jsonObject.put( "my_ip_port", WebUtil.getServerIpPort(ctx));
                    jsonObject.put( "incoming_update", SqlIncSync.getInstance().getIncomingUpdate());
                    jsonObject.put( "outgoing_update", SqlIncSync.getInstance().getOutgoingUpdate());

                    LogUtil.log(LogUtil.LogType.SYNC_REST, "sync_state response: "+jsonObject.toString(2));

                    return jsonObject.toString();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            case ping_test:{

                String md5_payload = params.get(CConst.MD5_PAYLOAD);
                String encodedPayload = params.get(CConst.PAYLOAD);
                String numberOfTests = params.get(CConst.COUNTER);

                JSONObject response = SqlSyncTest.getInstance()
                        .checkResult("ping", numberOfTests, md5_payload, encodedPayload);

                if( WebUtil.responseMatch(response, CConst.RESPONSE_CODE_SUCCESS_100))
                    WorkerCommand.quePongTest(ctx);

                return response.toString();
            }
            case pong_test:{

                String md5_payload = params.get(CConst.MD5_PAYLOAD);
                String encodedPayload = params.get(CConst.PAYLOAD);
                String numberOfTests = params.get(CConst.COUNTER);

                JSONObject response = SqlSyncTest.getInstance()
                        .checkResult("pong", numberOfTests, md5_payload, encodedPayload);
                return response.toString();
            }

            default:
                LogUtil.log(LogUtil.LogType.SYNC_REST, "Invalid command: "+cmd);
        }

        return new JSONObject().toString();
    }
}
