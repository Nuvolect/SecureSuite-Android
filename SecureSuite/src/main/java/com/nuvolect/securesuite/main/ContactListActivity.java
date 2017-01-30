package com.nuvolect.securesuite.main;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.BackupRestore;
import com.nuvolect.securesuite.data.ImportVcard;
import com.nuvolect.securesuite.data.MyContacts;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.license.LicenseManager;
import com.nuvolect.securesuite.license.LicensePersist;
import com.nuvolect.securesuite.license.LicenseUtil;
import com.nuvolect.securesuite.util.ActionBarUtil;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.FileBrowserDbRestore;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.LogUtil.LogType;
import com.nuvolect.securesuite.util.Passphrase;
import com.nuvolect.securesuite.util.PermissionUtil;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.UriUtil;
import com.nuvolect.securesuite.util.Util;
import com.nuvolect.securesuite.util.WorkerService;
import com.nuvolect.securesuite.webserver.CrypServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;

/**
 * An activity representing a list of Contacts. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link ContactDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ContactListFragment} and the item details (if present) is a
 * {@link ContactDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link ContactListFragment.Callbacks} interface to listen for item selections.
 */
public class ContactListActivity extends Activity
        implements ContactListFragment.Callbacks,
        ContactEditFragment.Callbacks,
        ContactDetailFragment.Callbacks {

    private final static boolean DEBUG = LogUtil.DEBUG;
    public boolean mTwoPane; //Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
    private ContactListFragment m_clf_fragment;
    private static Activity m_act;
    private static Context m_ctx;

    private ArrayAdapter<CharSequence> adapter;
    private OnNavigationListener navigationListener;
    private Messenger mService = null;
    private boolean mIsBound;
    private long m_contact_id;
    private int m_theme;
    private Bundle m_savedInstanceState;
    private static String mNewDbPassphrase;
    private static String mNewDbPath;
    private ActionBar actionBar;

    //    IncomingHandler mHandler = new IncomingHandler( this );
    private static Handler mainHandler = new Handler();
    public static boolean m_appUpgraded = false;
    private String m_pendingImportSingleContactId;

    //
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG){
            String state = savedInstanceState==null?"null":"not null";
            LogUtil.log("ContactListActivity onCreate savedInstanceState: "+state);
        }

        m_act = this;
        m_ctx = getApplicationContext();
        m_savedInstanceState = savedInstanceState;

        mTwoPane = false;

        // Action bar progress setup.  Needs to be called before setting the content view
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        m_theme = AppTheme.activateTheme(m_act);
        setContentView(R.layout.contact_list_activity);

        /**
         * Kick off the license manager.  Among other tasks, the license manager captures
         * a license account.  The license account is used as part of the database encryption key.
         * Consequently, do not initialize the database prior to capturing the license account.
         */
        LicenseManager.getInstance(m_act).checkLicense(m_act, mLicenseManagerListener);
    }

    LicenseManager.LicenseCallbacks mLicenseManagerListener = new LicenseManager.LicenseCallbacks(){

        @Override
        public void licenseResult(LicenseManager.LicenseResult license) {

            if(DEBUG) LogUtil.log("License result: "+license.toString());
            LicensePersist.setLicenseResult(m_ctx, license);

            switch ( license) {
                case NIL:
                    break;
                case REJECTED_TERMS:
                    m_act.finish();
                    break;
                case WHITELIST_USER:
                case PREMIUM_USER:{
                    SqlCipher.getInstance(m_ctx);
                    if (LockActivity.lockDisabled || ! LockActivity.lockCodePresent(m_ctx))
                        startGui();
                    else {
                        Intent i = new Intent(getApplicationContext(), LockActivity.class);
                        i.putExtra(CConst.VALIDATE_LOCK_CODE, Cryp.getLockCode(m_ctx));
                        startActivityForResult(i, CConst.VALIDATE_LOCK_CODE_ACTION);
                    }
                    break;
                }
                default:
                    break;
            }

            if( CrypServer.isServerEnabled()) {

                CrypServer.enableServer( m_act, true );
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if(DEBUG)LogUtil.log("ContactListActivity onResume");

        setProgressBarIndeterminateVisibility( Persist.getProgressBarActive( m_ctx ));

        m_theme = AppTheme.activateWhenChanged( m_act, m_theme);

        mTwoPane = findViewById(R.id.contact_detail_container) != null;

//         Restore navigation to the persisted state
        if( actionBar != null)
            actionBar.setSelectedNavigationItem( Persist.getNavChoice(m_act));

        if( m_clf_fragment == null)
            m_clf_fragment = (ContactListFragment) getFragmentManager()
                    .findFragmentByTag(CConst.CONTACT_LIST_FRAGMENT_TAG);

        /*
         * If the import is running, restore the progress dialog.  The dialog is required
         * to not only show progress, but to block the user from using the app until
         * the import is completed.
         */
        if( Persist.getImportInProgress(m_act) > 0)//import_cloud
            CloudImportDialog.cloudImportProgressDialog( m_act);

//        CustomDialog.rateThisApp(m_act, false);// testing == false
//        CustomDialog.makeDonation(m_act, false);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Start the communications framework.
        doBindService();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Stop the communications framework.
        doUnbindService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(DEBUG)LogUtil.log("ContactListActivity onPause");

        CloudImportDialog.dismissProgressDialog( m_act );
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(DEBUG)LogUtil.log("ContactListActivity onDestroy");

        CloudImportDialog.dismissProgressDialog( m_act );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if(DEBUG) LogUtil.log("CLA.onBackPressed");
        /**
         * When the user presses back button, force lock screen to appear
         * next time user starts the app. This also solves the problem
         * of presenting the lock screen when this activity is restarted
         * for internal reasons.
         */
        // Make sure the database is present to solve odd java.lang.NullPointerException in sqlCipher.getCryp
        SqlCipher.getInstance(m_act);

        LockActivity.lockDisabled = false;
        if( LockActivity.lockCodePresent(m_act))
            Toast.makeText(m_act, "Lock Enabled", Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Start the GUI, method assumes that a master account is already set
     */
    private void startGui(){

        SqlCipher.getInstance(m_ctx);
        /**
         * Detect app upgrade and provide a placeholder for managing upgrades, database changes, etc.
         */
        m_appUpgraded = LicenseUtil.appUpgraded(m_act);

        if( m_appUpgraded ) {

            Toast.makeText(getApplicationContext(), "Application upgraded", Toast.LENGTH_LONG).show();

            // Execute upgrade methods
        }

        // Set default settings
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        // Set the progress bar to off, otherwise some devices will default to on
        setProgressBarIndeterminateVisibility( false );

        // Load group data into memory, used for group titles and people counts
        MyGroups.loadGroupMemory();

        boolean isFirstTime = Persist.isStartingUp(m_ctx);
        if(isFirstTime){

            String account = LicensePersist.getLicenseAccount(m_ctx);
            Cryp.setCurrentAccount( account);
            MyGroups.addBaseGroupsToNewAccount(m_ctx, account);
            Cryp.setCurrentGroup( MyGroups.getDefaultGroup(account));

            try {
                // Import a default contact when starting first time
                InputStream vcf = getResources().getAssets().open(CConst.APP_VCF);
                ImportVcard.importVcf(m_ctx, vcf, Cryp.getCurrentGroup());

            } catch (IOException e) {
                LogUtil.logException(ContactListActivity.class, e);
            }

            // First time, request phone management access
            if( ! hasPermission( READ_PHONE_STATE))
                PermissionUtil.requestReadPhoneState(m_act, CConst.CALLER_ID_REQUEST_READ_PHONE_STATE);
        }

        // Support for action bar pull down menu
        adapter = ArrayAdapter.createFromResource(this, R.array.action_bar_spinner_menu,
                android.R.layout.simple_spinner_dropdown_item);

        // Action bar spinner menu callback
        navigationListener = new OnNavigationListener() {

            // List items from resource
            String[] navItems = getResources().getStringArray(R.array.action_bar_spinner_menu);

            @Override
            public boolean onNavigationItemSelected(int position, long id) {

                if(DEBUG)LogUtil.log("ContactListActivity NavigationItemSelected: "+ navItems[position]);

                // Do stuff when navigation item is selected
                switch( CConst.NavMenu.values()[ position ]){

                    case contacts:{

                        // Persist the navigation selection for fragments to pick up
                        Persist.setNavChoice( m_act, position, navItems[position]);

                        break;
                    }

                    case groups:{

                        // Persist the navigation selection for fragments to pick up
                        Persist.setNavChoice( m_act, position, navItems[position]);

                        // Dispatch to the main group list activity
                        Intent intent = new Intent(m_act, GroupListActivity.class);
                        startActivity(intent);

                        // Remove this activity from the stack
                        // Group list is the only activity on the stack
                        m_act.finish();

                        break;
                    }
                    case passwords:{

                        /**
                         * Restore the spinner such that the Server is never persisted
                         * and never shows.
                         */
                        actionBar.setSelectedNavigationItem( Persist.getNavChoice(m_act));
                        PasswordFragment f = PasswordFragment.newInstance(m_act);
                        f.start();
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
        ActionBarUtil.setNavigationMode(actionBar, ActionBar.NAVIGATION_MODE_LIST);
        ActionBarUtil.setDisplayShowTitleEnabled(actionBar, false);
        ActionBarUtil.setListNavigationCallbacks(actionBar, adapter, navigationListener);
        AppTheme.applyActionBarTheme( m_act, actionBar);

        // Start with the previous contact or reset to a valid contact
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
        if( isFirstTime || m_savedInstanceState == null){

            if (findViewById(R.id.contact_detail_container) != null) {
                // Setup for single or dual fragments depending on display size
                // The detail container view will be present only in the large-screen layouts
                // (res/values-large and res/values-sw600dp). If this view is present, then the
                // activity should be in two-pane mode.
                mTwoPane = true;

                m_clf_fragment = startContactListFragment();

                // In two-pane mode, list items should be given the 'activated' state when touched.
                m_clf_fragment.setActivateOnItemClick(true);

                // In two-pane mode, show the detail view in this activity by
                // adding or replacing the detail fragment using a fragment transaction.
                startContactDetailFragment();

            }else{
                mTwoPane = false;
                m_clf_fragment = startContactListFragment();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        if( mTwoPane){
            Fragment editFrag = getFragmentManager().findFragmentByTag(CConst.CONTACT_EDIT_FRAGMENT_TAG);
            if( editFrag != null && editFrag.isVisible()){
                inflater.inflate(R.menu.contact_list_contact_edit, menu);
                LogUtil.log("=============CLA.onOptionsMenu: contact_list_contact_edit");
            }
            else{

                inflater.inflate(R.menu.contact_list_contact_detail, menu);
                LogUtil.log("=============CLA.onOptionsMenu: contact_list_contact_detail");
            }
        }
        else{

            inflater.inflate(R.menu.contact_list_single_menu, menu);
            LogUtil.log("=============CLA.onOptionsMenu: contact_list_single_menu");
        }

        if( (LicenseManager.mIsWhitelistUser || Boolean.valueOf( m_act.getString(R.string.verbose_debug)))
                && DeveloperDialog.isEnabled()){

            Util.showMenu( menu, R.id.menu_developer );
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        SharedMenu.POST_CMD post_cmd = SharedMenu.POST_CMD.NIL;

        switch (item.getItemId()) { // Handle presses on the action bar items

            case android.R.id.home:{

                if(DEBUG) LogUtil.log("CLA.onOptionItemSelect home button");
                break;
            }
            case R.id.menu_restore_from_storage:{

                if( hasPermission( READ_EXTERNAL_STORAGE)){
                    ConfirmRestoreDatabaseDialogFragment dialog = new ConfirmRestoreDatabaseDialogFragment();
                    dialog.show( getFragmentManager(), "ConfirmRestoreTag");
                }else{
                    PermissionUtil.requestReadExternalStorage(m_act, CConst.RESTORE_CONTACTS_DATABASE);
                }
                return true;
            }
            default:
                post_cmd = SharedMenu.processCmd( m_act, item, m_contact_id, postCmdCallbacks);
        }

        doPostCommand( post_cmd);

        return super.onOptionsItemSelected(item);
    }

    private void doPostCommand( SharedMenu.POST_CMD post_cmd){

        switch( post_cmd){
            case ACT_RECREATE:
                m_act.recreate();
                break;
            case DONE:
                break;
            case NIL:
                break;
            case REFRESH_LEFT_DEFAULT_RIGHT:
                startContactListFragment();
                invalidateOptionsMenu();
                if( mTwoPane)
                    startContactDetailFragment();
                break;
            case SETTINGS_FRAG:
                break;
            case START_CONTACT_DETAIL:
                startContactDetailFragment();
                break;
            case START_CONTACT_EDIT:
                m_contact_id = Persist.getCurrentContactId(m_act);
                startContactEditFragment();
                break;
        }
    }

    SharedMenu.PostCmdCallbacks postCmdCallbacks = new SharedMenu.PostCmdCallbacks() {
        @Override
        public void postCommand(SharedMenu.POST_CMD post_cmd) {

            doPostCommand( post_cmd);
        }
    };

    @Override
    public void onLongPressContact(net.sqlcipher.Cursor cursor ) {

        LongPressContact.longPress(m_act, cursor, longPressContactCallbacks);
    }

    LongPressContact.LongPressContactCallbacks longPressContactCallbacks  = new LongPressContact.LongPressContactCallbacks(){

        public void postCommand(SharedMenu.POST_CMD post_cmd) {

            switch( post_cmd){

                case ACT_RECREATE:
                    m_act.recreate();
                    break;
                case REFRESH_LEFT_DEFAULT_RIGHT:
                    startContactListFragment();
                    if( mTwoPane)
                        startContactDetailFragment();
                    break;
                case START_CONTACT_EDIT:
                    m_contact_id = Persist.getCurrentContactId(m_act);
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

        LogUtil.log("CLA.onActivityResult() requestCode: "+requestCode);

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
            case CConst.BROWSE_RESTORE_FOLDER_ACTION:{

                if( data == null)
                    break;
                Bundle activityResultBundle = data.getExtras();
                if( activityResultBundle == null)
                    break;

                mNewDbPath = activityResultBundle.getString(CConst.RESTORE_BACKUP_PATH);

                RestoreDatabaseDialogFragment dialog = new RestoreDatabaseDialogFragment();
                dialog.show( getFragmentManager(), "RestoreTag");
                break;
            }
            case CConst.VALIDATE_LOCK_CODE_ACTION:{

                if( resultCode == RESULT_OK)
                    startGui();
                else
                    finish();
                break;
            }
            case CConst.VALIDATE_LOCK_CODE_TEST_ACTION:{

                if( resultCode == RESULT_OK)
                    Toast.makeText(m_act, "Lock code validated", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(m_act, "Lock code validation failed", Toast.LENGTH_SHORT).show();
                break;
            }
            case CConst.CHANGE_LOCK_CODE_TEST_ACTION:{

                if( resultCode == RESULT_OK){

                    Bundle activityResultBundle = data.getExtras();
                    String lockCode = activityResultBundle.getString(CConst.CHANGE_LOCK_CODE);
                    Cryp.setLockCode(m_act, lockCode);
                    if( lockCode.isEmpty())
                        Toast.makeText(m_act, "Lock code cleared", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(m_act, "Lock code changed", Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(m_act, "Lock code change failed", Toast.LENGTH_SHORT).show();
                break;
            }
            case CConst.NO_ACTION:
                break;
            default:
                /**
                 * Manage request in common class for all activities
                 */
                SharedMenu.sharedOnActivityResult(m_act, requestCode, resultCode, data);
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean hasPermission(String perm) {
        return(ContextCompat.checkSelfPermission(this, perm)== PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch ( requestCode){

            case CConst.RESTORE_CONTACTS_DATABASE:{

                ConfirmRestoreDatabaseDialogFragment dialog = new ConfirmRestoreDatabaseDialogFragment();
                dialog.show( getFragmentManager(), "ConfirmRestoreTag");
            }

            default:
                SharedMenu.sharedOnRequestPermissionsResult( m_act, requestCode, permissions, grantResults);
        }
    }

    public static class ConfirmRestoreDatabaseDialogFragment extends DialogFragment {

        public ConfirmRestoreDatabaseDialogFragment(){ }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Do you want to restore a database?");
            builder.setMessage(
                    "Your current database will be lost. "
                            +"You will be asked to select the database to restore "
                            +"followed by a passphrase.");

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {

                    // Kickoff a browser activity here.
                    // When user selects folder, onActivityResult called with the result.
                    Intent intent = new Intent();
                    intent.setClass( m_act, FileBrowserDbRestore.class);
                    getActivity().startActivityForResult(intent, CConst.BROWSE_RESTORE_FOLDER_ACTION);
                }
            })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Toast.makeText(m_act, "Action cancelled", Toast.LENGTH_SHORT).show();
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    public static class RestoreDatabaseDialogFragment extends DialogFragment {

        private EditText m_passphraseEt;
        private AlertDialog.Builder builder;

        public RestoreDatabaseDialogFragment(){ }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Please enter the passphrase");

            m_passphraseEt = new EditText(m_act);
            builder.setView(m_passphraseEt);

            builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {

                    //Do nothing here because we override this button later to change the close behavior.
                    //However, we still need this because on older versions of Android unless we
                    //pass a handler the button doesn't get instantiated
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Toast.makeText(m_act, "Action cancelled", Toast.LENGTH_SHORT).show();
                    // User cancelled the dialog
                }
            });

            final AlertDialog dialog = builder.create();
            dialog.show();

            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    mNewDbPassphrase = m_passphraseEt.getText().toString();

                    if( mNewDbPassphrase.length() < 4){

                        Toast.makeText(m_act, "Passphrase minimum length is 4 characters", Toast.LENGTH_LONG).show();
                        Toast.makeText(m_act, "No changes made", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    File a = null;
                    File b = null;
                    try {
                        // Put aside the current database files in case they need to be restored
                        a = BackupRestore.renameDbTemp(m_ctx, SqlCipher.ACCOUNT_DB_NAME);
                        b = BackupRestore.renameDbTemp(m_ctx, SqlCipher.DETAIL_DB_NAME);

                        // Check if database is CrypSafe or SecureSuite app
                        if( BackupRestore.testCrypSafeRestore(m_ctx, mNewDbPath)){

                            // Copy CrypSafe database files to the app
                            BackupRestore.copyCrypSafeDbToApp( m_ctx, mNewDbPath, "crypsafe1_db");
                            BackupRestore.copyCrypSafeDbToApp( m_ctx, mNewDbPath, "crypsafe2_db");
                        }else{
                            // Copy the database files to the app
                            BackupRestore.copyDbToApp( m_ctx, mNewDbPath, SqlCipher.ACCOUNT_DB_NAME);
                            BackupRestore.copyDbToApp( m_ctx, mNewDbPath, SqlCipher.DETAIL_DB_NAME);
                        }

                    } catch (IOException e) {
                        LogUtil.logException(m_ctx, LogType.RESTORE_DB, e);
                        Toast.makeText(m_act, "Database exception", Toast.LENGTH_SHORT).show();
                    }
                    boolean success = SqlCipher.testPassphrase(m_ctx, mNewDbPassphrase);

                    if( success){
                        LogUtil.log("Restore backup success");

                        //Save the passphrase, cleanup old database, inform user and restart
                        Passphrase.setDbPassphrase(m_ctx, mNewDbPassphrase);
                        BackupRestore.deleteDbTemp(m_ctx, a);
                        BackupRestore.deleteDbTemp(m_ctx, b);
                        Toast.makeText(m_act, "Restore successful", Toast.LENGTH_LONG).show();
                        Toast.makeText(m_act, "Restarting...", Toast.LENGTH_LONG).show();
                        Util.restartApplication(m_ctx);
                    }
                    else{
                        LogUtil.log("Restore backup fail");
                        //Inform user of failure, restore database, let user try another passphrase
                        Toast.makeText(m_act, "Passphrase or database invalid", Toast.LENGTH_LONG).show();
                        Toast.makeText(m_act, "Passphrase or database invalid", Toast.LENGTH_LONG).show();
                        BackupRestore.restoreDbTemp(m_ctx, a);
                        BackupRestore.restoreDbTemp(m_ctx, b);
                    }
                }
            });

            return dialog;
        }
    }

    /**
     * Handler of incoming messages from service.
     */
    static class IncomingHandler extends Handler {

        WeakReference<ContactListActivity> mContactListActivity;

        public IncomingHandler(ContactListActivity incomingHandler) {
            mContactListActivity = new WeakReference<ContactListActivity>(incomingHandler);
        }

        @Override
        public void handleMessage(Message msg) {
            if( mContactListActivity.get() == null ){

                ContactListActivity contactListActivity = new ContactListActivity();
                contactListActivity._handleMessage(msg);
            }else
                mContactListActivity.get()._handleMessage(msg);

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

                CloudImportDialog.updateProgress( m_act, bundle);
                break;
            }
        /*
         * A final message is sent when the import is complete.
         */
            case IMPORT_CLOUD_CONTACTS_COMPLETE:{

                String mainAccountImported = CloudImportDialog.getMainAccountImported();
                if( !mainAccountImported.isEmpty()){

                    LogUtil.log("CLA _handleMessage importedAccount: "+mainAccountImported);
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

                if(DEBUG) LogUtil.log(LogType.CLA,""+cmd+bundle);

                if( bundle.getString(CConst.UI_TYPE_KEY).contentEquals(CConst.RECREATE)){

                    m_act.recreate();//FUTURE refresh specific fragments
                }
                else
                if(DEBUG) LogUtil.log(LogType.CLA,"UI_TYPE_KEY no match: "+bundle.getString(CConst.UI_TYPE_KEY));
            }

            default:
                if(DEBUG) LogUtil.log(LogType.CLA,"_handleMessage default: "+cmd+" "+bundle);
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

            if(DEBUG) LogUtil.log(LogType.CLA,"onServiceConnected: "+className.getClassName());

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

    /**
     * Callback method from {@link ContactListFragment.Callbacks} indicating that
     * the item with the given ID was selected.
     */
    @Override
    public void onContactSelected() {

        m_contact_id = Persist.getCurrentContactId(m_ctx);
        startContactDetailFragment();
    }

    @Override
    public void onAccountSelected(String account, long first_contact_id) {

        if( mTwoPane){

            m_contact_id = first_contact_id;
            Persist.setCurrentContactId(m_ctx, first_contact_id);
            startContactDetailFragment();
        }
    }

    @Override
    public void onGroupSelected(int group_id, long first_contact_id) {

        if( mTwoPane && first_contact_id > 0){

            m_contact_id = first_contact_id;
            Persist.setCurrentContactId(m_ctx, first_contact_id);
            startContactDetailFragment();
        }
    }

    private void startContactEditFragment() {

        if( mTwoPane ){
            ContactEditFragment fragment = new ContactEditFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace( R.id.contact_detail_container, fragment, CConst.CONTACT_EDIT_FRAGMENT_TAG);
            ft.commit();
        }else{

            Intent i = new Intent(getApplicationContext(), ContactEditActivity.class);
            startActivity(i);
        }
    }
    private void startContactDetailFragment() {

        if( mTwoPane ){
                ContactDetailFragment frag = new ContactDetailFragment();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace( R.id.contact_detail_container, frag, CConst.CONTACT_DETAIL_FRAGMENT_TAG);
                ft.commit();
        }else{

            Intent i = new Intent(getApplicationContext(), ContactDetailActivity.class);
            startActivity(i);
        }
    }

    /**
     * Called when the editor has finished with a contact.  Show the detail of the contact.
     * Modifications may or may not have been made.
     */
    @Override
    public void onEditContactFinish(boolean contactModified) {

        // Edit finished, restore contact detail fragment
        if( contactModified){

            startContactListFragment();
            startContactDetailFragment();
        }
        else
            startContactDetailFragment();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private ContactListFragment startContactListFragment() {

        boolean isDestroyed = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && m_act.isDestroyed())
            isDestroyed = true;

        if( m_act == null || isDestroyed || m_act.isFinishing())
            return null;

        ContactListFragment clf = new ContactListFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace( R.id.contact_list_container, clf, CConst.CONTACT_LIST_FRAGMENT_TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(null);
        ft.commit();
        return clf;
    }

    /**
     * No need to do anything.  The group list will never be shown when this activity is running.
     * But all activities implementing ContactDetailFragment must implement it.
     */
    @Override
    public void refreshGroupList() {
    }
}
