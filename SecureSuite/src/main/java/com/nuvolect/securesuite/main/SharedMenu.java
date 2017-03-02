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
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.BackupRestore;
import com.nuvolect.securesuite.data.CleanupFragment;
import com.nuvolect.securesuite.data.ExportVcf;
import com.nuvolect.securesuite.data.ImportContacts;
import com.nuvolect.securesuite.data.ImportVcard;
import com.nuvolect.securesuite.data.MergeGroup;
import com.nuvolect.securesuite.data.MyContacts;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlFullSyncSource;
import com.nuvolect.securesuite.license.AppSpecific;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.DialogUtil;
import com.nuvolect.securesuite.util.FileBrowserImportVcf;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.PermissionUtil;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.Util;
import com.nuvolect.securesuite.util.WorkerCommand;

import net.sqlcipher.Cursor;

import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.nuvolect.securesuite.data.BackupRestore.backupToStorage;
import static com.nuvolect.securesuite.data.MyContacts.invalidContact;

public class SharedMenu extends Activity {

    private static final int REQUEST_ID_BACKUP_TO_STORAGE = 123;

    public enum POST_CMD {
        ACT_RECREATE, REFRESH_LEFT_DEFAULT_RIGHT, DONE, NIL, START_CONTACT_EDIT, SETTINGS_FRAG, START_CONTACT_DETAIL}

    private static final boolean DEBUG = LogUtil.DEBUG;
    private static Activity m_act;
    private static MenuItem m_item;
    private static int m_item_id;
    private static long m_contact_id;
    private static int m_group_id;
    private static PostCmdCallbacks m_post_cmd_callbacks;
    private static Cursor m_cursor;

    public interface PostCmdCallbacks {

        void postCommand(SharedMenu.POST_CMD post_cmd);

    }

    public static void processCmd(Activity act, MenuItem item) {

        m_act = act;
        m_item = item;
        m_item_id = item.getItemId();

        process();

        return ;
    }
    public static POST_CMD processCmd(Activity act, MenuItem item, long contact_id){

        m_act = act;
        m_item = item;
        m_item_id = item.getItemId();
        m_cursor = null;
        m_contact_id = contact_id;
        m_cursor = null;

        return process();
    }

    /**
     * Full featured process command at the contact level.  Passing the cursor gives
     * better access to neighboring contacts in the case of a delete.
     * @param act
     * @param cursor
     * @param item_id
     * @return
     */
    public static POST_CMD processCmd(Activity act, Cursor cursor, int item_id){

        m_act = act;
        m_item = null;
        m_cursor = cursor;
        m_item_id = item_id;
        m_contact_id = m_cursor.getLong( SqlCipher.index_contact_id );

        long contact_id = m_contact_id;
        if(DEBUG) LogUtil.log("SharedMenu processCmd: "+SqlCipher.contactInfo( contact_id));

        long next_id = SqlCipher.getNextContactId( m_cursor );
        if(DEBUG) LogUtil.log("SharedMenu processCmd next: "+SqlCipher.contactInfo( next_id));

        return process();
    }

    public static POST_CMD processCmd(Activity act, MenuItem item, long contact_id, int group_id,
                                      PostCmdCallbacks postCmdCallbacks) {
        m_act = act;
        m_item = item;
        m_item_id = item.getItemId();
        m_contact_id = contact_id;
        m_group_id = group_id;
        m_post_cmd_callbacks = postCmdCallbacks;
        m_cursor = null;

        return process();
    }
    public static POST_CMD processCmd(Activity act, MenuItem item, long contact_id,
                                      PostCmdCallbacks postCmdCallbacks) {
        m_act = act;
        m_item = item;
        m_item_id = item.getItemId();
        m_contact_id = contact_id;
        m_post_cmd_callbacks = postCmdCallbacks;
        m_cursor = null;

        return process();
    }
    public static POST_CMD processCmd(Activity act, MenuItem item, int group_id,
                                      PostCmdCallbacks postCmdCallbacks) {
        m_act = act;
        m_item = item;
        m_item_id = item.getItemId();
        m_contact_id = 0;
        m_group_id = group_id;
        m_post_cmd_callbacks = postCmdCallbacks;
        m_cursor = null;

        return process();
    }

