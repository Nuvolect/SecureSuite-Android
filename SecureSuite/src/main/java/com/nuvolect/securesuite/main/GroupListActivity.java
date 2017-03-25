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

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
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
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.MyAccounts;
import com.nuvolect.securesuite.data.MyContacts;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.license.LicenseManager;
import com.nuvolect.securesuite.license.LicensePersist;
import com.nuvolect.securesuite.util.ActionBarUtil;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.DialogAccount;
import com.nuvolect.securesuite.util.DialogConfirm;
import com.nuvolect.securesuite.util.DialogInput;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.LogUtil.LogType;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.UriUtil;
import com.nuvolect.securesuite.util.WorkerService;

import java.io.File;
import java.lang.ref.WeakReference;

//FUTURE scroll down long list, when long press delete, do not reset to top of list

/**
 * Activity for group centric list display, vs. contact centric list display.
 */
public class GroupListActivity extends Activity
        implements GroupListFragment.Callbacks,
        ContactDetailFragment.Callbacks,
        ContactEditFragment.Callbacks,
        GroupDetailFragment.Callbacks,
        DialogInput.Callbacks,
        DialogConfirm.Callbacks,
        DialogAccount.Callbacks {

    private final boolean DEBUG = LogUtil.DEBUG;
    private boolean mIsBound;
    private Messenger mService = null;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    public boolean mTwoPane;
    static Activity m_act;
    private ArrayAdapter<CharSequence> action_bar_adapter;
    private OnNavigationListener navigationListener;
    public enum GLA_RIGHT_FRAGMENT { GROUP_DETAIL, CONTACT_DETAIL, CONTACT_EDIT };

    public String m_selected_account;
    public static int m_group_id;;
    private long m_contact_id;
    private ActionBar actionBar;
    private MenuInflater m_inflater;
    private int m_theme;
    private enum MSG_ID { ADD_ACCOUNT, DELETE_ACCOUNT, SELECT_DELETE_ACCOUNT};

    /** progress dialog to show user that the import is processing. */
    private ProgressDialog m_importProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG)LogUtil.log("GLA onCreate");

        m_act = this;
        mTwoPane = false;

        // Action bar progress setup.  Needs to be called before setting the content view
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        m_theme = AppTheme.activateTheme(m_act);
        setContentView(R.layout.group_list_activity);

        // This activity is the top, DO NOT show the Up button in the action bar.
        ActionBarUtil.setDisplayHomeAsUpEnabled(m_act, false);

        // Get the current group id or if first time, set default account and group id
        SqlCipher.getInstance(m_act);
        m_group_id = Cryp.getCurrentGroup();

        if( m_group_id == 0 || m_selected_account == null){
            m_selected_account = Cryp.getCurrentAccount();
            if( m_selected_account.isEmpty()){

                m_selected_account = MyAccounts.getFirstAccount();
                Cryp.setCurrentAccount( m_selected_account);
            }
            m_group_id = MyGroups.getDefaultGroup( m_selected_account);
            Cryp.setCurrentGroup( m_group_id);
        }

        // Support for action bar pull down menu
        action_bar_adapter = ArrayAdapter.createFromResource(this, R.array.action_bar_spinner_menu,
                android.R.layout.simple_spinner_dropdown_item);

        // Action bar spinner menu callback
        navigationListener = new OnNavigationListener() {

            // List items from resource
            String[] navItems = getResources().getStringArray(R.array.action_bar_spinner_menu);

            @Override
            public boolean onNavigationItemSelected(int position, long id) {

                if(DEBUG)LogUtil.log("GLA NavigationItemSelected: "+ navItems[position]);

                // Do stuff when navigation item is selected
                switch( CConst.NavMenu.values()[ position ]){

                    case contacts: {

                        // Persist the choice so it can be restored when the user returns
                        Persist.setNavChoice( m_act, position, navItems[position]);

                        Intent i = new Intent(m_act, ContactListActivity.class);
                        startActivity(i);
                        m_act.finish();
                        break;
                    }

                    case groups:{
                        // Persist the choice so it can be restored when the user returns
                        Persist.setNavChoice( m_act, position, navItems[position]);
                        break;
                    }
                    case passwords:{

                        /**
                         * Restore the spinner such that the Password is never persisted
                         * and never shows.
                         */
                        actionBar.setSelectedNavigationItem( Persist.getNavChoice(m_act));
                        PasswordFragment f = PasswordFragment.newInstance(m_act);
                        f.start();
                        break;
                    }
                    case finder:{

                        Intent intent = new Intent(m_act, FinderActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case server:{

                        /**
                         * Restore the spinner such that the Password is never persisted
                         * and never shows.
                         */
                        actionBar.setSelectedNavigationItem( Persist.getNavChoice(m_act));
                        ServerFragment f = ServerFragment.newInstance(m_act);
                        f.start();
                        break;
                    }
                    default:
                }
                return true;
            }
        };

        actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setListNavigationCallbacks(action_bar_adapter, navigationListener);
        AppTheme.applyActionBarTheme( m_act, actionBar);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if( savedInstanceState == null){

            if ( findViewById(R.id.group_detail_container) != null) {
                // Setup for single or dual fragments depending on display size
                // The detail container view will be present only in the
                // large-screen layouts (res/values-large and res/values-sw600dp).
                // If this view is present, then the activity should be in two-pane mode.
                mTwoPane = true;
                startGroupListFragment();

                /**
                 * Restore the right fragment its persisted state, defaulting to group detail
                 */
                if( ! SqlCipher.validContactId(Persist.getCurrentContactId(m_act))){

                    Persist.setGlaRightFragment(m_act, GLA_RIGHT_FRAGMENT.GROUP_DETAIL);
                    invalidateOptionsMenu();
                }

                switch( Persist.getGlaRightFragment(getApplicationContext())){

                    default:
                    case GROUP_DETAIL:{

                        GroupListFragment fragment = startGroupListFragment();

                        // In two-pane mode, list items should be given the 'activated' state when touched.
                        fragment.setActivateOnItemClick(true);

                        startGroupDetailFragment( m_group_id);
                    }
                    break;
                    case CONTACT_DETAIL:{

                        m_contact_id = Persist.getCurrentContactId(getApplicationContext());

                        startContactDetailFragment();
                    }
                    break;
                }
            }else{
                startGroupListFragment();
            }
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
    protected void onPause() {
        super.onPause();
        if(DEBUG)LogUtil.log("GLA onPause");

        doUnbindService();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(DEBUG)LogUtil.log("GLA onResume");

        setProgressBarIndeterminateVisibility(
                Persist.getProgressBarActive( m_act ));

        // Test against previous theme, update when changed by settings
        m_theme = AppTheme.activateWhenChanged( m_act, m_theme);

        mTwoPane = findViewById(R.id.group_detail_container) != null;

        // Restore navigation to the persisted state
        actionBar.setSelectedNavigationItem( Persist.getNavChoice(m_act));

        // Starts the communications framework.
        doBindService();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if(DEBUG)LogUtil.log("GLA onDestroy");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        /**
         * When the user presses back button, force lock screen to appear
         * next time user starts the app. This also solves the problem
         * of presenting the lock screen when this activity is restarted
         * for internal reasons.
         */
        LockActivity.lockDisabled = false;
        if( LockActivity.lockCodePresent(m_act))
            Toast.makeText(m_act, "Lock Enabled", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu items for use in the action bar
        m_inflater = getMenuInflater();

        if( mTwoPane)

            switch( Persist.getGlaRightFragment(getApplicationContext())){
                case GROUP_DETAIL:
                    m_inflater.inflate(R.menu.group_list_group_detail, menu);
                    break;
                case CONTACT_DETAIL:
                    m_inflater.inflate(R.menu.group_list_contact_detail, menu);
                    break;
                case CONTACT_EDIT:
                    m_inflater.inflate(R.menu.group_list_contact_edit, menu);
                    break;
            }
        else
            m_inflater.inflate(R.menu.group_list_single_menu, menu);

        if( (LicenseManager.mIsWhitelistUser || Boolean.valueOf( m_act.getString(R.string.verbose_logging)))
                && DeveloperDialog.isEnabled()){

            MenuItem menuItem = menu.findItem(R.id.menu_developer);
            menuItem.setVisible( true );
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        SharedMenu.POST_CMD post_cmd = SharedMenu.POST_CMD.NIL;

        switch (item.getItemId()) { // Handle presses on the action bar items

            case R.id.menu_add_account:{ // search ADD_ACCOUNT

                DialogInput frag = DialogInput.newInstance( "Account name or email?",
                        "Accounts hold contacts and groups.", MSG_ID.ADD_ACCOUNT.ordinal());
                frag.show(getFragmentManager(), "dialog_input");
                break;
            }
            case R.id.menu_delete_account:{ // search DELETE_ACCOUNT

                DialogAccount frag = DialogAccount.newInstance( "Select account", MSG_ID.SELECT_DELETE_ACCOUNT.ordinal());
                frag.show(getFragmentManager(), "accountDialog");
                break;
            }
            default:
                if( mTwoPane){

                    if( m_contact_id <= 0)
                        m_contact_id = Persist.getCurrentContactId(m_act);
                    post_cmd = SharedMenu.processCmd( m_act,
                            item, m_contact_id, m_group_id, postCmdCallbacks);
                }
                else
                    post_cmd = SharedMenu.processCmd( m_act, item, m_group_id, postCmdCallbacks);
                LogUtil.log("GLA onOptionsItemSelected default: "+item.toString());
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
                startGroupListFragment();
                if( mTwoPane)
                    startGroupDetailFragment( m_group_id);
                break;
            case START_CONTACT_DETAIL:
                Persist.setGlaRightFragment(m_act, GLA_RIGHT_FRAGMENT.CONTACT_DETAIL);
                invalidateOptionsMenu();
                startContactDetailFragment();
                break;
            case START_CONTACT_EDIT:
                Persist.setGlaRightFragment(m_act, GLA_RIGHT_FRAGMENT.CONTACT_EDIT);
                invalidateOptionsMenu();
                startContactEditFragment();
                break;
            case SETTINGS_FRAG:
                SettingsFragment.startSettingsFragment(m_act, R.id.group_list_container);
                break;
            case NIL:
            case DONE:
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
                    startGroupListFragment();
                    if( mTwoPane)
                        startGroupDetailFragment( m_group_id);
                    break;
                case START_CONTACT_EDIT:
                    Persist.setGlaRightFragment(m_act, GLA_RIGHT_FRAGMENT.CONTACT_EDIT);
                    invalidateOptionsMenu();
                    startContactEditFragment();
                    break;
                case NIL:
                case DONE:
                default:
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        LogUtil.log("GLA.onActivityResult(): "+requestCode);

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
                /**
                 * Manage request in common class for all activities
                 */
                SharedMenu.sharedOnActivityResult(m_act, requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch ( requestCode){

            default:
                SharedMenu.sharedOnRequestPermissionsResult( m_act, requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onAccountSelected(String account_name) {

        m_selected_account = account_name;
        m_group_id = Cryp.getCurrentGroup();
        if( m_group_id == 0){

            m_group_id = MyGroups.getDefaultGroup( m_selected_account);
            Cryp.setCurrentGroup( m_group_id);
        }
        if( mTwoPane)
            startGroupDetailFragment( m_group_id);
    }

    /**
     * Callback method from {@link GroupListFragment.Callbacks} indicating that
     * the item with the given ID was selected.
     * Note that method is passed the row _id and not the group_id.
     */
    @Override
    public void onGroupSelected(int _id) {

        if( _id == CConst.GROUP_ALL_IN_ACCOUNT || _id == CConst.GROUP_STARRED_IN_ACCOUNT)
            m_group_id = _id;
        else
            m_group_id = MyGroups.getGroupId(_id);

        Cryp.setCurrentGroup( m_group_id);
        Persist.setGlaRightFragment( m_act, GLA_RIGHT_FRAGMENT.GROUP_DETAIL);
        invalidateOptionsMenu();
        startGroupDetailFragment( m_group_id);
    }
    /**
     * Callback method from {@link GroupDetailFragment.Callbacks} indicating that
     * the contact with the given ID was selected.
     */
    @Override
    public void onContactSelected() {

        m_contact_id = Persist.getCurrentContactId(m_act);

        // Save right fragment configuration to restore later from onCreate
        Persist.setGlaRightFragment( m_act, GLA_RIGHT_FRAGMENT.CONTACT_DETAIL);
        invalidateOptionsMenu();
        startContactDetailFragment();
    }
    /**
     * Handler of incoming messages from service.
     */
    static class IncomingHandler extends Handler {

        WeakReference<GroupListActivity> mGroupListActivity;

        public IncomingHandler(GroupListActivity incomingHandler) {
            mGroupListActivity = new WeakReference<GroupListActivity>(incomingHandler);
        }

        @Override
        public void handleMessage(Message msg) {
            if( mGroupListActivity.get() == null ){

                GroupListActivity groupListActivity = new GroupListActivity();
                groupListActivity._handleMessage( msg);
            }else
                mGroupListActivity.get()._handleMessage( msg);

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

        /*
         * Messages are sent from the server for each contact imported.
         */
            case IMPORT_CLOUD_CONTACTS_UPDATE:{//import_cloud

                CloudImportDialog.updateProgress( m_act, bundle);
                break;
            }
            case IMPORT_CLOUD_CONTACTS_COMPLETE:{

                String mainAccountImported = CloudImportDialog.getMainAccountImported();
                if( !mainAccountImported.isEmpty()){

                    LogUtil.log("GLA _handleMessage importedAccount: "+mainAccountImported);
                    Cryp.setCurrentAccount( mainAccountImported);
                    int group = MyGroups.getDefaultGroup( mainAccountImported);
                    Cryp.setCurrentGroup( group);
                    long contactId = MyContacts.getFirstContactInGroup( group);
                    Cryp.setCurrentContact( m_act, contactId);
                }

                CloudImportDialog.complete( m_act);
                break;
            }
            case REFRESH_USER_INTERFACE:{

                LogUtil.log(LogType.GLA, ""+cmd+bundle);

                if( bundle.getString(CConst.UI_TYPE_KEY).contentEquals(CConst.RECREATE)){

                    MyGroups.loadGroupMemory();
                    m_act.recreate();//FUTURE refresh fragments
                }
                else
                if(DEBUG) LogUtil.log(LogType.GLA,"UI_TYPE_KEY no match: "+bundle.getString(CConst.UI_TYPE_KEY));
            }

            default:
                if(DEBUG) LogUtil.log(LogType.GLA,"_handleMessage default: "+cmd+" "+bundle);
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
                bundle.putString(CConst.SUBSCRIBER, "GLA");
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

    /**
     * Callback from dialog fragment
     */
    @Override
    public void onInputEntered(String input_text, int msg_id) {

        if( msg_id == MSG_ID.ADD_ACCOUNT.ordinal()){  // add_account

            String newAccount = input_text.replaceAll("\\W+", "");

            if( newAccount.isEmpty() || newAccount.length() > 30){

                Toast.makeText(m_act, "Account name empty or too long", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check for duplicate account name
            String[] accounts = MyAccounts.getAccounts();

            for( String account : accounts){

                if( account.contentEquals(newAccount)){

                    Toast.makeText(m_act, "Invalid, duplicate account name", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Make new account the current account
            Cryp.setCurrentAccount( newAccount);
            Persist.setCurrentAccountType( m_act, CConst.CUSTOM_ACCOUNT);

            // Add minimum set of groups
            MyGroups.addBaseGroupsToNewAccount(m_act, newAccount);

            m_act.finish();
            m_act.startActivity(m_act.getIntent());
        }
    }

    /**
     * Callback from dialog fragment
     */
    @Override
    public void onInputCancel() {  // no action on cancel
    }

    @Override
    public void onConfirmOk(int msg_id) {

        if( msg_id == MSG_ID.DELETE_ACCOUNT.ordinal()){

            MyAccounts.deleteAccount( m_act, m_account_to_delete);
            String account = MyAccounts.getFirstAccount();
            Cryp.setCurrentAccount( account);
            Cryp.setCurrentGroup( MyGroups.getDefaultGroup(account));

            m_act.finish();
            m_act.startActivity(m_act.getIntent());
        }
    }

    @Override
    public void onConfirmCancel(int msg_id) { // no action on cancel
    }

    @Override
    public void onAccountSelectOk(String account, int msg_id) {

        if( msg_id == MSG_ID.SELECT_DELETE_ACCOUNT.ordinal()){

            if( account.contentEquals(LicensePersist.getLicenseAccount(m_act))){

                Toast.makeText(m_act, "Cannot delete account: "+account, Toast.LENGTH_LONG).show();
                return;
            }

            m_account_to_delete = account;

            DialogConfirm frag = DialogConfirm.newInstance( "Delete account: "+account+"?",
                    "All contacts and groups will be deleted along with "+account,
                    MSG_ID.DELETE_ACCOUNT.ordinal());
            frag.show(getFragmentManager(), "dialog_confirm");
        }
    }

    @Override
    public void refreshFragment(String fragment_tag) {
    }

    private GroupListFragment startGroupListFragment(){

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        GroupListFragment fragment = new GroupListFragment();
        if( fragment != null)
            fragment.notifyChanged();
        ft.replace(R.id.group_list_container, fragment, CConst.GROUP_LIST_FRAGMENT_TAG);
        ft.commit();
        return fragment;
    }

    private void startContactDetailFragment(){

        if (mTwoPane) {
            m_cdf_fragment = new ContactDetailFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.replace(R.id.group_detail_container, m_cdf_fragment, CConst.CONTACT_DETAIL_FRAGMENT_TAG);
            ft.commit();
        } else {
            // In single-pane mode, simply start the detail activity for the selected ID.
            Intent intent = new Intent(this, ContactDetailActivity.class);
            startActivity(intent);
        }
    }
    private void startContactEditFragment() {
        if (mTwoPane) {
            ContactEditFragment fragment = new ContactEditFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.replace(R.id.group_detail_container, fragment, CConst.CONTACT_EDIT_FRAGMENT_TAG);
            ft.commit();
        } else {
            // In single-pane mode, simply start the edit activity for the selected ID.
            Intent intent = new Intent(this, ContactEditActivity.class);
            startActivity(intent);
        }
    }

    private void startGroupDetailFragment(int group_id){

        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a fragment transaction.
            Cryp.setCurrentGroup( group_id);
            m_gdf_fragment = new GroupDetailFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.replace(R.id.group_detail_container, m_gdf_fragment, CConst.GROUP_DETAIL_FRAGMENT_TAG);
            ft.commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent intent = new Intent(this, GroupDetailActivity.class);
            Cryp.setCurrentGroup( group_id);
            startActivity(intent);
        }
    }

    @Override
    public void onEditContactFinish(boolean contactModified) {

        if( contactModified){

            MyGroups.loadGroupMemory();
            startContactDetailFragment();
            startGroupListFragment();
        }
    }

    /**
     * Membership of the a group has changed, refresh the list.
     */
    @Override
    public void refreshGroupList() {

        GroupListFragment frag = (GroupListFragment) getFragmentManager().findFragmentByTag(CConst.GROUP_LIST_FRAGMENT_TAG);
        if( frag != null && frag.isVisible())
            frag.notifyChanged();
    }
}