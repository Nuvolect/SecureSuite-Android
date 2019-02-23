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

import com.nuvolect.securesuite.main.CConst;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static com.nuvolect.securesuite.main.App.getContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

/**
 * {@link KeystoreUtil} utility tests.
 */
public class KeystoreUtilTest {

    private String testKeyAlias = "testKeyAlias";
    private byte[] clearBytesToEncrypt;

    @Before
    public void getReady() {

        try {
            Context ctx = getContext();
            KeystoreUtil.init( ctx );

            clearBytesToEncrypt = "clear text to encrypt".getBytes("UTF-8");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createKey() throws Exception {

        Context ctx = getContext();
        boolean keyCreated = KeystoreUtil.createKeyNotExists( ctx, this.testKeyAlias);
        assertThat( keyCreated, is( true ));

        KeystoreUtil.deleteKey( ctx, this.testKeyAlias, true);
    }

    @Test
    public void encryptDecrypt() throws Exception {

        LogUtil.log( KeystoreUtilTest.class, "encryptDecrypt test starting");

        KeystoreUtil.createKeyNotExists( getContext(), this.testKeyAlias);

        JSONObject cipherObj = KeystoreUtil.encrypt( this.testKeyAlias, this.clearBytesToEncrypt, true);
        assertThat( cipherObj.getString("error"), is(""));
        assertThat( cipherObj.getString("success"), is("true"));
        assertThat( cipherObj.getString("ciphertext"), not(""));
        assertThat( cipherObj.getString("ciphertext"), not(this.clearBytesToEncrypt.toString()));
        assertThat( cipherObj.getString("ciphertext"), not(this.testKeyAlias));

        JSONObject clearTextObj = KeystoreUtil.decrypt( this.testKeyAlias, cipherObj.getString("ciphertext"), true);
        assertThat( clearTextObj.getString("error"), is(""));
        assertThat( clearTextObj.getString("success"), is("true"));
        assertThat( clearTextObj.getString("cleartext"), is( new String(this.clearBytesToEncrypt)));

        KeystoreUtil.deleteKey( getContext(), this.testKeyAlias, true);

        LogUtil.log( KeystoreUtilTest.class, "encryptDecrypt test ending");
    }

    @Test
    public void deleteKey() throws Exception {

        Context ctx = getContext();
        KeystoreUtil.deleteKey( this.testKeyAlias);
        boolean keyCreated = KeystoreUtil.createKeyNotExists( ctx, this.testKeyAlias);
        assertThat( keyCreated, is( true ));

        JSONObject obj = KeystoreUtil.deleteKey( getContext(), this.testKeyAlias, true);
        assertThat( obj.getString("error"), is(""));


        // Try to delete it a second time, should be gone
        obj = KeystoreUtil.deleteKey( getContext(), this.testKeyAlias, true);
        assertThat( obj.getString("error"), not(""));

        boolean notFound = obj.getString("error").contains("not found");
        assertThat( notFound, is(true));
    }

    @Test
    public void getKeys() throws Exception {

        int indexKey1 = -1, indexKey2 = -1, indexKey3 = -1;

        // Create 3 keys
        KeystoreUtil.createKeyNotExists( getContext(), "jibberishKey1");
        KeystoreUtil.createKeyNotExists( getContext(), "jibberishKey2");
        KeystoreUtil.createKeyNotExists( getContext(), "jibberishKey3");

        JSONArray keys = KeystoreUtil.getKeys();

        for( int i =0; i < keys.length(); i++){

            JSONObject key = keys.getJSONObject( i );
            String alias = key.getString("alias");
            if( alias.contentEquals("jibberishKey1"))
                indexKey1 = i;
            if( alias.contentEquals("jibberishKey2"))
                indexKey2 = i;
            if( alias.contentEquals("jibberishKey3"))
                indexKey3 = i;
        }

        // Make sure keys exist
        assertThat( indexKey1, not(-1));
        assertThat( indexKey2, not(-1));
        assertThat( indexKey3, not(-1));

        // Make sure keys are unique
        assertThat( indexKey1, not( indexKey2));
        assertThat( indexKey1, not( indexKey3));
        assertThat( indexKey2, not( indexKey3));

        KeystoreUtil.deleteKey( getContext(), "jibberishKey1", true);
        KeystoreUtil.deleteKey( getContext(), "jibberishKey2", true);
        KeystoreUtil.deleteKey( getContext(), "jibberishKey3", true);

        indexKey1 = -1; indexKey2 = -1; indexKey3 = -1;
        keys = KeystoreUtil.getKeys();

        for( int i =0; i < keys.length(); i++){

            JSONObject key = keys.getJSONObject( i );
            if( key.has("jibberishKey1"))
                indexKey1 = i;
            if( key.has("jibberishKey2"))
                indexKey2 = i;
            if( key.has("jibberishKey3"))
                indexKey3 = i;
        }
        // Make sure deleted keys are not returned with getKeys
        assertThat( indexKey1, is(-1));
        assertThat( indexKey2, is(-1));
        assertThat( indexKey3, is(-1));
    }

    @Test
    public void scenario1() throws Exception{

        Context ctx = getContext();
        String s = "the quick brown fox jumped over the lazy dog";

        byte[] cipherBytes = KeystoreUtil.encrypt(ctx, CConst.APP_KEY_ALIAS, s.getBytes());
        byte[] clearBytes = KeystoreUtil.decrypt(CConst.APP_KEY_ALIAS, cipherBytes);
        String result = new String( clearBytes);
        assertThat( "encryption end to end", result.contentEquals( s));
        assertThat( result, is(s));
    }
}