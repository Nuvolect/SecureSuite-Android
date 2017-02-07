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

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.MyContacts;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.util.ActionBarUtil;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.LogUtil.LogType;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.UriUtil;

import java.io.File;

/**
 * An activity representing a single Contact edit screen.
 */
public class ContactEditActivity extends Activity
implements ContactEditFragment.Callbacks
{

    private static final boolean DEBUG = LogUtil.DEBUG;
    Activity m_act;
    private long m_contact_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG)LogUtil.log("ContactEditActivity onCreate");

        m_act = this;
        AppTheme.activateTheme(m_act);
        setContentView(R.layout.contact_edit_activity);

        ActionBar actionBar = getActionBar();
        ActionBarUtil.setDisplayShowTitleEnabled(actionBar, false);
        ActionBarUtil.setDisplayHomeAsUpEnabled(actionBar, true);
        AppTheme.applyActionBarTheme( m_act, actionBar);

        // Start with the passed in contact or reset to a valid contact
        m_contact_id = Persist.getCurrentContactId(m_act);

        if( m_contact_id <= 0 || ! SqlCipher.validContactId( m_contact_id)){
            m_contact_id = SqlCipher.getFirstContactID();
            Persist.setCurrentContactId(m_act, m_contact_id);
        }

        if (savedInstanceState == null) {

            startContactEditFragment();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }
    @Override
    public void onPause() {
        super.onPause();
        if(DEBUG)LogUtil.log("ContactEditActivity onPause");
    }
    @Override
    public void onResume() {
        super.onResume();
        if(DEBUG)LogUtil.log("ContactEditActivity onResume");
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if(DEBUG)LogUtil.log("ContactEditActivity onDestroy");
    }
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.contact_edit_single_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        MyContacts.manageEmptyContact(m_act);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        SharedMenu.POST_CMD post_cmd = SharedMenu.POST_CMD.NIL;

        switch (item.getItemId()) {

        case android.R.id.home:
            MyContacts.manageEmptyContact(m_act);
            m_act.finish();
            return true;
        default:
            post_cmd = SharedMenu.processCmd( m_act, item, m_contact_id);
        }
        switch( post_cmd){

        case ACT_RECREATE:
            m_act.recreate();
            break;
        case REFRESH_LEFT_DEFAULT_RIGHT:{
            // Only delete will reach here in this activity, finish and show the list
            m_act.finish();
            break;
        }
        case START_CONTACT_EDIT:
            m_contact_id = Persist.getCurrentContactId(m_act);
            startContactEditFragment();
            break;
        case NIL:
        case DONE:
        case SETTINGS_FRAG:
            break;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        LogUtil.log("CEA.onActivityResult()");

        switch( requestCode ){

        case CConst.BROWSE_IMPORT_PHOTO_ACTION:{

            if ( resultCode == RESULT_OK && data != null && data.getData() != null) {

                Uri _uri = data.getData();
                String path = UriUtil.getPathFromUri(m_act, _uri);

                boolean fileExists = path != null && new File(path).exists();
                boolean isFile = path != null && new File(path).isFile();
                boolean goodToGo = fileExists && isFile;

                if( goodToGo ){

                    ContactEditFragment fragment = (ContactEditFragment) getFragmentManager().findFragmentByTag(CConst.CONTACT_EDIT_FRAGMENT_TAG);
                    fragment.readPhoto( path );
                }else {
                    Toast.makeText(m_act, "Image import failed", Toast.LENGTH_SHORT).show();
                    LogUtil.log( LogType.CONTACT_EDIT, "image path is null");
                }
            }
            break;
        }
        default:
            if(DEBUG)LogUtil.log("ContactEditActivity.onActivityResult default");
        }
    }

    @Override
    public void onEditContactFinish(boolean contactModified) {

        m_act.finish();
    }

    private void startContactEditFragment() {

        ContactEditFragment fragment = new ContactEditFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace( R.id.contact_edit_container, fragment, CConst.CONTACT_EDIT_FRAGMENT_TAG);
        ft.commit();
    }
}
