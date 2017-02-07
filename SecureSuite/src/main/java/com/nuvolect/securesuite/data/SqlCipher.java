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

package com.nuvolect.securesuite.data;

import android.content.ContentValues;
import android.content.Context;
import android.widget.Toast;

import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.DbPassphrase;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.LogUtil.LogType;
import com.nuvolect.securesuite.util.Passphrase;
import com.nuvolect.securesuite.util.Persist;

import net.sqlcipher.Cursor;
import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


/**
 * There are two databases, detail_db with contact details and account_db for names and groups.
 * The databases are separate to get around cursor management issues of the sqlcipher implementation.
 * the account-db has the display name, favorite star and group information.
 * It backs the contact list view and has it't cursor managed by a loader, but open all of the time.
 * The detail_db has one record for each contact and is managed in the more traditional way of
 * The detail_db has one record for each contact and is managed in the more traditional way of
 * creating a cursor, reading or writing data, and closing the cursor.
 * <pre>
 * JSON pairs are stored using the key as a label and value as the target data.
 * Example phone: Key: Home, Value: 123-456-7890
 * Example email: Key: Home, Value: jon@doe.com
 * </pre>
 */
public class SqlCipher {

    private static final boolean DEBUG = LogUtil.DEBUG;
    private static final boolean DEBUG_IMPORT = true;


    private static SqlCipher sInstance;
    private static Context m_ctx;
    public static final int DATABASE_VERSION = 2;
    public static SQLiteDatabase detail_db;
    public static SQLiteDatabase account_db;
    public static final String DETAIL_DB_NAME = "detail_db";
    public static final String DETAIL_TABLE = "detail_table";
    public static int index_contact_id = 1;

    /**
     * List of display_names used to detect duplicates during the import process.
     * Initialized with the initImport() method.
     */
    public static List<String> displayNames = new ArrayList<String>();

    public static final String[] DETAIL_DB_COLUMNS =
    /**/{"_id","contact_id","display_name","comms_select","kv","photo","email","phone","address","im","website","date","relation","internetcall"};

    public enum DTab
    /**/{ _id,  contact_id,  display_name,  comms_select,  kv,  photo,  email,  phone,  address,  im,  website,  date,  relation,  internetcall };
    public enum KvTab
    { name_first, name_last, name_middle, name_prefix, name_suffix, nickname, note, organization,
        phonetic_family, phonetic_given, phonetic_middle, title, username, password
    };

    /**
     * <pre>
     * Account data is organized with a database join method to get all the contacts of a user group.
     * GROUP_TITLE_TABLE - a table of all groups in the entire contact database
     * ACCOUNT_DATA_TABLE - one record for every group a user is in
     * Join the DATA and TITLE tables to get all of the groups of any one user in an account
     * </pre>
     */
    public static final String ACCOUNT_DB_NAME     = "account_db";
    public static final String ACCOUNT_TABLE       = "account_table";
    public static final String GROUP_TITLE_TABLE   = "group_title_table";
    public static final String ACCOUNT_DATA_TABLE  = "account_data_table";
    public static final String ACCOUNT_CRYP_TABLE  = "account_cryp_table";
    public static final String INC_SYNC_TABLE      = "inc_sync_table";

    public static final String[] ACCOUNT_DB_COLUMNS =
    /**/{"_id","contact_id","display_name","display_name_source","last_name","starred","account_name","account_type","cloud_c_id","version","cloud_version"};
    public enum ATab
    /**/{ _id,  contact_id,  display_name,  display_name_source,  last_name,  starred,  account_name,  account_type,  cloud_c_id,  version,  cloud_version
    };

    public static final String[] GROUP_TITLE_COLUMNS =
            {"_id","group_id","title","account_name","account_type"};
    public enum GTTab
    /**/{ _id,  group_id,  title,  account_name,  account_type };

    public static final String[] ACCOUNT_DATA_COLUMNS =
            {"_id","contact_id","group_id" };
    public enum ADTab
    /**/{ _id,  contact_id,  group_id  };

    public static final String[] ACCOUNT_CRYP_COLUMNS =
            {"_id","key","value" };
    public enum ACTab
    /**/{ _id,  key,  value  };

    public static final String[] INC_SYNC_COLUMNS =
            {"_id","key","value" };
    public enum ISTab
    /**/{ _id,  type,  contact_id  };

    public SqlCipher(Context applicationContext) {
        m_ctx = applicationContext;
    }

    /**
     * Initialize SQL Cipher database
     * @param context
     * @return
     */
    public static synchronized SqlCipher getInstance(Context context) {

        if( sInstance == null) {
            // Always pass in the Application Context
            sInstance = new SqlCipher(context.getApplicationContext());
            initializeSQLCipher();
        }
        return sInstance;
    }

    public static synchronized void initializeSQLCipher() {

        SQLiteDatabase.loadLibs(m_ctx);
        File detail_databaseFile = m_ctx.getDatabasePath(DETAIL_DB_NAME);
        File account_databaseFile = m_ctx.getDatabasePath(ACCOUNT_DB_NAME);

        boolean newInstall = !detail_databaseFile.exists();

        if( newInstall ){

            detail_databaseFile.mkdirs();
            detail_databaseFile.delete();
            account_databaseFile.mkdirs();
            account_databaseFile.delete();
        }

        DbPassphrase.createDbKeystore( m_ctx);
        String passphrase = DbPassphrase.getDbPassphrase(m_ctx);
        try {
            detail_db = SQLiteDatabase.openOrCreateDatabase(detail_databaseFile, passphrase, null);
            account_db = SQLiteDatabase.openOrCreateDatabase(account_databaseFile, passphrase, null);
        } catch (Exception e) {
            LogUtil.logException(LogType.SQLCIPHER, e);
        }

        // If first time install, create DB but do not put anything in it
        if( newInstall ){

            /*
             * When updating, also update:
             *      ImportVcf.vcfToContact
             *      SqlCipher.createEmptyContact
             *      CloudContacts.replaceDetailRecord
             */
            detail_db.execSQL("CREATE TABLE " + DETAIL_TABLE + " ("
                    + DTab._id             + " integer primary key,"
                    + DTab.contact_id      + " long unique," //contact_id
                    + DTab.display_name    + " text not null COLLATE NOCASE," //simple text
                    + DTab.comms_select    + " text," // object of kv communication selections
                    + DTab.kv              + " text," // object of json objects
                    + DTab.photo           + " text," // base64 encoded
                    + DTab.email           + " text," // json array of json objects
                    + DTab.phone           + " text," // json array of json objects
                    + DTab.address         + " text," // json array of json objects
                    + DTab.website         + " text," // json array of json objects
                    + DTab.im              + " text," // json array of json objects
                    + DTab.date            + " text," // json array of json objects
                    + DTab.relation        + " text," // json array of json objects
                    + DTab.internetcall    + " text"  // json array of json objects
                    + ");");

            account_db.execSQL("CREATE TABLE " + ACCOUNT_TABLE + " ("
                    + ATab._id                 + " integer primary key,"
                    + ATab.contact_id          + " long unique," //contact_id
                    + ATab.display_name        + " text not null COLLATE NOCASE," //simple text
                    + ATab.display_name_source + " text,"
                    + ATab.last_name           + " text not null," // For sorting, also in DTab.kv
                    + ATab.starred             + " text," // "0" or "1"
                    + ATab.account_name        + " text,"
                    + ATab.account_type        + " text,"
                    + ATab.cloud_c_id          + " long," //Non-zero google cloud contact_id
                    + ATab.version             + " integer," // Local version, bump for each update
                    + ATab.cloud_version       + " integer"  // Cloud version for syncing updates
                    + ");");

            account_db.execSQL("CREATE TABLE " + ACCOUNT_DATA_TABLE + " ("
                    + ADTab._id             + " integer primary key,"
                    + ADTab.contact_id      + " long,"
                    + ADTab.group_id        + " integer"
                    + ");");

            account_db.execSQL("CREATE TABLE " + GROUP_TITLE_TABLE + " ("
                    + GTTab._id             + " integer primary key,"
                    + GTTab.group_id        + " integer,"
                    + GTTab.title           + " text,"
                    + GTTab.account_name    + " text,"
                    + GTTab.account_type    + " text"
                    + ");");

            account_db.execSQL("CREATE TABLE " + ACCOUNT_CRYP_TABLE + " ("
                    + ACTab._id             + " integer primary key,"
                    + ACTab.key             + " text unique,"
                    + ACTab.value           + " text"
                    + ");");

            account_db.execSQL("CREATE TABLE " + INC_SYNC_TABLE + " ("
                    + ISTab._id             + " integer primary key,"
                    + ISTab.type            + " integer,"
                    + ISTab.contact_id      + " long"
                    + ");");

            detail_db.setVersion( DATABASE_VERSION);
            account_db.setVersion( DATABASE_VERSION);
        }
    }



    public static synchronized void deleteDatabases(Context ctx){

        ctx.deleteDatabase(ACCOUNT_DB_NAME);
        ctx.deleteDatabase(DETAIL_DB_NAME);
    }

    public static int getDatabaseVersion(){

        int db_version = detail_db.getVersion();
        return db_version;
    }

    /**
     * Apply a new passphrase to each database,
     * @param ctx
     * @param newKey
     * @return
     */
    public static synchronized boolean rekey(Context ctx, String newKey){

        boolean success = true;
        try {
            String oldKey = DbPassphrase.getDbPassphrase(m_ctx);

            String sql = "PRAGMA key ='"+oldKey+"'";
            account_db.execSQL( sql );

            sql = "PRAGMA rekey ='"+newKey+"'";
            account_db.execSQL( sql );

            sql = "PRAGMA key ='"+oldKey+"'";
            detail_db.execSQL( sql );

            sql = "PRAGMA rekey ='"+newKey+"'";
            detail_db.execSQL( sql );


        } catch (SQLException e) {
            success = false;
            LogUtil.logException(ctx, LogType.SQLCIPHER, e);
        }

        if( success)
            DbPassphrase.setDbPassphrase(m_ctx, newKey);

        return success;
    }

    /**
     * Test the given passphrase with the current pair of databases.
     * @param ctx
     * @param mNewDbPassphrase
     * @return
     */
    public static synchronized boolean testPassphrase(Context ctx, String mNewDbPassphrase) {

        try {
            SQLiteDatabase.loadLibs(m_ctx);
            File detail_databaseFile = m_ctx.getDatabasePath(DETAIL_DB_NAME);
            File account_databaseFile = m_ctx.getDatabasePath(ACCOUNT_DB_NAME);

            detail_db = SQLiteDatabase.openOrCreateDatabase(detail_databaseFile, mNewDbPassphrase, null);
            account_db = SQLiteDatabase.openOrCreateDatabase(account_databaseFile, mNewDbPassphrase, null);

            if( getDbSize() > 0){
                return true;
            }
        } catch (Exception e) {
            LogUtil.log(LogType.SQLCIPHER, "testPassphrase failed");
            LogUtil.logException( ctx, LogType.SQLCIPHER, e);
            return false;
        }
        return false;
    }

    public static synchronized int getDbSize(){

        String [] projection = { DTab._id.toString() };

        Cursor c = detail_db.query( DETAIL_TABLE, projection, null, null, null, null, null);

        int size = c.getCount();
        c.close();
        return size;
    }

