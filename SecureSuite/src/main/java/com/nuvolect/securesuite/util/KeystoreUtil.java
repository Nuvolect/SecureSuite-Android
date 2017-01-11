package com.nuvolect.securesuite.util;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Android keystore experimentation.
 */
public class KeystoreUtil {


    public static JSONObject testLockScreenEnabled(Context ctx){

        String LOCKSCREEN_ENABLED_TEST = "lockscreen_enabled_test";
        String lockscreenEnabled  = "false";

        JSONObject result = createKey(ctx, LOCKSCREEN_ENABLED_TEST);
        try {

            if( result.getString("error").contains("Secure lock screen must be enabled")){

                lockscreenEnabled = "false";
            }else
            if( result.getString("success").contentEquals("true"))
                lockscreenEnabled = "true";

            result.put("lockscreen_enabled",lockscreenEnabled);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
    /**
     * Creates a public and private key and stores it using the Android Key Store, so that only
     * this application will be able to access the keys.
     */
    public static JSONObject createKey(Context ctx, String key_alias) {

        SecretKey key = null;
        String error = "";
        String success = "false";
        JSONObject result = new JSONObject();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            try {
                KeyGenParameterSpec.Builder builder =
                        new KeyGenParameterSpec.Builder(key_alias,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);

                KeyGenParameterSpec keySpec = builder
                        .setKeySize(256)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setRandomizedEncryptionRequired(true)
                        .setUserAuthenticationRequired(true)
                        .setUserAuthenticationValidityDurationSeconds(5 * 60)
                        .build();

                KeyGenerator kg = null;
                kg = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                kg.init(keySpec);
                key = kg.generateKey();

                // key retrieval
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);

                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry)ks.getEntry(key_alias, null);
                key = entry.getSecretKey();

                success = "true";

            } catch (NoSuchAlgorithmException | NoSuchProviderException | IOException
                    | CertificateException | UnrecoverableEntryException
                    | InvalidAlgorithmParameterException | KeyStoreException e)
            {
                error = e.getCause().toString();
                LogUtil.logException(ctx, LogUtil.LogType.ACA_UTIL, e);
            }
        }else{
            error = "OS "+ Build.VERSION.SDK_INT+" <= "+ Build.VERSION_CODES.M;
        }
        try {
            result.put("error", error);
            result.put("success", success);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static JSONObject getKey(Context ctx, String key_alias, char[] password) {

        byte[] key_value_bytes = {};
        JSONObject result = new JSONObject();
        String error = "";
        boolean success = false;
        try {

            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if( ks.containsAlias( key_alias)){

                java.security.Key key = ks.getKey( key_alias, password);

                KeyStore.PasswordProtection passwordProtection =
                        new KeyStore.PasswordProtection(password);

                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) ks.getEntry(key_alias, passwordProtection);
                SecretKey myKey = entry.getSecretKey();
                key_value_bytes = myKey.getEncoded();
                success = true;

            }else{
                error = "Key alias "+key_alias+" not found";
            }


        } catch (KeyStoreException | CertificateException
                | NoSuchAlgorithmException | IOException | UnrecoverableEntryException e) {

            error = e.getCause().toString();
            LogUtil.logException(ctx, LogUtil.LogType.ACA_UTIL, e);
        }

        try {
            result.put("error", error);
            result.put("key_value", key_value_bytes.toString());
            result.put("success", success);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static JSONObject putKey(Context ctx, String key_alias, char[] password, String value) {

        JSONObject result = new JSONObject();
        String error = "";
        try {

            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if( ks.containsAlias( key_alias)){

//                SecretKey secretKey = (SecretKey) ks.getKey( key_alias, password);
//
//                KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry( secretKey);
//
//                KeyStore.PasswordProtection passProt = new KeyStore.PasswordProtection(password);
//
//                ks.setEntry( key_alias, skEntry, passProt);
//
//                FileOutputStream fos = new FileOutputStream(value);
//                ks.store( fos, password);
//                fos.close();

                SecretKey key = KeyGenerator.getInstance("AES").generateKey();

                ks.load(null, "clavedekey".toCharArray());

                KeyStore.PasswordProtection pass = new KeyStore.PasswordProtection("fedsgjk".toCharArray());
                KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(key);
                ks.setEntry("secretKeyAlias", skEntry, pass);

                FileOutputStream fos = ctx.openFileOutput("bs.keystore", Context.MODE_PRIVATE);
                ks.store(fos, "clavedekey".toCharArray());
                fos.close();

            }else{

                error = "Key alias "+key_alias+" not found";
            }


        } catch (KeyStoreException | CertificateException
                | NoSuchAlgorithmException | IOException e) {

            error = e.getCause().toString();
            LogUtil.logException(ctx, LogUtil.LogType.ACA_UTIL, e);
            e.printStackTrace();
        }

        try {
            result.put("error", error);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static JSONObject deleteKey(Context ctx, String alias){

        JSONObject result = new JSONObject();
        String error = "";
        try {

            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if( ks.containsAlias( alias)){

                ks.deleteEntry( alias);
            }else{

                error = "Key alias "+alias+" not found";
            }


        } catch (KeyStoreException | CertificateException
                | NoSuchAlgorithmException | IOException e) {

            error = e.getCause().toString();
            LogUtil.logException(ctx, LogUtil.LogType.ACA_UTIL, e);
            e.printStackTrace();
        }

        try {
            result.put("error", error);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Get the names of all keys created by our app
     * @return
     */
    public static JSONArray getKeys() {

        KeyStore ks = null;
        JSONArray keys = new JSONArray();

        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            Enumeration<String> ksEnumeration = ks.aliases();

            while( ksEnumeration.hasMoreElements()){

                JSONObject obj = new JSONObject();
                String ksEnum = ksEnumeration.nextElement();
                obj.put("alias", ksEnum);
                keys.put( obj );
            }

        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return keys;
    }
}
