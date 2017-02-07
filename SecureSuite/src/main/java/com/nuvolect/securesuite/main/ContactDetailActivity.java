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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.util.ActionBarUtil;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.WorkerService;

import java.lang.ref.WeakReference;

/**
 * An activity representing a single Person detail screen. This activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link ContactListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link ContactDetailFragment}.
 */
public class ContactDetailActivity extends Activity
implements ContactDetailFragment.Callbacks {

    private final boolean DEBUG = LogUtil.DEBUG;
    private static Activity m_act;
    private long m_contact_id;
    private boolean wasPaused = false;
    private Messenger mService = null;
    private boolean mIsBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG)LogUtil.log("ContactDetailActivity onCreate");

        m_act = this;
        wasPaused = false;
        AppTheme.activateTheme(m_act);
        setContentView(R.layout.contact_detail_activity);

        ActionBar actionBar = getActionBar();
        ActionBarUtil.setDisplayShowTitleEnabled(actionBar, false);
        ActionBarUtil.setDisplayHomeAsUpEnabled(actionBar, true);
        AppTheme.applyActionBarTheme( m_act, actionBar);

        m_contact_id = Persist.getCurrentContactId(m_act);

        if( m_contact_id <= 0 || ! SqlCipher.validContactId( m_contact_id)){
            m_contact_id = SqlCipher.getFirstContactID();
            Persist.setCurrentContactId(m_act, m_contact_id);
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {

            startContactDetailFragment();
        }
    }
    @Override
    public void onStart() {
        super.onStart();

        // Start the communications framework.
        doBindService();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Stop the communications framework.
        doUnbindService();
    }
    @Override
    public void onPause() {
        super.onPause();
        if(DEBUG)LogUtil.log("ContactDetailActivity onPause");

        /*
         * Manage the refresh when returning from the edit activity.
         * Refresh the detail activity after it has been edited.
         */
        wasPaused = true;
    }
    @Override
    public void onResume() {
        super.onResume();
        if(DEBUG)LogUtil.log("ContactDetailActivity onResume");

        if( wasPaused){

            // Contact may have been deleted, reset id if necessary and exit back to list
            if( m_contact_id <= 0 || ! SqlCipher.validContactId( m_contact_id)){
                m_contact_id = SqlCipher.getFirstContactID();
                Persist.setCurrentContactId(m_act, m_contact_id);
                MyGroups.loadGroupMemory();
                m_act.finish();
            }

            startContactDetailFragment();
            wasPaused = false;
        }
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if(DEBUG)LogUtil.log("ContactDetailActivity onDestroy");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.contact_detail_single_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        SharedMenu.POST_CMD post_cmd = SharedMenu.POST_CMD.NIL;

        switch (item.getItemId()) {

        case android.R.id.home:
            m_act.finish();
            return true;

        default:
            post_cmd = SharedMenu.processCmd( m_act, item, m_contact_id);
        }

        switch( post_cmd){

        case ACT_RECREATE:
            m_act.recreate();
            break;
        case REFRESH_LEFT_DEFAULT_RIGHT:
            // Only delete will reach here in this activity, finish and show the list
            m_act.finish();
            break;
        case START_CONTACT_EDIT:
            m_contact_id = Persist.getCurrentContactId(m_act);
            startContactEditActivity();
            break;
        case NIL:
        case DONE:
        default:
        }
        return super.onOptionsItemSelected(item);
    }
    /**
     * Handler of incoming messages from service.
     */
    static class IncomingHandler extends Handler {

        WeakReference<ContactDetailActivity> mContactDetailActivity;

        public IncomingHandler(ContactDetailActivity incomingHandler) {
            mContactDetailActivity = new WeakReference<ContactDetailActivity>(incomingHandler);
        }

        @Override
        public void handleMessage(Message msg) {
            if( mContactDetailActivity.get() == null ){

                ContactDetailActivity activity = new ContactDetailActivity();
                activity._handleMessage(msg);
            }else
                mContactDetailActivity.get()._handleMessage(msg);

            super.handleMessage(msg);
        }
    }

    /**
     * This class and method receives message commands and the message handler
     * on a separate thread. You can enter messages from any thread.
     */
    public void _handleMessage(Message msg) {

        Bundle bundle = msg.getData();
        WorkerService.WorkTask cmd = WorkerService.WorkTask.values()[msg.what];

        switch (cmd) {

        /*
         * Messages are sent from the server for each contact imported.
         */
            case IMPORT_CLOUD_CONTACTS_UPDATE:{//import_cloud

                break;
            }
        /*
         * A final message is sent when the import is complete.
         */
            case IMPORT_CLOUD_CONTACTS_COMPLETE:{

                Persist.setImportInProgress(m_act, 0);
                Persist.setProgressBarActive( m_act, false );
                m_act.setProgressBarIndeterminateVisibility( false );

                break;
            }
            case REFRESH_USER_INTERFACE:{

                if(DEBUG) LogUtil.log(LogUtil.LogType.CDA,""+cmd+bundle);

                if( bundle.getString(CConst.UI_TYPE_KEY).contentEquals(CConst.CONTACTS)){

                    m_act.recreate();//FUTURE refresh fragments
//                    startContactDetailFragment();// produces error: Activity has been destroyed
                }
            }

            default:
                if(DEBUG) LogUtil.log(LogUtil.LogType.CDA,"default: "+cmd+" "+bundle);
                break;
        }
    }
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    Messenger mMessenger = new Messenger(new IncomingHandler( null));

    /**
     * Class for interacting with the main interface of WorkerService.
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);

            if(DEBUG) LogUtil.log(LogUtil.LogType.CDA,"onServiceConnected: "+className.getClassName());

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null, WorkerService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;

            // As part of the sample, tell the user what happened.
            Toast.makeText(getApplicationContext(), "Service disconnected", Toast.LENGTH_SHORT).show();
        }
    };
    /**
     * Starts the communications framework.
     */
    void doBindService() {

        // Establish a connection with the service. We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent( this, WorkerService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    /**
     * Stops the communications framework.
     */
    void doUnbindService() {

        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, WorkerService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private void startContactDetailFragment(){

        ContactDetailFragment frag = new ContactDetailFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace( R.id.contact_detail_container, frag, CConst.CONTACT_DETAIL_FRAGMENT_TAG);
        ft.commit();
    }
    private void startContactEditActivity() {

        Intent i = new Intent(getApplicationContext(), ContactEditActivity.class);
        startActivity(i);
    }
    /**
     * No need to do anything.  The group list will never be shown when this activity is running.
     * But all activities implementing ContactDetailFragment must implement it.
     */
    @Override
    public void refreshGroupList() {
    }
}
