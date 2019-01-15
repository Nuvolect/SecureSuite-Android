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
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

import com.nuvolect.securesuite.main.CConst;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;


/**
 * Methods and dispatch for using Android's keystore system.
 *
 * From the docs:
 * The Android Keystore system lets you store cryptographic keys in a container to make it
 * more difficult to extract from the device. Once keys are in the keystore, they can be used
 * for cryptographic operations with the key material remaining non-exportable. Moreover,
 * it offers facilities to restrict when and how keys can be used, such as requiring
 * user authentication for key use or restricting keys to be used only in certain
 * cryptographic modes.
 *
 * https://developer.android.com/training/articles/keystore
 */
public class KeystoreUtil {

    private static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final String KEYSTORE_PROVIDER_ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final int BASE64 = Base64.URL_SAFE;
    private static SecureRandom random = new SecureRandom();

    /**
     * Initaliaze the Android keystore. Return false if there are issues otherwise true;
     *
     * @param ctx
     * @return
     */
    public static boolean init( Context ctx){

        boolean success = false;

        if( ! keyExists(CConst.APP_KEY_ALIAS)){

            try {
                createKey( ctx, CConst.APP_KEY_ALIAS);
                success = true;
            } catch (Exception e) {
                LogUtil.logException( KeystoreUtil.class, e);
            }
        }
        else
            success = true;

        return  success;
    }
    /**
     * Test if the Android system lockscreen is enabled by creating a keystore item.
     * Creating the item will fail if the lockscreen is not enabled.
     *
     * @param ctx
     * @return
     */
    public static JSONObject testAndroidLockscreenEnabled(Context ctx){

        String LOCKSCREEN_TEST = "lockscreen_test";
        String lockscreenEnabled  = "disabled";

        JSONObject result = createKey(ctx, LOCKSCREEN_TEST, true);
        try {

            if( result.getString("error").contains("Secure lock screen must be enabled")){

                lockscreenEnabled = "disabled";
            }else
            if( result.getString("success").contentEquals("true")){

                lockscreenEnabled = "enabled";
                deleteKey( LOCKSCREEN_TEST);
            }

            result.put(LOCKSCREEN_TEST,lockscreenEnabled);

        } catch (Exception e) {
            LogUtil.logException( KeystoreUtil.class, e);
        }
        return result;
    }

    /**
     * Create a public/private key in the Andoid keystore. If an existing key exists, overwrite it.
     *
     * @param ctx
     * @param key_alias
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws IOException
     * @throws UnrecoverableEntryException
     * @throws InvalidAlgorithmParameterException
     */
    public static void createKey(Context ctx, String key_alias ) throws NoSuchProviderException,
            NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException,
            UnrecoverableEntryException, InvalidAlgorithmParameterException {

        deleteKey( key_alias);// Delete existing key, if any

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 100);

        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(ctx)
                .setAlias(key_alias)
                .setSubject(new X500Principal("CN=DeepDive, O=Nuvolect"))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();

