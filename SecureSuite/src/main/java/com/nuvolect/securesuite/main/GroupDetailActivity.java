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

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
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
import com.nuvolect.securesuite.util.ActionBarUtil;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.LogUtil.LogType;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.UriUtil;
import com.nuvolect.securesuite.util.WorkerService;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * An activity representing a single Person detail screen. This activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link ContactListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link ContactDetailFragment}.
 */
public class GroupDetailActivity extends Activity
        implements GroupDetailFragment.Callbacks, ContactDetailFragment.Callbacks {

    private final boolean DEBUG = false;
    private static Activity m_act;
    private int m_group_id;
    private int m_theme;
    //    private boolean wasPaused = false;
    private boolean mIsBound;
    private Messenger mService = null;

    /** progress dialog to show user that the import is processing. */
    private ProgressDialog m_importProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_act = this;
        //        wasPaused = false;

        if(DEBUG)LogUtil.log("GroupDetailActivity onCreate");

        m_theme = AppTheme.activateTheme(m_act);
        setContentView(R.layout.group_detail_activity);

        // Show the Up button in the action bar.
        ActionBarUtil.setDisplayHomeAsUpEnabled(m_act, true);
        AppTheme.applyActionBarTheme( m_act, getActionBar());

        // Get the group id, check if it is valid and if not assign a default
        m_group_id = Cryp.getCurrentGroup();
        if( ! MyGroups.validGroupIdPseudo(m_group_id)){

            String account = Cryp.getCurrentAccount();
            m_group_id = MyGroups.getDefaultGroup(account);
            Cryp.setCurrentGroup( m_group_id);
        }

        // Unless android magic happens, start the main fragment
        if (savedInstanceState == null) {

            startGroupDetailFragment( m_group_id);
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
        if(DEBUG)LogUtil.log("GroupDetailActivity onPause");
        /*
         * Manage the refresh when returning from the edit activity.
         * Refresh the detail activity after it has been edited.
         */
        //        wasPaused = true;
    }
    @Override
    public void onResume() {
        super.onResume();
        if(DEBUG)LogUtil.log("GroupDetailActivity onResume");

        setProgressBarIndeterminateVisibility(
                Persist.getProgressBarActive( m_act ));

        // Test against previous theme, update when changed by settings
        m_theme = AppTheme.activateWhenChanged( m_act, m_theme);

        //        if( wasPaused){

        m_group_id = Cryp.getCurrentGroup();

        // Group may have been deleted, reset id if necessary and exit back to list
        if( ! MyGroups.validGroupIdPseudo( m_group_id)){
            String account = Cryp.getCurrentAccount();
            m_group_id = MyGroups.getDefaultGroup(account);
            Cryp.setCurrentGroup( m_group_id);
            //                MyGroups.loadGroupMemory();
            //                m_act.finish();
            //            }

            startGroupDetailFragment(m_group_id);
            //            wasPaused = false;
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(DEBUG)LogUtil.log("GroupDetailActivity onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.group_detail_single_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        SharedMenu.POST_CMD post_cmd = SharedMenu.POST_CMD.NIL;

        switch (item.getItemId()) {

            case android.R.id.home:
                m_act.finish();// Go to the previous activity on the stack
                return true;

            default:
                post_cmd = SharedMenu.processCmd( m_act, item, m_group_id, postCmdCallbacks);
                LogUtil.log("GroupDetailActivity onOptionsItemSelected default: "+item.toString());
        }
        doPostCommand( post_cmd);

        return super.onOptionsItemSelected(item);
    }

    private void doPostCommand( SharedMenu.POST_CMD post_cmd){

        switch( post_cmd){

            case ACT_RECREATE:
                m_act.recreate();
                break;
            case REFRESH_LEFT_DEFAULT_RIGHT:
                startGroupDetailFragment(m_group_id);
                break;
            case START_CONTACT_EDIT:
                startContactEditFragment( Persist.getCurrentContactId(m_act));
                break;
            case START_CONTACT_DETAIL: {
                Intent detailIntent = new Intent(m_act, ContactDetailActivity.class);
                startActivity(detailIntent);
                break;
            }
            case NIL:
            case DONE:
            case SETTINGS_FRAG:
            default:
        }
    }

    SharedMenu.PostCmdCallbacks postCmdCallbacks = new SharedMenu.PostCmdCallbacks() {
        @Override
        public void postCommand(SharedMenu.POST_CMD post_cmd) {

            doPostCommand( post_cmd);
        }
    };

    @Override
    public void onLongPressContact(net.sqlcipher.Cursor cursor) {

        LongPressContact.longPress(m_act, cursor, longPressContactCallbacks);
    }

    LongPressContact.LongPressContactCallbacks longPressContactCallbacks  = new LongPressContact.LongPressContactCallbacks(){

        public void postCommand(SharedMenu.POST_CMD post_cmd) {

            switch( post_cmd){

                case ACT_RECREATE:
                    m_act.recreate();
                    break;
                case REFRESH_LEFT_DEFAULT_RIGHT:

                    startGroupDetailFragment( m_group_id);
                    break;
                case START_CONTACT_EDIT:
                    startContactEditFragment( Persist.getCurrentContactId(m_act));
                    break;
                case NIL:
                case DONE:
                default:
            }
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

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
//            case CConst.IMPORT_VCARD_BROWSE_ACTION:{
//
//                if ( resultCode == RESULT_OK) {
//
//                    Bundle activityResultBundle = data.getExtras();
//                    String path = activityResultBundle.getString(CConst.IMPORT_VCF_PATH);
//
//                    new ImportVcardAsync( ).execute(path);
//                }
//                break;
//            }
//            case CConst.IMPORT_SINGLE_CONTACT_PICKER:{
//
//                if ( resultCode == RESULT_OK) {
//
//                    Uri result = data.getData();
//                    String id = result.getLastPathSegment();
//                    LogUtil.log("Cloud contact ID: " + id);
//                    boolean success = true;
//
//                    if( id == null || id.isEmpty())
//                        success = false;
//                    else{
//
//                        long cloud_contact_id = Long.valueOf( id );
//                        success = ImportContacts.importSingleContact(m_act, cloud_contact_id);
//                    }
//                    if( ! success)
//                        Toast.makeText(m_act, "Contact import error", Toast.LENGTH_SHORT).show();
//                }
//                break;
//            }
            default:
                /**
                 * Manage request in common class for all activities
                 */
                SharedMenu.sharedOnActivityResult(m_act, requestCode, resultCode, data);
        }
    }

//    private class ImportVcardAsync extends AsyncTask<String, Integer, Long>
//    {
//        public ImportVcardAsync(){
//
//            m_importProgressDialog = new ProgressDialog(m_act);
//        }
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//
//            m_importProgressDialog.setMessage("Import starting...");
//            m_importProgressDialog.show();
//        }
//        @Override
//        protected Long doInBackground(String...paths) {
//
//            String path = paths[0];
//            ImportVcard.ImportProgressCallbacks callbacks = new ImportVcard.ImportProgressCallbacks() {
//                @Override
//                public void progressReport(int importProgress) {
//
//                    publishProgress( importProgress );
//                }
//            };
//            long contact_id = ImportVcard.importVcf(m_act, path, m_group_id, callbacks);
//
//            return contact_id;
//        }
//
//        @Override
//        protected void onProgressUpdate(Integer... values) {
//            super.onProgressUpdate(values);
//
//            int vcardsImported = values[0];
//
//            if( m_importProgressDialog == null ) {
//                m_importProgressDialog = new ProgressDialog(m_act);
//                m_importProgressDialog.show();
//            }
//
//            if( m_importProgressDialog != null && m_importProgressDialog.isShowing())
//                m_importProgressDialog.setMessage("Import progress: " + vcardsImported);
//        }
//
//        @Override
//        protected void onPostExecute(Long contact_id) {
//
//            if( m_importProgressDialog!= null && m_importProgressDialog.isShowing())
//                m_importProgressDialog.dismiss();
//
//            if( contact_id > 0)
//                Toast.makeText(m_act, "Import complete", Toast.LENGTH_LONG).show();
//            else
//                Toast.makeText(m_act, "Import failed", Toast.LENGTH_LONG).show();
//            m_act.setProgressBarIndeterminateVisibility( false );
//
//            startGroupDetailFragment( m_group_id);
//        }
//    }
    /**
     * Handler of incoming messages from service.
     */
    static class IncomingHandler extends Handler {

        WeakReference<GroupDetailActivity> mGroupDetailActivity;

        public IncomingHandler(GroupDetailActivity incomingHandler) {
            mGroupDetailActivity = new WeakReference<GroupDetailActivity>(incomingHandler);
        }

        @Override
        public void handleMessage(Message msg) {
            if( mGroupDetailActivity.get() == null ){

                GroupDetailActivity activity = new GroupDetailActivity();
                activity._handleMessage(msg);
            }else
                mGroupDetailActivity.get()._handleMessage( msg);

            super.handleMessage(msg);
        }
    }

    IncomingHandler mHandler = new IncomingHandler( this );

    /**
     * This class and method receives message commands and the message handler
     * on a separate thread. You can enter messages from any thread.
     * @param msg
     */
    public void _handleMessage(Message msg) {

        Bundle bundle = msg.getData();
        WorkerService.WorkTask cmd = WorkerService.WorkTask.values()[msg.what];

        switch (cmd) {

            case IMPORT_CLOUD_CONTACTS_COMPLETE:{

                m_act.recreate();
                break;
            }
            case REFRESH_USER_INTERFACE:{

                LogUtil.log(LogType.GDA, ""+cmd+bundle);

                if( bundle.getString(CConst.UI_TYPE_KEY).contentEquals(CConst.CONTACTS)){

                    MyGroups.loadGroupMemory();
                    m_act.recreate();//FUTURE refresh fragments
                }
            }

            default:
                break;
        }
    }
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler( null));

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

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Bundle bundle = new Bundle();
                bundle.putString(CConst.SUBSCRIBER, "GDA");
                Message msg = Message.obtain(null, WorkerService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                msg.setData( bundle);
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
    private String m_account_to_delete;
    private ContactDetailFragment m_cdf_fragment;
    private GroupDetailFragment m_gdf_fragment;
    /**
     * Starts the communications framework.
     */
    void doBindService() {

        // Establish a connection with the service. We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent( getApplicationContext(), WorkerService.class), mConnection, Context.BIND_AUTO_CREATE);
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
    private void startContactEditFragment(long contact_id) {

        // In single-pane mode, simply start the edit activity for the selected ID.
        Intent intent = new Intent(this, ContactEditActivity.class);
        startActivity(intent);
    }

    /**
     * Start the GroupDetailFragment.  If it is already running it will go
     * through an onPause - onResume cycle.
     * @param group_id
     */
    private void startGroupDetailFragment(int group_id){

        Cryp.setCurrentGroup( group_id);
        GroupDetailFragment fragment = new GroupDetailFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
//        ft.add(R.id.group_detail_container, fragment);
        ft.replace(R.id.group_detail_container, fragment);
        ft.commit();
    }

    /**
     * User selected a contact from the group details.
     */
    @Override
    public void onContactSelected() {

        Intent detailIntent = new Intent(m_act, ContactDetailActivity.class);
        startActivity(detailIntent);
    }
    /**
     * No need to do anything.  The group list will never be shown when this activity is running.
     * But all activities implementing ContactDetailFragment must implement it.
     */
    @Override
    public void refreshGroupList() {
    }
}
