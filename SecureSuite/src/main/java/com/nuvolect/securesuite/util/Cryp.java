package com.nuvolect.securesuite.util;

import android.content.Context;

import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.main.CConst;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Persist encrypted key value data.
 */
public class Cryp {

    private static final String GROUP_JSON = "group_json";
    public static final String APP_VERSION = "app_version";

    /**
     * Return the value of a related key, or return an empty string
     * if the key is not found.
     * @param key
     * @return
     */
    public static String get(String key){

        if( key == null || key.isEmpty())
            return "";

        return SqlCipher.getCryp(key);
    }

    /**
     * Return the value of a related key, or return the default value
     * if the key is not found.
     * @param key
     * @param defValue
     * @return
     */
    public static String get(String key, String defValue) {

        String value = SqlCipher.getCryp(key);

        if( value == null || value.isEmpty()){

            put( key, defValue);
            return defValue;
        }
        else{

            return value;
        }
    }

    /**
     * Persist the value referenced by a key.  Return the number of
     * records updated: 0, first time update, 1, value updated, 2+ error.
     * @param key
     * @param value
     * @return
     */
    public static int put(String key, String value){

        if( value == null)
            LogUtil.log("put key is NULL - ERROR -------------------------------------------------------------------------");

        return SqlCipher.putCryp(key, value);
    }

    /**
     * Return the integer value referenced by a key.
     * Return 0 if the key does not exist.
     * @param key
     * @return
     */
    public static int getInt(Context ctx, String key) {

        String v = get( key);
        if( v.isEmpty())
            return 0;
        else
            return Integer.valueOf( v );
    }

    /**
     * Return the integer value referenced by a key.
     * If the key does not exist, save the default as the key and
     * return it.
     * @param key
     * @param defInt
     * @return
     */
    public static int getInt(Context ctx, String key, int defInt) {

        String v = get( key);
        if( v.isEmpty()) {
            putInt(ctx, key, defInt);
            return defInt;
        }
        else
            return Integer.valueOf( v );
    }

    /**
     * Persist the value referenced by a key.  Return the number of
     * records updated: 0, first time update, 1, value updated, 2+ error.
     * @param key
     * @param val
     */
    public static int putInt(Context ctx, String key, int val) {

        return put(key, String.valueOf(val));
    }

    /**
     * Set the current group in relation to the current account. The get() method will
     * return the specific group in relation to the current account.
     * @param current_group
     */
    public static void setCurrentGroup( int current_group ) {

        if( current_group == 0)
            LogUtil.log("Cryp.setCurrentGroup ERROR: " + current_group);

        String account = get(CConst.ACCOUNT);
        JSONObject group_json = new JSONObject();
        try {

            group_json = new JSONObject( get(GROUP_JSON, "{}"));
            group_json.put( account, current_group);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        put(GROUP_JSON, group_json.toString());
    }

    /**
     * Get the current group in relation to the current account
     * @return int group_id
     */
    public static int getCurrentGroup() {

        int current_group = 0;
        String account = get(CConst.ACCOUNT);

        try {

            JSONObject group_json = new JSONObject( get(GROUP_JSON, "{}"));
            if( group_json.has(account))
                current_group = group_json.getInt( account );
            else{
                current_group = MyGroups.getDefaultGroup(account);
                setCurrentGroup( current_group);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return current_group;
    }

    public static String getCurrentAccount() {

        return get(CConst.ACCOUNT);
    }

    public static void setCurrentAccount(String account) {

        put( CConst.ACCOUNT, account);
    }

    public static void setLockCode(Context ctx, String code) {

        put( CConst.LOCK_CODE, code );
    }

    public static String getLockCode(Context ctx) {

        return get( CConst.LOCK_CODE );
    }

    public static void setCurrentContact(Context ctx, long contactId) {

        Persist.setCurrentContactId( ctx, contactId);
    }
}
