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
import android.content.SharedPreferences;
import android.util.Base64;

import com.nuvolect.securesuite.main.GroupListActivity.GLA_RIGHT_FRAGMENT;

import java.util.Locale;

public class Persist {

    /**
     * Persist data to Android app private storage.
     *
     * DESIGN PATTERN TO PERSIST ENCRYPTED STRING DATA:
     * 1. Convert string data to a byte[], no encoding required
     * 2. Encrypt the byte[], creating a new byte[]
     * 3. Create a string for storage by encoding the byte[] with Base64
     * 4. Persist the string
     * 5. Clean up any clear text data
     *
     * RETRIEVE AND RESTORE PERSISTED AND ENCRYPTED STRING DATA:
     * 1. Read persisted data into a string
     * 2. Decode the string back into a byte[] using Base64 decode
     * 3. Decrypt the byte[], creating a new byte[]
     * 4. Decode the byte[] into a string with UTF-8.
     * 5. Clean up any clear text data
     */

    private static final String PERSIST_NAME           = "ss_persist";

    // Persist keys, some calling methods pass their own keys, be sure to avoid conflicts
    public static final String ACCOUNT_NAME           = "account_name";
    public static final String ACCOUNT_TYPE           = "account_type";
    public static final String CURRENT_CONTACT_ID     = "contact_id";
    public static final String CURRENT_ACCOUNT_TYPE   = "account_type";
    public static final String EMPTY_CONTACT_ID       = "empty_contact_id";
    public static final String GLA_FRAGMENT_ENUM      = "gla_fragment_enum";
    public static final String IMPORT_IN_PROGRESS     = "import_in_progress";
    public static final String NAV_MENU_TITLE 		  = "nav_menu_title";
    public static final String NEXT_EXPORT_VCF        = "next_export_vcf";
    public static final String PROFILE_ID             = "profile_id";
    public static final String PROGRESS_BAR_ACTIVE    = "progress_bar_active";
    public static final String STARTING_UP            = "starting_up";
    public static final String NAV_MENU_CHOICE 	      = "nav_menu_choice";

    public static final String CIPHER_VFS_PASSWORD    = "cipher_vfs_password";// Encrypted, string
    public static final String SQL_DB_PASSWORD        = "sql_db_password";    // Encrypted, string
    public static final String PORT_NUMBER            = "port_number";        // Encrypted, int
    public static final String SELFSIGNED_KS_KEY      = "selfsigned_ks_key";  // Encrypted, string

    /**
     * Remove all persistent data.
     */
    public static void clearAll(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        pref.edit().clear().commit();
    }

    /**
     * Check if a specific key is persisted.
     * @param ctx
     * @param persistKey
     * @return
     */
    public static boolean keyExists(Context ctx, String persistKey) {

        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.contains( persistKey);
    }

    public static void setNavChoice(Context ctx, int navMenuPosition, String navMenuTitle) {

        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putInt(NAV_MENU_CHOICE, navMenuPosition).commit();
        pref.edit().putString(NAV_MENU_TITLE, navMenuTitle).commit();
    }

    public static int getNavChoice(Context ctx) {

        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);

