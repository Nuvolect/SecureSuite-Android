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

package com.nuvolect.securesuite.webserver.connector;//

import android.content.Context;

import com.nuvolect.securesuite.util.KeystoreUtil;
import com.nuvolect.securesuite.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * test
 *
 * Run a specific test and return the result
 * Input JSON
 * {
 *     "test_id": {decode_uri, encode_uri}
 * }
 * JSON returned
 * {
 *     "error":"",      // empty if no error
 *     "result":"",     // results of test
 *     "test_id":"",    // test id that was executed
 *     "delta_time":"", // String time appended with " ms"
 * }
 */
public class CmdDebug {

    private static Context m_ctx;

    enum TEST_ID {
        create_key,
        decrypt,
        delete_key,
        encrypt,
        get_keys,
        lockscreen_test,
    }

    public static ByteArrayInputStream go(Context ctx, Map<String, String> params) {

        m_ctx = ctx;

        try {
            JSONObject wrapper = new JSONObject();

            String error = "";

            TEST_ID test_id = null;
            try {
                test_id = TEST_ID.valueOf(params.get("test_id"));
            } catch (IllegalArgumentException e) {
                error = "Error, invalid command: "+params.get("cmd");
            }
            long timeStart = System.currentTimeMillis();

            assert test_id != null;

            try {
                switch ( test_id ){

                    case create_key:{

                        JSONObject result = KeystoreUtil.createKey( m_ctx, params.get("key_alias"));
                        wrapper.put("result",result);
                        break;
                    }
                    case delete_key:{

                        JSONObject result = KeystoreUtil.deleteKey( m_ctx, params.get("key_alias"));
                        wrapper.put("result",result);
                        break;
                    }
                    case encrypt:{

                        String key_alias = params.get("key_alias");
                        String clear_text = params.get("cleartext");
                        JSONObject result = KeystoreUtil.encrypt( key_alias, clear_text);
                        wrapper.put("result",result);
                        break;
                    }
                    case decrypt:{

                        String key_alias = params.get("key_alias");
                        String cipher_text_b64 = params.get("ciphertext");
                        JSONObject result = KeystoreUtil.decrypt( key_alias, cipher_text_b64);
                        wrapper.put("result",result);
                        break;
                    }
                    case get_keys:{

                        JSONArray result = KeystoreUtil.getKeys();
                        wrapper.put("keys",result);
                        break;
                    }
                    case lockscreen_test:{

                        JSONObject result = KeystoreUtil.testAndroidLockscreenEnabled( m_ctx);
                        wrapper.put("result",result);
                        break;
                    }
                    default:
                        error = "Invalid test: "+test_id;
                }
            } catch (Exception e) {
                LogUtil.logException(m_ctx, LogUtil.LogType.CMD_DEBUG, e);
                error = "Exception";
            }

            wrapper.put("error", error);
            wrapper.put("test_id", test_id.toString());
            wrapper.put("delta_time",
                    String.valueOf(System.currentTimeMillis() - timeStart) + " ms");

            if(LogUtil.DEBUG)
                LogUtil.log(LogUtil.LogType.CMD_DEBUG, wrapper.toString(2));

            return new ByteArrayInputStream(wrapper.toString(2).getBytes("UTF-8"));

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

}
