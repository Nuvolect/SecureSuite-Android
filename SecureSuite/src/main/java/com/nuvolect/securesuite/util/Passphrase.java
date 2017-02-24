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

package com.nuvolect.securesuite.util;

import android.content.Context;

import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlIncSync;
import com.nuvolect.securesuite.util.LogUtil.LogType;

import org.json.JSONArray;
import org.json.JSONException;

public class Passphrase {

    public static int ALPHA_UPPER = 1;
    public static int ALPHA_LOWER = 2;
    public static int NUMERIC     = 4;
    public static int SPECIAL     = 8;
    public static int HEX         = 16;
    public static int SYSTEM_MODE = ALPHA_UPPER | ALPHA_LOWER | NUMERIC;

    public static final String PASSWORD_GEN_HISTORY = "password_gen_history";
    public static final String PASSWORD_TARGET      = "password_target";
    public static final String PASSWORD_LENGTH      = "password_length";
    public static final String PASSWORD_GEN_MODE    = "password_gen_mode";

    public static String generateRandomString(int length, int mode) {

        StringBuffer buffer = new StringBuffer();
        String characters = "";

        if( (mode & ALPHA_UPPER) > 0)
            characters += "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if( (mode & ALPHA_LOWER) > 0)
            characters += "abcdefghijklmnopqrstuvwxyz";
        if( (mode & NUMERIC) > 0)
            characters += "0123456789";
        if( (mode & SPECIAL) > 0)
            characters += "!$%@#";
        if( (mode & HEX) > 0)
            characters += "0123456789abcdef";

        if( characters.isEmpty())
            characters = "0123456789";

        int charactersLength = characters.length();

        for (int i = 0; i < length; i++) {
            double index = Math.random() * charactersLength;
            buffer.append(characters.charAt((int) index));
        }
        return buffer.toString();
    }

    /**
     * Return the target password.  This is the password currently displayed in the spinner
     * @param ctx
     * @return
     */
    public static String getGenPassword(Context ctx){

        return SqlCipher.getCryp( PASSWORD_TARGET);
    }

    public static void setGenPassword(Context ctx, String target_password){

        // Inform system to sync data
        SqlIncSync.getInstance().crypSync(ctx, PASSWORD_TARGET);

        SqlCipher.putCryp( PASSWORD_TARGET, target_password);
    }

    public static int getPasswordLength(Context ctx){

        String value = SqlCipher.getCryp(PASSWORD_LENGTH);
        if( value.isEmpty())
            return 20;

        return Integer.parseInt(value);
    }

    public static void setPasswordLength(Context ctx, int password_length){

        // Inform system to sync data
        SqlIncSync.getInstance().crypSync(ctx, PASSWORD_LENGTH);

        SqlCipher.putCryp( PASSWORD_LENGTH, password_length+"");
    }

    public static int getPasswordGenMode(Context ctx) {

        String value = SqlCipher.getCryp(PASSWORD_GEN_MODE);
        if( value.isEmpty())
            return 0x0f;

        return Integer.parseInt(value);
    }

    public static void setPasswordGenMode(Context ctx, int mode){

        // Inform system to sync data
        SqlIncSync.getInstance().crypSync(ctx, PASSWORD_GEN_MODE);

        SqlCipher.putCryp( PASSWORD_GEN_MODE, mode+"");
    }

    /**
     * Return a string array with the password history, most recent in position zero
     * @param ctx
     * @return
     */
    public static String[] getPasswordGenHistory(Context ctx) {

        // Get a saved JSONArray and convert it to a string array
        String historyString = SqlCipher.getCryp(PASSWORD_GEN_HISTORY);
        if( historyString.isEmpty())
            historyString = new JSONArray().toString();

        String[] password_list = new String[0];
        JSONArray jarray = new JSONArray();
        try {
            jarray = new JSONArray( historyString);

            password_list = new String[ jarray.length()];

            // Copy the JSON into the string array, most recent at [0]
            for( int i = 0, j = jarray.length()-1; i < jarray.length(); i++){

                password_list[i] = jarray.getString( j-- );
            }

        } catch (JSONException e) {
            LogUtil.logException(ctx, LogType.PASSWORD, e);
        }

        return password_list;
    }

    /**
     * Append to the history by pushing a history onto the front (position 0) of
     * the list.  When the max list length is reached, the oldest history is purged.
     * @param ctx
     * @param newPassword
     * @return
     */
    public static int appendPasswordHistory(Context ctx, String newPassword){

        String historyString = SqlCipher.getCryp(PASSWORD_GEN_HISTORY);
        if( historyString.isEmpty())
            historyString = new JSONArray().toString();

        JSONArray jarray = new JSONArray();
        try {
            jarray = new JSONArray( historyString);

            // Append the new password
            jarray.put(newPassword);

            // Trim the to 20 entries, simple .remove(0) requires API KITKAT
            if( jarray.length() > 20){

                JSONArray j2 = new JSONArray();
                for(int i = 1; i<jarray.length(); i++)
                    j2.put( jarray.get(i));

                jarray = j2;
            }

        } catch (JSONException e) {
            LogUtil.logException(ctx, LogType.PASSWORD, e);
        }
        // Inform system to sync data
        SqlIncSync.getInstance().crypSync(ctx, PASSWORD_GEN_HISTORY);

        SqlCipher.putCryp( PASSWORD_GEN_HISTORY, jarray.toString());

        return jarray.length();
    }


    /**
     * Clear the history of passwords and request data synchronization with companion device.
     * @param ctx
     */
    public static void clearPasswordGenHistory(Context ctx) {

        JSONArray jarray = new JSONArray();
        SqlCipher.putCryp( PASSWORD_GEN_HISTORY, jarray.toString());

        // Inform system to sync data
        SqlIncSync.getInstance().crypSync(ctx, PASSWORD_GEN_HISTORY);
    }
}