    /**
     * Process a menu command and return a post command to be executed in the
     * calling activity
     * @return post command.
     */
    private static POST_CMD process(){

        switch( m_item_id ){

            case R.id.menu_shared_search:{
                SearchDialog.manageSearch(m_act, new SearchDialog.SearchCallbacks() {
                    @Override
                    public void onContactSelected(long contact_id) {

                        if( invalidContact( contact_id)){
                            Toast.makeText(m_act, "Invalid contact",Toast.LENGTH_SHORT).show();
                            return;
                        }

                        LogUtil.log("SharedMenu.shared_search id selected: " + contact_id);
                        Persist.setCurrentContactId(m_act, contact_id);
                        if (m_post_cmd_callbacks == null) {

                            LogUtil.log("ERROR SharedMenu.shared_search callbacks null ");
                        } else {

                            m_post_cmd_callbacks.postCommand(POST_CMD.START_CONTACT_DETAIL);
                        }
                    }
                });
                break;
            }
            case R.id.menu_add_contact:{
                m_contact_id = SqlCipher.createEmptyContact(m_act, Cryp.getCurrentGroup());
                Persist.setCurrentContactId(m_act, m_contact_id);

                // In case the user discards contact, it will be deleted
                Persist.setEmptyContactId( m_act, m_contact_id);
                return POST_CMD.START_CONTACT_EDIT;
            }
            case R.id.menu_delete_contact:{
                if( invalidContact( m_contact_id)){
                    Toast.makeText( m_act, "Select a contact to delete", Toast.LENGTH_SHORT).show();
                    return POST_CMD.DONE;
                }
                else{
                    long nextId = 0;
                    if( m_cursor != null)
                        nextId = SqlCipher.getNextContactId( m_cursor);

                    boolean success;
                    if( MyGroups.isInTrash( m_contact_id)){

                        success = 1 == SqlCipher.deleteContact(m_act, m_contact_id, true);//sync==true
                        if( success )
                            Toast.makeText( m_act, "Contact deleted", Toast.LENGTH_SHORT).show();
                    }
                    else{

                        success = MyGroups.trashContact(m_act, Cryp.getCurrentAccount(), m_contact_id);
                        if( success )
                            Toast.makeText( m_act, "Item moved to trash", Toast.LENGTH_SHORT).show();
                    }
                    if(! success)
                        LogUtil.log("SharedMenu.delete_contact delete failure: "+m_contact_id);

                    MyContacts.setValidId( m_act, nextId);

                    MyGroups.loadGroupMemory();
                    return POST_CMD.REFRESH_LEFT_DEFAULT_RIGHT;
                }
            }
            case R.id.menu_edit_contact:{
                if( invalidContact( m_contact_id)){
                    Toast.makeText(m_act, "Invalid contact",Toast.LENGTH_SHORT).show();
                    return POST_CMD.NIL;
                }
                else
                    return POST_CMD.START_CONTACT_EDIT;
            }
            case R.id.menu_edit_save:
            case R.id.menu_edit_discard:{
                return POST_CMD.START_CONTACT_DETAIL;
            }
            case R.id.menu_share_contact:{
                if( invalidContact( m_contact_id)){
                    Toast.makeText(m_act, "Invalid contact",Toast.LENGTH_SHORT).show();
                    return POST_CMD.NIL;
                }
                ExportVcf.emailVcf(m_act, Persist.getCurrentContactId(m_act));
                return POST_CMD.NIL;
            }
            case R.id.menu_set_profile:
                Persist.setProfileId(m_act, m_contact_id);
                Toast.makeText(m_act, "Profile set", Toast.LENGTH_SHORT).show();
                return POST_CMD.REFRESH_LEFT_DEFAULT_RIGHT;

            case R.id.menu_delete_group:{

                // Test for All other contacts group, it can't be deleted
                if( MyGroups.isBaseGroup( m_group_id)){

                    Toast.makeText(m_act, "This group cannot be removed", Toast.LENGTH_LONG).show();
                }else{
                    // Delete the group but not the contacts in the group.
                    // Remove memory based and DB group title and data records matching group ID.
                    // Sync transaction to companion device.
                    MyGroups.deleteGroup( m_act, m_group_id, true);// Sync transaction is true

                    // Group is now gone, select a default group
                    int g_id = MyGroups.getDefaultGroup( Cryp.getCurrentAccount());
                    Cryp.setCurrentGroup( g_id);

                    return POST_CMD.REFRESH_LEFT_DEFAULT_RIGHT;
                }
                break;
            }
            case R.id.menu_add_group:{
                Intent intent = new Intent(m_act, EditGroupActivity.class);
                m_act.startActivity(intent);
                break;
            }
            case R.id.menu_edit_group:{
                if( MyGroups.isPseudoGroup( m_group_id))
                    Toast.makeText(m_act, "Can't edit this group directly", Toast.LENGTH_SHORT).show();
                else {
                    Intent intent = new Intent(m_act, EditGroupActivity.class);
                    intent.putExtra(CConst.GROUP_ID_KEY, Cryp.getCurrentGroup());
                    m_act.startActivity(intent);
                }
                break;
            }
            case R.id.menu_merge_group:
                MergeGroup.mergeGroup(m_act);
                break;
            //FUTURE integrate OpenPGP https://github.com/open-keychain/openpgp-api
            case R.id.menu_email_group:
                GroupSendEmail.emailGroup(m_act);
                break;
            case R.id.menu_text_group:{
                GroupSendSms.startGroupSms(m_act);
                break;
            }
            case R.id.menu_import_vcard:{

                if( hasPermission( READ_EXTERNAL_STORAGE)){

                    // Kickoff a browser activity here.
                    // When user selects file, onActivityResult called with the result.
                    Intent intent = new Intent();
                    intent.setClass( m_act, FileBrowserImportVcf.class);
                    m_act.startActivityForResult(intent, CConst.IMPORT_VCARD_BROWSE_ACTION);
                }else
                    PermissionUtil.requestReadExternalStorage(m_act, CConst.IMPORT_VCARD_REQUEST_EXTERNAL_STORAGE);
                break;
            }
            case R.id.menu_import_single_contact:{

                if( hasPermission( READ_CONTACTS)) {
                    /**
                     * Launch the contact picker intent.
                     * Results returned in onActivityResult()
                     */
                    Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                            ContactsContract.Contacts.CONTENT_URI);
                    m_act.startActivityForResult(contactPickerIntent, CConst.IMPORT_SINGLE_CONTACT_PICKER);
                }else
                    PermissionUtil.requestReadExternalStorage(m_act, CConst.IMPORT_SINGLE_CONTACT_REQUEST_READ);
                break;
            }
            case R.id.menu_import_account_contacts:{

                if( hasPermission( READ_CONTACTS) && hasPermission( GET_ACCOUNTS) ){

                    CloudImportDialog.openDialog( m_act);

                } else{
                    if( ! hasPermission( READ_CONTACTS) ){

                        PermissionUtil.requestReadContacts(m_act,
                                CConst.IMPORT_ACCOUNT_CONTACTS_REQUEST_READ_CONTACTS);
                        break;
                    }
                    if( ! hasPermission( GET_ACCOUNTS )){

                        PermissionUtil.requestGetAccounts(m_act,
                                CConst.IMPORT_ACCOUNT_CONTACTS_REQUEST_GET_ACCOUNTS);
                        break;
                    }
                }
                break;
            }
            case R.id.menu_export_group:{

                if( hasPermission( WRITE_EXTERNAL_STORAGE)){

                    ExportVcf.exportGroupVcardAsync( m_act, m_group_id);
                    Toast.makeText(m_act, "Exporting...", Toast.LENGTH_LONG).show();
                }else{
                    PermissionUtil.requestWriteExternalStorage(m_act,0);
                }
                break;
            }
            case R.id.menu_export_account:{

                if( hasPermission( WRITE_EXTERNAL_STORAGE)){
                    ExportVcf.exportAccountVcardAsync(m_act, Cryp.getCurrentAccount());
                    Toast.makeText(m_act, "Exporting...", Toast.LENGTH_LONG).show();
                }else{
                    PermissionUtil.requestWriteExternalStorage(m_act,0);
                }
                break;
            }
            case R.id.menu_export_all:{

                if( hasPermission( WRITE_EXTERNAL_STORAGE)){
                    ExportVcf.exportAllVcardAsync(m_act);
                    Toast.makeText(m_act, "Exporting...", Toast.LENGTH_LONG).show();
                }else{
                    PermissionUtil.requestWriteExternalStorage(m_act,0);
                }
                break;
            }
            case R.id.menu_cleanup:{
                CleanupFragment f = CleanupFragment.newInstance(m_act);
                f.startFragment();
                break;
            }
            case R.id.menu_backup_to_email:{
                if( hasPermission( WRITE_EXTERNAL_STORAGE)){
                    BackupRestore.backupToEmail(m_act);
                }else{
                    PermissionUtil.requestWriteExternalStorage(m_act,0);
                }
                break;
            }
            case R.id.menu_backup_to_storage:{

                if( hasPermission( WRITE_EXTERNAL_STORAGE))
                    BackupRestore.backupToStorage( m_act);
                else
                    PermissionUtil.requestWriteExternalStorage( m_act, REQUEST_ID_BACKUP_TO_STORAGE);
                break;
            }
            case R.id.menu_backup_to_lan:{

                SqlFullSyncSource.getInstance().backupDiag(m_act);
                break;
            }
            case R.id.menu_accounts:{
                // import android.provider.Settings;
                m_act.startActivity(new Intent(Settings.ACTION_SYNC_SETTINGS));
                break;
            }
            case R.id.menu_password_generator:{
                PasswordFragment f = PasswordFragment.newInstance(m_act);
                f.start();
                break;
            }
            case R.id.menu_empty_trash:{

                DialogUtil.emptyTrash(m_act, m_post_cmd_callbacks);
                break;
            }
            case R.id.menu_settings:{
                Intent intent = new Intent(m_act, SettingsActivity.class);
                m_act.startActivity(intent);
                break;
            }
            case R.id.menu_help:{
                String url = AppSpecific.APP_WIKI_URL;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                m_act.startActivity(i);
                break;
            }
            case R.id.menu_developer:{

                DeveloperDialog.start(m_act);
                break;
            }
            default:
                if(DEBUG && m_item != null)
                    LogUtil.log("SharedMenu.default: "+m_item.getTitle());
        }
        return POST_CMD.NIL;
    }

    private static boolean hasPermission(String perm) {
        return(ContextCompat.checkSelfPermission( m_act, perm)==
                PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch( requestCode){

            case REQUEST_ID_BACKUP_TO_STORAGE:{

                if( grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    backupToStorage( m_act);
                }
                break;
            }
            default:
        }
    }

    public static void sharedOnRequestPermissionsResult(
            Activity act, int requestCode, String[] permissions, int[] grantResults) {

        switch ( requestCode){

            /**
             * Service the result of a permission request
             */
            case CConst.IMPORT_VCARD_REQUEST_EXTERNAL_STORAGE:{

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Kickoff a browser activity here.
                    // When user selects file, onActivityResult called with the result.
                    Intent intent = new Intent();
                    intent.setClass( act, FileBrowserImportVcf.class);
                    act.startActivityForResult(intent, CConst.IMPORT_VCARD_BROWSE_ACTION);
                } else {

                    Toast.makeText(act, "Sorry, external storage permission required for import", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case CConst.IMPORT_SINGLE_CONTACT_REQUEST_READ:{
                /**
                 * Launch the contact picker intent.
                 * Results returned in onActivityResult()
                 */
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                        ContactsContract.Contacts.CONTENT_URI);
                m_act.startActivityForResult(contactPickerIntent, CConst.IMPORT_SINGLE_CONTACT_PICKER);
                break;
            }
            case CConst.IMPORT_ACCOUNT_CONTACTS_REQUEST_READ_CONTACTS:{

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    CloudImportDialog.openDialog( act );

                } else {

                    Toast.makeText(m_act, "Sorry, read contacts permission required for import", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case CConst.CALLER_ID_REQUEST_READ_PHONE_STATE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Do nothing
//                    Toast.makeText(m_act, "Caller identification enabled", Toast.LENGTH_SHORT).show();

                } else {

                    Toast.makeText(m_act, "Calls will not be identified without Read Phone State", Toast.LENGTH_SHORT).show();
                }
                break;
            default:{

                LogUtil.log("SharedMenu sharedOnRequestPermissionResult UNMANAGED/ERROR requestCode: "+requestCode);

                for( int i = 0; i < permissions.length; i++)
                    LogUtil.log("SharedMenuUtil permissions: "+permissions[i]+"  "+grantResults[i]);

            }
        }
    }

    public static void sharedOnActivityResult(
            Activity act, int requestCode, int resultCode, Intent data) {

        if(DEBUG) LogUtil.log("SharedMenu myOnActivityResult requestCode: "+requestCode);

        switch ( requestCode){

            case CConst.IMPORT_VCARD_BROWSE_ACTION:{

                if ( resultCode == RESULT_OK) {

                    Bundle activityResultBundle = data.getExtras();
                    String path = activityResultBundle.getString(CConst.IMPORT_VCF_PATH);

                    new ImportVcardAsync( ).execute(path);
                }
                break;
            }
            case CConst.IMPORT_SINGLE_CONTACT_PICKER:{

                if ( resultCode == RESULT_OK) {

                    Uri result = data.getData();
                    String id = result.getLastPathSegment();
                    LogUtil.log("Cloud contact ID: "+id);
                    boolean success = true;

                    if( id == null || id.isEmpty())
                        success = false;
                    else{

                        long cloud_contact_id = Long.valueOf( id );
                        success = ImportContacts.importSingleContact( m_act, cloud_contact_id);
                    }
                    if( ! success)
                        Toast.makeText(m_act, "Contact import error", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(m_act, "Contact imported", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case CConst.RESPONSE_CODE_SHARE_VCF:{

                LogUtil.log("SharedMenu sharedOnActivityResult SHARE_VCF delete ");
                Util.cleanupTempFolder(act);
                break;
            }
            default:{

                LogUtil.log("sharedOnActivityResult UNMANAGED/ERROR requestCode: "+requestCode);
            }
        }
    }


    private static class ImportVcardAsync extends AsyncTask<String, Integer, Long> {

        /** progress dialog to show user that the import is processing. */
        private ProgressDialog m_importProgressDialog = null;

        public ImportVcardAsync(){

            m_importProgressDialog = new ProgressDialog(m_act);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            m_importProgressDialog.setMessage("Import starting...");
            m_importProgressDialog.show();
        }

        @Override
        protected Long doInBackground(String...paths) {

            String path = paths[0];

            ImportVcard.ImportProgressCallbacks callbacks = new ImportVcard.ImportProgressCallbacks() {
                @Override
                public void progressReport(int importProgress) {

                    publishProgress( importProgress );
                }
            };

            long contact_id = ImportVcard.importVcf(m_act, path, Cryp.getCurrentGroup(), callbacks);

            return contact_id;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            int vcardsImported = values[0];

            if( m_importProgressDialog == null ) {
                m_importProgressDialog = new ProgressDialog(m_act);
                m_importProgressDialog.show();
            }

            if( m_importProgressDialog != null && m_importProgressDialog.isShowing())
                m_importProgressDialog.setMessage("Import progress: " + vcardsImported);
        }

        @Override
        protected void onPostExecute(Long contact_id) {

            if( m_importProgressDialog!= null && m_importProgressDialog.isShowing())
                m_importProgressDialog.dismiss();

            if( contact_id > 0)
                Toast.makeText(m_act, "Import complete", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(m_act, "Import failed", Toast.LENGTH_LONG).show();

            m_act.setProgressBarIndeterminateVisibility( false );

            WorkerCommand.refreshUserInterface(m_act, CConst.RECREATE);
            //FUTURE send message to refresh UI or specific fragments
//            m_act.startContactListFragment();
//            if( mTwoPane)
//                startContactDetailFragment();
        }
    }
}
