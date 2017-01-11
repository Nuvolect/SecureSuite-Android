package com.nuvolect.securesuite.webserver.connector;//

import android.content.Context;

import com.nuvolect.securesuite.util.CryptoM;
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
        get_key,
//        generate_key,
        get_keys,
        test_lockscreen, delete_key, put_key,
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

                        JSONObject result = CryptoM.createKey( m_ctx, params.get("key_alias"));
                        wrapper.put("result",result);
                        break;
                    }
                    case delete_key:{

                        JSONObject result = CryptoM.deleteKey( m_ctx, params.get("key_alias"));
                        wrapper.put("result",result);
                        break;
                    }
                    case put_key:{

                        char[] password = params.get("password").toCharArray();
                        String value = params.get("value");
                        String key_alias = params.get("key_alias");
                        JSONObject result = CryptoM.putKey( m_ctx, key_alias, password, value);
                        wrapper.put("result",result);
                        break;
                    }
                    case get_key:{

                        char[] password = params.get("password").toCharArray();
                        JSONObject result = CryptoM.getKey( m_ctx, params.get("key_alias"), password);
                        wrapper.put("result",result);
                        break;
                    }
                    case get_keys:{

                        JSONArray result = CryptoM.getKeys();
                        wrapper.put("keys",result);
                        break;
                    }
                    case test_lockscreen:{

                        JSONObject result = CryptoM.testLockScreenEnabled( m_ctx);
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
