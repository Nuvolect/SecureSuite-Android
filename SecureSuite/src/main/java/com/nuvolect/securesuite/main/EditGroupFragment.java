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

package com.nuvolect.securesuite.main;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.MyAccounts;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.LogUtil.LogType;
import com.nuvolect.securesuite.util.Safe;
import com.nuvolect.securesuite.util.Util;

import net.sqlcipher.Cursor;

import java.util.HashSet;
import java.util.Iterator;

public class EditGroupFragment extends Fragment {

    private static final boolean DEBUG = false;
    private EditGroupSearchAdapter mSearchAdapter;
    private EditGroupListAdapter mGroupListAdapter;
    private Cursor mCursor;
    private EditText mSearchNameEt;
    private TextWatcher mTextWatcher;
    private View m_rootView;
    private String mCurrentAccount;
    private boolean mBullshitAndroidCrap;
    private String mGroupTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBullshitAndroidCrap = savedInstanceState != null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        // Save group name
        // Save search string

        outState.putParcelableArrayList(
                CConst.MEMBERS, EditGroupListAdapter.m_members);

        // Convert the set of long into an array of long so we can save it
        long[] a2 = new long[ EditGroupListAdapter.m_deleteMemberSet.size()];
        Iterator<Long> iterator = EditGroupListAdapter.m_deleteMemberSet.iterator();
        int i = 0;

        while( iterator.hasNext())
            a2[ i++ ] = iterator.next();

        outState.putLongArray(CConst.MEMBERS_DELETED, a2);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(DEBUG)LogUtil.log("EditGroupFragment onDestroy");

        if( mCursor != null && !mCursor.isClosed())
            mCursor.close();
        mCursor = null;
        mSearchAdapter = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(DEBUG)LogUtil.log("EditGroupFragment onPause");

        if( mSearchNameEt != null )
            mSearchNameEt.removeTextChangedListener(mTextWatcher);

        mGroupListAdapter = null;
        mSearchAdapter = null;
        mBullshitAndroidCrap = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(DEBUG)LogUtil.log("EditGroupFragment onResume");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {

        mCurrentAccount = Cryp.getCurrentAccount();
        float screenWidth = Util.screenWidth( getActivity());
        LogUtil.log("EditGroupFragment onCreate: "+(int)screenWidth);

        if( screenWidth >= 512)
            m_rootView = inflater.inflate(R.layout.edit_group_fragment_wide, container, false);
        else
            m_rootView = inflater.inflate(R.layout.edit_group_fragment_narrow, container, false);

        // Add the theme background outline and fill color behind fragment
        AppTheme.applyDrawableShape(getActivity(), m_rootView);

        String account = Cryp.getCurrentAccount( );
        TextView accountTv = (TextView) m_rootView.findViewById(R.id.accountTv);
        accountTv.setText(account);

        mSearchNameEt = (EditText) m_rootView.findViewById(R.id.searchNameEt);

        // Set up search box
        mTextWatcher = new TextWatcher() {
            public void onTextChanged(CharSequence s, int arg1, int arg2, int arg3) {

                try {
                    String search = s.toString();//m_searchNameEt.getText().toString().trim();
                    if(DEBUG)LogUtil.log( "EditGroupFragment.afterTextChanged: "+search);

                    if( savedInstanceState != null && mBullshitAndroidCrap){

                        mBullshitAndroidCrap = false;
                        return;
                    }

                    int count = updateSearchAdapter( search);
                    ListView groupSearchLv = (ListView) m_rootView.findViewById(R.id.groupSearchLv);
                    if( s.length() == 0)
                        groupSearchLv.setVisibility(ListView.GONE);
                    else
                        groupSearchLv.setVisibility(ListView.VISIBLE);

                    if(DEBUG)LogUtil.log( "EditGroupFragment.afterTextChanged.count: "+count);
                    mSearchNameEt.requestFocus();

                } catch (Exception e) {
                    LogUtil.logException(getActivity(), LogType.EDIT_GROUP, e);
                }
            }
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
            @Override
            public void afterTextChanged(Editable arg0) { }
        };
        mSearchNameEt.addTextChangedListener( mTextWatcher );

        ListView groupListLv = (ListView) m_rootView.findViewById(R.id.groupListLv);
        mGroupListAdapter = new EditGroupListAdapter( getActivity(), 0, R.layout.edit_group_list_item);
        groupListLv.setAdapter(mGroupListAdapter);

        if( savedInstanceState == null){// Create data structures from scratch

            if( EditGroupActivity.m_group_id > 0){

                // If this is an existing group, add it's contacts to the GUI
                long[] contacts = MyGroups.getContacts( EditGroupActivity.m_group_id);

                for( long contact : contacts){

                    EditGroupListAdapter.addMember( getActivity(), contact);
                }
                // Refresh the adapter and list to show new data
                mGroupListAdapter.notifyDataSetChanged();
            }
        }
        else{ // Restore data structures from the savedInstanceState

            EditGroupListAdapter.m_members = savedInstanceState.getParcelableArrayList(CConst.MEMBERS);

            if( DEBUG ){
                for( EditGroupMember member : EditGroupListAdapter.m_members)
                    LogUtil.log("Restore member: "+member.display_name);
            }

            // First restore to an array then create the set
            long[] a2 = savedInstanceState.getLongArray(CConst.MEMBERS_DELETED);
            EditGroupListAdapter.m_deleteMemberSet = new HashSet<Long>();

            for( long contact_id : a2)
                EditGroupListAdapter.m_deleteMemberSet.add( contact_id );

            // Refresh the adapter and list to show new data
            mGroupListAdapter.notifyDataSetChanged();
        }
        if( EditGroupActivity.m_group_id > 0){  // Display group name for existing groups

            mGroupTitle = MyGroups.mGroupTitle.get(EditGroupActivity.m_group_id);
            EditText groupNameEt = (EditText) m_rootView.findViewById(R.id.groupNameEt);
            groupNameEt.setText(mGroupTitle);
            /**
             * Don't allow editing of a base group name
             */
            if( MyGroups.isBaseGroup(EditGroupActivity.m_group_id)) {
                groupNameEt.setEnabled(false);
                groupNameEt.setFocusable(false);
            }
        }else
            mGroupTitle = "";

        return m_rootView;
    }
    private EditGroupSearchAdapter.Callbacks m_listener = new EditGroupSearchAdapter.Callbacks(){

        @Override
        public void contactSelected(long contact_id, View v) {
            if(DEBUG)LogUtil.log( "EditGroupFragment.contactSelected: "+contact_id);

            TextView displayNameTv = (TextView) v.findViewById(R.id.displayNameTv);
            String display_name = displayNameTv.getText().toString();

            ImageView thumbnailIv = (ImageView) v.findViewById(R.id.thumbnailIv);
            Bitmap thumbnail =
                    ((BitmapDrawable)thumbnailIv.getDrawable()).getBitmap();

            Toast.makeText(getActivity(), display_name+" added", Toast.LENGTH_SHORT).show();

            EditGroupListAdapter.addMember(
                    contact_id, display_name, thumbnail);
            mGroupListAdapter.notifyDataSetChanged();
        }
    };

