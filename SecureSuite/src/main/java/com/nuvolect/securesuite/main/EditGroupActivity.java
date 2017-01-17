package com.nuvolect.securesuite.main;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.util.ActionBarUtil;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.LogUtil;

/*
 * Group editor for new and existing groups.  If a group id is passed in, the members
 * of that group populate the m_members list.  Deleted users are maintained on the
 * m_deleteMemberSet.  When a devices is rotated, or a user leaves and returns to the
 * activity, the members and delete sets are saved and restored.
 */
public class EditGroupActivity extends Activity{

    private final boolean DEBUG = LogUtil.DEBUG;
    private Activity m_act;
    private int m_theme;
    private EditGroupFragment m_fragment;
    public static int m_group_id;
    public static boolean m_newGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG){
            String state = savedInstanceState==null?"null":"not null";
            LogUtil.log("EditGroupActivity onCreate savedInstanceState: "+state);
        }

        m_act = this;

        if( getIntent().hasExtra(CConst.GROUP_ID_KEY)){

            Bundle extras = getIntent().getExtras();
            m_group_id = extras.getInt(CConst.GROUP_ID_KEY);
            m_newGroup = false;
        }
        else{
            m_group_id = 0;
            m_newGroup = true;
        }

        m_theme = AppTheme.activateTheme(m_act);
        setContentView(R.layout.edit_group_activity);

        ActionBar actionBar = getActionBar();
        ActionBarUtil.setDisplayShowTitleEnabled(actionBar, true);
        ActionBarUtil.setDisplayHomeAsUpEnabled(actionBar, true);

        AppTheme.applyActionBarTheme( m_act, actionBar);

        if( savedInstanceState == null){

            m_fragment = startEditGroupFragment();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(DEBUG)LogUtil.log("EditGroupActivity onPause");

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(DEBUG)LogUtil.log("EditGroupActivity onDestroy");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(DEBUG)LogUtil.log("EditGroupActivity onResume");

        // Test against previous theme, update when changed by settings
        m_theme = AppTheme.activateWhenChanged( m_act, m_theme);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu items for use in the action bar
        getMenuInflater().inflate(R.menu.edit_group_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case android.R.id.home:
        case R.id.menu_edit_discard:
            m_act.finish();
            return true;

        case R.id.menu_edit_save:{
            m_fragment = (EditGroupFragment)
                    getFragmentManager().findFragmentByTag(CConst.EDIT_GROUP_FRAGMENT_TAG);
            m_fragment.save( m_act);
            m_act.finish();
            return true;
        }
        default:
            SharedMenu.processCmd( m_act, item);  // Just for the help command
        }
        return super.onOptionsItemSelected(item);
    }

    private EditGroupFragment startEditGroupFragment() {

        EditGroupFragment fragment = new EditGroupFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add( R.id.edit_group_container, fragment, CConst.EDIT_GROUP_FRAGMENT_TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(null);
        ft.commit();
        return fragment;
    }
}