        KeyPairGenerator generator = KeyPairGenerator.getInstance( "RSA","AndroidKeyStore");
        generator.initialize(spec);
        KeyPair keyPair = generator.generateKeyPair();
    }

    /**
     * Creates a public and private key and stores it using the Android Key Store, so that only
     * this application will be able to access the keys.
     */
    public static JSONObject createKey(Context ctx, String key_alias, boolean verbose) {

        String public_key = "", privateKeyEntryString = "";
        String error = "";
        JSONObject result = new JSONObject();

        try {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 100);

            KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(ctx)
                    .setAlias(key_alias)
                    .setSubject(new X500Principal("CN=DeepDive, O=Nuvolect"))
                    .setSerialNumber(BigInteger.ONE)
                    .setStartDate(start.getTime())
                    .setEndDate(end.getTime())
                    .build();

            KeyPairGenerator generator = KeyPairGenerator.getInstance( "RSA","AndroidKeyStore");
            generator.initialize(spec);

            KeyPair keyPair = generator.generateKeyPair();
            public_key = keyPair.getPublic().toString();

            // Return details of the private key
            KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
            ks.load(null);
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)ks.getEntry( key_alias, null);
            privateKeyEntryString = privateKeyEntry.toString();

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException |
                UnrecoverableEntryException | KeyStoreException | CertificateException |
                IOException | NoSuchProviderException  e) {

            error = e.getMessage();
            LogUtil.logException( KeystoreUtil.class, e);
        }

        try {
            if( verbose){
                result.put("public_key", public_key);
                result.put("private_key_entry", privateKeyEntryString);
                result.put("error", error);
            }
            result.put("success", error.length()==0?"true":"false");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Use a private key indexed by alias to encrypt plain text.
     *
     * @param key_alias
     * @param clearChars, clear text char[]
     * @param verbose
     */
    public static JSONObject encrypt(String key_alias, char[] clearChars, boolean verbose) {

        byte[] clearBytes = Passphrase.toBytes(clearChars);
        return encrypt( key_alias, clearBytes, verbose);
    }

    /**
     * Use a private key indexed by alias to encrypt plain text.
     * Convert String to byte[] using .getBytes("UTF-8")
     *
     * @param key_alias
     * @param clearBytes clear text byte[]
     * @return
     */
    public static JSONObject encrypt(String key_alias, byte[] clearBytes, boolean verbose){

        String cipherTextB64 = "";
        JSONObject result = new JSONObject();
        String error = "";
        try {

            KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
            ks.load(null);
            if( ks.containsAlias( key_alias)){

//                KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)ks.getEntry( key_alias, null);
//                RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

                PrivateKey privateKey = (PrivateKey) ks.getKey(key_alias, null);
                PublicKey publicKey = ks.getCertificate(key_alias).getPublicKey();

                Cipher rsaCipher = Cipher.getInstance( CIPHER_ALGORITHM );
                rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                CipherOutputStream cipherOutputStream = new CipherOutputStream( outputStream, rsaCipher);
                cipherOutputStream.write(clearBytes);
                cipherOutputStream.close();

                byte [] cipherBytes = outputStream.toByteArray();
                outputStream.close();
                cipherTextB64 = Base64.encodeToString(cipherBytes, BASE64);

            }else{
                error = "Key alias not found: "+key_alias;
            }

        } catch ( Exception e) {

            error = e.getCause().toString();
            LogUtil.logException( KeystoreUtil.class, e);
        }

        try {
            if( verbose){

                result.put("ciphertext", cipherTextB64);
                result.put("error", error);
            }
            result.put("success", error.length()==0?"true":"false");

        } catch (JSONException e) {
            LogUtil.logException( KeystoreUtil.class, e);
        }

        return result;
    }

    /**
     * Using the given Android keystore key, encrypt clear bytes and return encrypted bytes.
     *
     * @param key_alias
     * @param clearBytes
     * @return
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws UnrecoverableEntryException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     */
    public static byte[] encrypt(Context ctx, String key_alias, byte[] clearBytes)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            UnrecoverableEntryException, NoSuchPaddingException, InvalidKeyException,
            NoSuchProviderException, InvalidAlgorithmParameterException {

        byte[] cipherBytes = new byte[0];

        // If a specific keystore key has not been created, create it
        KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
        ks.load(null);

        Enumeration<String> aliases = ks.aliases();

        if( ! ks.containsAlias( key_alias)) {

            createKey( ctx, key_alias);
        }

//        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)ks.getEntry( key_alias, null);
//        RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

        PrivateKey privateKey = (PrivateKey) ks.getKey(key_alias, null);
        PublicKey publicKey = ks.getCertificate(key_alias).getPublicKey();

        Cipher rsaCipher = Cipher.getInstance( CIPHER_ALGORITHM );
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream( outputStream, rsaCipher);
        cipherOutputStream.write(clearBytes);
        cipherOutputStream.close();

        cipherBytes = outputStream.toByteArray();
        outputStream.close();

        return cipherBytes;
    }

    /**
     * Basic KeyStore encryption. Assumes key has already been created.
     *
     * @param key_alias
     * @param clearBytes
     * @return
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws UnrecoverableEntryException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     */
    public static byte[] encrypt(String key_alias, byte[] clearBytes)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException,
            IOException, UnrecoverableEntryException, NoSuchPaddingException,
            InvalidKeyException {

        byte[] cipherBytes = new byte[0];

        KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
        ks.load(null);

        PrivateKey privateKey = (PrivateKey) ks.getKey(key_alias, null);
        PublicKey publicKey = ks.getCertificate(key_alias).getPublicKey();

        Cipher rsaCipher = Cipher.getInstance( CIPHER_ALGORITHM );
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream( outputStream, rsaCipher);
        cipherOutputStream.write(clearBytes);
        cipherOutputStream.close();

        cipherBytes = outputStream.toByteArray();
        outputStream.close();

        return cipherBytes;
    }

    /**
     *
     * @param key_alias
     * @param cipherBytes
     * @return
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws UnrecoverableEntryException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     */
    public static byte[] decrypt(String key_alias, byte[] cipherBytes)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            UnrecoverableEntryException, NoSuchPaddingException, InvalidKeyException {

        byte[] decryptBuffer = new byte[2048];
        byte[] decryptedResult = new byte[0];

        if( key_alias == null || key_alias.length() == 0)
            throw new IllegalArgumentException("key alias null or length zero");

        if( cipherBytes == null || cipherBytes.length == 0)
            throw new IllegalArgumentException("cipher bytes null or length zero");

        KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
        ks.load(null);

        if( ks.containsAlias( key_alias)){

            PrivateKey privateKey = (PrivateKey) ks.getKey(key_alias, null);
            PublicKey publicKey = ks.getCertificate(key_alias).getPublicKey();

            Cipher cipher = Cipher.getInstance( CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(cipherBytes), cipher);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int bytesRead;
            while ((bytesRead = cipherInputStream.read(decryptBuffer, 0, 2048)) == 2048){

                outputStream.write(decryptBuffer);
            }
            // Reached the end, write remaining bytes to the stream
            if( bytesRead != -1)
                outputStream.write(decryptBuffer, 0, bytesRead);
            decryptedResult = outputStream.toByteArray();

            cipherInputStream.close();
            outputStream.close();
        }
        else{
            throw new IllegalArgumentException("key not found");
        }

        return decryptedResult;
    }


    /**
     * Use a private key indexed by an alias to decrypt text.
     *
     * @param key_alias
     * @param cipherTextB64
     * @return
     */
    public static JSONObject decrypt(String key_alias, String cipherTextB64, boolean verbose){

        String cleartext = "", privateKeyEntryString="";
        JSONObject result = new JSONObject();
        String error = "";
        try {

            KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
            ks.load(null);
            if( ks.containsAlias( key_alias)){

                PrivateKey privateKey = (PrivateKey) ks.getKey(key_alias, null);
                PublicKey publicKey = ks.getCertificate(key_alias).getPublicKey();

                Cipher cipher = Cipher.getInstance( CIPHER_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, privateKey);

                CipherInputStream cipherInputStream = new CipherInputStream(
                        new ByteArrayInputStream(Base64.decode( cipherTextB64, BASE64)), cipher);

                //FIXME this works but is terribly inefficient, find a better way
                ArrayList<Byte> values = new ArrayList<>();
                int nextByte;
                while ((nextByte = cipherInputStream.read()) != -1) {
                    values.add((byte)nextByte);
                }
                cipherInputStream.close();

                byte[] bytes = new byte[values.size()];
                for(int i = 0; i < bytes.length; i++) {
                    bytes[i] = values.get(i).byteValue();
                }
                cleartext = new String(bytes, 0, bytes.length, "UTF-8");

                if( ! cipherTextB64.isEmpty() && cleartext.isEmpty())
                    error = "ERROR: Decrypt produced empty string";

            }else{
                error = "Key alias not found: "+key_alias;
            }

        } catch ( Exception e) {

            error = e.getMessage();
            LogUtil.logException( KeystoreUtil.class, e);
        }

        try {
            if( verbose ){

                result.put("cleartext", cleartext);
                result.put("private_key_entry", privateKeyEntryString);
                result.put("error", error);
            }
            result.put("success", error.length()==0?"true":"false");

        } catch (JSONException e) {
            LogUtil.logException( KeystoreUtil.class, e);
        }

        return result;
    }

    /**
     * Delete an Android keystore key. If the key does not exist do nothing.
     *
     * @param alias
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static void deleteKey( String alias)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {

        KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
        ks.load(null);

        Enumeration<String> aliases = ks.aliases();

        ks.load(null);

        if( ks.containsAlias( alias)){

            ks.deleteEntry( alias);
        }
    }

    /**
     * Delete a specific key indexed by alias.
     *
     * @param ctx
     * @param alias
     * @return
     */
    public static JSONObject deleteKey(Context ctx, String alias, boolean verbose){

        JSONObject result = new JSONObject();
        String error = "";
        try {

            KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
            ks.load(null);
            if( ks.containsAlias( alias)){

                ks.deleteEntry( alias);
            }else{

                error = "Key alias "+alias+" not found";
            }


        } catch (KeyStoreException | CertificateException
                | NoSuchAlgorithmException | IOException e) {

            error = e.getCause().toString();
            LogUtil.logException( KeystoreUtil.class, e);
        }

        try {
            if( verbose ){

                result.put("error", error);
            }
            result.put("success", error.length()==0?"true":"false");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Get the names of all keys created by our app
     *
     * @return
     */
    public static JSONArray getKeys() {

        KeyStore ks = null;
        JSONArray keys = new JSONArray();

        try {
            ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
            ks.load(null);
            Enumeration<String> ksEnumeration = ks.aliases();

            while( ksEnumeration.hasMoreElements()){

                JSONObject obj = new JSONObject();
                String key_alias = ksEnumeration.nextElement();
                LogUtil.log(KeystoreUtil.class, "keystore key: "+key_alias);
                obj.put("alias", key_alias);

                PrivateKey privateKey = (PrivateKey) ks.getKey(key_alias, null);
                PublicKey publicKey = ks.getCertificate(key_alias).getPublicKey();

                String s = privateKey.toString();

                obj.put("certificate", s);

                keys.put( obj );
            }
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException |
                IOException | JSONException | UnrecoverableEntryException e) {

            LogUtil.logException(KeystoreUtil.class, e);
        }

        return keys;
    }

    /**
     * Test of android keystore key exists.
     * @param keyAlias
     * @return
     */
    public static boolean keyExists( String keyAlias) {

        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
            ks.load(null);

            return ks.containsAlias( keyAlias);

        } catch (Exception e) {
            LogUtil.logException(KeystoreUtil.class, e);
        }
        return false;
    }

    /**
     * Create a public/private key if it does not already exist.
     *
     * @param ctx
     * @param keyAlias
     * @return boolean: true if the key is created.
     */
    public static boolean createKeyNotExists(Context ctx, String keyAlias) {

        boolean keyCreated = false;
        try {

            if( keyExists( keyAlias))
                return false;

            /**
             * Key does not exist. Create the key.
             */
            JSONObject jsonObject = createKey( ctx, keyAlias, true);
            keyCreated = jsonObject.getString( "success").contentEquals( "true");

        } catch ( Exception e) {

            LogUtil.logException(KeystoreUtil.class, e);
        }

        return keyCreated;
    }

    public static void dumpToLog(Context ctx) {

        JSONArray keys = getKeys();

        for( int i = 0; i < keys.length(); i++){

            try {
                JSONObject object = keys.getJSONObject( i );

                String alias = object.getString("alias");
                String certificate = object.getString("certificate");

                LogUtil.log(KeystoreUtil.class, "alias: "+alias);
                LogUtil.log(KeystoreUtil.class, "certificate: "+certificate);
            } catch (JSONException e) {
                LogUtil.logException(KeystoreUtil.class, e);
            }

        }
    }
}
