package com.nuvolect.securesuite.data;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.main.DialogUtil;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Passphrase;
import com.nuvolect.securesuite.util.Util;

import java.io.File;
import java.io.IOException;

/**
 * Short term use to migrate existing CrypSafe users to SecureSuite.
 * This class will be deleted after most/all users have converted.
 */
public class MigrateCrypSafeDB {

    static Activity m_act;
    private static Intent sendIntent;

    public static boolean sendIntent(Activity act) {

        m_act = act;
        sendIntent = new Intent("com.nuvolect.crypsafe.action.GET_DB");
        if( sendIntent != null){

            sendIntent.putExtra("appSecret","c501251dcc731b739660652564c54929");

            LogUtil.log(MigrateCrypSafeDB.class, "intent request: "+sendIntent.toString());
        }
        else
            LogUtil.log(MigrateCrypSafeDB.class, "intent null");

        if (sendIntent == null || !Util.isIntentAvailable( m_act, sendIntent)) {

            return false;// CrypSafe not installed, database not copied
        }
        else{

            DialogUtil.confirmDialog(act,
                    "Upgrade from CrypSafe?",
                    "Cancel if you want an empty database, otherwise contacts and passwords will be copied from CrypSafe. App will restart.",
                    callbacks);

            return true;
        }
    }

    static DialogUtil.DialogUtilCallbacks callbacks = new DialogUtil.DialogUtilCallbacks() {
        @Override
        public void confirmed(boolean confirmed) {

            LogUtil.log(MigrateCrypSafeDB.class, "callbacks intent: "+sendIntent.toString());
            m_act.startActivity(sendIntent);
            m_act.startActivityForResult(sendIntent, CConst.COPY_DB_RESULT_212);
        }
    };

    public static void saveDB(Bundle bundle) {

        Bundle db_bundle = bundle.getBundle("db_bundle");
//
//        LogUtil.log(MigrateCrypSafeDB.class, "Got to saveDB, mappings: "+db_bundle.size());
//
//        Set<String> set = db_bundle.keySet();
//        for( String s : set){
//
//            LogUtil.log( MigrateCrypSafeDB.class, "key: "+s);
//        }
        String error = "Key for 'error' missing.";
        if( db_bundle.containsKey("error"))
            error = db_bundle.getString("error");

        if( ! error.isEmpty()){
            LogUtil.log( MigrateCrypSafeDB.class, "saveDB error: "+error);
        }

        if( error.isEmpty() && db_bundle.containsKey("account_db") && db_bundle.containsKey("detail_db")){

            byte[] account_db_bytes = db_bundle.getByteArray("account_db");
            byte[] detail_db_bytes = db_bundle.getByteArray("detail_db");
            LogUtil.log( MigrateCrypSafeDB.class, "saveDB account db length: "+account_db_bytes.length);
            LogUtil.log( MigrateCrypSafeDB.class, "saveDB detail db length: "+detail_db_bytes.length);

            File account_db_file = m_act.getDatabasePath( SqlCipher.ACCOUNT_DB_NAME);
            File detail_db_file = m_act.getDatabasePath( SqlCipher.DETAIL_DB_NAME);

            try {
                Util.writeFile( account_db_bytes, account_db_file);
                Util.writeFile( detail_db_bytes, detail_db_file);

                String passphrase = db_bundle.getString("passphrase");
                Passphrase.setDbPassphrase(m_act, passphrase);

                Boolean success = SqlCipher.testPassphrase( m_act, passphrase);
                String m = success?"Data copy successful":"Data copy failed";
                Toast.makeText(m_act, m, Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                LogUtil.logException(MigrateCrypSafeDB.class, e);
            }
            Util.restartApplication(m_act);
        }
        else
            LogUtil.log( MigrateCrypSafeDB.class, "db_bundle missing keys");

    }
}