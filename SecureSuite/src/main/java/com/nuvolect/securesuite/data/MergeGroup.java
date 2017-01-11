package com.nuvolect.securesuite.data;

import java.util.ArrayList;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import com.nuvolect.securesuite.data.SqlCipher.GTTab;

public class MergeGroup {

    static SQLiteDatabase account_db = SqlCipher.account_db;
    static String GROUP_TITLE_TABLE = SqlCipher.GROUP_TITLE_TABLE;
    static String[] GROUP_TITLE_COLUMNS = SqlCipher.GROUP_TITLE_COLUMNS;
    static boolean[] groupsSelectedBool;
    private static Activity m_act;

    /**
     * Import a group into the current group.  If a group is selected and imported
     * restart the current activity.
     * @param act
     * @return
     */
    public static void mergeGroup(Activity act){

        String title = "Merge groups";
        m_act = act;
        boolean singleAccount = 1 == MyAccounts.numberOfAccounts();

        /**
         * Get a list of group-account pairs, sorted by account.
         */
        final ArrayList<String> groupTitleAccountList = new ArrayList<String>();
        final ArrayList<Integer> groupIdList = new ArrayList<Integer>();

        Cursor c = account_db.query(GROUP_TITLE_TABLE, GROUP_TITLE_COLUMNS,
                null, null, null, null, GTTab.account_name.toString());

        while( c.moveToNext()){

            String account = ", "+c.getString(GTTab.account_name.ordinal());
            if( singleAccount)
                account = "";
            String grpTitle= c.getString(GTTab.title.ordinal());
            groupTitleAccountList.add(grpTitle+account);

            groupIdList.add( c.getInt(GTTab.group_id.ordinal()));
        }
        c.close();

        // Setup a boolean[] and a CharSequence[] list for the dialog
        groupsSelectedBool = new boolean[ groupIdList.size()];
        for( int i = 0; i < groupIdList.size(); i++)
            groupsSelectedBool[ i ] = false;

        final CharSequence[] groupAccountList = groupTitleAccountList.toArray(
                new CharSequence[groupTitleAccountList.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder( m_act );
        builder.setTitle(title)
        .setMultiChoiceItems( groupAccountList, groupsSelectedBool, new DialogInterface.OnMultiChoiceClickListener() {

            public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                //Dialog sets boolean array for you
            }
        })
        .setPositiveButton("Merge", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                MyGroups.mergeGroups( m_act, groupIdList, groupsSelectedBool );
                m_act.recreate();
            }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Toast.makeText(m_act, "Canceled", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
}
