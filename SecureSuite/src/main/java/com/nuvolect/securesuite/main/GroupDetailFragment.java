package com.nuvolect.securesuite.main;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.TextView;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.Util;

import net.sqlcipher.Cursor;

public class GroupDetailFragment extends Fragment {

    private final boolean DEBUG = LogUtil.DEBUG;
    public int m_group_id = -1;
    private Activity m_act ;
    private GroupDetailCursorAdapter m_groupDetailCursorAdapter;
    private int mCountInGroup=0;
    private Cursor m_cursor = null;

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onContactSelected(){ }

        @Override
        public void onLongPressContact(Cursor m_cursor) { }
    };

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;
    private TextView m_groupTitleTv;
    private TextView m_groupCount;
    private boolean m_wasPaused;
    private String m_account;
    private String m_search = "";

    public interface Callbacks {

        /**
         * Callback for when an item has been selected.
         */
        public void onContactSelected();

        public void onLongPressContact(Cursor m_cursor);
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

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GroupDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_act = getActivity();
        m_group_id = Cryp.getCurrentGroup();
        mCountInGroup = MyGroups.getGroupCountPseudo(m_group_id);
        m_wasPaused = false;

        if(DEBUG)LogUtil.log("GDF onCreate, m_group_id: "+m_group_id);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(DEBUG)LogUtil.log("GDF onPause, m_group_id: "+m_group_id);

        m_wasPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(DEBUG)LogUtil.log("GDF onResume, m_group_id: "+m_group_id);

        if( m_wasPaused){

            updateAdapter();
            m_wasPaused = false;
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(DEBUG)LogUtil.log("GDF onDestroy, m_group_id: "+m_group_id);

        if( m_cursor != null && !m_cursor.isClosed())
            m_cursor.close();
        m_cursor = null;
        m_groupDetailCursorAdapter = null;
    }

//    /**
//     * Reassign to a new group and update cursor.
//     * @param selectGroup
//     */
//    public void notifyChanged( int selectGroup){
//
//        m_group_id = selectGroup;
//        m_groupDetailCursorAdapter.notifyDataSetChanged();
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        m_group_id = Cryp.getCurrentGroup();
        mCountInGroup = MyGroups.getGroupCountPseudo(m_group_id);

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.group_detail_fragment, container, false);

        // Add the theme background outline and fill color behind fragment
        AppTheme.applyDrawableShape(m_act, rootView);

        boolean existingGroup = MyGroups.validGroupIdPseudo(m_group_id);
        m_account = Cryp.getCurrentAccount();

        if( ! existingGroup){

            m_group_id = MyGroups.getDefaultGroup( Cryp.getCurrentAccount());
            Cryp.setCurrentGroup( m_group_id);
        }
        m_groupTitleTv  = (TextView) rootView.findViewById(R.id.group_name_title);
        m_groupCount  = (TextView) rootView.findViewById(R.id.group_name_count);

        int m_h2_color = AppTheme.getThemeColor( m_act, R.attr.h2_color);
        m_groupCount.setTextColor( m_h2_color);

        GridView gridView = (GridView) rootView.findViewById(R.id.groupDetailGridView);

        updateAdapter();
        gridView.setAdapter(m_groupDetailCursorAdapter);
        gridView.setOnItemClickListener( tileClickListener);
        gridView.setOnItemLongClickListener(m_onItemLongClickListener);

        return rootView;
    }

    public void updateAdapter(){

        Cursor new_cursor = MyGroups.getGroupContactsCursorPseudo(m_account, m_group_id, m_search);

        if( m_groupDetailCursorAdapter == null ){
            // First time, create an adapter, cursor may still be null
            m_groupDetailCursorAdapter = new GroupDetailCursorAdapter( m_act,
                    new_cursor,
                    0,  // flags, not using
                    R.layout.group_detail_contact_tile
                    );
        }else{

            // Only update the cursor
            m_groupDetailCursorAdapter.changeCursor( new_cursor);
            m_groupDetailCursorAdapter.notifyDataSetChanged();
        }
        // Update header with title and count
        m_groupTitleTv.setText( MyGroups.getGroupTitlePseudo(m_group_id));
        mCountInGroup = new_cursor.getCount();

        m_groupCount.setText(Util.plural(mCountInGroup, "Contact"));
        // Save the cursor so it can be closed in onDestroy
        m_cursor = new_cursor;
    }

    private OnItemClickListener tileClickListener = new OnItemClickListener(){
        public void onItemClick(AdapterView<?> parent, View v, int position, long row_id){

            // Get the actual contact_id from the grid's _id
            long contact_id = SqlCipher.getATabContactId(row_id);
            Persist.setCurrentContactId(m_act, contact_id);

            if(DEBUG)
                LogUtil.log("GDF.OnItemClickListener onClick: "+SqlCipher.get( contact_id, DTab.display_name));

            mCallbacks.onContactSelected();
        }
    };

    OnItemLongClickListener m_onItemLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long _id) {

            long contact_id = SqlCipher.getATabContactId(_id);
            Persist.setCurrentContactId(m_act, contact_id);
            if(DEBUG) LogUtil.log("GDF onItemLongClickListener: "+SqlCipher.contactInfo( contact_id));

            long next_id = SqlCipher.getNextContactId( m_cursor );
            if(DEBUG) LogUtil.log("GDF onItemLongClickListener next: "+SqlCipher.contactInfo( next_id));

            mCallbacks.onLongPressContact( m_cursor);// pass back to activity
            return true;
        }
    };
}
