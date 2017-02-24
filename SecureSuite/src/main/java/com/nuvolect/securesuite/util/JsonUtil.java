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

package com.nuvolect.securesuite.util;//

import com.nuvolect.securesuite.webserver.MiniTemplator;
import com.nuvolect.securesuite.data.SqlCipher;

import net.sqlcipher.Cursor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;


/**
 * String and JSON utilities.  Better come up with a better name.
 */
public class JsonUtil {
    /**
     * Fetch a long from a JSON object.
     * JSON conversion to a long is lossy in the few most upper bits because it passes the long
     * through floating point double. This method first extracts it to a string and then uses
     * a Long utility to convert it to a long, and is lossless.
     * @param key
     * @param longObj
     * @return
     * @throws JSONException
     */
    public static long getLong( String key, JSONObject longObj) throws JSONException {

        long answer = 0;

        String stringObj = longObj.getString( key );
        answer = Long.parseLong(stringObj);

        return answer;
    }
    /**
     * Return a string containing details of a contact.
     * @param c
     * @param key - using DTab
     * @param maxLen - maximum length of the string before ellipsize are applied
     * @return
     */
    public static String contactDetail(Cursor c, SqlCipher.DTab key, int maxLen) {

        String s = "";
        try {
            JSONArray jsonArray = new JSONArray( c.getString(key.ordinal()));
            int len = jsonArray.length();
            if( len <= 0)
                return s;

            JSONObject item = jsonArray.getJSONObject(0);

            Iterator<?> item_keys = item.keys();
            String item_label = (String) item_keys.next();
            final String item_value = item.getString(item_label);

            String itemsAfterFirst = "";
            if( len > 1){
                itemsAfterFirst = "(+"+len+")";
                maxLen -= 3;
            }
            s = StringUtil.ellipsize( item_value, maxLen)+itemsAfterFirst;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return s;
    }
    /**
     * Return a string containing details of a contact.
     * @param contact_id - specific contact to reference
     * @param dTab - position in the cursor to find the data element
     * @param maxLen - maximum length of the string before ellipsize are applied
     * @return
     */
    public static String contactDetail(long contact_id, SqlCipher.DTab dTab, int maxLen) {

        String s = "";
        try {
            JSONArray jsonArray = new JSONArray( SqlCipher.get(contact_id, dTab));
            int len = jsonArray.length();
            if( len <= 0)
                return s;

            JSONObject item = jsonArray.getJSONObject(0);

            Iterator<?> item_keys = item.keys();
            String item_label = (String) item_keys.next();
            final String item_value = item.getString(item_label);

            String itemsAfterFirst = "";
            if( len > 1){
                int additionalItems = len - 1;
                itemsAfterFirst = "(+"+ additionalItems +")";
                maxLen -= 3;
            }
            s = StringUtil.ellipsize( item_value, maxLen)+itemsAfterFirst;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return s;
    }

    /**
     * Fill a detail block in the contact detail page.  If the detail is absent, fill nothing.
     * @param t
     * @param tVar
     * @param id
     * @param dTab
     */
    public static void fillDetail(MiniTemplator t, String tVar, Long id, SqlCipher.DTab dTab) {

        try {
            JSONArray jsonArray = new JSONArray( SqlCipher.get(id, dTab));
            int len = jsonArray.length();
            if( len > 0){

                for( int i = 0; i < len; i++){

                    JSONObject item = jsonArray.getJSONObject(i);
                    Iterator<?> item_keys = item.keys();
                    String item_label = (String) item_keys.next();
                    String item_value = item.getString(item_label);
                    t.setVariable(tVar+"_cat", item_label);
                    t.setVariable(tVar+"_value", item_value);
                    switch (dTab){
                        case website:
                            if( item_value.startsWith("http"))
                                t.setVariable("website_http_value", item_value);
                            else
                                t.setVariable("website_http_value", "http://"+item_value);
                            break;
                        default:
                    }
                    t.addBlock(tVar);
                }
                t.addBlock(tVar+"_post");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Given a JSON object with a single object, return the value component.
     * Assertion thrown when number of kv pair != 1;
     * @param kvPair
     * @return
     * @throws JSONException
     */
    public static String getValue(JSONObject kvPair) throws JSONException {

        assert kvPair.length() == 1;

        Iterator<?> item_keys = kvPair.keys();
        String item_label = (String) item_keys.next();

        return kvPair.getString(item_label);
    }

    /**
     * Given a JSON object with a single object, return the key component.
     * Assertion thrown when number of kv pair != 1;
     * @param kvPair
     * @return
     */
    public static String getKey(JSONObject kvPair) {

        assert kvPair.length() == 1;

        Iterator<?> item_keys = kvPair.keys();

        return (String) item_keys.next();
    }
}