        return pref.getInt(NAV_MENU_CHOICE, 0); // Default 0 is first on the list i.e. Contacts
    }

    public static void setCurrentAccountType(Context ctx, String accountType){
        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putString(CURRENT_ACCOUNT_TYPE, accountType).commit();
    }
    public static String getCurrentAccountType(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.getString(CURRENT_ACCOUNT_TYPE, "no account type");
    }

    public static void setProgressBarActive(Context ctx, boolean b) {
        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putBoolean( PROGRESS_BAR_ACTIVE, b).commit();
    }
    public static boolean getProgressBarActive(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean( PROGRESS_BAR_ACTIVE, false);
    }

    public static void setGlaRightFragment(Context ctx, GLA_RIGHT_FRAGMENT fragEnum) {
        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putInt( GLA_FRAGMENT_ENUM, fragEnum.ordinal()).commit();
    }
    public static GLA_RIGHT_FRAGMENT getGlaRightFragment(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        int ordnal = pref.getInt( GLA_FRAGMENT_ENUM, GLA_RIGHT_FRAGMENT.GROUP_DETAIL.ordinal());
        return GLA_RIGHT_FRAGMENT.values()[ordnal];
    }

    public static void setCurrentContactId(Context ctx, long contact_id) {

        if( contact_id == -1)
            throw new IllegalArgumentException("SecureSuite Persist.setCurrentContactId contact_id == -1");

        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putLong( CURRENT_CONTACT_ID, contact_id).commit();
    }
    public static long getCurrentContactId(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        long contact_id = pref.getLong( CURRENT_CONTACT_ID, 0);

        if( contact_id == -1)
            throw new IllegalArgumentException("SecureSuite Persist.getCurrentContactId contact_id == -1");

        return  contact_id;
    }

    /** Return the next filename in sequence */
    public static String getNextVcfFilename(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        int i = 1 + pref.getInt(NEXT_EXPORT_VCF, 0);
        pref.edit().putInt( NEXT_EXPORT_VCF, i).commit();
        return String.format(Locale.US, "%05d.vcf", i);
    }

    public static long getProfileId(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.getLong( PROFILE_ID, 0);
    }
    public static void setProfileId(Context ctx, long contact_id) {
        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putLong( PROFILE_ID, contact_id).commit();
    }

    public static boolean isStartingUp(Context ctx) {

        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        boolean startingUp = pref.getBoolean(STARTING_UP, true);
        pref.edit().putBoolean( STARTING_UP, false).commit();
        return startingUp;
    }

    /** Set the number of contacts that will be imported, > zero means import in progress */
    public static void setImportInProgress(Context ctx, int total) {
        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putInt( IMPORT_IN_PROGRESS, total).commit();
    }
    /** Get the number of contacts that will be imported, > zero means import in progress */
    public static int getImportInProgress(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.getInt( IMPORT_IN_PROGRESS, 0);
    }

    public static void setAccountName(Context ctx, String accountName){
        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putString(ACCOUNT_NAME, accountName).commit();
    }
    public static String getAccountName(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.getString(ACCOUNT_NAME, "");
    }

    public static void setAccountType(Context ctx, String accountType){
        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putString(ACCOUNT_TYPE, accountType).commit();
    }
    public static String getAccountType(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.getString(ACCOUNT_TYPE, "no account type");
    }

    /** Save an empty contact ID, used to know if it has been modified when saving */
    public static void setEmptyContactId(Context ctx, long contact_id) {
        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putLong( EMPTY_CONTACT_ID, contact_id).commit();
    }
    /** Get a contact that was formerly empty and still may be so.  used to know if it has been modified when saving */
    public static long getEmptyContactId(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.getLong( EMPTY_CONTACT_ID, 0);
    }

    /**
     * Delete a specific key.
     * @param ctx
     * @param keyToDelete
     * @return
     */
    public static boolean deleteKey(Context ctx, String keyToDelete){

        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.edit().remove( keyToDelete).commit();
    }

    /**
     * Simple get.  Return empty string if not found
     * @param ctx
     * @param key
     * @return
     */
    public static String get(Context ctx, String key) {

        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.getString(key, "");
    }
    /**
     * Simple get.  Return default string if not found
     * @param ctx
     * @param key
     * @return
     */
    public static String get(Context ctx, String key, String defaultString) {

        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.getString(key, defaultString);
    }

    /**
     * Simple put value with the given key.
     * Return true if successful, otherwise false.
     * @param ctx
     * @param key
     * @param value
     */
    public static boolean put(Context ctx, String key, String value){
        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        return pref.edit().putString(key, value).commit();
    }

    public static void putPort(Context ctx, int port) {

        byte[] crypBytes = new byte[0];
        try {
            crypBytes = CrypUtil.encryptInt( port);
            String crypString = Base64.encodeToString(crypBytes, Base64.DEFAULT);

            put( ctx, PORT_NUMBER, crypString);
        } catch (Exception e) {
            LogUtil.logException(LogUtil.LogType.PERSIST, e);
        }
    }

    public static int getPort(Context ctx, int default_port) {

        if( ! keyExists( ctx, PORT_NUMBER))
            return default_port;

        try {
            String crypString = get( ctx, PORT_NUMBER);
            final byte[] crypBytes = CrypUtil.toBytesUTF8( crypString);

            return CrypUtil.decryptInt( crypBytes);
        } catch (Exception e) {
            LogUtil.logException(LogUtil.LogType.PERSIST, e);
        }
        return default_port;
    }

    /**
     * Put a new cipher virtual file system password.
     *
     * Note that CipherVfsPassword and DbSqlPassword need to be separate methods.
     * While it is tempting to combine these methods, only the SQL DB can be
     * rekeyed at this time. If the SQL DB is rekeyed then the CIPHER VFS will
     * have an invalid password. Revisit this subject when the CIPHER VFS can
     * be rekeyed.
     *
     * @param ctx
     * @param clearBytes
     */
    public static void putCipherVfsPassword(Context ctx, byte[] clearBytes) {

        try {
            // Encrypt the byte array, creating a new byte array
            byte[] crypBytes = CrypUtil.encrypt( clearBytes);

            // Encode the array for storage
            String crypEncodedString = CrypUtil.encodeToB64( crypBytes);

            // Store it as a string
            put( ctx, CIPHER_VFS_PASSWORD, crypEncodedString);

        } catch (Exception e) {
            LogUtil.logException(LogUtil.LogType.PERSIST, e);
        }
    }

    /**
     * Get the cipher virtual file system password.
     * If the password does not exist, create it and save it.
     *
     * @param ctx
     * @return
     */
    public static byte[] getCipherVfsPassword(Context ctx) {

        byte[] clearBytes = new byte[0];

        if (!keyExists(ctx, CIPHER_VFS_PASSWORD)) {

            clearBytes = Passphrase.generateRandomPasswordBytes(32, Passphrase.SYSTEM_MODE);
            putCipherVfsPassword(ctx, clearBytes);

        } else {

            // Get the encoded and encrypted string
            String crypEncodedString = get(ctx, CIPHER_VFS_PASSWORD);

            // Decode the string back into a byte array using Base64 decode
            byte[] crypBytes = CrypUtil.decodeFromB64(crypEncodedString);

            // Decrypt the byte array, creating a new byte array
            try {
                clearBytes = CrypUtil.decrypt(crypBytes);
            } catch (Exception e) {
                LogUtil.logException(LogUtil.LogType.PERSIST, e);
            }
        }
        return clearBytes;
    }

    /**
     * Put a new DB password. This is only done when called by getDbPassword and
     * the password is crated and when the database is re-keyed from settings.
     *
     * Note that CipherVfsPassword and DbSqlPassword need to be separate methods.
     * While it is tempting to combine these methods, only the SQL DB can be
     * rekeyed at this time. If the SQL DB is rekeyed then the CIPHER VFS will
     * have an invalid password. Revisit this subject when the CIPHER VFS can
     * be rekeyed.
     *
     * @param ctx
     * @param clearBytes
     */
    public static void putSqlDbPassword(Context ctx, byte[] clearBytes) {

        try {
            // Encrypt the byte array, creating a new byte array
            byte[] crypBytes = CrypUtil.encrypt( clearBytes);

            // Encode the array for storage
            String crypEncodedString = CrypUtil.encodeToB64( crypBytes);

            // Store it as a string
            put( ctx, SQL_DB_PASSWORD, crypEncodedString);

        } catch (Exception e) {
            LogUtil.logException(LogUtil.LogType.PERSIST, e);
        }
    }

    /**
     * Get the database password.
     * If the password does not exist, create it and save it.
     *
     * @param ctx
     * @return
     */
    public static byte[] getSqlDbPassword(Context ctx) {

        byte[] clearBytes = new byte[0];

        if( ! keyExists( ctx, SQL_DB_PASSWORD)){

            clearBytes = Passphrase.generateRandomPasswordBytes(32, Passphrase.SYSTEM_MODE);
            putSqlDbPassword( ctx, clearBytes);

        }else{

            // Get the encoded and encrypted string
            String crypEncodedString = get( ctx, SQL_DB_PASSWORD);

            // Decode the string back into a byte array using Base64 decode
            byte[] crypBytes = CrypUtil.decodeFromB64( crypEncodedString);

            // Decrypt the byte array, creating a new byte array
            try {
                clearBytes = CrypUtil.decrypt( crypBytes);
            } catch (Exception e) {
                LogUtil.logException(LogUtil.LogType.PERSIST, e);
            }
        }
        return clearBytes;
    }

    public static void putSelfsignedKsKey(Context ctx, char[] clearChars) {

        // Convert it to bytes, no encoding yet
        byte[] clearBytes = CrypUtil.toBytesUTF8( clearChars);
        String encryptedEncodedString = null;

        try {
            // Encrypt the byte array, creating a new byte array
            byte[] encryptedBytes = CrypUtil.encrypt(clearBytes);

            // Prepare for storage by converting the byte array to a Base64 encoded string
            encryptedEncodedString = CrypUtil.encodeToB64( encryptedBytes );
        } catch (Exception e) {
            LogUtil.logException(LogUtil.LogType.PERSIST, e);
        }

        // Store it as a string
        put( ctx, SELFSIGNED_KS_KEY, encryptedEncodedString);

        // Clean up
        clearBytes = CrypUtil.cleanArray( clearBytes);
    }

    public static char[] getSelfsignedKsKey(Context ctx) {

        // Get the encoded and encrypted string
        String cryptedEncodedString = get( ctx, SELFSIGNED_KS_KEY);

        // Decode the string back into a byte array using Base64 decode
        byte[] crypBytes = CrypUtil.decodeFromB64(cryptedEncodedString);

        // Decrypt the byte array, creating a new byte array
        byte[] clearBytes = new byte[0];
        try {
            clearBytes = CrypUtil.decrypt( crypBytes);
        } catch (Exception e) {
            LogUtil.logException(LogUtil.LogType.PERSIST, e);
        }

        // Decode the byte array creating a new String using UTF-8 encoding
        char[] clearChars = CrypUtil.toChar( clearBytes);

        // Clean up
        clearBytes = CrypUtil.cleanArray( clearBytes);

        return clearChars;
    }
}