    public int updateSearchAdapter( String search){

        Cursor new_cursor = MyAccounts.getAccountCursor( mCurrentAccount, search);
        int count = new_cursor.getCount();

        if( mSearchAdapter == null){

            mSearchAdapter = new EditGroupSearchAdapter(
                    getActivity(),
                    new_cursor,
                    0,
                    R.layout.edit_group_search_item,
                    m_listener);

            ListView groupSearchLv = (ListView) m_rootView.findViewById(R.id.groupSearchLv);
            groupSearchLv.setAdapter(mSearchAdapter);

        }else{

            mSearchAdapter.changeCursor( new_cursor);
            mSearchAdapter.notifyDataSetChanged();
        }
        // Save the cursor so it can be closed in onDestroy
        mCursor = new_cursor;
        return count;
    }

    /**
     * Save the group configuration.  If it is a new group, the new group is created
     * and group membership records are created. For existing groups, the membership
     * records are updated and the group name may be changed unless is a special
     * group name such as My Contacts that cannot be changed.
     */
    public void save(Context ctx) {

        String newGroupName = validateGroupName();

        if( EditGroupActivity.m_newGroup ){

            int group_id = MyGroups.addGroup(  getActivity(), newGroupName,
                    mCurrentAccount, CConst.GROUP_ACCOUNT_TYPE);

            Cryp.setCurrentGroup( group_id);

            // Create group membership records
            EditGroupListAdapter.commitGroupMembershipUpdates( ctx,  group_id);

            // Update in-memory data structures
            MyGroups.loadGroupMemory();

            Toast.makeText( getActivity(), "New group created", Toast.LENGTH_SHORT).show();
        }
        else{
            int group_id = EditGroupActivity.m_group_id;

            if( ! newGroupName.contentEquals( MyGroups.mGroupTitle.get( group_id))) {

                // Group name changed, update it
                MyGroups.updateGroupTitle( group_id, newGroupName );
            }

            // Update group membership records
            EditGroupListAdapter.commitGroupMembershipUpdates( ctx, group_id);

            // Update in-memory data structures
            MyGroups.loadGroupMemory();

            Toast.makeText( getActivity(), "Group saved", Toast.LENGTH_SHORT).show();
        }
    }

    private String validateGroupName(){

        EditText groupNameEt = (EditText) m_rootView.findViewById(R.id.groupNameEt);
        String newGroupName = Safe.safeString( groupNameEt.getText().toString().trim());

        if( newGroupName.isEmpty())
            newGroupName = "New Group";

        boolean groupNameChanged = ! mGroupTitle.contentEquals(newGroupName);

        if( groupNameChanged && MyGroups.isBaseGroup( newGroupName)){

            Toast.makeText( getActivity(), "Group name conflicts with system group name", Toast.LENGTH_LONG).show();
            newGroupName = "New Group";
        }
        return newGroupName;
    }
}
