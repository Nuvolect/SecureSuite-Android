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

package com.nuvolect.securesuite.data;

import android.content.ContentValues;
import android.content.Context;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.nuvolect.securesuite.data.SqlCipher.ADTab;
import com.nuvolect.securesuite.data.SqlCipher.ATab;
import com.nuvolect.securesuite.data.SqlCipher.GTTab;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;

import net.sqlcipher.Cursor;
import net.sqlcipher.DatabaseUtils;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MyGroups {

    private static final boolean DEBUG = LogUtil.DEBUG;
    static SQLiteDatabase account_db = SqlCipher.account_db;
    static String ACCOUNT_TABLE = SqlCipher.ACCOUNT_TABLE;
    static String GROUP_TITLE_TABLE = SqlCipher.GROUP_TITLE_TABLE;
    static String ACCOUNT_DATA_TABLE = SqlCipher.ACCOUNT_DATA_TABLE;
    static String[] ACCOUNT_DATA_COLUMNS = SqlCipher.ACCOUNT_DATA_COLUMNS;
    static String[] GROUP_TITLE_COLUMNS = SqlCipher.GROUP_TITLE_COLUMNS;

    /**
     * In-memory mapping of group cloud group ID to local group ID.
     * Only used during import.
     */
    public static SparseIntArray mCloudRemapGroupId;
    /**
     * In-memory mapping of group ID to group account
     */
    public static SparseArray<String> mGroupAccount;
    /**
     * In-memory mapping of group ID to group title
     */
    public static SparseArray<String> mGroupTitle;
    /**
     * In-memory mapping of group ID to group count
     */
    public static SparseIntArray mGroupCount;
    /**
     * Cache to get a group ID from an account + group title
     */
    static HashMap<String, Integer> mGroupIdByAccountPlusTitle;
    /**
     * Empty in-memory structures.
     */
    public static void initGroupMemory(){

        mGroupAccount = new SparseArray<String>();
        mGroupTitle = new SparseArray<String>();
        mGroupCount = new SparseIntArray();
        mCloudRemapGroupId = new SparseIntArray();
        mGroupIdByAccountPlusTitle = new HashMap<String, Integer>();
    }

    /**
     * During import, cloud group ID's can collide with user and internally created group ID's.
     * Map the cloud_group_id into a new local group id. Save the remap-into the group management
     * memory system and return the new group ID.
     * Must call MyGroups.initGroupMemory() prior to use.
     */
    public static int addRemapGroupId(int cloud_group_id, int local_group_id){

        mCloudRemapGroupId.append(cloud_group_id, local_group_id);

        return mCloudRemapGroupId.size();
    }
    /**
     * Remap an external cloud group ID to an internal group id.  Only used during import process.
     * @param candidate_id
     * @return
     */
    public static int remapGroupId(int candidate_id){

        int remap_id = mCloudRemapGroupId.get(candidate_id);

        if( remap_id > 0)
            return remap_id;// ID was remapped
        else
            return candidate_id;  // No remapp ID found, candidate is good
    }

    /**
     * Check if the account and title match an existing group
     * @param group_title
     * @param account_name
     * @return the matching ID, zero for no match, -1 for an error
     */
    public static int testGroupMatch( String group_title, String account_name ){

        String selection = DatabaseUtils.concatenateWhere(
                GTTab.account_name + "=?",
                GTTab.title+ "=?");

        String[] selectionArgs = new String[]{
                account_name,
                group_title };

        Cursor c = account_db.query(GROUP_TITLE_TABLE, null,
                selection, selectionArgs, null, null, null);

        int count = c.getCount();

        if( count == 0){

            c.close();
            return 0; // no match
        }
        else if( count == 1){
            c.moveToFirst();
            int group_id = c.getInt(GTTab.group_id.ordinal());
            c.close();
            return group_id;
        }
        else{

            LogUtil.log("ERROR group collision not 0 or 1 group definitions: "+count+
                    ", "+group_title+", "+account_name);
            c.close();
            return -1;
        }
    }
    /**
     * Check if the account, title and id match an existing group
     * @param group_id
     * @param group_title
     * @param account_name
     * @return
     */
    public static boolean testGroupMatch( int group_id, String group_title, String account_name ){

        String a = DatabaseUtils.concatenateWhere(
                GTTab.group_id + " =? ",
                GTTab.account_name + "=?");
        String selection = DatabaseUtils.concatenateWhere(a,
                GTTab.title+ "=?");

        String[] selectionArgs = new String[]{
                String.valueOf( group_id),
                "account_name",
                "group_title" };

        String[] columns = new String[]{ GTTab._id.toString() };

        Cursor c = account_db.query(GROUP_TITLE_TABLE, columns,
                selection, selectionArgs, null, null, null);

        int count = c.getCount();
        c.close();

        if( count == 0)
            return false;
        else if( count == 1)
            return true;
        else{

            LogUtil.log("ERROR group collision not 0 or 1 group definitions: " + count);
            return true;
        }
    }
    public static boolean groupNameInUse(String title, String account) {

        if( testGroupMatch(title, account) > 0)
            return true;

        title = title.toLowerCase();

        if( title.contentEquals(CConst.TRASH.toLowerCase())
                || title.contentEquals(CConst.STARRED.toLowerCase())
                || title.contentEquals(CConst.MY_CONTACTS.toLowerCase())
                || title.contentEquals(CConst.ALL_IN_ACCOUNT.toLowerCase()))
            return true;
        else
            return false;
    }
    /**
     * Add a group to in memory storage. Log an error and return -1 if the
     * group already exists.
     *
     * @param ctx
     * @param group_id
     * @param group_title
     * @param account
     * @return group_id or -1 on error
     */
    public static int addGroupIdMem(Context ctx, int group_id, String group_title, String account){

        if( mGroupTitle.indexOfKey( group_id) < 0){

            mGroupTitle.append( group_id, group_title);
            mGroupCount.append( group_id, 0);// Set count to zero for the new group
            mGroupAccount.append( group_id,  account);
            return group_id;
        }else{

            LogUtil.log("ERROR saving group id when there already is one: "+group_id);
            return -1;
        }
    }
    /**
     * Test the group id to see if it is a group or one of the special pseudo groups.
     *
     * @param group_id
     * @return
     */
    public static boolean validGroupIdPseudo( int group_id){

        if( group_id > 0)
            return mGroupCount.indexOfKey(group_id) >= 0;

        if( group_id == CConst.GROUP_ALL_IN_ACCOUNT)
            return true;

        if( group_id == CConst.GROUP_STARRED_IN_ACCOUNT)
            return true;

        return false;
    }
    /**
     * Test the group id to see if it is a valid group
     *
     * @param group_id
     * @return
     */
    public static boolean validGroupId( int group_id){

        if( group_id > 0)
            return mGroupCount.indexOfKey(group_id) >= 0;

        return false;
    }
    /**
     * Load in-memory copies of groupTitle and groupCount.
     * @return  # of groups
     */
    public static int loadGroupMemory() {

        initGroupMemory();

        /**
         * Patch to fix null pointer exception.
         * Either static memory is loaded before the database is initialized,
         * or for some reason the database is not initialized.
         * FUTURE identify why account_db is null
         */
        if( account_db == null){
            account_db = SqlCipher.account_db;
        }

        Cursor c = account_db.query(GROUP_TITLE_TABLE, GROUP_TITLE_COLUMNS, null, null, null, null, null);

        // Save title and ID of each group
        while( c.moveToNext()){

            int group_id =   c.getInt(GTTab.group_id.ordinal());
            String title =   c.getString(GTTab.title.ordinal());

                String account = c.getString(GTTab.account_name.ordinal());
                mGroupTitle.put  ( group_id, title );
                mGroupAccount.put( group_id, account );
        }
        c.close();

        // Save count figures for each group
        for( int i = 0; i<mGroupTitle.size(); i++){

            int group_id = mGroupTitle.keyAt(i);
            Cursor c2 = getGroupContactsCursor(group_id);
            mGroupCount.put(group_id, c2.getCount());
            c2.close();
        }
        return mGroupTitle.size();
    }

    /**
     * Return the group title with consideration for
     * pseudo groups All in account and Starred
     * @param g_id
     * @return
     */
    public static String getGroupTitlePseudo(int g_id) {

        if( g_id == CConst.GROUP_ALL_IN_ACCOUNT)
            return CConst.ALL_IN_ACCOUNT;
        if( g_id == CConst.GROUP_STARRED_IN_ACCOUNT)
            return CConst.STARRED;

        return mGroupTitle.get(g_id);
    }

    public static int getGroupCountPseudo(int g_id) {

        if( g_id > 0)
            return mGroupCount.get(g_id);

        String account = Cryp.getCurrentAccount();

        if( g_id == CConst.GROUP_ALL_IN_ACCOUNT)
            return  MyAccounts.getContactCount(account);

        if( g_id == CConst.GROUP_STARRED_IN_ACCOUNT)
            return MyAccounts.getStarredCount(account);

        throw new RuntimeException("getGroupTitlePseudo, no group matched id: "+g_id);
    }


    /**
     * Return My Contacts as the default group in a specific account.
     * @param account
     * @return int group or 0 on error
     */
    public static int getDefaultGroup(String account) {

        if(account.isEmpty())
            return 0;

        int contacts_group_id = getGroupId(account, CConst.MY_CONTACTS);

        return contacts_group_id;
    }

    /**
     * Add a new group title incrementing the group ID to the the next number.
     * It will not collide with an existing group ID but can wrap around beyond
     * the size of an Int.
     * @param ctx
     * @param group_title  - New group title
     * @param account      - Account for new group
     * @param account_type - String, Google's account type, currently unused
     * @return group id    - int, new group ID
     */
    public static int addGroup(Context ctx, String group_title, String account, String account_type){

        // Get the next assigned group ID
        int group_id = getNextEmptyGroupId(ctx);

        addGroup(ctx, group_id, group_title, account, account_type);

        return group_id; // Return the new group ID
    }

    /**
     * Scan current current group IDs and return an unused group ID.
     * @param ctx
     * @return
     */
    private static int getNextEmptyGroupId(Context ctx) {

        /**
         * Start with zero.  If this is startup and there are no groups then maxId + 1 == 1.
         * Otherwise find the highest group number and add 1 to make it unique.
         */
        int maxId = 0;

        for( int i = 0; i < mGroupTitle.size(); i++) {

            maxId = Math.max( maxId, mGroupTitle.keyAt(i));
        }
        return maxId + 1;
    }

    /**
     * Adds a new group to the system.  Duplicate groups will not be added.
     * Groups matching title and account are added to the group remap data structure
     * and the remap ID is returned.
     * @param ctx
     * @param group_title  - New group title
     * @param candidate_group_id     - New group id
     * @param account      - Account for new group
     * @param account_type - String, Google's account type, currently unused
     * @return group id    - int, existing or new group ID
     */
    public static int addGroup(Context ctx, int candidate_group_id, String group_title, String account,
                               String account_type){

        /**
         * Collision tests
         * a. ID, title and account match : We have it, nothing to add
         * b. title and account match, ID does not : Remap candidate ID to existing local ID
         * c. title and account don't match, ID is already in use : Remap candidate ID to new ID
         */
        /**
         * Test a. ID, title and account match : We have it, nothing to add
         */
        if( MyGroups.testGroupMatch( candidate_group_id, group_title, account)){

            // Complete match on id, title and account, no need to add this group
            if(DEBUG) LogUtil.log("addGroup, Test a. ID, title and account match : We have it, nothing to add: "
                    +candidate_group_id+", "+group_title+", "+account);

            return candidate_group_id;
        }
        /**
         * Test b. title and account match, ID does not : Remap candidate ID to local ID
         */
        int local_group_id = MyGroups.testGroupMatch(group_title, account);

        if( local_group_id > 0) {  // Local group found with the same title and account

            //The title and account collide, map to a new group

            MyGroups.addRemapGroupId(candidate_group_id, local_group_id);
            if(DEBUG)
                LogUtil.log("AddGroup, Test b. title and account match, ID does not : Remap candidate ID to local ID :"
                        +candidate_group_id + ", local_group_id: "+local_group_id+", "+group_title+", "+account);

            return local_group_id;
        }
        /**
         * Test c. title and account don't match, ID is already in use : Remap candidate ID to new ID
         */

        if( mGroupTitle.get(candidate_group_id) != null){

            /**
             * Local group ID with same index is already in use.
             * Create a new group and remap the candidate ID to use it.
             */
            int newId = getNextEmptyGroupId(ctx);
            MyGroups.addRemapGroupId(candidate_group_id, newId);
            MyGroups.addGroupIdMem(ctx, newId, group_title, account);
            MyGroups.addGroupTitle( newId, group_title, account, account_type);

            if(DEBUG) {
                LogUtil.log("addGroup Test c. title and account don't match, ID is already in use : Remap candidate ID to new ID");
                LogUtil.log("candidate id: "+candidate_group_id + ", newId: "  + newId + ", " + group_title + ", " + account);
            }
            return newId;

        }else{

            // No match for this group, insert it using the cloud group ID
            // Memory structures first
            MyGroups.addGroupIdMem(ctx, candidate_group_id, group_title, account);
            // And to to the database
            MyGroups.addGroupTitle( candidate_group_id, group_title, account, account_type);
            if(DEBUG)
                LogUtil.log("group is unique: "+candidate_group_id+", "+group_title+", "+account);
            return candidate_group_id;
        }
    }

    public static long addGroupTitle(int id, String title,
                                     String account_name, String account_type) {

        ContentValues cv = new ContentValues();
        cv.put( GTTab.group_id.toString(),      id);
        cv.put( GTTab.title.toString(),         title);
        cv.put( GTTab.account_name.toString(),  account_name);
        cv.put( GTTab.account_type.toString(),  account_type);

        account_db.beginTransaction();
        long rowId = account_db.insert(GROUP_TITLE_TABLE, null, cv);
        if( rowId > 0)
            account_db.setTransactionSuccessful();
        account_db.endTransaction();
        return rowId;
    }

    public static SparseArray<String> getGroupTitles() {

        SparseArray<String> groups = new SparseArray<String>();

        Cursor c = account_db.query(GROUP_TITLE_TABLE, GROUP_TITLE_COLUMNS,
                null, null, null, null, null);

        while( c.moveToNext()){

            int group_id = c.getInt(   GTTab.group_id.ordinal());
            String title = c.getString(GTTab.title.ordinal());
            groups.put( group_id, title );
        }
        c.close();

        return groups;
    }

    /**
     * Get title associated with a group_id.  If the group does not exist an empty string is returned.
     * On error, when there is more than one title record or there is a database error, null is returned.
     * @param group_id
     * @return
     */
    public static String getGroupTitle(int group_id) {

        String where = ADTab.group_id.toString()+"=?";
        String[] args = new String[]{ String.valueOf( group_id )};

        Cursor c = account_db.query(GROUP_TITLE_TABLE, GROUP_TITLE_COLUMNS,
                where, args, null, null, null);

        if( c.getCount() == 0){

            c.close();
            return "";
        }
        if( c.getCount() > 1) {

            c.close();
            return null;
        }
        if( c.getCount() == 1) {

            c.moveToFirst();
            String title = c.getString(GTTab.title.ordinal());
            c.close();
            return title;
        }

        return null;
    }

    /**
     * Associate a contact ID with a specific group by creating group membership
     * records in the account_db database. The method filters duplicate group membership
     * requests. Assumes test for invalid group is already done.
     *
     * Bump in memory mGroupCount when successful.
     *
     * @param contact_id
     * @param group_id
     * @return
     */
    public static boolean addGroupMembership(Context ctx,
                                             long contact_id, int group_id, boolean syncTransaction ) {

        if( inGroup( contact_id, group_id))  // Prevent duplicates
            return true;

//        if( validateContactGroup(ctx, contact_id, group_id) > 0)//FUTURE fix problems with groups during import
//            LogUtil.log("addGroupMembership ERROR");

        ContentValues cv = new ContentValues();
        cv.put( ADTab.group_id.toString(),     group_id);
        cv.put( ADTab.contact_id.toString(),   contact_id);

        account_db.beginTransaction();
        long rowId = account_db.insert(ACCOUNT_DATA_TABLE, null, cv);
        if( rowId > 0)
            account_db.setTransactionSuccessful();
        account_db.endTransaction();

        /**
         * Bump in-memory group count tally
         */
        mGroupCount.put(group_id, 1 + mGroupCount.get(group_id));
        /**
         * Inform sync system of the update
         */
        if( syncTransaction)
            SqlIncSync.getInstance().updateContact( ctx, contact_id);

        return rowId > 0;
    }

    /**
     * Confirm that the contact and group share the same account.
     *
     * @param ctx
     * @param contact_id
     * @param group_id
     * @return # of errors
     */
//    public static int validateContactGroup(Context ctx, long contact_id, int group_id){
//
//        String group_account = mGroupAccount.get( group_id);
//        String contact_account = SqlCipher.get( contact_id, ATab.account_name);
//FUTURE fix problems with groups during import
    /**
     * The problem has to do with remapping groups IDs during import.
     * Each group ID is associated with an account, ie., can only be used by contacts in that account.
     * Each contact ID is associated with an account.  There is an error when contacts are associated
     * with groups in a different account.  Validate groups detects the erroronous groups and can also delete them.
     */
//        if( SqlCipher.validateGroups(ctx, false) > 0){
//
//            String display_name = SqlCipher.get(contact_id, ATab.display_name);
//            LogUtil.log(
//                    " validateGroups ERROR, group_id: "+group_id+" display_name: "+display_name+" for contact_id: "+contact_id);
//            return 1;
//        }
//        if( ! group_account.contentEquals(contact_account)){
//
//            String display_name = SqlCipher.get(contact_id, ATab.display_name);
//            LogUtil.log(
//                    " ERROR contact and group do not have same account.  group account: "+group_account
//                            +" contact account: "+contact_account);
//            LogUtil.log(
//                    " group_id: "+group_id+" display_name: "+display_name+" for contact_id: "+contact_id);
//            return 1;
//        }
//        return 0;
//    }

    /**
     * Delete the record that associates a group with a contact_id.  The group is not deleted,
     * only the contacts association with a group is deleted.
     * @param contact_id
     * @param group_id
     * @return
     */
    public static int deleteGroupRecords(Context ctx, long contact_id, int group_id, boolean syncTransaction) {

        int rows = 0;
        account_db.beginTransaction();
        try {

            String where = ADTab.contact_id.toString()+"=? AND "+ ADTab.group_id.toString()+"=?";
            String[] args = new String[]{String.valueOf( contact_id ), String.valueOf( group_id )};
            rows = account_db.delete(ACCOUNT_DATA_TABLE, where, args);

            if( rows > 0)
                account_db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            account_db.endTransaction();
        }
        /**
         * Sync the transaction to the companion device
         */
        if( syncTransaction)
            SqlIncSync.getInstance().updateContact(ctx, contact_id);

        return rows;
    }
    /**
     * Delete all group records associated with an account.
     * @param group_id
     * @return number of rows deleted or -1 on error.
     */
//    public static int deleteGroupRecords( int group_id) {
//
//        int rows = -1;
//        account_db.beginTransaction();
//        try {
//
//            String where = ADTab.group_id.toString()+"=?";
//            String[] args = new String[]{ String.valueOf( group_id )};
//            rows = account_db.delete(ACCOUNT_DATA_TABLE, where, args);
//
//            if( rows > 0)
//                account_db.setTransactionSuccessful();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            account_db.endTransaction();
//        }
//        return rows;
//    }

    /**
     * Delete all group records associated with a contact.
     * @param contact_id
     * @return int number of records deleted, negative on error
     */
    public static int deleteGroupRecords( long contact_id ){

        String where = SqlCipher.ADTab.contact_id+"=?";
        String[] args = new String[]{String.valueOf( contact_id )};
        int totalRows = 0;

        account_db.beginTransaction();
        int rows = account_db.delete(ACCOUNT_DATA_TABLE, where, args);
        if( rows > 0){
            totalRows += rows;
            account_db.setTransactionSuccessful();
        }
        else if( rows < 0){
            if(DEBUG) LogUtil.log(LogUtil.LogType.MY_GROUPS, "ERROR deleteContact account_data_table: "+contact_id);
        }
        account_db.endTransaction();

        return totalRows;
    }

    /**
     * Add the contact_id to membership in the default group for that account.
     * @param contact_id
     * @param account
     * @return true: group membership was added successfully
     */
    public static boolean addMyContactsGroup( Context ctx, long contact_id, String account) {

        int group_id = getGroupId( account, CConst.MY_CONTACTS);

        return addGroupMembership( ctx, contact_id, group_id, false);
    }

    /**
     * <pre>
     * Return a cursor for all groups of a specific account
     * </pre>
     * @return Cursor to group
     */
    public static Cursor getGroupAccountCursor(String account) {

        String selection = GTTab.account_name.toString() + "=?";
        String[] selectionArgs = new String[]{ account };

        Cursor c = account_db.query(GROUP_TITLE_TABLE, null,
                selection, selectionArgs, null, null, GTTab.title.toString()+" ASC");

        //        if(true) {
        //            Util.dumpCursorDescription( c, "getTitleCursor");
        //
        //            while( c.moveToNext()){
        //
        //                String groupName = c.getString( c.getColumnIndex(GTTab.title.toString()) );
        //                int group_id = c.getInt( c.getColumnIndex(GTTab.group_id.toString()) );
        //                LogUtil.log(account+", id: "+group_id+", title: "+groupName);
        //            }
        //            c.moveToPosition(-1);
        //        }

        return c;
    }

    /**
     * Return a cursor for the group ID.  The group ID may be that of a standard group or
     * a pseudo group such as All contacts in account or Starred contacts.
     * @param group_id
     * @return
     */
    public static Cursor getGroupContactsCursorPseudo(String account, int group_id, String search) {

        Cursor c = null;
        if( group_id == CConst.GROUP_ALL_IN_ACCOUNT)// all contacts in account
            c = MyAccounts.getAccountCursor(account, search);
        else
        if( group_id == CConst.GROUP_STARRED_IN_ACCOUNT)// starred contacts in account
            c = MyAccounts.getAccountStarredCursor(account, search);
        else
            c = MyGroups.getGroupContactsCursor(group_id, search);

        return c;
    }

    /**
     * Return a cursor that references all contacts of a group
     * @param group_id
     * @return
     */
    public static Cursor getGroupContactsCursor(int group_id) {

        // Documentation states that _id must be included in the cursor or it will not work
        // http://developer.android.com/reference/android/widget/CursorAdapter.html
        String query = "SELECT DISTINCT "
                +ACCOUNT_TABLE+"."+ATab._id+", "
                +ACCOUNT_TABLE+"."+ATab.contact_id+", "
                +ACCOUNT_TABLE+"."+ATab.display_name+", "
                +ACCOUNT_TABLE+"."+ATab.starred+", "
                +ACCOUNT_TABLE+"."+ATab.last_name
                +" FROM "+ACCOUNT_TABLE
                +" INNER JOIN "+ACCOUNT_DATA_TABLE
                +" ON "+ACCOUNT_TABLE+"."+ATab.contact_id.toString()+"="+ACCOUNT_DATA_TABLE+"."+ADTab.contact_id
                +" WHERE "+ACCOUNT_DATA_TABLE+"."+ADTab.group_id+"=?"
                +" ORDER BY "+ATab.display_name.toString()+" ASC";

        Cursor c = account_db.rawQuery(query, new String[]{String.valueOf( group_id )});

        return c;
    }
    /**
     * Return a cursor that references all contacts of a group, filtered by search
     * @param group_id
     * @return
     */
    public static Cursor getGroupContactsCursor(int group_id, String search) {

        if( search.isEmpty())
            return getGroupContactsCursor(group_id);

        // Documentation states that _id must be included in the cursor or it will not work
        // http://developer.android.com/reference/android/widget/CursorAdapter.html
        String query = "SELECT DISTINCT "
                +ACCOUNT_TABLE+"."+ATab._id+", "
                +ACCOUNT_TABLE+"."+ATab.contact_id+", "
                +ACCOUNT_TABLE+"."+ATab.display_name+", "
                +ACCOUNT_TABLE+"."+ATab.starred+", "
                +ACCOUNT_TABLE+"."+ATab.last_name
                +" FROM "+ACCOUNT_TABLE
                +" INNER JOIN "+ACCOUNT_DATA_TABLE
                +" ON "+ACCOUNT_TABLE+"."+ATab.contact_id+"="+ACCOUNT_DATA_TABLE+"."+ADTab.contact_id
                +" WHERE "+ACCOUNT_DATA_TABLE+"."+ADTab.group_id+"=?"
                +" AND "+ACCOUNT_TABLE+"."+ATab.display_name+" LIKE ?"
                +" ORDER BY "+ATab.display_name+" ASC";

        String[] selectionArgs = new String[]{
                String.valueOf( group_id ),
                "%" + search + "%"
        };

//        LogUtil.log("query: "+query);
//        LogUtil.log("selectionArgs: "+selectionArgs[0]+", '"+selectionArgs[1]+"'");

        Cursor c = account_db.rawQuery(query, selectionArgs);

//        LogUtil.log("count: " + c.getCount());

        return c;
    }

    public static long[] getContacts( int group_id ){

        Cursor c = getGroupContactsCursor(group_id);
        int columnIndex = c.getColumnIndex(ADTab.contact_id.toString());

        long[] contacts = new long[ c.getCount()];

        for(int i=0; c.moveToNext(); ){

            long contact_id = c.getLong(columnIndex);
            contacts[ i++ ] = contact_id;
        }
        c.close();

        return contacts;
    }

    /**
     * Return a set containing all of the contact ids of a group
     * @param group_id
     * @return
     */
    public static Set<Long> getContactSet(int group_id) {

        long[] contacts = MyGroups.getContacts(group_id);

        Set<Long> contactSet = new HashSet<Long>();

        for( long contact : contacts)
            contactSet.add( contact);

        return contactSet;
    }

    /**
     * Return the first contact ID under cursor.  If there are none, return -1;
     * Save and restore the cursor position.
     * The cursor remains open.
     * @param c
     * @return
     */
    public static long getFirstContactInCursor( Cursor c ){

        if( c == null)
            return -1;

        if( c.getCount() <= 0)
            return -1;

        int position = c.getPosition();

        int columnIndex = c.getColumnIndex(ADTab.contact_id.toString());
        c.moveToFirst();
        long contact_id = c.getLong(columnIndex);

        c.moveToPosition(position);

        return contact_id;
    }

    /**
     * Delete a group and all associated group records from the system.
     * In memory and database records for this group will be removed.
     * returns int
     *    >0 : # of Group associated records deleted, title ==1 plus 1 for each contact group record
     *     0 : Group not deleted, it is one of {My Contacts, Trash, All in account, Starred in account}
     *    -1 : Error, group id not found
     * @param g_id
     */
    public static int deleteGroup(Context ctx, int g_id, boolean syncTransaction) {

        LogUtil.log("MyGroups.deleteGroup: " + mGroupTitle.get(g_id));

        int rows = 0;
        try {
            if( g_id == CConst.GROUP_ALL_IN_ACCOUNT || g_id == CConst.GROUP_STARRED_IN_ACCOUNT)
                return 0;
            String title = mGroupTitle.get(g_id);
            if( title.isEmpty())
                return -1;
            if( title.contentEquals("My Contacts") || title.contentEquals("Trash"))
                return 0;

            account_db.beginTransaction();
            /**
             * First delete all records that associate contacts with a group.
             * Then delete the record defining the group title
             */
            String where = ADTab.group_id.toString()+"='"+g_id+"'";
            rows = account_db.delete(ACCOUNT_DATA_TABLE, where, null);

            where = GTTab.group_id.toString()+"='"+g_id+"'";
            rows += account_db.delete(GROUP_TITLE_TABLE, where, null);

            if( rows > 0)
                account_db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            account_db.endTransaction();

            // Reset in-memory group data
            if( mGroupIdByAccountPlusTitle.containsValue( g_id)){

                String account = mGroupAccount.get( g_id);
                String title = mGroupTitle.get( g_id );
                mGroupIdByAccountPlusTitle.remove( account + title );
            }
            mGroupTitle.delete(g_id);
            mGroupCount.delete(g_id);
            mGroupAccount.delete(g_id);
        }
        /**
         * Synchronize action on companion device
         */
        if( syncTransaction)
            SqlIncSync.getInstance().deleteGroup( ctx, g_id);

        return rows > 0 ? rows : -1;
    }

    /**
     * Force the deletion of a group
     * @param g_id
     * @return
     */
    public static int deleteGroupForce(Context ctx, int g_id, boolean syncTransaction) {

        LogUtil.log("MyGroups.deleteGroup: " + mGroupTitle.get(g_id));

        int rows = 0;
        try {
            account_db.beginTransaction();
            /**
             * First delete the record defining the group then delete all
             * records that assoicate contacts with a group.
             */
            String where = ADTab.group_id.toString()+"='"+g_id+"'";
            rows = account_db.delete(ACCOUNT_DATA_TABLE, where, null);

            where = GTTab.group_id.toString()+"='"+g_id+"'";
            rows += account_db.delete(GROUP_TITLE_TABLE, where, null);

            if( rows > 0)
                account_db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            account_db.endTransaction();

            /**
             * Synchronize action on companion device
             */
            if( syncTransaction)
                SqlIncSync.getInstance().deleteGroup( ctx, g_id);

            // Reset in-memory group data
            loadGroupMemory();
        }
        return rows > 0 ? rows : -1;
    }

    /**
     * Return true if a contact_id is a member of a a group, else return false.
     * @param contact_id
     * boolean - true when contact_id is a member of a group
     */
    public static boolean inGroup(long contact_id, int group_id) {

        String where1 = ADTab.contact_id.toString()+"=?";
        String where2 = ADTab.group_id.toString()+"=?";
        String where = DatabaseUtils.concatenateWhere(where1, where2);

        String[] args = new String[]{
                String.valueOf( contact_id ),
                String.valueOf( group_id ),
        };

        Cursor c = account_db.query(
                ACCOUNT_DATA_TABLE, ACCOUNT_DATA_COLUMNS, where, args, null, null, null);

        int count = c.getCount();
        c.close();

        if( count == 1)
            return true;
        if( count == 0)
            return false;

        throw new RuntimeException("inGroup, can only be member of a group 0 or 1 times: "+count);
    }
    /**
     * Return an array of groups associated with a contact_id
     * @param contact_id
     * @return int[]
     */
    public static int[] getGroups(long contact_id) {

        String where = ADTab.contact_id.toString()+"='"+contact_id+"'";
        Cursor c = account_db.query(ACCOUNT_DATA_TABLE, ACCOUNT_DATA_COLUMNS, where, null, null, null, null);

        int[] groups = new int[ c.getCount()];
        int i=0;

        while( c.moveToNext()){

            groups[ i++ ] = c.getInt( ADTab.group_id.ordinal());
        }
        c.close();

        return groups;
    }

    /**
     * Given the group row _id, return the group_id
     * @param _id
     * @return int group_id
     */
    public static int getGroupId(int _id) {

        String[] projection = {GTTab.group_id.toString()};
        String where = GTTab._id.toString()+"=?";
        String[] args = new String[]{String.valueOf( _id )};

        Cursor c = account_db.query(GROUP_TITLE_TABLE, projection, where, args, null, null, null);
        c.moveToFirst();

        if( c.getCount() != 1){
            c.close();
            throw new RuntimeException("getGroupId, get should only find one record");
        }
        int group_id = c.getInt( 0 );// only item in projection

        c.close();
        return group_id;
    }

    /**
     * Return a text string of all the groups a user is in.
     * Group names are in mixed case as they were received from Android.
     * @param contact_id
     * @return Mixed case string list of group names
     */
    public static String getGroupTitles(long contact_id) {

        /**
         * TODO hack to solve spurious crash problem
         */
        if( mGroupTitle == null)
            initGroupMemory();

        int[] groups = MyGroups.getGroups(contact_id);

        String groupList = "";
        String commaSpace = "";

        for( int group_id : groups){
            groupList += commaSpace + MyGroups.mGroupTitle.get( group_id );
            commaSpace = ", ";
        }
        return groupList;
    }

    /**
     * Return a string array of all groups of a user.
     * Group names are in mixed case as they were received from Android.
     * @param contact_id
     * @return
     */
    public static String[] getGroupTitleArray(long contact_id) {

        int[] groups = MyGroups.getGroups(contact_id);
        String[] titles = new String[ groups.length ];
        int i = 0;

        for( int group_id : groups){
            titles[ i++] = MyGroups.mGroupTitle.get( group_id );
        }
        return titles;
    }

    /**
     * Get all of the groups for a specific account
     * @param account
     * @return
     */
    public static CharSequence[] getGroupTitles(String account) {

        Cursor c = getGroupAccountCursor(account);
        ArrayList<String> groupList = new ArrayList<String>();

        while( c.moveToNext()){

            groupList.add( c.getString( c.getColumnIndex( GTTab.title.toString())));
        }
        c.close();
        return groupList.toArray(new CharSequence[ groupList.size()]);
    }
    /**
     * Get all of the groups for a specific account
     * @param account
     * @return
     */
    public static CharSequence[] getGroupTitlesExceptTrash(String account) {

        Cursor c = getGroupAccountCursor(account);
        ArrayList<String> groupList = new ArrayList<String>();

        while( c.moveToNext()){

            String groupTitle = c.getString( c.getColumnIndex( GTTab.title.toString()));
            if( ! groupTitle.contentEquals(CConst.TRASH))
                groupList.add( groupTitle);
        }
        c.close();
        return groupList.toArray(new CharSequence[ groupList.size()]);
    }
    /**
     * Get the checkstate for a contacts current set of groups.  The checkstate is an array of
     * booleans where true == is a member of the group.  This array matches the names of the group
     * provided by getGroupTitlesExceptTrash.
     * Coordinated list family member.
     * @param account
     * @param contact_id
     * @return
     */
    public static boolean[] getGroupCheckStateExceptTrash(String account, long contact_id) {

        Set<Integer> groupSet = getGroupSet(contact_id);
        Cursor c = getGroupAccountCursor( account);
        int trashGroupId = getGroupId( account, CConst.TRASH);

        boolean[] groupList = new boolean[ c.getCount()];

        int i = 0;
        while( c.moveToNext()){

            int group_id = c.getInt( c.getColumnIndex( GTTab.group_id.toString()));
            if( group_id == trashGroupId)
                continue;

            if( groupSet.contains( group_id))
                groupList[ i++] = true;
            else
                groupList[ i++] = false;
        }
        c.close();
        return groupList;
    }
    /**
     * Get all of the groups for a specific account.
     * Coordinated list family member.
     * @param account
     * @return
     */
    public static String[] getGroupTitlesStringArray(String account) {

        Cursor c = getGroupAccountCursor( account);
        String[] titles = new String[ c.getCount()];

        int i=0;
        while( c.moveToNext()){

            titles[ i++ ] = c.getString(c.getColumnIndex(GTTab.title.toString()));
        }
        c.close();

        return titles;
    }

    /**
     * Get all of the group ids belonging to a specific account.
     * Coordinated list family member.
     * @param account
     * @return int[] of group_id
     */
    public static int[] getGroupIds(String account) {

        Cursor c = getGroupAccountCursor( account);
        int[] groupIds = new int[ c.getCount()];

        int i=0;
        while( c.moveToNext()){

            groupIds[ i++ ] = c.getInt(c.getColumnIndex(GTTab.group_id.toString()));
        }
        c.close();
        return groupIds;
    }

    /**
     * Get the checkstate for a contacts current set of groups.  The checkstate is an array of
     * booleans where true == is a member of the group.  This array matches the names of the group
     * provided by getGroups.
     * Coordinated list family member.
     * @param account
     * @param contact_id
     * @return
     */
    public static boolean[] getGroupCheckState(String account, long contact_id) {

        Set<Integer> groupSet = getGroupSet(contact_id);
        Cursor c = getGroupAccountCursor( account);

        boolean[] groupList = new boolean[ c.getCount()];

        int i = 0;
        while( c.moveToNext()){

            int group_id = c.getInt( c.getColumnIndex( GTTab.group_id.toString()));
            if( groupSet.contains( group_id))
                groupList[ i++] = true;
            else
                groupList[ i++] = false;
        }
        c.close();
        return groupList;
    }

    /**
     * Return a set containing all of the group ids of a contact
     * @param contact_id
     * @return
     */
    public static Set<Integer> getGroupSet(long contact_id) {

        int[] groups = MyGroups.getGroups(contact_id);

        Set<Integer> groupSet = new HashSet<Integer>();

        for( int group : groups)
            groupSet.add( group);

        return groupSet;
    }

    /**
     * Add base set of groups to a new account
     * @param ctx
     * @param account
     * returns boolean: success/fail
     */
    public static boolean addBaseGroupsToNewAccount(Context ctx, String account) {

        if( account.isEmpty())
            return false;

        addGroup(ctx, CConst.MY_CONTACTS, account, "");
        addGroup(ctx, CConst.TRASH, account, "");
        addGroup(ctx, CConst.FRIENDS, account, "");
        addGroup(ctx, CConst.COWORKERS, account, "");
        loadGroupMemory();

        return true;
    }

    /**
     * Add required groups for all accounts. When syncing a new device,
     * a new account may be synced with out Trash or My Contacts.
     * @param ctx
     */
    public static void addRequiredGroups(Context ctx) {

        String[] accounts = MyAccounts.getAccounts();

        for( String account : accounts){

            if( 0 == getGroupId( account, CConst.TRASH))
                addGroup(ctx, CConst.TRASH, account, "");

            if( 0 == getGroupId( account, CConst.MY_CONTACTS))
                addGroup(ctx, CConst.MY_CONTACTS, account, "");
        }
    }

    /**
     * Find a group_id by account and group title.
     * @param account
     * @param title
     * @return int group id or 0 if the group does not exist and -1 on error
     */
    public static int getGroupId(String account, String title) {

        /**
         * Cache the result to speed things up a  bit
         */
        if( mGroupIdByAccountPlusTitle.containsKey( account + title)){

            return mGroupIdByAccountPlusTitle.get( account + title);
        }

        String a = GTTab.account_name + "=?";
        String b = GTTab.title + "=?";
        String selection = DatabaseUtils.concatenateWhere(a, b);
        String[] selectionArgs = new String[]{ account, title};
        String[] projection = {GTTab.group_id.toString()};

        Cursor c = account_db.query(GROUP_TITLE_TABLE,
                projection, selection, selectionArgs, null, null, null);

        int count = c.getCount();

        if( count == 0){
            c.close();
            return 0;
        }else
        {
            if( c.getCount() != 1){

                LogUtil.log("MyGroups ERROR count not 1/0: "+ account+", title: "+title);
                c.close();
                return -1;
            }else
            {
                c.moveToFirst();
                int group_id = c.getInt( 0 ); // only thing in projection
                c.close();

                /**
                 * Populate the cache.
                 */
                mGroupIdByAccountPlusTitle.put(account + title, group_id);

                return group_id;
            }
        }
    }
    /**
     * Update a group title
     * @param group_id
     * @param group_title
     */
    public static void updateGroupTitle( int group_id, String group_title ) {

        String where = GTTab.group_id.toString()+"=?";
        String[] args = new String[]{String.valueOf( group_id )};

        account_db.beginTransaction();
        long rows = 0;

        ContentValues cv = new ContentValues();
        cv.put( GTTab.title.toString(), group_title);
        rows = account_db.update( GROUP_TITLE_TABLE, cv, where, args);

        if( rows == 1)
            account_db.setTransactionSuccessful();
        else
            LogUtil.log("Error updateGroupTitle");

        account_db.endTransaction();
    }

    /**
     * Test for a pseudoGroup.
     * @param group_id
     * @return
     */
    public static boolean isPseudoGroup(int group_id) {

        if( group_id == CConst.GROUP_ALL_IN_ACCOUNT || group_id == CConst.GROUP_STARRED_IN_ACCOUNT)
            return true;

        return false;
    }
    /**
     * Test if group is one of the base special groups, i.e., can't be deleted or renamed group
     * @param group_id
     * @return true when group is one of {My Contacts, Trash, All in group, Starred in group}
     */
    public static boolean isBaseGroup(int group_id) {

        if( group_id == CConst.GROUP_ALL_IN_ACCOUNT || group_id == CConst.GROUP_STARRED_IN_ACCOUNT)
            return true;
        else
        if( mGroupTitle.get(group_id).contains(CConst.MY_CONTACTS))
            return true;
        else
        if( mGroupTitle.get(group_id).contains(CConst.TRASH))
            return true;
        else
            return false;
    }

    /**
     * Test if group is one of the base special groups, i.e., can't be deleted or renamed group
     * @param title
     * @return
     */
    public static boolean isBaseGroup(String title) {

        if( title.contentEquals(CConst.ALL_IN_ACCOUNT))
            return true;
        else
        if( title.contentEquals(CConst.STARRED))
            return true;
        else
        if( title.contentEquals(CConst.MY_CONTACTS))
            return true;
        else
        if( title.contentEquals(CConst.TRASH))
            return true;

        return false;
    }

    /**
     * Import contacts into the current group.  This involves creating a group ID record
     * matching the current group, for each of the contacts in the selected groups.
     * @param ctx
     * @param groupIdList
     * @param groupsSelectedBool
     */
    public static void mergeGroups(Context ctx,
                                   ArrayList<Integer> groupIdList, boolean[] groupsSelectedBool) {

        int current_group = Cryp.getCurrentGroup();

        for( int i = 0; i< groupsSelectedBool.length; i++){

            if( groupsSelectedBool[i]){

                // Get cursor for all contacts in selected group

                Cursor c = getGroupContactsCursor(groupIdList.get(i));

                while( c.moveToNext()){

                    long contact_id = c.getLong(c.getColumnIndex( ADTab.contact_id.toString()));

                    // Add current group ID record to each contacts
                    addGroupMembership( ctx, contact_id, current_group, true);
                }
                c.close();
            }
        }
    }
    public static int dumpGroupTitleTable() {
        LogUtil.log("DUMP: " + GROUP_TITLE_TABLE);

        Cursor c = account_db.query(GROUP_TITLE_TABLE, null,
                null, null, null, null, GTTab.account_name.toString()+" ASC");
        int errors = 0;

        while( c.moveToNext()){

            String account = c.getString( c.getColumnIndex(GTTab.account_name.toString()) );
            String title = c.getString(c.getColumnIndex(GTTab.title.toString()));
            int group_id = c.getInt(c.getColumnIndex(GTTab.group_id.toString()));

            int count = mGroupCount.get(group_id);

            LogUtil.log(
                    "account: "+account +
                            ", group_id: " +group_id+
                            ", title: "+title+
                            ", count: "+count
            );

            if( mGroupCount.indexOfKey(group_id) < 0){
                ++errors;
                LogUtil.log("ERROR group_id not in memory"+
                        ", account: "+account +
                        ", group_id: " +group_id+
                        ", title: "+title
                );
            }
        }
        c.close();
        return errors;
    }

    public static void dumpAccountDataTable() {
        LogUtil.log("DUMP: " + ACCOUNT_DATA_TABLE);

        Cursor c = account_db.query(ACCOUNT_DATA_TABLE, null,
                null, null, null, null, null);

        while( c.moveToNext()){

            long contact_id = c.getLong(c.getColumnIndex(ADTab.contact_id.toString()));
            int group_id = c.getInt(c.getColumnIndex(ADTab.group_id.toString()));

            LogUtil.log(
                    "contact_id: "+contact_id+
                            ", group_id: " +group_id+
                            ""
            );
        }
        c.close();
    }

    /**
     * Invert the group selection of a contact.  If it was a member of the group, it will
     * no longer be a member and if it was not a member it is now a member of the group.
     * @param contact_id
     * @param group_id
     */
    public static void invertGroupSelect(Context ctx, Long contact_id, int group_id) {

        String title = mGroupTitle.get(group_id);

        if( inGroup(contact_id, group_id)){

            deleteGroupRecords( ctx, contact_id, group_id, true);
        }else{

            addGroupMembership( ctx, contact_id, group_id, true);
        }
        loadGroupMemory();
    }

    /**
     * Delete all contacts in the account associated with the group Trash.
     * Return >= 0 : number of contacts deleted
     *          -2 : Trash group does not exist
     *          -1 : Error
     */
    public static int emptyTrash(Context ctx, String account) {

        // Get group ID for this accounts trash.
        int trash_group_id = getGroupId(account, CConst.TRASH);

        if( trash_group_id == 0)
            return 0;

        if (trash_group_id < 0)
            return -1;

        /**
         * Get all contacts associated with Trash
         */
        long[] contacts_in_trash = getContacts(trash_group_id);
        int number_in_trash = contacts_in_trash.length;

        /**
         * Remove the trash group from each contact and delete the contact
         */
        for( long contact_id : contacts_in_trash){

            SqlCipher.deleteContact(ctx, contact_id, true);
        }
        /**
         * Set in-memory trash count to zero
         */
        mGroupCount.put(trash_group_id, 0);

        return number_in_trash;
    }

    /**
     * Associate contact with group Trash and remove association with other groups.
     * @param contact_id
     * returns boolean: successful / failure
     */
    public static boolean trashContact( Context ctx, String account, long contact_id) {

        int trash_group_id = getGroupId( account, CConst.TRASH);
        if( trash_group_id < 0)
            return false;

        if( trash_group_id == 0) {        // No Trash group defined yet so create it
            addGroup(ctx, CConst.TRASH, account, "");
            loadGroupMemory();
        }

//        int[] groups = getGroups(contact_id);

        /**
         * First remove all groups associated with a contact.
         */
//        for( int id : groups){
//
//            deleteGroupRecords( ctx, contact_id, id, false);// Sync==false, don't sync yet
//        }

        deleteGroupRecords( contact_id);
        /**
         * Associate contact with Trash group
         */
        boolean success = addGroupMembership(ctx, contact_id, trash_group_id, true);// Sync==true

        /**
         * Remove Starred
         */
        long updates = SqlCipher.put(contact_id, SqlCipher.ATab.starred, "0");
        if( updates != 1)
            success = false;

        return success;
    }

    /**
     * Iterate all accounts and cleanup groups by removing all
     * empty groups except {Trash, My Contacts}
     */
    public static String removeNonBaseEmptyGroups(Context ctx) {

        String log = "";
        String newLine = "";
        loadGroupMemory();// Make sure memory is current

        /**
         * Make copies of the group data that can be iterated on while
         * the main group data is updated.
         */
        SparseIntArray groupCount = mGroupCount.clone();
        SparseArray<String> groupTitle = mGroupTitle.clone();
        SparseArray<String> groupAccount = mGroupAccount.clone();

        for( int i =0; i < groupCount.size(); i++){

            int g_count = groupCount.valueAt( i );
            if( g_count == 0){

                int g_id = groupCount.keyAt( i );
                String g_title = groupTitle.get(g_id);

                boolean isTrash = g_title.contains(CConst.TRASH);
                boolean isMyContacts = g_title.contains(CConst.MY_CONTACTS);
                boolean isFriends = g_title.contains(CConst.FRIENDS);
                boolean isCoworkers = g_title.contains(CConst.COWORKERS);

                if( ! isTrash && ! isMyContacts && ! isFriends && ! isCoworkers ){

                    int result =deleteGroup(ctx, g_id, true);// Delete group, update group memory and sync
                    if( result > 0) {
                        log = log + newLine
                                +"Empty group "+g_title+", "+groupAccount.get(g_id)+" deleted\n";
                        newLine = "\n";
                    }
                }
            }
        }
        if( log.isEmpty())
            log = "No empty groups found\n";

        return log;
    }

    /**
     * Query contact groups and return true if contact is in group Trash.
     * @param contact_id
     * @return
     */
    public static boolean isInTrash(long contact_id) {

        String[] groupTitles = getGroupTitleArray(contact_id);

        for( String title : groupTitles){

            if( title.contentEquals(CConst.TRASH))
                return true;
        }

        return false;
    }

    /**
     * Iterate through all of the account and remove any groups with
     * the title "Starred in Android".  This group is redundent and is
     * created during import from Google.
     */
    public static String removeStarredInAndroidGroups(Context ctx) {

        String[] accounts = MyAccounts.getAccounts();
        String log = "";
        String newLine = "";

        for( String account : accounts){

            int g_id = getGroupId( account, CConst.STARRED_IN_ANDROID);

            if( g_id > 0){

                int result = deleteGroup(ctx, g_id, true);

                if( result > 0) {
                    log = log + newLine
                            +CConst.STARRED_IN_ANDROID+" in "+account+" deleted\n";
                    newLine = "\n";
                }
            }
        }
        if( log.isEmpty())
            log = "No \""+CConst.STARRED_IN_ANDROID+"\" groups found\n";
        return log;
    }

    /**
     * Rename a group.  Assume that all validation checks are complete.
     * @param ctx
     * @param newGroupTitle
     * @param old_group_id
     * @param account
     * returns boolean success
     */
    public static boolean renameGroup(Context ctx, String newGroupTitle, int old_group_id, String account) {

        /**
         * 1. Get all contacts in the old group
         * 2. Create the new group
         * 3. Add all contacts to the new group
         * 4. Delete the old group
         * 5. Reload memory structures
         */
        boolean sync_transactions = true;
        long[] contacts_in_group = getContacts(old_group_id);
        int new_group_id = addGroup(ctx, newGroupTitle, account, "");

        for( long contact_id : contacts_in_group){

            addGroupMembership(ctx, contact_id, new_group_id, sync_transactions);
        }
        boolean success = deleteGroup(ctx, old_group_id, sync_transactions) > 0;
        MyGroups.loadGroupMemory();

        return success;
    }
}