    /**
     * Return a string with metrics on the database.
     * @return
     */
    public static String dbCounts(){

        try {
            return getDbCounts().toString(4);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Inverts the star state of a contact.  Returns the inverted state "1" for star and "0" no star.
     * @param contactId
     * @return
     */
    public static synchronized String invertStarred(long contactId){

        String star = SqlCipher.get(contactId, SqlCipher.ATab.starred);// 0 == unstarred
        if (star.startsWith("0"))
            put(contactId, SqlCipher.ATab.starred, "1");
        else
            put(contactId, SqlCipher.ATab.starred, "0");
        return star;
    }

    /**
     * Get a JSON array of the entire database.
     * Warning, ok for small DB, will be memory hog on large databases
     * <pre>
     * Array of all contacts
     *   Array of contact data
     *     0 JSON object of key: "name"
     *     1 JSON object of key: "phone", JSON Array
     *     2 JSON object of key: "email", JSON Array
     *     3 JSON object of key: "address", JSON Array
     * </pre>
     * @return
     */
    public static synchronized JSONArray getAll(){

        JSONArray allArray = new JSONArray();

        Cursor c = detail_db.query(DETAIL_TABLE, DETAIL_DB_COLUMNS, null, null, null, null, null);
        c.moveToFirst();

        JSONObject itemObject;
        JSONArray itemArray;
        JSONArray contactArray;

        while (c.isAfterLast() == false)
        {
            contactArray = new JSONArray();

            try {
                itemObject = new JSONObject().put(DTab.kv.toString(), c.getString(DTab.kv.ordinal()));
                contactArray.put(itemObject);
                itemArray = new JSONArray( c.getString(DTab.phone.ordinal()));
                itemObject = new JSONObject().put(DTab.phone.toString(), itemArray);
                contactArray.put(itemObject);
                itemArray = new JSONArray( c.getString(DTab.email.ordinal()));
                itemObject = new JSONObject().put(DTab.email.toString(), itemArray);
                contactArray.put(itemObject);
                itemArray = new JSONArray( c.getString(DTab.address.ordinal()));
                itemObject = new JSONObject().put(DTab.address.toString(), itemArray);
                contactArray.put(itemObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            allArray.put(contactArray);
            c.moveToNext();
        }
        c.close();

        return allArray;
    }

    /**
     * <pre>
     * Return a contact as a simple formJSONObject. Optionally include group data.
     * The contact is stored in a json wrapper with objects for:
     * CConst.CONTACT
     * CConst.GROUP_TITLE (optional)
     * CCONST.ACCOUNT_DATA (optional)
     * All strings are base64 encoded. This solves a JSON conversion problem involving special characters.
     * @param contact_id
     * @return JSONObject of the contact.
     *
     * example
     *
     * {
     * "contact": {
     * "contact_id": 395,
     * "_id": 357,
     * "display_name": "Rosellen",
     * "comms_select": "{}",
     * "kv": "{\"name_first\":\"Rosellen\",\"name_last\":\"\",\"name_middle\":\"\",\"name_prefix\":\"\",\"name_suffix\":\"\",\"nickname\":\"\",\"note\":\"<HTCData><Facebook>id:1451304821\\/friendof:776629544<\\/Facebook><\\/HTCData>\",\"organization\":\"\",\"phonetic_family\":\"\",\"phonetic_given\":\"\",\"phonetic_middle\":\"\",\"title\":\"\",\"user_name\":\"\",\"password\":\"\"}",
     * "photo": "",
     * "email": "[{\"Other\":\"virtualcatholic@gmail.com\"}]",
     * "phone": "[]",
     * "address": "[]",
     * "website": "[]",
     * "im": "[]",
     * "dates": "[]",
     * "relation": "[]",
     * "internetcall": "[]",
     * "display_name_source": "40",
     * "first_name": "Rosellen",
     * "last_name": "",
     * "starred": "0",
     * "account_name": "goknightro@gmail.com",
     * "account_type": "com.google",
     * "cloud_c_id": 395,
     * "version": 3,
     * "cloud_version": 3
     * },
     * "account_data": [
     * {
     * "_id": 3373,
     * "group_id": 7,
     * "contact_id": 395
     * }
     * ],
     * "group_title": [
     * {
     * "_id": 7,
     * "group_id": 7,
     * "title": "My Contacts",
     * "account_name": "goknightro@gmail.com",
     * "account_type": "com.google",
     * "group_visible": 1
     * }
     * ]
     * }
     * </pre>
     */
    public static synchronized JSONObject getContact(long contact_id, boolean include_group_data)
            throws UnsupportedEncodingException {

        JSONObject wrapper = new JSONObject();
        JSONObject contact = new JSONObject();
        JSONArray account_data = new JSONArray();
        JSONArray group_title = new JSONArray();

        String where = ATab.contact_id+"=?";
        String[] args = new String[]{String.valueOf( contact_id )};

        try {

            Cursor c = detail_db.query(DETAIL_TABLE, DETAIL_DB_COLUMNS, where, args, null, null, null);

            if( c.getCount() == 0){
                c.close();
                return wrapper;
            }
            else
                c.moveToFirst();

            contact.put(DTab.contact_id.toString(),   contact_id);
            contact.put(DTab._id.toString(), c.getInt(DTab._id.ordinal()));
            // Use the ATab version of display_name
//            contact.put(DTab.display_name.toString(), encodeBase64(c, DTab.display_name.ordinal()));
            contact.put(DTab.comms_select.toString(), encodeBase64(c, DTab.comms_select.ordinal()));
            contact.put(DTab.kv.toString(),           encodeBase64(c, DTab.kv.ordinal()));
            contact.put(DTab.photo.toString(),        encodeBase64(c, DTab.photo.ordinal()));
            contact.put(DTab.email.toString(),        encodeBase64(c, DTab.email.ordinal()));
            contact.put(DTab.phone.toString(),        encodeBase64(c, DTab.phone.ordinal()));
            contact.put(DTab.address.toString(),      encodeBase64(c, DTab.address.ordinal()));
            contact.put(DTab.website.toString(),      encodeBase64(c, DTab.website.ordinal()));
            contact.put(DTab.im.toString(),           encodeBase64(c, DTab.im.ordinal()));
            contact.put(DTab.date.toString(),        encodeBase64(c, DTab.date.ordinal()));
            contact.put(DTab.relation.toString(),     encodeBase64(c, DTab.relation.ordinal()));
            contact.put(DTab.internetcall.toString(), encodeBase64(c, DTab.internetcall.ordinal()));
            c.close();

            c = account_db.query(ACCOUNT_TABLE, ACCOUNT_DB_COLUMNS, where, args, null, null, null);
            c.moveToFirst();

            contact.put(ATab._id.toString(),           c.getInt(ATab._id.ordinal()));
            // We already have contact_id
            contact.put(ATab.display_name.toString(), encodeBase64(c, ATab.display_name.ordinal()));
//Debug start
//            String display_name = c.getString(ATab.display_name.ordinal());
//            LogUtil.log(LogType.SQLCIPHER, "getContact: "+display_name+", "+contact_id);
//Debug end
            contact.put(ATab.display_name_source.toString(), encodeBase64(c, ATab.display_name_source.ordinal()));
            contact.put(ATab.last_name.toString(),     encodeBase64(c, ATab.last_name.ordinal()));
            contact.put(ATab.starred.toString(),       encodeBase64(c, ATab.starred.ordinal()));
            contact.put(ATab.account_name.toString(),  encodeBase64(c, ATab.account_name.ordinal()));
            contact.put(ATab.account_type.toString(),  encodeBase64(c, ATab.account_type.ordinal()));
            contact.put(ATab.cloud_c_id.toString(),    c.getLong(ATab.cloud_c_id.ordinal()));
            contact.put(ATab.version.toString(),       c.getInt(ATab.version.ordinal()));
            contact.put(ATab.cloud_version.toString(), c.getInt(ATab.cloud_version.ordinal()));
            c.close();
            wrapper.put(CConst.CONTACT, contact);

            if( include_group_data){

                c = account_db.query(ACCOUNT_DATA_TABLE, ACCOUNT_DATA_COLUMNS, where, args, null, null, null);
                ArrayList<Integer> group_ids = new ArrayList<Integer>();

                while( c.moveToNext()){

                    int group_id = c.getInt(ADTab.group_id.ordinal());

                    JSONObject obj = new JSONObject();
                    obj.put(CConst._ID,       c.getInt(ADTab._id.ordinal()));
                    obj.put(CConst.GROUP_ID,  group_id);
                    obj.put(CConst.CONTACT_ID,c.getLong(ADTab.contact_id.ordinal()));
                    account_data.put(obj);

                    group_ids.add( group_id);// Keep ids to query in next step
                }
                c.close();

                wrapper.put(CConst.ACCOUNT_DATA, account_data);

                for( int g_id : group_ids){

                    where = GTTab.group_id+"=?";
                    args = new String[]{String.valueOf( g_id )};
                    c = account_db.query(GROUP_TITLE_TABLE, GROUP_TITLE_COLUMNS, where, args, null, null, null);
                    c.moveToFirst();

                    JSONObject obj = new JSONObject();
                    obj.put(CConst._ID,          c.getInt( GTTab._id.ordinal()));
                    obj.put(CConst.GROUP_ID,     c.getInt(   GTTab.group_id.ordinal()));
                    obj.put(CConst.TITLE,        encodeBase64(c, GTTab.title.ordinal()));
                    obj.put(CConst.ACCOUNT_NAME, encodeBase64(c, GTTab.account_name.ordinal()));
                    obj.put(CConst.ACCOUNT_TYPE, encodeBase64(c, GTTab.account_type.ordinal()));
                    group_title.put(obj);

                    c.close();
                }
                wrapper.put(CConst.GROUP_TITLE, group_title);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            LogUtil.logException(LogType.SQLCIPHER, e);
        }

        return wrapper;
    }

    private static String encodeBase64(Cursor c, int ordinal) throws UnsupportedEncodingException {

        byte[]  bytesEncoded = Base64.encodeBase64(c.getString(ordinal).getBytes("utf-8"));
        return new String( bytesEncoded);
    }

    private static String decodeBase64(JSONObject jsonObject, String key) throws JSONException, UnsupportedEncodingException {

        String encoded = jsonObject.getString(key);
        byte[] decoded = Base64.decodeBase64(encoded.getBytes("utf-8"));

        return new String( decoded);
    }

    /**
     * <pre>
     * Create or update a contact from the simple JSON form. Some elements are optional.
     * The contact is stored in a json wrapper with objects for:
     * CConst.CONTACT:      JSONObject
     * CConst.GROUP_TITLE:  JSONArray (optional)
     * CCONST.ACCOUNT_DATA: JSONArray (optional)
     * @param wrapper
     * @return long contact_id of the contact or -1 on error.
     * </pre>
     */
    public static synchronized long createOrUpdateContact(JSONObject wrapper) throws UnsupportedEncodingException {

        long contact_id = -1;
        String display_name = "";
        try {
            JSONObject contact = wrapper.getJSONObject(CConst.CONTACT);
            ContentValues detail_cv = new ContentValues();

            contact_id = contact.getLong(  DTab.contact_id.toString());
            display_name = decodeBase64(contact, DTab.display_name.toString());
            detail_cv.put( DTab._id.toString(),         contact.getInt(DTab._id.toString()));
            detail_cv.put( DTab.contact_id.toString(),  contact_id);
            detail_cv.put( DTab.display_name.toString(),display_name);
            detail_cv.put( DTab.comms_select.toString(),decodeBase64(contact, DTab.comms_select.toString()));
            detail_cv.put( DTab.kv.toString(),          decodeBase64(contact, DTab.kv.toString()));
            detail_cv.put( DTab.photo.toString(),       decodeBase64(contact, DTab.photo.toString()));
            detail_cv.put( DTab.email.toString(),       decodeBase64(contact, DTab.email.toString()));
            detail_cv.put( DTab.phone.toString(),       decodeBase64(contact, DTab.phone.toString()));
            detail_cv.put( DTab.address.toString(),     decodeBase64(contact, DTab.address.toString()));
            detail_cv.put( DTab.website.toString(),     decodeBase64(contact, DTab.website.toString()));
            detail_cv.put( DTab.im.toString(),          decodeBase64(contact, DTab.im.toString()));
            detail_cv.put( DTab.date.toString(),        decodeBase64(contact, DTab.date.toString()));
            detail_cv.put( DTab.relation.toString(),    decodeBase64(contact, DTab.relation.toString()));
            detail_cv.put( DTab.internetcall.toString(),decodeBase64(contact, DTab.internetcall.toString()));

            detail_db.beginTransaction();
            long row = detail_db.insertWithOnConflict(DETAIL_TABLE, null, detail_cv, SQLiteDatabase.CONFLICT_REPLACE);
            if( row > 0)
                detail_db.setTransactionSuccessful();
            detail_db.endTransaction();
            if( row == -1)
                return -1;

            ContentValues account_cv = new ContentValues();
            account_cv.put( ATab._id.toString(),                 contact.getInt(ATab._id.toString()));
            account_cv.put( ATab.contact_id.toString(),          contact_id);
            account_cv.put( ATab.display_name.toString(),        display_name);
            account_cv.put( ATab.display_name_source.toString(), decodeBase64(contact, ATab.display_name_source.toString()));
            account_cv.put( ATab.last_name.toString(),           decodeBase64(contact, ATab.last_name.toString()));
            account_cv.put( ATab.starred.toString(),             decodeBase64(contact, ATab.starred.toString()));
            account_cv.put( ATab.account_name.toString(),        decodeBase64(contact, ATab.account_name.toString()));
            account_cv.put( ATab.account_type.toString(),        decodeBase64(contact, ATab.account_type.toString()));
            account_cv.put( ATab.cloud_c_id.toString(),          contact.getLong(  ATab.cloud_c_id.toString()));
            account_cv.put( ATab.version.toString(),             contact.getInt(   ATab.version.toString()));
            account_cv.put( ATab.cloud_version.toString(),       contact.getInt(   ATab.cloud_version.toString()));

            account_db.beginTransaction();
            row = account_db.insertWithOnConflict(ACCOUNT_TABLE, null, account_cv, SQLiteDatabase.CONFLICT_REPLACE);
            if( row > 0)
                account_db.setTransactionSuccessful();
            account_db.endTransaction();
            if( row == -1)
                return -1;

            /**
             * Check the payload for group data, if so sync it too
             */
            if( wrapper.has((CConst.GROUP_TITLE))){

                JSONArray group_title = wrapper.getJSONArray(CConst.GROUP_TITLE);

                for( int i = 0; i < group_title.length(); i++){

                    JSONObject title = group_title.getJSONObject( i );
                    ContentValues title_cv = new ContentValues();
                    title_cv.put(GTTab._id.toString(),           title.getInt( GTTab._id.toString()));
                    title_cv.put(GTTab.group_id.toString(),      title.getInt( GTTab.group_id.toString()));
                    title_cv.put(GTTab.title.toString(),         decodeBase64( title, GTTab.title.toString()));
                    title_cv.put(GTTab.account_name.toString(),  decodeBase64( title, GTTab.account_name.toString()));
                    title_cv.put(GTTab.account_type.toString(),  decodeBase64( title, GTTab.account_type.toString()));

                    account_db.beginTransaction();
                    row = account_db.insertWithOnConflict(GROUP_TITLE_TABLE, null, title_cv, SQLiteDatabase.CONFLICT_REPLACE);
                    if( row > 0)
                        account_db.setTransactionSuccessful();
                    account_db.endTransaction();
                    if( row == -1)
                        return -1;
                }

                /**
                 * Remove any existing group records before restoring with current records
                 */
                MyGroups.deleteGroupRecords( contact_id );

                JSONArray account_data = wrapper.getJSONArray(CConst.ACCOUNT_DATA);

                for( int i = 0; i < account_data.length(); i++){

                    JSONObject group = account_data.getJSONObject( i );
                    ContentValues group_cv = new ContentValues();
                    group_cv.put(ADTab.contact_id.toString(), group.getLong(ADTab.contact_id.toString()));
                    group_cv.put(ADTab._id.toString(),        group.getInt(ADTab._id.toString()));
                    group_cv.put(ADTab.group_id.toString(),   group.getInt(ADTab.group_id.toString()));

                    account_db.beginTransaction();
                    row = account_db.insertWithOnConflict(ACCOUNT_DATA_TABLE, null, group_cv, SQLiteDatabase.CONFLICT_REPLACE);
                    if( row > 0)
                        account_db.setTransactionSuccessful();
                    account_db.endTransaction();
                    if( row == -1)
                        return -1;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            LogUtil.logException(LogType.SQLCIPHER, e);
            return -1;
        }
        if( DEBUG )
            LogUtil.log(LogType.SQLCIPHER, "Updated or added: "+display_name);
        return contact_id;
    }

    /**
     * Basic diagnostic info: contact name, id and account,
     * otherwise the contactId if invalid.
     * @param contact_id
     * @return
     */
    public static synchronized String contactInfo( long contact_id){

        if( contact_id <= 0)
            return "contact_id is "+contact_id;

        String displayName = SqlCipher.get(contact_id, ATab.display_name);
        String account = SqlCipher.get(contact_id, ATab.account_name);
        return displayName+", "+contact_id+", "+account;
    }

    /**<pre>
     * Delete a contact.  This includes records from:
     *     detail_db.detail_table
     *     account_db.account_table
     *     account_db.account_data_table
     * A check is made for the profile contact.
     * @param contact_id
     * @param syncTransaction
     * @return number of contacts deleted
     * </pre>
     */
    public static synchronized int deleteContact(Context ctx, long contact_id, boolean syncTransaction) {

        if(DEBUG) {
            if (validContactId(contact_id))
                LogUtil.log("deleteContact: " + contactInfo(contact_id));
            else
                LogUtil.log("deleteContact, invalid contact ID: " + contact_id);
        }

        //Check if it was the profile contact, if so reset profile contact
        if( contact_id == Persist.getProfileId(ctx)){

            Persist.setProfileId(ctx, 0);
        }

        //OK to reuse where and args, all databases use the same contact_id name
        int rows = 0;
        int totalRows = 0;
        String where = DTab.contact_id+"=?";
        String[] args = new String[]{String.valueOf( contact_id )};

        //Delete from both account and detail db
        detail_db.beginTransaction();
        rows = detail_db.delete(DETAIL_TABLE, where, args);
        if( rows == 1){
            ++totalRows;
            detail_db.setTransactionSuccessful();
        }
        else{
            if(DEBUG) LogUtil.log("ERROR deleteContact, detail_table: "+contact_id);
        }
        detail_db.endTransaction();

        account_db.beginTransaction();
        rows = account_db.delete(ACCOUNT_TABLE, where, args);
        if( rows == 1){
            ++totalRows;
            account_db.setTransactionSuccessful();
        }
        else{
            if(DEBUG) LogUtil.log("ERROR deleteContact account_table: "+contact_id);
        }
        account_db.endTransaction();

        //Delete any group records
        account_db.beginTransaction();
        rows = account_db.delete(ACCOUNT_DATA_TABLE, where, args);
        if( rows > 0){
            totalRows += rows;
            account_db.setTransactionSuccessful();
        }
        else if( rows < 0){
            if(DEBUG) LogUtil.log("ERROR deleteContact account_data_table: "+contact_id);
        }
        account_db.endTransaction();

        /**
         * Record the contact_id for incremental database synchronization
         */
        if( syncTransaction)
            SqlIncSync.getInstance().deleteContact(ctx, contact_id);

        return totalRows;
    }

    /**
     * Return the contact_id given the _id
     * @param _id
     * @return long
     */
    public static synchronized long getATabContactId(long _id) {

        if( _id == 0){

            if(DEBUG) LogUtil.log("ERROR _id 0");
            //                throw new RuntimeException("Can have exactly one ID specific contact record");
            return 0;
        }

        String[] projection = {ATab.contact_id.toString()};
        String where = ATab._id+"=?";
        String[] args = new String[]{String.valueOf( _id )};

        Cursor c = account_db.query(ACCOUNT_TABLE, projection, where, args, null, null, null);
        c.moveToFirst();

        if( c.getCount() != 1){
            c.close();
            throw new RuntimeException("getATabContactId, get should only find one record");
        }
        long contact_id = c.getLong(0);

        c.close();
        return contact_id;
    }

    /**
     * Give a cursor and position, return the contact_id. Return -1 if the cursor does
     * not have that position.
     * @param c
     * @param position
     * @return
     */
    public static synchronized long getDTabContactId(Cursor c, int position) {

        if( c.moveToPosition( position )){

            return c.getLong(c.getColumnIndex( DTab.contact_id.toString()));
        }
        else
            return -1;
    }

    /**
     * Get the string referenced.  Works for data in the ACCOUNT database
     * @param contact_id
     * @param atabEnum
     * @return String data
     */
    public static synchronized String get(long contact_id, ATab atabEnum) {

        if( contact_id == 0){

            if(DEBUG) LogUtil.log("contact_id 0");
        }

        String[] columns = new String[]{ atabEnum.toString() };
        String where = ATab.contact_id+"=?";
        String[] args = new String[]{String.valueOf( contact_id )};

        Cursor c = account_db.query(ACCOUNT_TABLE, columns, where, args, null, null, null);
        c.moveToFirst();
        int count = c.getCount();

        if( count != 1){

            c.close();
            throw new RuntimeException(
                    "get found: "+count+" != 1 one record for contact_id: "+contact_id);
        }

        String returnString = c.getString(0);

        c.close();
        return returnString;
    }

    /**
     * Get the int referenced.  Works for data in the ACCOUNT database
     * @param contact_id
     * @param atabEnum
     * @return int version of the contact or -1 if the contact does not exist
     */
    public static synchronized int getInt(long contact_id, ATab atabEnum) {

        if( contact_id == 0){

            if(DEBUG) LogUtil.log("contact_id 0");
        }

        String[] columns = new String[]{ atabEnum.toString() };
        String where = ATab.contact_id+"=?";
        String[] args = new String[]{String.valueOf( contact_id )};

        Cursor c = account_db.query(ACCOUNT_TABLE, columns, where, args, null, null, null);
        c.moveToFirst();
        int count = c.getCount();

        if( count > 1){

            c.close();
            throw new RuntimeException(
                    "get found: "+count+" > 1 one record for contact_id: "+contact_id);
        }
        if( count == 0){

            c.close();
            return -1;
        }

        int value = c.getInt(0);

        c.close();
        return value;
    }

    /**
     * Get the string referenced.  Works for data in the DETAIL database
     * @param contact_id
     * @param dbEnum
     * @return String data
     */
    public static synchronized String get(long contact_id, DTab dbEnum) {

        if( contact_id <= 0){

            if(DEBUG) LogUtil.log("SqlCipher ERROR get contact_id: :"+contact_id);
            return "";
        }

        String[] columns = new String[]{ dbEnum.toString() };
        String where = DTab.contact_id+"=?";
        String[] args = new String[]{String.valueOf( contact_id )};

        Cursor c = detail_db.query(DETAIL_TABLE, columns, where, args, null, null, null);
        c.moveToFirst();
        int count = c.getCount();

        if( count != 1){
            c.close();
            throw new RuntimeException(
                    "get count !=1: "+count+" should only find one record for id:"+contact_id);
        }

        String returnString = c.getString(0);

        c.close();
        return returnString;
    }

    public static String getFirstItem(int contact_id, DTab dTab) {

        try {
            String jsonStr = get(contact_id, dTab);
            JSONArray jsonArray = new JSONArray( jsonStr);
            if( jsonArray.length() == 0)
                return "";

            JSONObject jsonObject = jsonArray.getJSONObject(0);
            Iterator<String> keys = jsonObject.keys();
            String value = jsonObject.getString( keys.next());
            return value;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Test the local contact version to see if it is newer or not exists.
     * @param contact_id
     * @param version
     * @return boolean
     */
    public static synchronized boolean versionNewerOrNotExists(long contact_id, int version) {

        int local_version = getInt(contact_id, ATab.version);

        return local_version == -1 || version > local_version;
    }
    /**
     * Increment the account table version number.  This is called from
     * two update methods, one each for the Account and Detail tables.
     * @param contact_id
     * @return The updated version is returned.
     */
    public static synchronized int incrementVersion(long contact_id){

        int version = 1 + getInt(contact_id, ATab.version);
        updateInt(contact_id, ATab.version, version);

        return version;
    }

    /**
     * Replace the existing int with the value string in the ACCOUNT db.
     * @param contact_id
     * @param atabEnum ATab enum key
     * @param value
     * @return int number of rows updated, 1 == success, 0 == fail
     */
    public static synchronized long updateInt(long contact_id, ATab atabEnum, int value){

        if( contact_id == 0){

            if(DEBUG) LogUtil.log("contact_id 0");
        }

        String where = ATab.contact_id+"=?";
        String[] args = new String[]{String.valueOf( contact_id )};

        ContentValues cv = new ContentValues();
        cv.put(atabEnum.toString(), value);

        account_db.beginTransaction();
        long rows = account_db.update(ACCOUNT_TABLE, cv, where, args);
        if( rows == 1)
            account_db.setTransactionSuccessful();
        account_db.endTransaction();

        return rows;
    }
    /**
     * Replace the existing string with the value string in the ACCOUNT db.
     * @param contact_id
     * @param atab ATab enum key
     * @param value
     * @return int number of rows updated, 1 == success, 0 == fail
     */
    public static synchronized long put(long contact_id, ATab atab, String value){

        if( contact_id == 0){

            if(DEBUG) LogUtil.log("contact_id 0");
        }

        String where = ATab.contact_id+"=?";
        String[] args = new String[]{String.valueOf( contact_id )};

        ContentValues cv = new ContentValues();
        cv.put(atab.toString(), value);

        account_db.beginTransaction();
        long rows = account_db.update(ACCOUNT_TABLE, cv, where, args);
        if( rows == 1) {
            account_db.setTransactionSuccessful();
            account_db.endTransaction();
            /**
             * Bump the database record version for syncing databases
             */
            incrementVersion(contact_id);

            /**
             * Note incremental update
             */
             SqlIncSync.getInstance().updateContact(m_ctx, contact_id);
        }else
            account_db.endTransaction();

        return rows;
    }

    /**
     * Replace the existing string with the value string in the DETAIL db.
     * @param contact_id
     * @param dtab DTab enum key
     * @param value
     * @return
     */
    public static synchronized long put(long contact_id, DTab dtab, String value){

        if( contact_id == 0){

            if(DEBUG) LogUtil.log("contact_id 0");
        }

        String where = DTab.contact_id+"=?";
        String[] args = new String[]{String.valueOf( contact_id )};

        ContentValues cv = new ContentValues();
        cv.put( dtab.toString(), value);

        detail_db.beginTransaction();
        long rows = detail_db.update( DETAIL_TABLE, cv, where, args);
        if( rows == 1) {
            detail_db.setTransactionSuccessful();
            detail_db.endTransaction();
            /**
             * Bump the database record version for syncing databases
             */
            incrementVersion(contact_id);

            SqlIncSync.getInstance().updateContact(m_ctx, contact_id);
        }
        else
            detail_db.endTransaction();

        return rows;
    }

    /**
     * Update the element (index) value of a contact detail.  Elements like email, mobile, etc.
     * can have more than one instance.  Each instance has two parts, the value and the category or
     * label. This method updates the value part.
     * @param contact_id
     * @param dtab
     * @param index
     * @param label
     * @return
     */
    public static synchronized boolean updateLabel(long contact_id, DTab dtab, int index, String label){

        try {
            // Get the collection of k/v pairs, k == the category or label and v is value
            JSONArray array = new JSONArray( get(contact_id, dtab));

            // Get the specific object to update
            JSONObject obj = array.getJSONObject(index);

            // Find the key or label associated with the iteam
            Iterator<?> item_keys = obj.keys();
            String item_label = (String) item_keys.next();

            // Get the value so we can make an object with the new label.
            String item_value = obj.getString(item_label);

            // Make the new object using old value but new label
            JSONObject newObj = new JSONObject();
            newObj.put(label, item_value);

            // Restore the object to the same position in the array
            array.put(index, newObj);

            // Update the database
            put(contact_id, dtab, array.toString());

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Update the element (index) value of a contact detail.  Elements like email, mobile, etc.
     * can have more than one instance.  Each instance has two parts, the value and the category or
     * label. This method updates the value part.
     * @param contact_id
     * @param dtab
     * @param index
     * @param value
     * @return
     */
    public static synchronized boolean updateValue(long contact_id, DTab dtab, int index, String value){

        try {
            // Get the collection of k/v pairs, k == the category or label and v is value
            JSONArray array = new JSONArray( get(contact_id, dtab));

            // Get the specific object to update
            JSONObject obj = array.getJSONObject(index);

            // Find the key or label associated with the iteam
            Iterator<?> item_keys = obj.keys();
            String item_label = (String) item_keys.next();

            // Update the value using the label
            obj.put(item_label, value);

            // Restore the object to the same position in the array
            array.put(index, obj);

            // Update the database
            put(contact_id, dtab, array.toString());

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Delete the indexed value of a contact detail. Elements like email, mobile, etc.
     * can have more than one instance.  This method deletes both parts, the value and the category
     * or label.
     * @param contact_id
     * @param dtab
     * @param index
     * @return
     */
    public static synchronized boolean deleteIndexedItem(long contact_id, DTab dtab, int index){

        try {
            // Get the array of k/v pairs, k == the category or label and v is value
            JSONArray array = new JSONArray( get(contact_id, dtab));

            JSONArray newArray = new JSONArray();

            /**
             * Copy while omitting item to delete.  We can just use 'remove', it requires KITKAT
             */
            for( int i = 0; i < array.length(); i++){

                if( i != index)
                    newArray.put( array.get( i ));
            }

            // Update the database
            put(contact_id, dtab, newArray.toString());

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Add a detail item to a JSONArray list, save as a string.
     * @param contact_id
     * @param dtab
     * @param value
     * @return long, number of rows updated
     */
    public static synchronized long add(long contact_id, DTab dtab, String key, String value){

        long additions = 0;
        try {
            JSONArray array = new JSONArray( get(contact_id, dtab));
            JSONObject obj = new JSONObject();
            obj.put(key, value);
            array.put(obj);
            additions = put(contact_id, dtab, array.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return additions;
    }

    /**
     * Get secondary information about a contact first looking for an
     * email and second a phone number.  If there is no email or phone number
     * an empty string is returned.
     *
     * @param contact_id
     * @return
     */
    public static synchronized String getContactSecondaryInfo(long contact_id){

        String info = "";
        try {
            String emailJsonStr = get( contact_id, DTab.email);
            if( ! emailJsonStr.contains("[]")){

                JSONArray itemArray = new JSONArray( emailJsonStr);
                JSONObject item = itemArray.getJSONObject( 0 );
                Iterator<?> item_keys = item.keys();
                String item_label = (String) item_keys.next();
                info = item.getString(item_label);
            }
            if( info.isEmpty()){

                String phoneJsonStr = get( contact_id, DTab.phone);
                if( ! phoneJsonStr.contains("[]")){

                    JSONArray itemArray = new JSONArray( phoneJsonStr);
                    JSONObject item = itemArray.getJSONObject( 0 );
                    Iterator<?> item_keys = item.keys();
                    String item_label = (String) item_keys.next();
                    info = item.getString(item_label);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return info;
    }

    public static synchronized long createEmptyContact(Context ctx, int currentGroupId) {

        long contact_id = getNextUnusedContactID();

        // 1. Update detail db
        ContentValues detail_cv = new ContentValues();
        detail_cv.put( DTab.contact_id.toString(),   contact_id);
        detail_cv.put( DTab.display_name.toString(), "");
        detail_cv.put( DTab.comms_select.toString(),  "{}");
        // kv is in following block
        detail_cv.put( DTab.photo.toString(),        "");
        detail_cv.put( DTab.email.toString(),        "[]");
        detail_cv.put( DTab.phone.toString(),        "[]");
        detail_cv.put( DTab.address.toString(),      "[]");
        detail_cv.put( DTab.website.toString(),      "[]");
        detail_cv.put( DTab.im.toString(),           "[]");
        detail_cv.put( DTab.date.toString(),        "[]");
        detail_cv.put( DTab.relation.toString(),     "[]");
        detail_cv.put( DTab.internetcall.toString(), "[]");

        JSONObject kv = new JSONObject();

        for( KvTab kvObj : KvTab.values())
            try {
                kv.put(kvObj.toString(), "");
            } catch (JSONException e) { e.printStackTrace(); }

        detail_cv.put( DTab.kv.toString(),           kv.toString());

        detail_db.beginTransaction();
        long row = detail_db.insert( DETAIL_TABLE, null, detail_cv);
        if( row > 0)
            detail_db.setTransactionSuccessful();
        detail_db.endTransaction();

        // 2. Update account db
        ContentValues account_cv = new ContentValues();
        account_cv.put( ATab.contact_id.toString(),          contact_id);
        account_cv.put( ATab.display_name.toString(),        "");
        account_cv.put( ATab.display_name_source.toString(), "");
        account_cv.put( ATab.last_name.toString(),           "");
        account_cv.put( ATab.starred.toString(),             CConst.STARRED_0);
        account_cv.put( ATab.account_name.toString(),        Cryp.getCurrentAccount());
        account_cv.put( ATab.account_type.toString(),        Persist.getCurrentAccountType(ctx));
        account_cv.put( ATab.cloud_c_id.toString(),          0L);
        account_cv.put(ATab.version.toString(), 1);
        account_cv.put(ATab.cloud_version.toString(), 1);

        account_db.beginTransaction();
        row = account_db.insert( ACCOUNT_TABLE, null, account_cv);
        if( row > 0)
            account_db.setTransactionSuccessful();
        account_db.endTransaction();

        // add current group if non-zero
        if( currentGroupId > 0 ){

            MyGroups.addGroupMembership( ctx, contact_id, currentGroupId, true);
            MyGroups.mGroupCount.put(currentGroupId, 1+ MyGroups.mGroupCount.get(currentGroupId));
        }
        /**
         * Record the contact_id for incremental database synchronization
         */
        SqlIncSync.getInstance().insertContact(ctx, contact_id);

        return contact_id;
    }

    /**
     * Get ready for import, initialize metrics, hash maps etc.
     */
    public static synchronized void initImport() {

        displayNames.clear();
    }

    /**
     * Clean up and close after import.
     */
    public static synchronized void closeImport() {

        displayNames.clear();// release structure to memory management
    }

    /**
     * Return the id of the first contact if there is one, otherwise return -1 indicating there
     * are no contacts. The ID may be in any account.
     * @return
     */
    public static synchronized long getFirstContactID() {

        String [] projection = { ATab.contact_id.toString() };

        Cursor c = account_db.query(ACCOUNT_TABLE, projection,
                null, null, null, null, ATab.display_name+" ASC");

        if( c.getCount() <= 0 ){

            c.close();
            return -1;
        }

        c.moveToFirst();
        long contact_id = c.getLong( 0 );
        c.close();

        if( contact_id == 0){

            if(DEBUG) LogUtil.log("contact_id 0");
        }

        return contact_id;
    }
    /**
     * Return the next unused contact id otherwise return -1 indicating there are no more contacts IDs.
     * @return
     */
    public static synchronized long getNextUnusedContactID() {

        String [] projection = { ATab.contact_id.toString() };

        Cursor c = account_db.query(ACCOUNT_TABLE, projection,
                null, null, null, null, ATab.contact_id+" DESC");

        if( c.getCount() <= 0 ){

            c.close();
            return 1;  // No contacts yet, start with 1
        }

        c.moveToFirst();
        long contact_id = c.getLong( 0 );
        c.close();

        try {
            ++contact_id;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        return contact_id;
    }

    /**
     * <pre>
     * Return the ID next to current ID.
     * First try the position after the current position.
     * If at the end, take the previous position.
     * If that is before the first position, get the first contact id.
     * </pre>
     */
    public static synchronized long getNextContactId( Cursor cursor ) {

        if( cursor == null || cursor.isClosed())
           return getFirstContactID();

        int savePosition = cursor.getPosition();// Save so it can be restored
        long next_id = 0;
        int index_contact_id = 1;  // second position in the cursor

        if( ! cursor.isLast() && ! cursor.isAfterLast()){

            cursor.moveToNext();
            next_id = cursor.getLong( index_contact_id );
        }else{
            if( cursor.isLast()){
                cursor.moveToPrevious();
                if( ! cursor.isBeforeFirst()){

                    next_id = cursor.getLong( index_contact_id );
                }
            }
        }

        if( next_id == 0)
            next_id = getFirstContactID();
        cursor.moveToPosition(savePosition);
        return next_id;
    }

    /**
     * Get a single value from the KV object set and return it as a string.
     * @param contact_id
     * @param kvEnum
     * @return value of KV object, string.
     */
    public static synchronized String getKv(long contact_id, KvTab kvEnum) {
        String kvReturn = "";
        String kvString = get(contact_id, DTab.kv);
        try {
            JSONObject kvJobj = new JSONObject( kvString);

            if( kvJobj.has(kvEnum.toString()))
                kvReturn = kvJobj.getString( kvEnum.toString());

        } catch (JSONException e) {
            kvReturn = "error";
            e.printStackTrace();
        }
        return kvReturn;
    }

    /**
     * Update a single element of the key value collection record
     * @param contact_id
     * @param kvEnum
     * @param value
     * @return - false of there was a exception
     */
    public static synchronized boolean putKv(long contact_id, KvTab kvEnum, String value){
        boolean error = false;

        if( contact_id == 0){

            if(DEBUG) LogUtil.log("contact_id 0");
        }

        try {
            JSONObject kvObj = new JSONObject( get( contact_id, DTab.kv));
            kvObj.put( kvEnum.toString(), value);

            put(contact_id, DTab.kv, kvObj.toString());

        } catch (JSONException e) {
            error = true;
            e.printStackTrace();
        }
        return error;
    }
    public static synchronized void setupImport(){

        mCloudRemapContactId = new HashMap<Long,Long>();

    }
    /**<pre>
     * contact_id = testIdTestVersion( account, cloud_c_id, cloud_version)
     * Return  0: cloud_c_id is current version, no update required
     * Return >0: cloud_c_id does not exists or is newer, update required
     * Return Long.MIN_VALUE, cloud_c_id is older than local version, no update required
     *
     * Assumes setupImport() is called prior to calling this method.
     *
     * 1. Search cloud_c_id in field cloud_c_id, get contact_id, loc_version, loc_account
     *
     * 2. count == 0
     * a. The loc_contact_id does not exist
     * a. loc_contact_id is clear
     * a. No contact_id collision, no remapping required
     * a. import this contact
     * a. return cloud_c_id (== cloud_id)
     *
     * 3. count > 0,
     * a. && no account match
     * a. cloud_c_id for this account is not in local database
     * a. there is a contact_id collision, remapping is required
     * a. import this contact
     * a. return next open new_contact_id
     *
     * 4. count > 0
     * b. && account match
     * b. cloud_c_id for this account is in local database
     * b. We don't know if contact_id is remapped
     *
     * b.1. cloud_version > loc_version
     * b.1. cloud version is newer
     * b.1. import this contact
     * b.1. if remapped, remapped value is in contact_id
     * b.1. return contact_id
     *
     * b.2. cloud_version = local_version
     * b.2. cloud version is the same
     * b.2. do not import this contact
     * b.2. return 0 (it exists, same version)
     *
     * b.3. cloud_version < local_version
     * b.3. The local version is newer than the cloud version
     * b.3. do not import this contact
     * b.3. return Long.MIN_VALUE
     *
     * Test data:
     * account   cloud_c_id  contact_id  loc_ver  _id
     * jack      1           1           1        1
     * grandma   1           1001        1        2
     * jack      2           2           1        3
     * jack      3           3           2        4
     * grandma   3           1002        2        4
     *
     * Test cases:
     * account   cloud_c_id  cloud_ver  result
     * jack      1           1          _id 1, b.2, return 0
     * jack      2           2          _id 3, b.1, return 2
     * grandma   1           1          _id 2, b.2, return 0
     * grandma   1           2          _id 2, b.1, return 1001
     * grandma   3           1          _id 4, b.3, return Long.MIN_VALUE
     * grandma   2           1          _id x, a., return 1003 (next open id)
     *</pre>
     */
    public static synchronized long testIdTestVersion(String cloud_account, long cloud_c_id, int cloud_version) {

        String [] projection = {
                ATab.account_name.toString(),
                ATab.contact_id.toString(),
                ATab.cloud_version.toString() };

        String where = ATab.cloud_c_id+"=?";
        String[] args = new String[]{String.valueOf( cloud_c_id )};

        Cursor c = account_db.query(ACCOUNT_TABLE, projection, where, args, null, null, null);

        int count = c.getCount();
        if(DEBUG_IMPORT) LogUtil.log("testIdTestVersion count: "+count+", cloud_account: "+cloud_account+", cloud_c_id: "+cloud_c_id+", cloud_version: "+ cloud_version);

        if( count == 0){
            c.close();
            if(DEBUG_IMPORT) LogUtil.log("case 2, cloud_c_id: "+cloud_c_id);
            return cloud_c_id;  //case 2
        }

        boolean accountMatch = false;
        int loc_version = 0;
        long contact_id=0;
        String account = "";

        while( c.moveToNext()){

            account = c.getString( c.getColumnIndex(ATab.account_name.toString()));

            if( account.contentEquals(cloud_account)){
                accountMatch = true;
                loc_version = c.getInt( c.getColumnIndex(ATab.cloud_version.toString()));
                contact_id = c.getLong( c.getColumnIndex(ATab.contact_id.toString()));
                break;
            }
        }
        c.close();

        if( ! accountMatch){ //case 3

            long remapId = getNextUnusedContactID();
            mCloudRemapContactId.put( cloud_c_id, remapId);
            if(DEBUG_IMPORT) LogUtil.log("case 3, return remapId: "+remapId);
            return remapId;

        }
        else{ // Case 4

            if( cloud_version > loc_version){//case 4.b.1

                if(DEBUG_IMPORT) LogUtil.log("case 4, > loc_version: "+loc_version+", return contact_id: "+contact_id);
                return contact_id;
            }
            else{
                if( cloud_version == loc_version){//case 4.b.2

                    if(DEBUG_IMPORT) LogUtil.log("case 4, = loc_version: "+loc_version+", return 0");
                    return 0L;
                }
                else{// case 4.b.3

                    if(DEBUG_IMPORT) LogUtil.log("case 4, < loc_version: "+loc_version+", return "+Long.MIN_VALUE);
                    return Long.MIN_VALUE;
                }
            }
        }
    }

    public static HashMap<Long, Long> mCloudRemapContactId;

    public static synchronized long remapContactIdTest( long candidate_id){

        Long remapId = mCloudRemapContactId.get(candidate_id);

        if( remapId == null)
            return candidate_id;  // ID not remapped
        else
            return remapId;       // ID is remapped
    }

    /**
     * Return the version number of a contact or zero if the contact does not exist.
     * This is the version when the contact was last imported or updated form the cloud.
     * Throws an exception if there are more than one contacts with the same ID.
     * @param contact_id
     * @return
     */
//    public static synchronized int getImportedContactVersion(long contact_id) {
//
//        if( contact_id == 0){
//
//            if(DEBUG) LogUtil.log("contact_id 0");
//        }
//
//        String [] projection = { ATab.cloud_version.toString() };
//
//        String where = ATab.contact_id+"=?";
//        String[] args = new String[]{String.valueOf( contact_id )};
//
//        Cursor c = account_db.query(ACCOUNT_TABLE, projection, where, args, null, null, null);
//
//        int count = c.getCount();
//
//        if( count == 0){
//            c.close();
//            return 0;  // non-existing contact, no version yet
//        }
//
//        if( count != 1){
//            c.close();
//            throw new RuntimeException("Can have exactly zero or one ID specific contact record");
//        }
//
//        c.moveToFirst();
//        int version = c.getInt(0);// only item in the projection is version at zero
//        c.close();
//
//        return version;
//    }

    /**
     * Update an account record.  Return true if successful.
     * @param contact_id
     * @param version
     * @param account_name
     * @param account_type
     * @param display_name
     * @param display_name_source
     * @return boolean success
     */
    public static synchronized boolean updateAccountRecord(
            String account_name, String account_type,
            long cloud_c_id, int version,
            long contact_id,
            String starred,
            String last_name, String display_name, int display_name_source) {

        if( contact_id == 0){

            if(DEBUG) LogUtil.log("contact_id 0");
            throw new RuntimeException("contact_id cannot be zero");
        }

        // Find the single record to be updated
        String selection = ATab.contact_id+"=?";
        String[] selectionArgs = new String[]{String.valueOf( contact_id )};

        ContentValues cv = new ContentValues();
        cv.put(ATab.contact_id.toString(),          contact_id);
        cv.put(ATab.cloud_version.toString(),       version);
        cv.put(ATab.version.toString(),             version);
        cv.put(ATab.cloud_c_id.toString(),          cloud_c_id);
        cv.put(ATab.starred.toString(),             starred);
        cv.put(ATab.account_name.toString(),        account_name);
        cv.put(ATab.account_type.toString(),        account_type);
        cv.put(ATab.last_name.toString(),           last_name);
        cv.put(ATab.display_name.toString(),        display_name);
        cv.put(ATab.display_name_source.toString(), display_name_source);

        account_db.beginTransaction();
        int rows = account_db.update(ACCOUNT_TABLE, cv, selection, selectionArgs);
        if( rows > 0)
            account_db.setTransactionSuccessful();
        else{
            if( rows == 0){
                long row = account_db.insert( ACCOUNT_TABLE, null, cv);
                if( row > 0){

                    account_db.setTransactionSuccessful();
                    rows = 1;
                }
            }else{

                if(DEBUG) LogUtil.log("updateContactRecord exception rows: "+rows+", contact+id"+contact_id);
                throw new RuntimeException("updateAccountRecord failed");
            }
        }
        account_db.endTransaction();

        return rows > 0;
    }

    /**
     * Update a detail record and return success status
     * @param contact_id
     * @param cv
     * @return
     */
    public static synchronized boolean updateDetailRecord(long contact_id, ContentValues cv) {

        if( contact_id == 0){

            if(DEBUG) LogUtil.log("contact_id 0");
        }

        // Find the single record to be updated
        String selection = DTab.contact_id+"=?";
        String[] selectionArgs = new String[]{String.valueOf( contact_id )};

        detail_db.beginTransaction();
        int rows = detail_db.update(DETAIL_TABLE, cv, selection, selectionArgs);
        if( rows > 0)
            detail_db.setTransactionSuccessful();
        else{
            if( rows == 0){
                long row = detail_db.insert(DETAIL_TABLE, null, cv);
                if( row > 0) {
                    detail_db.setTransactionSuccessful();
                    rows = 1;
                }
            }else{
                if(DEBUG) LogUtil.log("updateContactRecord exception rows: "+rows+", contact+id"+contact_id);
                throw new RuntimeException("updateDetailRecord failed");
            }
        }
        detail_db.endTransaction();

        return rows > 0;
    }

    /**
     * Update entire account table first and last name using the DTab Kv data
     * @return number of updates or -1 on error
     */
    public static synchronized int updateATabFirstLastName(){

        Cursor c = getATabCursor();

        long[] contactIds = new long[ c.getCount()];

        int contactCount =0;
        while( c.moveToNext()){

            contactIds[ contactCount++ ] = c.getLong( 0);
        }
        c.close();

        account_db.beginTransaction();
        boolean success = true;

        for( long id : contactIds){

            try {
                String kvString = get( id, DTab.kv);
                JSONObject kvJobj = new JSONObject( kvString);

                String last_name = kvJobj.getString(KvTab.name_last.toString());

                String where = ATab.contact_id+"=?";
                String[] args = new String[]{String.valueOf( id )};

                ContentValues cv = new ContentValues();
                cv.put(ATab.last_name.toString(), last_name);

                long rows = account_db.update(ACCOUNT_TABLE, cv, where, args);
                if( rows != 1){
                    success = false;
                    break;
                }

            } catch (JSONException e) {
                LogUtil.logException(SqlCipher.class, e);
                success = false;
                break;
            }
        }
        if( success )
            account_db.setTransactionSuccessful();

        account_db.endTransaction();

        return success? contactCount : -1;
    }

    /**
     * Update single contact first and last name using the DTab Kv data
     * @return number of updates or -1 on error
     */
    public static synchronized int updateATabFirstLastName(long contact_id){

        account_db.beginTransaction();
        boolean success = true;

            try {
                String kvString = get( contact_id, DTab.kv);
                JSONObject kvJobj = new JSONObject( kvString);

                String last_name = kvJobj.getString(KvTab.name_last.toString());

                String where = ATab.contact_id+"=?";
                String[] args = new String[]{String.valueOf( contact_id )};

                ContentValues cv = new ContentValues();
                cv.put(ATab.last_name.toString(), last_name);

                long rows = account_db.update(ACCOUNT_TABLE, cv, where, args);
                if( rows != 1){
                    success = false;
                }

            } catch (JSONException e) {
                e.printStackTrace();
                return -1;
            }

        if( success )
            account_db.setTransactionSuccessful();
        account_db.endTransaction();

        return success? 1 : -1;
    }

    /**
     * Replace select password data including the history and password gen parameters
     * Note that this will destroy the existing data.
     * @param accountCryp Array of objects containing row data
     * @return ArrayList of source _id processed
     */
    public static ArrayList<Integer> replaceCrypData( JSONArray accountCryp){

        ArrayList<Integer> source_id_processed = new ArrayList<Integer>();
        try {

            for(int i = 0; i < accountCryp.length(); i++) {

                JSONObject cryp = accountCryp.getJSONObject(i);
                int _id = cryp.getInt(ACTab._id.toString());
                String key = decodeBase64(cryp, ACTab.key.toString());
                String value = decodeBase64(cryp, ACTab.value.toString());

                if( key.contentEquals(Passphrase.PASSWORD_GEN_HISTORY))
                    Cryp.put(Passphrase.PASSWORD_GEN_HISTORY, value);
                if( key.contentEquals(Passphrase.PASSWORD_TARGET))
                    Cryp.put(Passphrase.PASSWORD_TARGET, value);
                if( key.contentEquals(Passphrase.PASSWORD_LENGTH))
                    Cryp.put(Passphrase.PASSWORD_LENGTH, value);
                if( key.contentEquals(Passphrase.PASSWORD_GEN_MODE))
                    Cryp.put(Passphrase.PASSWORD_GEN_MODE, value);
                source_id_processed.add(_id);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return source_id_processed;
    }

    /**
     * Return an array of all contact id in an account.
     * Return all accounts when account == null;
     * @param account
     * @return long[] list of contact id
     */
    static long[] getContactIds(String account) {

        String[] projection = {ATab.contact_id.toString()};
        String where = null;
        String[] args = null;

        if( account != null){

            where = ATab.account_name+"=?";
            args = new String[]{ account };
        }

        Cursor c = account_db.query( ACCOUNT_TABLE, projection, where, args, null, null, null);

        long[] contactIds = new long[ c.getCount()];

        int i=0;
        while( c.moveToNext()){

            contactIds[ i++ ] = c.getLong( 0);
        }
        c.close();
        return contactIds;
    }

    /** Return a HashSet of all contact IDs in SecureSuite */
    static HashSet<Long> getContactIds() {

        String[] projection = {ATab.contact_id.toString()};
        Cursor c = account_db.query( ACCOUNT_TABLE, projection, null, null, null, null, null);

        HashSet<Long> contactIds = new HashSet<Long>( c.getCount());

        while( c.moveToNext()){

            contactIds.add( c.getLong( 0));
        }
        c.close();
        return contactIds;
    }

    /**
     * Return first contact id in an account.
     * Return 0 if no valid contact ID is found.
     * @param account
     * @return long contact id
     */
    static long getFirstContactID(String account) {

        long contactId = 0;
        String[] projection = {ATab.contact_id.toString()};
        String where = null;
        String[] args = null;

        if( account != null && ! account.isEmpty()){

            where = ATab.account_name+"=?";
            args = new String[]{ account };
        }
        else
            return 0;

        Cursor c = account_db.query( ACCOUNT_TABLE, projection, where, args, null, null, null);

        if( c.moveToNext()){

            contactId = c.getLong( 0 );
        }

        c.close();
        return contactId;
    }
    /**
     * Return a cursor for all contact records including all accounts.
     * Projection is just contact_id.
     * @return Cursor
     */
    public static synchronized Cursor getATabCursor() {

        String[] projection = {ATab.contact_id.toString()};

        Cursor c = account_db.query( ACCOUNT_TABLE, projection, null, null, null, null, null);

        return c;
    }

    /**
     * Search across entire detail database with fields display_name, phone, email, website, address and kv.
     * Note that this searches across accounts. It searches across the detail database which does not
     * have a field for account.
     * //FUTURE limit this search to contacts on a single account
     * @param search
     * @return
     */
    public static synchronized Cursor getSearchDTabCursor(String search) {

        String[] projection = {
                DTab._id.toString(),
                DTab.contact_id.toString(),
                DTab.display_name.toString(),
                DTab.phone.toString(),
                DTab.email.toString(),
                DTab.website.toString(),
                DTab.kv.toString(),
                DTab.address.toString(),
        };

        String where = ""
            +"("+DTab.display_name+" LIKE ?) OR "
            +"("+DTab.phone+" LIKE ?) OR "
            +"("+DTab.email+" LIKE ?) OR "
            +"("+DTab.website+" LIKE ?) OR "
            +"("+DTab.kv+" LIKE ?) OR "
            +"("+DTab.address+" LIKE ?)";
        String[] args = new String[]{
                "%" + search + "%",
                "%" + search + "%",
                "%" + search + "%",
                "%" + search + "%",
                "%" + search + "%",
                "%" + search + "%",
                "%" + search + "%",
                "%" + search + "%",
                "%" + search + "%",
                "%" + search + "%",
        };

        String orderBy =
                "CASE "
                +"WHEN "+DTab.display_name+" LIKE ? THEN 1 "
                +"WHEN "+DTab.phone       +" LIKE ? THEN 2 "
                +"WHEN "+DTab.email       +" LIKE ? THEN 3 "
                +"WHEN "+DTab.website     +" LIKE ? THEN 3 "
                +"ELSE 4 "
        +"END LIMIT 50";

        Cursor c = detail_db.query(DETAIL_TABLE, projection, where, args, null, null, orderBy);

        LogUtil.log("getSearchDTabCursor size: " + c.getCount());

        return c;
    }

    /**
     * Check if there is exactly one contact for a specific ID and return true.
     * Return false if there are zero contacts, otherwise throw an exception.
     * @param contact_id
     * @return 1 contact is true, 0 is false, else throw an exception
     */
    public static synchronized boolean validContactId(long contact_id) {

        String where = ATab.contact_id.toString()+"='"+contact_id+"'";
        String[] projection = {ATab._id.toString()};

        Cursor c = account_db.query(ACCOUNT_TABLE, projection, where, null, null, null, null);

        int count = c.getCount();
        c.close();
        if( count > 1)
            throw new RuntimeException("validContactId, count not 0 or 1");

        return count == 1;
    }

    /**
     * Save a key/value pair to the database.  Both are string.
     * A int value with the number of rows updated is returned with success == 1.
     * @param key
     * @param value
     * @return
     */
    public static synchronized int putCryp(String key, String value){

        String where = ACTab.key+"=?";
        String[] args = new String[]{ key };

        ContentValues cv = new ContentValues();
        cv.put( ACTab.key.toString(), key);
        cv.put( ACTab.value.toString(), value);

        if( account_db.inTransaction()){

            account_db.endTransaction();
        }

        account_db.beginTransaction();
        int rows = account_db.update( ACCOUNT_CRYP_TABLE, cv, where, args);
        if( rows == 1)
            account_db.setTransactionSuccessful();
        else{
            if( rows == 0){
                long row = account_db.insert( ACCOUNT_CRYP_TABLE, null, cv);
                if( row > 0){

                    account_db.setTransactionSuccessful();
                    rows = 1;
                }
            }else{

                if(DEBUG) LogUtil.log("putCryp exception rows: "+rows+", key: "+key);
                throw new RuntimeException("putCryp failed");
            }
        }
        account_db.endTransaction();

        return rows;
    }
    /**
     * Return a string value indexed by a key.  If the key is not found, an empty
     * string is returned.
     * @param key
     * @return value or empty string if key is not found
     */
    public static synchronized String getCryp(String key){

        try {
            if( key == null || key.isEmpty())
                return "";

            String where = ACTab.key+"=?";
            String[] args = new String[]{ key };

            Cursor c = account_db.query(ACCOUNT_CRYP_TABLE, null, where, args, null, null, null);
            c.moveToFirst();

            String value = "";
            if( c.getCount() == 1){
                value = c.getString( 2 );
            }else
            if( c.getCount() > 1){
                c.close();
                throw new RuntimeException("get should only find zero or one record");
            }
            c.close();
            return value;

        } catch (Exception e) {
            LogUtil.logException(SqlCipher.class, e);
        }
        return "";
    }

    /**
     * Delete specific rows from the cryp database
     * @param key
     * @return
     */
    public static synchronized int clearCryp(String key){

        String where = ACTab.key+"=?";
        String[] args = new String[]{ key };
        int rows = account_db.delete(ACCOUNT_CRYP_TABLE, where, args);

        return rows;
    }

    /** A int value with the number of rows updated is returned with success == 1. */

    /**
     * Save synchronization data.  Type is the type of transaction defined by:
     * the ordinal of enum SqlIncSync.INC_SYNC_TYPE
     * {NIL,
     * INSERT_CONTACT, UPDATE_CONTACT, DELETE_CONTACT,
     * DELETE_GROUP,
     * PASSWORD_GEN_HISTORY, PASSWORD_TARGET, PASSWORD_LENGTH, PASSWORD_GEN_MODE
     * @param type
     * @param contact_id
     * @return
     */
    public static synchronized int putIncSync( int type, long contact_id){

        ContentValues cv = new ContentValues();

        cv.put(ISTab.type.toString(), type);
        cv.put(ISTab.contact_id.toString(), contact_id);
        int rows = 0;

        account_db.beginTransaction();
        long row = account_db.insert( INC_SYNC_TABLE, null, cv);
        if( row > 0){
            account_db.setTransactionSuccessful();
            rows = 1;
        }
        account_db.endTransaction();

        return rows;
    }

    /** Drop the table, never to be seen again */
    public static void dropIncSyncTable(){

        account_db.delete(INC_SYNC_TABLE, null, null);
    }

    /**
     * Load the manifest from the database with incremental changes.
     * @param syncIncManifest
     */
    public static synchronized void loadSyncManifest(SyncIncManifest syncIncManifest){

        syncIncManifest.init();
        Cursor c = account_db.query(INC_SYNC_TABLE, null, null, null, null, null, null);

        while( c.moveToNext()){

            int type = c.getInt( ISTab.type.ordinal());
            long id = c.getLong(ISTab.contact_id.ordinal());// or group ID

            SqlIncSync.INC_SYNC_TYPE sync_type = SqlIncSync.INC_SYNC_TYPE.values()[type];

            switch( sync_type){

                case NIL:
                    break;
                case INSERT_CONTACT:
                    syncIncManifest.contactInserts.add( id );
                    break;
                case UPDATE_CONTACT:
                    syncIncManifest.contactUpdates.add( id );
                    break;
                case DELETE_CONTACT:
                    syncIncManifest.contactDeletes.add( id );
                    break;
                case DELETE_GROUP:
                    syncIncManifest.groupDeletes.add((int) id);
                    break;
                case PASSWORD_GEN_HISTORY:
                case PASSWORD_TARGET:
                case PASSWORD_LENGTH:
                case PASSWORD_GEN_MODE:
                    syncIncManifest.crypSync.add( sync_type.ordinal());
                    break;
            }
        }
        c.close();
    }

    /**
     * Iterate through and test all the accounts, contacts and groups of contacts.
     * Tests are performed on the raw database tables and does not use any in-memory data.
     * 1. For each contact, confirm there is exactly one account and detail record
     * When the user is part of a group:
     * 2. Confirm group_id > 0, not -2, -1 or 0
     * 3. Confirm user is in the group a single time, not zero or multiple times in the same group
     * 4. Confirm group has a single title record
     * 5. Confirm detail table key value items are valid JSON
     * //FUTURE Check for unused/rogue group records.
     * //FUTURE Check for unused/rogue group title records
     //FUTURE cleanup any rogue group data records: records not associated with any contacts
     //FUTURE cleanup any rogue group title records: records with no associated group data, not My Contacts, not Trash
     * @param ctx
     * @return
     */
    public static synchronized int validateFixDb(Context ctx, boolean fixErrors) {

        int successCount = 0;
        int failCount = 0;
        int fixCount = 0;
        int contactCount = 0;
        logError(ctx, LogType.SQLCIPHER, "validateDbCounts starting ");

        String [] accountProjection = {
                ATab.contact_id.toString(),   //0
                ATab.display_name.toString(), //1
                ATab.last_name.toString(),    //2
                ATab.account_name.toString(), //3
        };

        Cursor ac = account_db.query(ACCOUNT_TABLE, accountProjection, null, null, null, null, null);

        while( ac.moveToNext()){

            long contact_id = ac.getLong(0);
            String display_name = ac.getString(1);
            String contact_account = ac.getString(3);

            /**
             * Check account records for complete names
             */
            String last_name = ac.getString(2);

            if( last_name == null ){

                logError( ctx, LogType.SQLCIPHER,
                        ACCOUNT_TABLE+" ERROR last_name null or empty: "+display_name+", "+contact_id);
                ++failCount;
            }
            else
                ++successCount;

            String [] detailProjection = {
                    DTab.contact_id.toString(),
            };
            String where = DTab.contact_id+"=?";
            String[] args = new String[]{String.valueOf( contact_id )};

            // Cursor is from detail db, test to see if it matches accouts db
            Cursor dc = detail_db.query(DETAIL_TABLE, detailProjection, where, args, null, null, null);
            int dCount = dc.getCount();
            dc.close();

            if( dCount != 1){

                logError( ctx, LogType.SQLCIPHER,
                        ACCOUNT_TABLE+" ERROR count: "+dCount+" != 1 for contact_id: "+contact_id);
                ++failCount;
            }
            else
                ++successCount;

            /**
             * 5. Confirm that detail table key value items are valid JSON
             */
            boolean jsonTest = true;
            try {
                String kvString = get( contact_id, DTab.kv);
                JSONObject kvJobj = new JSONObject( kvString);
            } catch (JSONException e) {
                logError(ctx, LogType.SQLCIPHER, "Invalid JSON DTab.kv for ID: "+contact_id);
                jsonTest = false;
            } catch ( RuntimeException e){
                logError(ctx, LogType.SQLCIPHER, "RuntimeException test 5. for ID: "+contact_id);
                ++failCount;
            }
            if( jsonTest)
                ++successCount;
            else
                ++failCount;

            // See if the user is part of any groups
            int[] groups = MyGroups.getGroups( contact_id);

            for( int group_id : groups){

                // Test that group is > 0
                if( group_id <= 0){

                    logError( ctx, LogType.SQLCIPHER,
                            ACCOUNT_TABLE+" ERROR group_id: "+group_id+" <= 0 in group for contact_id: "+contact_id);
                    ++failCount;
                    if( fixErrors){

                        int rows = MyGroups.deleteGroupForce( ctx, group_id, false);// don't sync
                        if( rows > 0) {
                            logError(ctx, LogType.SQLCIPHER,
                                    ACCOUNT_TABLE + " DB Fix group_id: " + group_id + " <= 0, group deleted for contact_id: " + contact_id);
                            ++fixCount;
                        }
                    }
                    continue;
                }
                else
                    ++successCount;

                // Test to find contact in groups
                int foundContact = 0;

                long[] contacts_in_group = MyGroups.getContacts( group_id);

                for( long c_id : contacts_in_group){

                    if( c_id == contact_id){
                        ++foundContact;
                    }
                }

                if( foundContact == 0){

                    logError( ctx, LogType.SQLCIPHER,
                            DETAIL_TABLE+" ERROR group_id: "+group_id+" zero times in group for contact_id: "+contact_id);
                    ++failCount;
                }else
                if( foundContact > 1){

                    logError( ctx, LogType.SQLCIPHER,
                            DETAIL_TABLE+" ERROR group_id: "+group_id+" > 1 times in group for contact_id: "+contact_id);
                    ++failCount;
                }
                else
                    ++successCount;

                // Check that the group has exactly one group title record

                String title = MyGroups.getGroupTitle(group_id);

                if( title == null || title.isEmpty()){

                    logError( ctx, LogType.SQLCIPHER,
                            DETAIL_TABLE+" ERROR title missing group_id: "+group_id+" for contact_id: "+contact_id);
                    ++failCount;
                }
                ++successCount;

                // Check that the contact and group are on the same account
                String group_account = MyGroups.mGroupAccount.get(group_id);

                if( group_account!= null && group_account.contentEquals(contact_account))
                    ++successCount;
                else{

                    logError( ctx, LogType.SQLCIPHER,
                            DETAIL_TABLE+" ERROR contact and group do not have same account.  group account: "+group_account
                                    +" contact account: "+contact_account);
                    logError( ctx, LogType.SQLCIPHER,
                            DETAIL_TABLE+" group_id: "+group_id+" display_name: "+display_name+" for contact_id: "+contact_id);
                    ++failCount;
                }
            }
            if( ++contactCount % 200 == 0)
                logError(ctx, LogType.SQLCIPHER,"Progress: "+contactCount);
        }
        ac.close();

        logError(ctx, LogType.SQLCIPHER,
                "validateDbCounts complete, success: " + successCount + ", fail: " + failCount + ", fixed: " + fixCount);

        return failCount;
    }


    /**
     * Look for issues with group membership.
     * //FUTURE fix problems with groups during import
     * The problem has to do with remapping groups IDs during import.
     * Each group ID is associated with an account, ie., can only be used by contacts in that account.
     * Each contact ID is associated with an account.  There is an error when contacts are associated
     * with groups in a different account.  Validate groups detects the erroronous groups and can also delete them.
     */
    public static int validateGroups(Context ctx, boolean fix_errors){

        int successCount = 0;
        int failCount = 0;
        int contactCount = 0;
        int fixCount = 0;

        String[] accounts = MyAccounts.getAccounts();

        for( String contact_account : accounts ){

            long[] contacts = getContactIds(contact_account);

            for( long contact_id : contacts){

                int[] groups = MyGroups.getGroups(contact_id);

                for( int group_id : groups){

                    // Check that the contact and group are on the same account
                    String group_account = MyGroups.mGroupAccount.get(group_id);

                    if( group_account!= null && group_account.contentEquals(contact_account))
                        ++successCount;
                    else{

                        if( fix_errors){

                            MyGroups.deleteGroupRecords(ctx, contact_id, group_id, false);
                            ++fixCount;

                            String display_name = get(contact_id, ATab.display_name);
                            logError( ctx, LogType.SQLCIPHER,
                                    DETAIL_TABLE+" fixed by delete, group_id: "+group_id+" display_name: "+display_name+" for contact_id: "+contact_id);
                        }else{


                        String display_name = get(contact_id, ATab.display_name);
                        logError( ctx, LogType.SQLCIPHER,
                                DETAIL_TABLE+" ERROR contact and group do not have same account.  group account: "+group_account
                                        +" contact account: "+contact_account);
                        logError( ctx, LogType.SQLCIPHER,
                                DETAIL_TABLE+" group_id: "+group_id+" display_name: "+display_name+" for contact_id: "+contact_id);
                        ++failCount;
                        }
                    }
                }
                if( ++contactCount % 200 == 0)
                    logError(ctx, LogType.SQLCIPHER,"Progress: "+contactCount);
            }
        }
        logError(ctx, LogType.SQLCIPHER,
                "validateDbGroups complete, success: " + successCount + ", fail: " + failCount + ", fixed: " + fixCount);
        return failCount;
    }

    public static synchronized void logError(Context ctx, LogType lt, String error){

        LogUtil.log(lt, error);
//        Toast.makeText(ctx, error, Toast.LENGTH_SHORT).show();
    }

    public static synchronized String fullSync(String jsonObject) {

        String inventory = "";
        try {
            JSONObject wrapper = new JSONObject( jsonObject );
            JSONObject records = wrapper.getJSONObject(CConst.RECORDS);
            JSONArray group_title = wrapper.getJSONArray(CConst.GROUP_TITLE);
            JSONArray account_data = wrapper.getJSONArray(CConst.ACCOUNT_DATA);

            inventory = "{"
                    +"\nwrapper     : "+wrapper.length()
                    +"\nrecords     : "+records.length()
                    +"\ngroup_title : "+group_title.length()
                    +"\naccount_data: "+account_data.length()
            ;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return inventory;
    }
    /**
     * */

    /** <pre>
     * Prepare a manifest of source data to be sent to the target machine
     * Example:
     {
     "detail_table": 1764,
     "account_table": 1764,
     "account_data_table": 3942,
     "group_title_table": 11,
     "account_cryp_table": 8
     }
     * @return JSONObject with manifest, keys match database table names
     * </pre>
     */
    public static JSONObject getDbCounts() {

        JSONObject counts = new JSONObject();
        try {

            String [] projection = { DTab._id.toString() };
            Cursor c = detail_db.query(DETAIL_TABLE, projection, null, null, null, null, null);
            counts.put(DETAIL_TABLE, c.getCount());
            c.close();

            c = account_db.query(ACCOUNT_TABLE, projection, null, null, null, null, null);
            counts.put(ACCOUNT_TABLE, c.getCount());
            c.close();

            c = account_db.query(ACCOUNT_DATA_TABLE, projection, null, null, null, null, null);
            counts.put(ACCOUNT_DATA_TABLE, c.getCount());
            c.close();

            c = account_db.query(GROUP_TITLE_TABLE, projection, null, null, null, null, null);
            counts.put(GROUP_TITLE_TABLE, c.getCount());
            c.close();

            c = account_db.query(ACCOUNT_CRYP_TABLE, projection, null, null, null, null, null);
            counts.put(ACCOUNT_CRYP_TABLE, c.getCount());
            c.close();

            c = account_db.query(INC_SYNC_TABLE, projection, null, null, null, null, null);
            counts.put(INC_SYNC_TABLE, c.getCount());
            c.close();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return counts;
    }

    /**
     * Return a JSONObject with the complete manifest of the database.  The manifest includes
     * JSONArrays of _id, named for each table, and a JSONObject with key "count" with a summary.
     * @return
     */
    public static JSONObject getSourceManifest(){

        JSONObject wrapper = new JSONObject();
        JSONObject counts = new JSONObject();
        int total = 0;
        try {

            JSONArray jsonArray = getManifestObject(detail_db, DETAIL_TABLE);
            counts.put(DETAIL_TABLE, jsonArray.length());
            wrapper.put(DETAIL_TABLE, jsonArray);
            total = jsonArray.length();

            jsonArray = getManifestObject(account_db, ACCOUNT_TABLE);
            counts.put(ACCOUNT_TABLE, jsonArray.length());
            wrapper.put(ACCOUNT_TABLE, jsonArray);
            total += jsonArray.length();

            jsonArray = getManifestObject(account_db, ACCOUNT_DATA_TABLE);
            counts.put(ACCOUNT_DATA_TABLE, jsonArray.length());
            wrapper.put(ACCOUNT_DATA_TABLE, jsonArray);
            total += jsonArray.length();

            jsonArray = getManifestObject(account_db, GROUP_TITLE_TABLE);
            counts.put(GROUP_TITLE_TABLE, jsonArray.length());
            wrapper.put(GROUP_TITLE_TABLE, jsonArray);
            total += jsonArray.length();

            jsonArray = getManifestObject(account_db, ACCOUNT_CRYP_TABLE);
            counts.put(ACCOUNT_CRYP_TABLE, jsonArray.length());
            wrapper.put(ACCOUNT_CRYP_TABLE, jsonArray);
            total += jsonArray.length();

            counts.put(CConst.TOTAL, total);
            wrapper.put(CConst.COUNTS, counts);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return wrapper;
    }

    /**
     * Build a JSONArray including the _id of each record in a table.
     * @param db
     * @param table
     * @return
     */
    public static JSONArray getManifestObject(SQLiteDatabase db, String table){

        JSONArray jsonArray = new JSONArray();
        String [] projection = { DTab._id.toString() };
        Cursor c = db.query(table, projection, null, null, null, null, null);

        while (c.moveToNext()){

            jsonArray.put(String.valueOf(c.getInt(0)));
        }
        c.close();

        return jsonArray;
    }

    /**
     * Drop all data tables.  Yes, erase everything in the database
     */
    public static void dropTables() {

        detail_db.delete(DETAIL_TABLE, null, null);
        account_db.delete(ACCOUNT_TABLE, null, null);
        account_db.delete(ACCOUNT_DATA_TABLE, null, null);
        account_db.delete(GROUP_TITLE_TABLE, null, null);
        account_db.delete(INC_SYNC_TABLE, null, null);
        // Don't drop, later only replace password elements
//        account_db.delete(ACCOUNT_CRYP_TABLE, null, null);
    }

    /**
     * Return a JSONObject with requirements specified by the request_manifest.
     * @param request_manifest
     * @param maxPayloadSize
     * @return
     */
    public static JSONObject getNextFullSyncIncement(
            SqlFullSyncTarget.SourceManifest request_manifest, int maxPayloadSize) {

        JSONObject payload = new JSONObject();
        int payloadSize = 0;
        String where = ATab._id+"=?";
        try {

            if( request_manifest.account_table.size() > 0){

                JSONArray array = new JSONArray();

                for( int _id : request_manifest.account_table){

                    String[] args = new String[]{String.valueOf( _id )};

                    Cursor c = account_db.query(ACCOUNT_TABLE, null, where, args, null, null, null);

                    if( c.getCount() != 1){
                        c.close();
                        break;
                    }
                    else
                        c.moveToFirst();

                    JSONObject obj = new JSONObject();
                    obj.put(ATab._id.toString(),           c.getInt(ATab._id.ordinal()));
                    obj.put(ATab.contact_id.toString(),    c.getLong(ATab.contact_id.ordinal()));
                    obj.put(ATab.display_name.toString(),  encodeBase64(c, ATab.display_name.ordinal()));
                    obj.put(ATab.display_name_source.toString(), encodeBase64(c, ATab.display_name_source.ordinal()));
                    obj.put(ATab.last_name.toString(),     encodeBase64(c, ATab.last_name.ordinal()));
                    obj.put(ATab.starred.toString(),       encodeBase64(c, ATab.starred.ordinal()));
                    obj.put(ATab.account_name.toString(),  encodeBase64(c, ATab.account_name.ordinal()));
                    obj.put(ATab.account_type.toString(),  encodeBase64(c, ATab.account_type.ordinal()));
                    obj.put(ATab.cloud_c_id.toString(),    c.getLong(ATab.cloud_c_id.ordinal()));
                    obj.put(ATab.version.toString(),       c.getInt(ATab.version.ordinal()));
                    obj.put(ATab.cloud_version.toString(), c.getInt(ATab.cloud_version.ordinal()));
                    c.close();
                    array.put( obj);
                    if( (payloadSize += obj.toString().length()) > maxPayloadSize){

                        payload.put(ACCOUNT_TABLE, array);
                        return payload;
                    }
                }
                payload.put(ACCOUNT_TABLE, array);
            }

            if( request_manifest.account_data_table.size() > 0){

                JSONArray array = new JSONArray();

                for( int _id : request_manifest.account_data_table){

                    String[] args = new String[]{String.valueOf( _id )};

                    Cursor c = account_db.query(ACCOUNT_DATA_TABLE, null, where, args, null, null, null);

                    if( c.getCount() != 1){
                        c.close();
                        break;
                    }
                    else
                        c.moveToFirst();

                    JSONObject obj = new JSONObject();
                    obj.put(ADTab._id.toString(),       c.getInt(ADTab._id.ordinal()));
                    obj.put(ADTab.group_id.toString(),  c.getInt(ADTab.group_id.ordinal()));
                    obj.put(ADTab.contact_id.toString(),c.getLong(ADTab.contact_id.ordinal()));
                    c.close();
                    array.put( obj);
                    if( (payloadSize += obj.toString().length()) > maxPayloadSize){

                        payload.put(ACCOUNT_DATA_TABLE, array);
                        break;
                    }
                }
                payload.put(ACCOUNT_DATA_TABLE, array);
            }

            if( request_manifest.detail_table.size() > 0){

                JSONArray array = new JSONArray();

                for( int _id : request_manifest.detail_table){

                    String[] args = new String[]{String.valueOf( _id )};

                    Cursor c = detail_db.query(DETAIL_TABLE, null, where, args, null, null, null);

                    if( c.getCount() == 0){
                        c.close();
                        throw new RuntimeException("getNextSyncIncrement, _id in source_manifest not found");
                    }
                    else
                        c.moveToFirst();

                    JSONObject obj = new JSONObject();
                    obj.put(DTab._id.toString(), _id);
                    obj.put(DTab.contact_id.toString(), c.getLong(DTab.contact_id.ordinal()));
                    obj.put(DTab.display_name.toString(), encodeBase64(c, DTab.display_name.ordinal()));
                    obj.put(DTab.comms_select.toString(), encodeBase64(c, DTab.comms_select.ordinal()));
                    obj.put(DTab.kv.toString(), encodeBase64(c, DTab.kv.ordinal()));
                    obj.put(DTab.photo.toString(), encodeBase64(c, DTab.photo.ordinal()));
                    obj.put(DTab.email.toString(), encodeBase64(c, DTab.email.ordinal()));
                    obj.put(DTab.phone.toString(), encodeBase64(c, DTab.phone.ordinal()));
                    obj.put(DTab.address.toString(), encodeBase64(c, DTab.address.ordinal()));
                    obj.put(DTab.website.toString(), encodeBase64(c, DTab.website.ordinal()));
                    obj.put(DTab.im.toString(), encodeBase64(c, DTab.im.ordinal()));
                    obj.put(DTab.date.toString(), encodeBase64(c, DTab.date.ordinal()));
                    obj.put(DTab.relation.toString(), encodeBase64(c, DTab.relation.ordinal()));
                    obj.put(DTab.internetcall.toString(), encodeBase64(c, DTab.internetcall.ordinal()));
                    c.close();
                    array.put( obj);
                    if( (payloadSize += obj.toString().length()) > maxPayloadSize){

                        payload.put(DETAIL_TABLE, array);
                        return payload;
                    }
                }
                payload.put(DETAIL_TABLE, array);
            }

            if( request_manifest.group_title_table.size() > 0){

                JSONArray array = new JSONArray();

                for( int _id : request_manifest.group_title_table){

                    String[] args = new String[]{String.valueOf( _id )};

                    Cursor c = account_db.query(GROUP_TITLE_TABLE, null, where, args, null, null, null);

                    if( c.getCount() != 1){
                        c.close();
                        break;
                    }
                    else
                        c.moveToFirst();

                    JSONObject obj = new JSONObject();
                    obj.put(CConst._ID,          c.getInt(       GTTab._id.ordinal()));
                    obj.put(CConst.GROUP_ID,     c.getInt(       GTTab.group_id.ordinal()));
                    obj.put(CConst.TITLE,        encodeBase64(c, GTTab.title.ordinal()));
                    obj.put(CConst.ACCOUNT_NAME, encodeBase64(c, GTTab.account_name.ordinal()));
                    obj.put(CConst.ACCOUNT_TYPE, encodeBase64(c, GTTab.account_type.ordinal()));
                    c.close();
                    array.put( obj);
                    if( (payloadSize += obj.toString().length()) > maxPayloadSize){

                        payload.put(GROUP_TITLE_TABLE, array);
                        return payload;
                    }
                }
                payload.put(GROUP_TITLE_TABLE, array);
            }
            if( request_manifest.account_cryp_table.size() > 0){

                JSONArray array = new JSONArray();

                for( int _id : request_manifest.account_cryp_table){

                    String[] args = new String[]{String.valueOf( _id )};

                    Cursor c = account_db.query(ACCOUNT_CRYP_TABLE, null, where, args, null, null, null);

                    if( c.getCount() != 1){
                        c.close();
                        break;
                    }
                    else
                        c.moveToFirst();

                    JSONObject obj = new JSONObject();
                    obj.put(ACTab._id.toString(),       c.getInt(  ACTab._id.ordinal()));
                    obj.put(ACTab.key.toString(),  encodeBase64(c, ACTab.key.ordinal()));
                    obj.put(ACTab.value.toString(),encodeBase64(c, ACTab.value.ordinal()));
                    c.close();
                    array.put( obj);
                    if( (payloadSize += obj.toString().length()) > maxPayloadSize){

                        payload.put(ACCOUNT_CRYP_TABLE, array);
                        return payload;
                    }
                }
                payload.put(ACCOUNT_CRYP_TABLE, array);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return payload;
    }

    /**
     * Process the next increment of data while updating the source manifest.
     * Each row that is processed is removed from the manifest.
     * @param syncIncrementDataObj: data from source server
     * @param source_manifest: manifest of what the target machine requires
     * @return updated manifest of remaining requirements
     */
    public static SqlFullSyncTarget.SourceManifest putNextSyncIncrement(
            JSONObject syncIncrementDataObj,
            SqlFullSyncTarget.SourceManifest source_manifest) {

        try {

            if( syncIncrementDataObj.has( DETAIL_TABLE)){

                JSONArray table = syncIncrementDataObj.getJSONArray( DETAIL_TABLE);
                detail_db.beginTransaction();

                for( int i = 0; i < table.length(); i++){

                    ContentValues cv = new ContentValues();
                    JSONObject rowObj = table.getJSONObject(i);
                    int _id = rowObj.getInt(                                  DTab._id.toString());
                    cv.put(DTab._id.toString(), _id);
                    cv.put(DTab.contact_id.toString(),         rowObj.getLong(DTab.contact_id.toString()));
                    cv.put(DTab.display_name.toString(), decodeBase64(rowObj, DTab.display_name.toString()));
                    cv.put(DTab.comms_select.toString(), decodeBase64(rowObj, DTab.comms_select.toString()));
                    cv.put(DTab.kv.toString(),           decodeBase64(rowObj, DTab.kv.toString()));
                    cv.put(DTab.photo.toString(),        decodeBase64(rowObj, DTab.photo.toString()));
                    cv.put(DTab.email.toString(),        decodeBase64(rowObj, DTab.email.toString()));
                    cv.put(DTab.phone.toString(),        decodeBase64(rowObj, DTab.phone.toString()));
                    cv.put(DTab.address.toString(),      decodeBase64(rowObj, DTab.address.toString()));
                    cv.put(DTab.website.toString(),      decodeBase64(rowObj, DTab.website.toString()));
                    cv.put(DTab.im.toString(),           decodeBase64(rowObj, DTab.im.toString()));
                    cv.put(DTab.date.toString(),         decodeBase64(rowObj, DTab.date.toString()));
                    cv.put(DTab.relation.toString(),     decodeBase64(rowObj, DTab.relation.toString()));
                    cv.put(DTab.internetcall.toString(), decodeBase64(rowObj, DTab.internetcall.toString()));

                    long row = detail_db.insertWithOnConflict(DETAIL_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                    if( row <= 0) {
                        detail_db.endTransaction();
                        throw new RuntimeException("pubNextSyncIncrement, database insertion error");
                    }
                    if( ! source_manifest.detail_table.remove(_id))
                        throw new RuntimeException("pubNextSyncIncrement, found _id not matching source_manifest");
                }
                detail_db.setTransactionSuccessful();
                detail_db.endTransaction();
            }
            if( syncIncrementDataObj.has( ACCOUNT_TABLE)){

                JSONArray table = syncIncrementDataObj.getJSONArray( ACCOUNT_TABLE);
                account_db.beginTransaction();

                for( int i = 0; i < table.length(); i++){

                    ContentValues cv = new ContentValues();
                    JSONObject rowObj = table.getJSONObject(i);
                    int _id = rowObj.getInt(                                          ATab._id.toString());
                    cv.put( ATab._id.toString(),                 _id);
                    cv.put( ATab.contact_id.toString(),          rowObj.getLong(ATab.contact_id.toString()));
                    cv.put( ATab.display_name.toString(),        decodeBase64(rowObj, ATab.display_name.toString()));
                    cv.put( ATab.display_name_source.toString(), decodeBase64(rowObj, ATab.display_name_source.toString()));
                    cv.put( ATab.last_name.toString(),           decodeBase64(rowObj, ATab.last_name.toString()));
                    cv.put( ATab.starred.toString(),             decodeBase64(rowObj, ATab.starred.toString()));
                    cv.put( ATab.account_name.toString(),        decodeBase64(rowObj, ATab.account_name.toString()));
                    cv.put( ATab.account_type.toString(),        decodeBase64(rowObj, ATab.account_type.toString()));
                    cv.put( ATab.cloud_c_id.toString(),          rowObj.getLong(      ATab.cloud_c_id.toString()));
                    cv.put( ATab.version.toString(),             rowObj.getInt(       ATab.version.toString()));
                    cv.put( ATab.cloud_version.toString(),       rowObj.getInt(       ATab.cloud_version.toString()));

                    long row = account_db.insertWithOnConflict(ACCOUNT_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                    if( row <= 0) {
                        account_db.endTransaction();
                        throw new RuntimeException("pubNextSyncIncrement, database insertion error");
                    }
                    if( ! source_manifest.account_table.remove(_id))
                        throw new RuntimeException("pubNextSyncIncrement, found _id not matching source_manifest");
                }
                account_db.setTransactionSuccessful();
                account_db.endTransaction();
            }

            if( syncIncrementDataObj.has( GROUP_TITLE_TABLE)){

                JSONArray table = syncIncrementDataObj.getJSONArray( GROUP_TITLE_TABLE);
                account_db.beginTransaction();

                for( int i = 0; i < table.length(); i++){

                    ContentValues cv = new ContentValues();
                    JSONObject rowObj = table.getJSONObject(i);

                    int _id = rowObj.getInt(                                     GTTab._id.toString());
                    cv.put(GTTab._id.toString(),           _id);
                    cv.put(GTTab.group_id.toString(),      rowObj.getInt(        GTTab.group_id.toString()));
                    cv.put(GTTab.title.toString(),         decodeBase64( rowObj, GTTab.title.toString()));
                    cv.put(GTTab.account_name.toString(),  decodeBase64( rowObj, GTTab.account_name.toString()));
                    cv.put(GTTab.account_type.toString(),  decodeBase64( rowObj, GTTab.account_type.toString()));

                    long row = account_db.insertWithOnConflict(GROUP_TITLE_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                    if( row <= 0) {
                        account_db.endTransaction();
                        throw new RuntimeException("pubNextSyncIncrement, database insertion error");
                    }
                    if( ! source_manifest.group_title_table.remove(_id))
                        throw new RuntimeException("pubNextSyncIncrement, found _id not matching source_manifest");
                }
                account_db.setTransactionSuccessful();
                account_db.endTransaction();
            }

            if( syncIncrementDataObj.has( ACCOUNT_DATA_TABLE)){

                JSONArray table = syncIncrementDataObj.getJSONArray( ACCOUNT_DATA_TABLE);
                account_db.beginTransaction();

                for( int i = 0; i < table.length(); i++){

                    ContentValues cv = new ContentValues();
                    JSONObject rowObj = table.getJSONObject(i);

                    int _id = rowObj.getInt(ADTab._id.toString());
                    cv.put(ADTab._id.toString(),        _id);
                    cv.put(ADTab.contact_id.toString(), rowObj.getLong(ADTab.contact_id.toString()));
                    cv.put(ADTab.group_id.toString(),   rowObj.getInt(  ADTab.group_id.toString()));

                    long row = account_db.insertWithOnConflict(ACCOUNT_DATA_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                    if( row <= 0) {
                        account_db.endTransaction();
                        throw new RuntimeException("pubNextSyncIncrement, database insertion error");
                    }
                    if( ! source_manifest.account_data_table.remove(_id))
                        throw new RuntimeException("pubNextSyncIncrement, found _id not matching source_manifest");
                }
                account_db.setTransactionSuccessful();
                account_db.endTransaction();
            }

            if( syncIncrementDataObj.has( ACCOUNT_CRYP_TABLE)){

                JSONArray table = syncIncrementDataObj.getJSONArray( ACCOUNT_CRYP_TABLE);
                ArrayList<Integer> source_id_processed = replaceCrypData(table);

                for( int _id : source_id_processed){

                    if( ! source_manifest.account_cryp_table.remove(_id))
                        throw new RuntimeException("pubNextSyncIncrement, found _id not matching source_manifest");
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return source_manifest;
    }

    /**
     * Given a manifest of requirements, assemble contacts to match with consideration of a
     * maxPayloadSize.  The manifest is not updated, this will be done by the target machine
     * and will be sent back if there are remaining updates to be made.
     * @param ctx
     * @param manifest
     * @param maxPayloadSize
     * @return JSONObject wrapper with optional JSONObjects "inserts" or "updates" with key: contact_id.
     */
    public static JSONObject incSyncGetIncrement(
            Context ctx, SyncIncManifest manifest, int maxPayloadSize) {

        /**
         * Iterate and build array lists for inserts and updates.
         * Don't do anything for deletes.
         */
        JSONObject wrapper = new JSONObject();
        JSONObject inserts = new JSONObject();
        JSONObject updates = new JSONObject();
        JSONObject cryp_sync = new JSONObject();
        boolean includeGroupData = true;
        int payloadSize = 0;
        try {

            for(long contact_id : manifest.contactInserts){

                JSONObject contact = getContact( contact_id, includeGroupData);
                payloadSize += contact.toString().length();

                if( payloadSize > maxPayloadSize)
                    break;
                inserts.put( String.valueOf( contact_id), contact );
            }
            if( inserts.length() > 0)
                wrapper.put(CConst.INSERTS, inserts );

            for(long contact_id : manifest.contactUpdates){

                JSONObject contact = getContact( contact_id, includeGroupData);

                /**
                 * Some photos can be large even though they are limited to a specific size.
                 */
                int contactSize = contact.toString().length();
                if( contactSize > maxPayloadSize){
                    String display_name = get(contact_id, ATab.display_name);
                    Toast.makeText(ctx, "Error, contact is too large to sync: "+display_name,
                            Toast.LENGTH_SHORT).show();
                    break;
                }

                payloadSize += contactSize;

                if( payloadSize > maxPayloadSize)
                    break;
                updates.put( String.valueOf( contact_id), contact );

            }
            if( updates.length() > 0)
                wrapper.put(CConst.UPDATES, updates );

            /**
             * Any password items that have changed will be on the list.
             * Append each to the JSON object.
             * These are tiny so don't bother with payload size.
             */
            for( int ord : manifest.crypSync){

                SqlIncSync.INC_SYNC_TYPE type = SqlIncSync.INC_SYNC_TYPE.values()[ ord ];

                switch ( type ){

                    case NIL:
                    case INSERT_CONTACT:
                    case UPDATE_CONTACT:
                    case DELETE_CONTACT:
                        break;
                    case PASSWORD_GEN_HISTORY:
                    case PASSWORD_TARGET:
                    case PASSWORD_LENGTH:
                    case PASSWORD_GEN_MODE:

                        cryp_sync.put( type.toString(), getCryp(type.toString()));
                        break;
                }

//                LogUtil.log("incSyncGetIncrement,  manifest: "+manifest.report());
//                LogUtil.log("incSyncGetIncrement, cryp_sync: "+cryp_sync.toString());
//
            }
            if( cryp_sync.length() > 0)
                wrapper.put(CConst.CRYP_SYNC, cryp_sync );

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return wrapper;
    }

    /** <pre>
     * Execute database transactions provided by the syncDataObj while updating the manifest.
     * Transaction types, each is a JSONObject inside the syncDataObj:
     * JSONObject: wrapper
     *   JSONObject inserts, each key: contact_id, value is contact data
     *   JSONObject updates, each key: contact_id, value is contact data
     *   JSONObject deletes, each key: contact_id, value is not used
     * @param ctx
     * @param syncDataObj
     * @param manifest
     * @return
     * </pre>
     */
    public static SyncIncManifest incSyncPutIncrement(Context ctx, JSONObject syncDataObj, SyncIncManifest manifest) {

        /**
         * Do not sync this transaction to the companion device, i.e, avoid infinitate loop
         */
        boolean syncTransactionIsFalse = false;
        try {
            /**
             * Execute deletes first, in case the user is short on space
             */
            if( manifest.contactDeletes.size() > 0){
                /**
                 * While we iterate on the set, remove items from the manifest as they are processed,
                 * hence make a copy first.
                 */
                HashSet<Long> delete_copy = new HashSet<Long>();
                delete_copy.addAll( manifest.contactDeletes);

                for( long contact_id : delete_copy){

                    if( MyContacts.contactExists( contact_id))
                        deleteContact(ctx, contact_id, syncTransactionIsFalse);
                    // Iterate on the copy and remove deletes from the manifest
                    boolean success = manifest.contactDeletes.remove(contact_id);
                    if( ! success)
                        throw new RuntimeException(
                                "incSyncPutIncrement, delete contact_id not found in manifest: "+contact_id);
                }
            }
            /**
             * Delete groups next.  It is possible that the user either created and deleted a group
             * within the sync period, or deleted and created a group.
             * New groups are not synced until associated with a contact.
             * Execute the delete first. Under some conditions there may be an undeleted group but
             * this is better than deleting groups last and deleting a group created in the same cycle.
             */
            if( manifest.groupDeletes.size() > 0){
                /**
                 * While we iterate on the set, remove items from the manifest as they are processed,
                 * hence make a copy first.
                 */
                HashSet<Integer> delete_copy = new HashSet<Integer>();
                delete_copy.addAll( manifest.groupDeletes);

                for( int group_id : delete_copy){

                    if( MyGroups.validGroupId( group_id))
                        MyGroups.deleteGroup(ctx, group_id, syncTransactionIsFalse);
                    // Iterate on the copy and remove deletes from the manifest
                    boolean success = manifest.groupDeletes.remove(group_id);
                    if( ! success)
                        throw new RuntimeException(
                                "incSyncPutIncrement, group_id not found in manifest: "+group_id);
                }
            }
            if( syncDataObj != null && manifest.contactInserts.size() > 0 ){

                JSONObject inserts = syncDataObj.getJSONObject(CConst.INSERTS);
                HashSet<Long> inserts_copy = new HashSet<Long>();
                inserts_copy.addAll( manifest.contactInserts);

                for( long contact_id : inserts_copy){

                    try {
                        String str_contact_id = String.valueOf(contact_id);
                        if( inserts.has( str_contact_id ))
                            createOrUpdateContact(inserts.getJSONObject( str_contact_id ));
                        else
                            continue;

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    boolean success = manifest.contactInserts.remove(contact_id);
                    if( ! success)
                        throw new RuntimeException(
                                "incSyncPutIncrement, insert contact_id not found in manifest: "+contact_id);
                }
            }
            if( syncDataObj != null && manifest.contactUpdates.size() > 0 ){

                JSONObject updates = syncDataObj.getJSONObject( CConst.UPDATES );
                HashSet<Long> updates_copy = new HashSet<Long>();
                updates_copy.addAll(manifest.contactUpdates);

                for( long contact_id : updates_copy){

                    try {
                        String str_contact_id = String.valueOf(contact_id);
                        if( updates.has( str_contact_id ))
                            createOrUpdateContact( updates.getJSONObject( str_contact_id));
                        else
                            continue;

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    boolean success = manifest.contactUpdates.remove(contact_id);
                    if( ! success)
                        throw new RuntimeException(
                                "incSyncPutIncrement, insert contact_id not found in manifest: "+contact_id);
                }
            }
            if (syncDataObj != null && manifest.crypSync.size() > 0) {

//                LogUtil.log("incSyncPutIncrement,    manifest: "+manifest.report());
//                LogUtil.log("incSyncPutIncrement, syncDataObj: "+syncDataObj.toString());

                JSONObject cryp_sync = syncDataObj.getJSONObject( CConst.CRYP_SYNC);
                HashSet<Integer> cryp_sync_copy = new HashSet<Integer>();
                cryp_sync_copy.addAll(manifest.crypSync);

                for( int ord : cryp_sync_copy){

                    String key = SqlIncSync.INC_SYNC_TYPE.values()[ ord ].toString();

                    String value = cryp_sync.getString(key);
                    putCryp(key, value);

                    boolean success = manifest.crypSync.remove( ord );
                    if( ! success)
                        throw new RuntimeException(
                                "incSyncPutIncrement, crypSync not found in manifest: "+ord);
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return manifest;
    }
}
