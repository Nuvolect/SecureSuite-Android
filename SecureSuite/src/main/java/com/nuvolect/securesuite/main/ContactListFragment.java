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

package com.nuvolect.securesuite.main;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.MyAccounts;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.StringUtil;
import com.nuvolect.securesuite.util.Util;

import net.sqlcipher.Cursor;

/**
 * A list fragment representing a list of People. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link ContactDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ContactListFragment extends ListFragment {

    private final static boolean DEBUG = true;//LogUtil.DEBUG;
    private ContactListCursorAdapter m_listCursorAdapter;
    private Cursor m_cursor;
    private Spinner accountSpinner;
    private Spinner groupSpinner;
    private int m_account_spinner_position = -1;
    private String[] m_account_list = new String[] {};
    private int m_group_spinner_position = -1;
    private String[] m_group_titles = new String[] {};
    private int[] m_group_ids = new int[] {};

    /**
     * Data that is persisted and restored using onPause / onResume lifecycle events
     */
    private String m_account="";
    private int m_group_id;
    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;
    private static TextView contactCountTv;  // contact count shown in header
    private static Activity m_act;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onContactSelected();
        public void onAccountSelected(String account, long first_contact_id);
        public void onGroupSelected(int group_id, long first_contact_id);
        public void onLongPressContact( Cursor cursor );
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override public void onContactSelected() { }
        @Override public void onAccountSelected(String account, long first_contact_id) { }
        @Override public void onGroupSelected(int group_id, long first_contact_id) {  }
        @Override public void onLongPressContact( Cursor cursor ) { }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ContactListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(DEBUG)LogUtil.log("CLF onCreate");
        m_act = getActivity();

        SqlCipher.getInstance(getActivity().getApplicationContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        /**
         * Restore from crypt persist search, group and account
         */
        m_account = Cryp.getCurrentAccount();
        LogUtil.log("CLF onResume m_account: "+m_account);
        m_group_id = Cryp.getCurrentGroup();
        if(DEBUG)LogUtil.log("CLF onResume: group_id: "+m_group_id);

        Cursor newCursor = updateCursor();
        updateAdapter(newCursor);
        getListView().setOnItemLongClickListener(m_onItemLongClickListener);

        accountSpinner = (Spinner) m_act.findViewById(R.id.accountSpinner);
        LogUtil.log("CLF onResume m_account_spinner_position: "+m_account_spinner_position);
        updateAccountSpinner();
        groupSpinner = (Spinner) m_act.findViewById(R.id.groupSpinner);
        updateGroupSpinner();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(DEBUG)LogUtil.log("CLF onPause");

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(DEBUG)LogUtil.log("CLF onDestroy");

        if( m_cursor != null && !m_cursor.isClosed())
            m_cursor.close();
        m_cursor = null;
        m_listCursorAdapter = null;
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.contact_list_content, container, false);

        // Add the theme background outline and fill color behind fragment
        AppTheme.applyDrawableShape(m_act, rootView);


        return rootView;
    }

    /**
     * Manage the account for the list view.  Load persisted account and set as default
     * account in spinner.  Configure a spinner adapter and watch for updates.  Reset
     * Adapter and cursor when spinner is used to select a new account and groups.
     */
    private void updateAccountSpinner(){

        LogUtil.log("CLF updateAccountSpinner: " + m_account);

        // Load current set of accounts
        m_account_list = MyAccounts.getAccounts();
        if( m_account_list.length == 0)
            return;

        // If first time, set default account to first account, if any
        if( m_account.isEmpty()){

            m_account = m_account_list[0];
            m_account_spinner_position = 0;
            Cryp.setCurrentAccount( m_account);
            m_group_id = Cryp.getCurrentGroup();
        }else{
            // Not first time, set spinner to current account
            m_account_spinner_position = findPosition( m_account_list, m_account);
        }

        ArrayAdapter<String> spinnerArrayAdapter;
        spinnerArrayAdapter = new ArrayAdapter<String>(
                m_act, android.R.layout.simple_spinner_item, m_account_list);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountSpinner.setAdapter(spinnerArrayAdapter);
        LogUtil.log("CLF setSelection: " +m_account_spinner_position);
        accountSpinner.setSelection(m_account_spinner_position, true);

        accountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {

                if (DEBUG)
                    LogUtil.log("CLF account onItemSelected: " + m_account_list[position]);

                m_account_spinner_position = position;
                m_account = m_account_list[position];
                Cryp.setCurrentAccount(m_account);
                m_group_id = Cryp.getCurrentGroup();

                LogUtil.log(LogUtil.LogType.CLF, "Account Spinner- Account, Group Id, Group: "
                        + m_account + ", " + m_group_id + ", " + MyGroups.mGroupTitle.get(m_group_id));

                updateGroupSpinner();
                m_cursor = updateCursor();
//                    Util.dumpCursorDescription("account spinner",m_cursor);
                m_listCursorAdapter.changeCursor(m_cursor);

                // Inform activity account was selected
                long candidate_id = MyGroups.getFirstContactInCursor(m_cursor);

                if( DEBUG)
                    LogUtil.log("CLF account onItemSelected first_contact: " +
                        SqlCipher.contactInfo(candidate_id));

                mCallbacks.onAccountSelected( m_account, candidate_id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private int findPosition(String[] list, String target) {

        for( int i = 0; i < list.length; i++){

            if( list[i].contentEquals(target)){
                return i;
            }
        }
        return 0;
    }
    /**
     * Manage the group for the list view.  Load persisted group and set as default
     * group in spinner.  Configure a spinner adapter and watch for updates.  Reset
     * Adapter and cursor when spinner is used to select a new account and groups.
     */
    private void updateGroupSpinner(){

        /**
         * Load current set of groups plus pseudo groups
         * Two extra elements are used to hold the title and pseudo group ID
         * for All in account and Starred in account.
         */
        int[] groupIds = MyGroups.getGroupIds(m_account);
        m_group_titles = new String[ groupIds.length + 2];
        m_group_ids = new int[ groupIds.length + 2];

        int nAllContacts = MyAccounts.getContactCount(m_account);
        m_group_titles[0] = CConst.ALL_IN_ACCOUNT + " ("+nAllContacts+")";
        m_group_ids[0] = CConst.GROUP_ALL_IN_ACCOUNT;
        int starContacts = MyAccounts.getStarredCount(m_account);
        m_group_titles[1] = CConst.STARRED + " ("+starContacts+")";
        m_group_ids[1] = CConst.GROUP_STARRED_IN_ACCOUNT;

        for( int i=0; i < groupIds.length; i++ ){

            int g_id = groupIds[ i ];
            int numContacts = MyGroups.mGroupCount.get( g_id );
            String rawTitle = MyGroups.mGroupTitle.get( g_id);

            if( rawTitle == null){
                MyGroups.loadGroupMemory();
                numContacts = MyGroups.mGroupCount.get( g_id );
                rawTitle = MyGroups.mGroupTitle.get( g_id);
            }

            String  groupTitle = StringUtil.ellipsize( rawTitle , 19);
            m_group_titles[2 + i] = groupTitle + " ("+numContacts+")";
            m_group_ids[2 + i] = g_id;
        }

        /**
         * Get the index of the spinner position matching the group.
         * Start with an invalid index.
         */
        m_group_spinner_position = -1;

        if( m_group_id == CConst.GROUP_ALL_IN_ACCOUNT)
            m_group_spinner_position = 0;
        else
        if( m_group_id == CConst.GROUP_STARRED_IN_ACCOUNT)
            m_group_spinner_position = 1;
        else{
            for( int j = 0; j < groupIds.length; j++){

                if( m_group_id == groupIds[ j ]){
                    m_group_spinner_position = j + 2;
                    break;
                }
            }
            if( m_group_spinner_position == -1)
                LogUtil.log(LogUtil.LogType.CLF, "Error group spinner -1");
        }

        ArrayAdapter<String> spinnerArrayAdapter;
        spinnerArrayAdapter = new ArrayAdapter<String>(
                m_act, android.R.layout.simple_spinner_item, m_group_titles);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(spinnerArrayAdapter);
        groupSpinner.setSelection(m_group_spinner_position, true);

        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {

                if (position != m_group_spinner_position) {

                    if (DEBUG)
                        LogUtil.log("selected: " + m_group_titles[position]);

                    m_group_spinner_position = position;// Save to trap false spinner events
                    m_group_id = m_group_ids[position];
                    Cryp.setCurrentGroup(m_group_id);

//                    LogUtil.log(LogUtil.LogType.CLF, "Group Spinner- Account, Group Id, Group: "
//                            + m_account + ", " + m_group_id + ", " + MyGroups.getGroupTitlePseudo(m_group_id));

                    Cursor cursor = updateCursor();
                    updateAdapter(cursor);

                    long first_contact_id = MyGroups.getFirstContactInCursor(cursor);

                    // Inform activity account was selected
                    mCallbacks.onGroupSelected(m_group_id, first_contact_id);
                }
            }

            @Override public void onNothingSelected(AdapterView<?> arg0) { }
        });
    }

    private Cursor updateCursor(){

        /**
         * Get a cursor that is specific to the current account/group selected.
         * This is used to get the id, star and display name.
         * The remaining details are fetched from the detail db
         */
        Cursor c = null;
        if( m_group_id == CConst.GROUP_ALL_IN_ACCOUNT)// all contacts in account
            c = MyAccounts.getAccountCursor(m_account, "");
        else {
            if (m_group_id == CConst.GROUP_STARRED_IN_ACCOUNT)// starred contacts in account
                c = MyAccounts.getAccountStarredCursor(m_account, "");
            else {
                if (m_group_id == 0)
                    LogUtil.log("CLF updateCursor, ERROR m_group_id == 0");
                else{

                    c = MyGroups.getGroupContactsCursor(m_group_id, "");
                }
            }
        }
        LogUtil.log(LogUtil.LogType.CLF, "updateCursor Spinner- Account, Group Id, Group: "
                +m_account+", "+m_group_id+", "+MyGroups.getGroupTitlePseudo(m_group_id));

        return c;
    }

    /**
     * Replace the existing cursor with the new cursor.  Android will manage closing of the
     * old cursor.  The new cursor is saved in the field: m_cursor that is closed in onDestroy().
     * @param new_cursor
     */
    public void updateAdapter( Cursor new_cursor){

        if( m_listCursorAdapter == null ){

            // First time, create an adapter, cursor may still be null
            m_listCursorAdapter = new ContactListCursorAdapter( getActivity(),
                    new_cursor,
                    0,  // flags, not using
                    R.layout.contact_list_item_activated);
            setListAdapter( m_listCursorAdapter);
            if(DEBUG)LogUtil.log("CLF updateAdapter, adapter created");
        }else{

            // Only update the cursor
            m_listCursorAdapter.changeCursor(new_cursor);
            m_listCursorAdapter.notifyDataSetChanged();
            if(DEBUG)LogUtil.log("CLF updateAdapter, adapter updated");
        }
        // Save the cursor so it can be closed in onDestroy
        m_cursor = new_cursor;

        if( contactCountTv != null){

            // Update the contact count in the header
            int count = m_cursor.getCount();
            contactCountTv.setText(Util.plural(count,"Contact"));
            int h2_color = AppTheme.getThemeColor( m_act, R.attr.h2_color);
            contactCountTv.setTextColor( h2_color);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState
                    .getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long _id) {
        super.onListItemClick(listView, view, position, _id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.

        long contact_id = SqlCipher.getATabContactId(_id);
        Persist.setCurrentContactId(m_act, contact_id);
        if(DEBUG) LogUtil.log("CLF onListItemClick: "+SqlCipher.contactInfo( contact_id));

        setActivatedPosition( position);

        mCallbacks.onContactSelected();
    }

    OnItemLongClickListener m_onItemLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long _id) {

            long contact_id = SqlCipher.getATabContactId(_id);
            Persist.setCurrentContactId(m_act, contact_id);
            if(DEBUG) LogUtil.log("CLF onItemLongClickListener: "+SqlCipher.contactInfo( contact_id));

            long next_id = SqlCipher.getNextContactId( m_cursor );
            if(DEBUG) LogUtil.log("CLF onItemLongClickListener next: "+SqlCipher.contactInfo( next_id));

            mCallbacks.onLongPressContact( m_cursor);// pass back to activity
            return true;// Pess is consumed == true
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {

        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
//        @SuppressWarnings("unused")
//        ListView lv = getListView();
//        getListView().setChoiceMode(
//                activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
//                        : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }
        mActivatedPosition = position;
    }

    //FUTURE replace with more efficient getContactAfter method
    /**
     * Return the contact ID next in the list after the target ID. This is used
     * when a contact is deleted and the next contact is shown in detail.
     * @param target_contact_id
     * @return
     */
    public long getContactAfter(long target_contact_id) {

        if( m_cursor == null)
            return 0;
        int position = m_cursor.getPosition();

        boolean found_it = false;

        m_cursor.moveToPosition(-1);
        while( m_cursor.moveToNext()){

            if( m_cursor.getLong(SqlCipher.index_contact_id) == target_contact_id){

                found_it = true;
                break;
            }
        }
        long next_id = 0;

        if( !found_it){

            // Did not find it, can't return next, restore cursor
            m_cursor.moveToPosition(position);
            return 0;
        }

        if( m_cursor.isLast()){

            // Found it but it was the last on the list, there is no next
            m_cursor.moveToPosition(position);
            return 0;
        }

        // Target id found, restore cursor and return next ID
        m_cursor.moveToNext();
        next_id = m_cursor.getLong(SqlCipher.index_contact_id);
        m_cursor.moveToPosition(position);

        return next_id;
    }
}
