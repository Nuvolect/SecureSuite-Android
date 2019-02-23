package com.nuvolect.securesuite.data;

import android.content.ContentValues;
import android.content.Context;

import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.CrypUtil;
import com.nuvolect.securesuite.util.LogUtil;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.junit.Test;

import java.io.File;

import static com.nuvolect.securesuite.data.SqlCipher.ACCOUNT_CRYP_TABLE;
import static com.nuvolect.securesuite.main.App.getContext;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test a SQLCipher database by creating database, a table, adding records and deleting everything.
 */
public class CryptoDbTest {

    public static final String TEST_DB_NAME = "test_db";
    public static SQLiteDatabase test_db;

    @Test
    public void dbCreateTest() {


        Context ctx = getContext();

        SQLiteDatabase.loadLibs(ctx);
        File dbFile = ctx.getDatabasePath(TEST_DB_NAME);

        dbFile.delete();
        assertThat( dbFile.exists(), is( false ));

        byte[] passBytes = CConst.STRING32.getBytes();
        char[] passChars = CrypUtil.toChar( passBytes);
        test_db = SQLiteDatabase.openOrCreateDatabase(dbFile.getAbsolutePath(), passChars, null);
        assertThat( dbFile.exists(), is( true ));

        test_db.execSQL("CREATE TABLE " + ACCOUNT_CRYP_TABLE + " ("
                + SqlCipher.ACTab._id             + " integer primary key,"
                + SqlCipher.ACTab.key             + " text unique,"
                + SqlCipher.ACTab.value           + " text"
                + ");");

        assertThat( dbFile.length() > 0, is( true ));
        assertThat( getDbSize( test_db) == 0, is( true ));

        putCryp( "key1", "value1");
        assertThat( getDbSize( test_db) == 1, is( true ));

        assertThat( test_db.isOpen(), is( true ));
        test_db.close();
        assertThat( test_db.isOpen(), is( false ));

        // Reopen the database, read and write a second time
        test_db = SQLiteDatabase.openOrCreateDatabase(dbFile.getAbsolutePath(), passChars, null);
        assertThat( test_db.isOpen(), is( true ));

        String value1 = getCryp( "key1");
        assertThat( value1.contentEquals("value1"), is( true));
        assertThat( getDbSize( test_db) == 1, is( true ));

        putCryp( "key2", "value2");
        assertThat( getDbSize( test_db) == 2, is( true ));

        int rowsEffected = putCryp("key1", "value one");
        assertThat( rowsEffected == 1, is( true));
        assertThat( getDbSize( test_db) == 2, is( true ));
        value1 = getCryp( "key1");
        assertThat( value1.contentEquals("value one"), is( true));
        assertThat( getDbSize( test_db) == 2, is( true ));

        assertThat( test_db.isOpen(), is( true ));
        test_db.close();
        assertThat( test_db.isOpen(), is( false ));

        boolean deleted = dbFile.delete();
        assertThat( deleted, is( true ));
        assertThat( dbFile.exists(), is( false ));
    }

    public static synchronized int getDbSize( SQLiteDatabase db){

        String [] projection = { SqlCipher.DTab._id.toString() };

        Cursor c = db.query( ACCOUNT_CRYP_TABLE, projection, null, null, null, null, null);

        int size = c.getCount();
        c.close();
        return size;
    }

    /**
     * Save a key/value pair to the database.  Both are string.
     * A int value with the number of rows updated is returned with success == 1.
     * @param key
     * @param value
     * @return
     */
    public static synchronized int putCryp(String key, String value){//TODO use char[]

        String where = SqlCipher.ACTab.key+"=?";
        String[] args = new String[]{ key };

        ContentValues cv = new ContentValues();
        cv.put( SqlCipher.ACTab.key.toString(), key);
        cv.put( SqlCipher.ACTab.value.toString(), value);

        if( test_db.inTransaction()){

            test_db.endTransaction();
        }

        test_db.beginTransaction();
        int rows = test_db.update( ACCOUNT_CRYP_TABLE, cv, where, args);
        if( rows == 1)
            test_db.setTransactionSuccessful();
        else{
            if( rows == 0){
                long row = test_db.insert( ACCOUNT_CRYP_TABLE, null, cv);
                if( row > 0){

                    test_db.setTransactionSuccessful();
                    rows = 1;
                }
            }else{

                LogUtil.log("putCryp exception rows: "+rows+", key: "+key);
                throw new RuntimeException("putCryp failed");
            }
        }
        test_db.endTransaction();

        return rows;
    }

    /**
     * Return a string value indexed by a key.  If the key is not found, an empty
     * string is returned.
     * @param key
     * @return value or empty string if key is not found
     */
    public static synchronized String getCryp(String key){//TODO use char[]

        try {
            if( key == null || key.isEmpty())
                return "";

            String where = SqlCipher.ACTab.key+"=?";
            String[] args = new String[]{ key };

            Cursor c = test_db.query(ACCOUNT_CRYP_TABLE, null, where, args, null, null, null);
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
}
