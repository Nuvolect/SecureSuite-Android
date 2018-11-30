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

import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.main.GroupListActivity.GLA_RIGHT_FRAGMENT;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Locale;

import javax.crypto.NoSuchPaddingException;

public class Persist {

    private static final String PERSIST_NAME           = "ss_persist";

    // Persist keys
    private static final String ACCOUNT_NAME           = "account_name";
    private static final String ACCOUNT_TYPE           = "account_type";
    private static final String CURRENT_CONTACT_ID     = "contact_id";
    private static final String CURRENT_ACCOUNT_TYPE   = "account_type";
    private static final String EMPTY_CONTACT_ID       = "empty_contact_id";
    private static final String GLA_FRAGMENT_ENUM      = "gla_fragment_enum";
    private static final String IMPORT_IN_PROGRESS     = "import_in_progress";
    private static final String NAV_MENU_CHOICE 	   = "nav_menu_choice";
    private static final String NAV_MENU_TITLE 		   = "nav_menu_title";
    private static final String NEXT_EXPORT_VCF        = "next_export_vcf";
    private static final String PASSPHRASE 			   = "passphrase";
    private static final String PROFILE_ID             = "profile_id";
    private static final String PROGRESS_BAR_ACTIVE    = "progress_bar_active";
    private static final String STARTING_UP            = "starting_up";


    /**
     * Remove all persistent data.
     */
    public static void clearAll(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        pref.edit().clear().commit();
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
        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putLong( CURRENT_CONTACT_ID, contact_id).commit();
    }
    public static long getCurrentContactId(Context ctx) {
        final SharedPreferences pref = ctx.getSharedPreferences( PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.getLong( CURRENT_CONTACT_ID, 0);
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
     * Encrypt clear char[] data with an app wide private key, then persist the encrypted results.
     *
     * @param ctx
     * @param persistKey
     * @param clearChar
     * @throws CertificateException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws NoSuchPaddingException
     * @throws UnrecoverableEntryException
     * @throws IOException
     */
    public static void putEncrypt(Context ctx, String persistKey, char[] clearChar)
            throws CertificateException, InvalidKeyException, NoSuchAlgorithmException,
            KeyStoreException, NoSuchPaddingException, UnrecoverableEntryException, IOException,
            NoSuchProviderException, InvalidAlgorithmParameterException {

        byte[] clearBytes = Passphrase.toBytes( clearChar);
        byte[] cryptBytes = KeystoreUtil.encrypt( ctx, CConst.APP_KEY_ALIAS, clearBytes);
        String cryptString = Base64.encodeToString( cryptBytes, Base64.NO_WRAP);

        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putString( persistKey, cryptString).commit();

        Passphrase.cleanArray( clearBytes);
    }


    /**
     * Read encrypted data, decrypt it with an app wide private key and return clear results.
     *
     * @param ctx
     * @return
     * @throws CertificateException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws NoSuchPaddingException
     * @throws UnrecoverableEntryException
     * @throws IOException
     */
    public static char[] getDecrypt(Context ctx, String persistKey)
            throws CertificateException, InvalidKeyException, NoSuchAlgorithmException,
            KeyStoreException, NoSuchPaddingException, UnrecoverableEntryException, IOException {

        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME, Context.MODE_PRIVATE);

        String encryptString = pref.getString( persistKey, CConst.NO_PASSPHRASE);
        byte[] encryptBytes = Base64.decode( encryptString, Base64.DEFAULT);
        byte[] clearBytes = KeystoreUtil.decrypt( CConst.APP_KEY_ALIAS, encryptBytes);
        char[] clearChars = Passphrase.toChars( clearBytes);

        Passphrase.cleanArray( clearBytes);

        return clearChars;
    }

    public static void setEncryptedPassphrase(Context ctx, String passphrase){//SPRINT remove
        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME,  Context.MODE_PRIVATE);
        pref.edit().putString(PASSPHRASE, passphrase).commit();
    }
    public static String getEncryptedPassphrase(Context ctx){//SPRINT remove
        final SharedPreferences pref = ctx.getSharedPreferences(PERSIST_NAME, Context.MODE_PRIVATE);
        return pref.getString(PASSPHRASE, CConst.NO_PASSPHRASE);
    }
}
