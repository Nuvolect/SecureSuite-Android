package com.nuvolect.securesuite.webserver;//

import android.content.Context;

import com.nuvolect.securesuite.data.ExportVcf;
import com.nuvolect.securesuite.data.ImportVcard;
import com.nuvolect.securesuite.data.MyAccounts;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlIncSync;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.main.SettingsActivity;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.JsonUtil;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Passphrase;
import com.nuvolect.securesuite.util.Safe;
import com.nuvolect.securesuite.util.StringUtil;
import com.nuvolect.securesuite.util.Util;

import net.sqlcipher.Cursor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.nuvolect.securesuite.util.LogUtil.log;

/**
 * Generate and manage the contact list web view.
 */
public class ListHtm {

    //WEBAPP settings option for different background
    //WEBAPP settings Comfortable, cozy and compact
    private static Context m_ctx;

    private static ArrayList<Long> mSelectId;
    private static HashMap<Integer, String> mGroupEdit; // Group edit plan, <Group ID, {0,1,-}>
    private static ArrayList<Long> mDisplayId;          // IDs of currently displayed contacts
    private static ArrayList<Long> mStarredId;          // IDs of displayed contacts also starred
    private static ArrayList<Long> mUnStarredId;        // IDs of displayed contacts not starred
    private static MiniTemplator t;
    private static int m_start_index = 0;
    private static int PAGE_SIZE = 50;
    private static int m_page_size = PAGE_SIZE;
    public static String m_account = "";
    public static String m_search = "";
    public static int m_group_id;
    private static int m_max_index;
    private static String templateFile = "list.htm";
    private static String m_single_shot_command = "";

    /**
     * List of the main keys to associate with a key value pair.  This is typically a button or
     * a menu item versus a more simple link.
     */
    enum KEYS {NIL,
        // Common to all pages
        account,        // Account menu select, value is account email
        group_id,       // Value is the group_id
        item_star,      // Item star, value is the contact_id
        link,           // Second level enum, Typically an html link, value is the link source
        search,         // Search string, value is the string
        theme,          // Value is user selected theme
        term,           // Search parameter //TODO, define term
        spacing,        // Page element spacing: comfortable, cozy, compact

        uri,                    // Full uri for routing
        queryParameterStrings,  // Raw parameters

        // Specific to this page
        cb_group,       // Group ID is the value
        cb_state,       // Group state is the value
        delete_1_contact,
        file_upload,    // Value is list of files
        inject_modal,   // Value is the filename of the modal to inject
        item,
        item_cb,
        new_group,      // Value is group title
        new_group_contacts,// Value is group title, apply to selected contacts
        rename_group,   // Value is new group title
        page_contacts,  // Value is # of contacts on page
        password,       // Value is poassword generated in js
        password_mode,  // Value is the 5 bit checkbox selection
        password_length,// Value is the password length
    }

    /**
     * Links use key link and do not have any parameters
     */
    enum LINKS {NIL,
        all_in_account,     // Switch to list page, show all in account
        cb_all,
        cb_none,
        cb_starred,
        cb_unstarred,
        chevron_left,
        chevron_right,
        delete_contacts,
        delete_group,
        display_comfortable,// Update CSS
        display_compact,    // Update CSS
        display_cozy,       // Update CSS
        dump_group,
        empty_trash,
        export_contacts,
        file_upload_cancel,
        find_merge,
        group_edit_apply,
        group_edit_cancel,
        merge_contacts,
        new_contact,
        password_cancel,
        print_contacts,
        restore_contacts,
        settings,
        sort_first,
        sort_last,
        starred_in_account,
    }

    public static void init(Context ctx){

        m_ctx = ctx;
        mSelectId = new ArrayList<Long>();
        mDisplayId = new ArrayList<Long>();
        mStarredId = new ArrayList<Long>();
        mUnStarredId = new ArrayList<Long>();
    }

    public static String render(Context ctx, String uniqueId, Map<String, String> params) {

        m_ctx = ctx;

        /**
         * Restore data for this session.
         */
        m_search =      CrypServer.get(uniqueId, "search");
        m_account =     CrypServer.get(uniqueId, "account");
        mSelectId =     CrypServer.getSelectId(uniqueId);
        mGroupEdit =    CrypServer.getGroupEdit(uniqueId);
        m_group_id =    Integer.valueOf(CrypServer.get(uniqueId, "group_id"));
        m_start_index = Integer.valueOf(CrypServer.get(uniqueId, "start_index"));
        m_page_size   = Integer.valueOf(CrypServer.get(uniqueId, "page_contacts", ""+PAGE_SIZE));

        /**
         * Parse parameters and process any updates.
         * Return an Action indicating what to do next
         */
        String action = parse(uniqueId, params);// set mContactId & others based on params

        if( action.startsWith("download:"))  // Check for download, action string includes filename
            return action;

        /**
         * Generate modals on another thread and continue generating html on this thread.
         * This way the html can be presented more quickly and modals will be ready
         * for use when needed. This assumes the modal is generated before the user presses
         * a button to use it.
         */
        new Thread(new Runnable() {
            @Override
            public void run() {

                buildGroupEditModal();
                PasswordModal.buildPasswordModal(m_ctx, false);

            }
        }).start();
        /**
         * Generate the page html and return it as the session response
         */
        return generateHtml( uniqueId, params);
    }

    private static String generateHtml(String uniqueId, Map<String, String> params) {

        String generatedHtml = "";
        HashMap<Integer, Integer> groupsThisPage = new HashMap<Integer, Integer>();

        try {
            t = new MiniTemplator(WebService.assetsDirPath+"/"+templateFile);

            /**
             * Add a single use command into the header, if any.  This can be used to
             * generate a header redirect, to clear file upload parameters.
             */
            t.setVariable("single_shot_command", m_single_shot_command);
            m_single_shot_command = "";

            int theme = SettingsActivity.getThemeNumber(m_ctx);  // Set current theme
            t.setVariable("theme", AppTheme.getThemeName(m_ctx, theme));

            /**
             * Restore persisted data until it is replaced by user action.
             */
            t.setVariable("mSearch", m_search);//WEBAPP search as you type, update pull-down dynamic

            String account_ellipse = StringUtil.ellipsize(m_account, 25);
            t.setVariable("account_ellipse", account_ellipse);

            /**
             * Format the list of accounts with LOGOUT at the end
             */
            String[] accounts = MyAccounts.getAccounts();
            for( String account_select : accounts){

                t.setVariable("account_select", account_select);
                t.addBlock("account_select");
            }

            /**
             * Format the left side list of groups Title(count)
             * proto: <a href="" onclick="post('url','group_link=1234')">Family(3)</a>
             */
            int nAllContacts = MyAccounts.getContactCount(m_account);
            String all_title = "All in account ("+ nAllContacts +")";
            if( m_group_id == CConst.GROUP_ALL_IN_ACCOUNT)// Make the current group BOLD
                t.setVariable("all_in_account", "<strong>" + all_title + "</strong>");
            else
                t.setVariable("all_in_account", all_title);

            int starContacts = MyAccounts.getStarredCount(m_account);
            String star_title = "Starred ("+ starContacts +")";
            if( m_group_id == CConst.GROUP_STARRED_IN_ACCOUNT)// Make the current group BOLD
                t.setVariable("starred_in_account", "<strong>" + star_title + "</strong>");
            else
                t.setVariable("starred_in_account", star_title);

            int[] groupIds = MyGroups.getGroupIds(m_account);
            String[] groupTitles = MyGroups.getGroupTitlesStringArray(m_account);

            for(int i=0; i < groupIds.length; i++) {

                int group_id = groupIds[i];
                String  groupTitle = StringUtil.ellipsize( groupTitles[i], 19);
                int numContacts = MyGroups.mGroupCount.get(group_id);

                String titleAndCount;// Make the current group BOLD
                if( group_id == m_group_id)
                    titleAndCount = "<strong>"+groupTitle + " (" + numContacts + ")</strong>";
                else
                    titleAndCount = groupTitle + " (" + numContacts + ")";

                t.setVariable("group_title", titleAndCount);
                t.setVariable("group_id", String.valueOf(group_id));
                t.addBlock("group_item");
            }

            ///WEBAPP search display name and common kv across all contacts in account
            /**
             * Get a cursor that is specific to the current account/group selected.
             * This is used to get the id, star and display name.
             * The remaining details are fetched from the detail db
             */
            Cursor c = null;
            if( m_group_id == CConst.GROUP_ALL_IN_ACCOUNT)// all contacts in account
                c = MyAccounts.getAccountCursor(m_account, m_search);
            else
            if( m_group_id == CConst.GROUP_STARRED_IN_ACCOUNT)// starred contacts in account
                c = MyAccounts.getAccountStarredCursor(m_account, m_search);
            else
                c = MyGroups.getGroupContactsCursor(m_group_id, m_search);

            log(LogUtil.LogType.LIST_HTM, "m_group_id  : " + m_group_id);
            log(LogUtil.LogType.LIST_HTM, "m_account   : " + m_account);
            log(LogUtil.LogType.LIST_HTM, "c.getCount(): " + c.getCount());

            m_max_index = Math.max(0, c.getCount() - 1);

            /**
             * Advance cursor one position in front of first contact on list
             */
            c.moveToPosition(m_start_index -1);

            int contact_id_colIndex   = c.getColumnIndex(SqlCipher.ATab.contact_id.toString());
            int starred_colIndex      = c.getColumnIndex(SqlCipher.ATab.starred.toString());
            int display_name_colIndex = c.getColumnIndex(SqlCipher.ATab.display_name.toString());

            String pageRangeStart = "";
            String pageRangeEnd="";
            String display_name="";
            /**
             * mDisplayId keeps a list of all the currently displayed contacts.
             * mSelectedId is a companion structure to record contacts selected via checkbox or click.
             */
            mDisplayId.clear();
            mStarredId.clear();
            mUnStarredId.clear();

            for( int n=0; n  < m_page_size && c.moveToNext(); n++){

                long contact_id = c.getLong( contact_id_colIndex );
                /**
                 * Keep the IDs.  Use for select-all, select-none, select-starred...
                 */
                mDisplayId.add( contact_id);
                t.setVariable("contact_id", String.valueOf(contact_id));

                if( ! mSelectId.contains( contact_id)){
                    t.setVariable("cb_state", "0");
                    t.setVariable("cb_icon", "glyphicon-unchecked");
                }
                else{
                    t.setVariable("cb_state", "1");
                    t.setVariable("cb_icon", "glyphicon-check");
                }


                String star = c.getString( starred_colIndex );
                if( star.startsWith("0")){
                    t.setVariable("star_state", "0");
                    t.setVariable("star_icon", "glyphicon-star-empty");
                    mUnStarredId.add(contact_id);
                }
                else{
                    t.setVariable("star_state", "1");
                    t.setVariable("star_icon", "glyphicon-star");
                    mStarredId.add(contact_id);
                }

                display_name = c.getString( display_name_colIndex );
                display_name = StringUtil.ellipsize(display_name, 22);
                t.setVariable("display_name", display_name);

                // Keep the starting few letters of the name range
                if( pageRangeStart.isEmpty()){
                    int i = display_name.indexOf(' ');
                    if( i <= 0)
                        i = Math.min( 3, display_name.length());
                    pageRangeStart = display_name.substring(0, i);
                }

                //FUTURE replace these database calls with getCursor call, reduce DB overhead 3x
                String email_or_web = JsonUtil.contactDetail( contact_id, SqlCipher.DTab.email, 22);
                if (email_or_web.isEmpty()) {

                    email_or_web = JsonUtil.contactDetail( contact_id, SqlCipher.DTab.website, 22);
                }
                t.setVariable("email", email_or_web);

                String phone = JsonUtil.contactDetail(contact_id, SqlCipher.DTab.phone, 19);
                t.setVariable("phone", phone);

                String address = JsonUtil.contactDetail( contact_id, SqlCipher.DTab.address, 22);
                t.setVariable("address", address);

                String[] groupArray = MyGroups.getGroupTitleArray(contact_id);
                for( String groupTitle : groupArray){
                    t.setVariable("contact_group_title", groupTitle);
                    t.addBlock("contact_group");
                }

                // Tally groups used with this page of contacts.
                if( mSelectId.contains(contact_id)){

                    int[] groups = MyGroups.getGroups(contact_id);
                    for( int group : groups){

                        int j = groupsThisPage.containsKey(group) ? groupsThisPage.get(group) : 0;
                        groupsThisPage.put(group, j + 1);
                    }
                }

                t.addBlock("contact");
            }
            c.close();

            // Keep the starting few letters of the name range
            int i = display_name.indexOf(' ');
            if( i <= 0)
                i = Math.min( 3, display_name.length());
            pageRangeEnd = display_name.substring(0, i);
            t.setVariable("page_name_range",pageRangeStart+" - "+pageRangeEnd);

            t.setVariable("notify_js", CrypServer.getNotify(uniqueId));

            /**
             * Customize menu when user is in the group trash
             */
            if( MyGroups.getGroupTitle( m_group_id).contentEquals(CConst.TRASH)){
                String javaScript="document.getElementById('menu_move_to_trash').style.display='none';";
                t.setVariable("post_command", javaScript);
            }

            generatedHtml = t.generateOutput();

        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
         * Key data will be restored each time from session data.  This supports
         * concurrent sessions from different users and browsers.
         */
        m_group_id = 0;
        m_account = "";
        m_search = "";

        return generatedHtml;
    }

    /**
     * Parse and process params specific to this session.  Return "download:filename.ext" to
     * facilitate a download for the next step, otherwise return GENERATE_HTML to
     * continue generating the page.
     * @param uniqueId
     * @param params
     * @return
     */
    private static String parse(String uniqueId, Map<String, String> params) {

        String key = "";
        String value = "";

        for (Map.Entry<String, String> entry : params.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();

            KEYS key_enum = KEYS.NIL;
            try {
                key_enum = KEYS.valueOf(key);
            } catch (Exception e) {
                log(LogUtil.LogType.LIST_HTM, "Error unknown key: " + key);
                LogUtil.logException(m_ctx, LogUtil.LogType.LIST_HTM, e);
            }
            LINKS link_enum = LINKS.NIL;

            switch (key_enum) {

                case NIL: {
                    log(LogUtil.LogType.LIST_HTM, "Error NIL key: " + key);
                    break;
                }
                case account:
                    /**
                     * Save account, reset all session data
                     */
                    m_account = value;
                    CrypServer.put(uniqueId, "account", m_account);

                    m_group_id = MyGroups.getDefaultGroup(m_account);
                    CrypServer.put(uniqueId, "group_id", m_group_id);

                    m_search = ""; // start with empty search
                    CrypServer.put(uniqueId, "search", m_search);

                    m_start_index = 0; // display from first contact
                    CrypServer.put(uniqueId, "start_index", m_start_index);

                    /**
                     * Selections are unique to each account, clear selections.
                     */
                    mSelectId.clear();
                    CrypServer.putSelectId(uniqueId, mSelectId);// Persist to session data
                    break;
                case group_id: {
                    /**
                     * Save the group_id, reset dependent session data
                     */
                    m_group_id = Integer.valueOf(value);
                    CrypServer.put(uniqueId, "group_id", m_group_id);

                    m_search = ""; // start with empty search
                    CrypServer.put(uniqueId, "search", m_search);

                    m_start_index = 0; // display from first contact
                    CrypServer.put(uniqueId, "start_index", m_start_index);
                    break;
                }
                case password:       // Do nothing, data picked up in companion case
                case password_length:// Ditto
                    break;
                case password_mode:{

                    /**
                     * Each session maintains its own password parameters.
                     */
                    int mode = Integer.valueOf( value);
                    CrypServer.put(uniqueId, "password_mode", mode);
                    log(LogUtil.LogType.LIST_HTM, "new password mode: " + mode);

                    int length = Integer.valueOf(params.get("password_length").trim());
                    CrypServer.put(uniqueId, "password_length", length);
                    log(LogUtil.LogType.LIST_HTM, "new password length: " + length);

                    /**
                     * Get the password from params.
                     * It was sent encoded UTF-8 and the server automatically decoded it.
                     */
                    String password = params.get("password");
                    CrypServer.put(uniqueId, "password", password);
                    log(LogUtil.LogType.LIST_HTM, "new password : " + password);

                    Passphrase.setPasswordGenMode(m_ctx, mode);
                    Passphrase.setPasswordLength(m_ctx, length);
                    Passphrase.appendPasswordHistory(m_ctx, password);

                    // Update modal for next usage
                    PasswordModal.buildPasswordModal(m_ctx, false);
                    break;
                }
                case page_contacts:{

                    CrypServer.put(uniqueId, "page_contacts", Integer.valueOf(value));
                    break;
                }
                case search:
                    m_search = value;
                    CrypServer.put(uniqueId, "search", m_search);

                    m_start_index = 0; // display from first contact
                    CrypServer.put(uniqueId, "start_index", m_start_index);
                    break;
                case item: {
                    long contactId = Long.valueOf(value);
                    break;
                }
                case item_cb: {// Invert checkbox item
                    long contactId = Long.valueOf(value);
                    if( mSelectId.contains(contactId))
                        mSelectId.remove(contactId);
                    else
                        mSelectId.add(contactId);
                    CrypServer.putSelectId(uniqueId, mSelectId);// Persist to session data
                    break;
                }
                case item_star: {
                    long contactId = Long.valueOf(value);
                    SqlCipher.invertStarred(contactId);
                    break;
                }
                case delete_1_contact: {

                    long contact_id = Long.valueOf(value);
                    boolean success;

                    if (MyGroups.isInTrash(contact_id)) {

                        success = 1 == SqlCipher.deleteContact(m_ctx, contact_id, true);
                        if (success) {
                            CrypServer.notify(uniqueId, "Contact deleted", "success");
                        }
                    }
                    else {

                        success = MyGroups.trashContact(m_ctx, Cryp.getCurrentAccount(), contact_id);
                        if (success) {
                            CrypServer.notify(uniqueId, "Contact moved to trash", "success");
                        }
                    }
                    if( ! success ){

                        CrypServer.notify(uniqueId, "Contact delete error", "warn");
                        log(LogUtil.LogType.LIST_HTM, "error delete_1_contact");
                    }
                    break;
                }
                case link:
                    try {
                        link_enum = LINKS.valueOf(value);
                    } catch (Exception e) {
                        log(LogUtil.LogType.LIST_HTM, "Error unknown link: " + value);
                        LogUtil.logException(m_ctx, LogUtil.LogType.LIST_HTM, e);
                    }
                    break;
                case cb_group:
                    /**
                     * Accept an update to the group edit plan and persist it to session data.
                     */
                    int groupId = Integer.valueOf( value );
                    String state = params.get("cb_state");

                    if( state.isEmpty()){
                        mGroupEdit.put(groupId,"-");
                        log(LogUtil.LogType.LIST_HTM, "Group: " + groupId + ", is indeterminate");
                    }
                    else
                    if( state.contains("1")){

                        mGroupEdit.put(groupId,"1");
                        log(LogUtil.LogType.LIST_HTM, "Group: " + groupId + ", is checked");
                    }
                    else
                    if( state.contains("0")){

                        mGroupEdit.put(groupId,"0");
                        log(LogUtil.LogType.LIST_HTM, "Group: " + groupId + ", is unchecked");
                    }
                    CrypServer.putGroupEdit(uniqueId, mGroupEdit);
                    break;
                case cb_state:
                    break;
                case new_group: {
                    String newGroupTitle = Safe.safeString(value);

                    if ( newGroupTitle.isEmpty())
                        CrypServer.notify(uniqueId, "Enter a new group name", "warn");
                    else if ( MyGroups.groupNameInUse(newGroupTitle, m_account))
                        CrypServer.notify(uniqueId, "Group already exists", "warn");
                    else
                        MyGroups.addGroup(m_ctx, newGroupTitle, m_account, "");
                    MyGroups.loadGroupMemory();
                    break;
                }
                case new_group_contacts: { // Create new group and add selected users
                    String newGroupTitle = Safe.safeString(value);

                    if ( newGroupTitle.isEmpty())
                        CrypServer.notify(uniqueId, "Enter a new group name", "warn");
                    else if ( MyGroups.groupNameInUse(newGroupTitle, m_account))
                        CrypServer.notify(uniqueId, "Group already exists", "warn");
                    else {
                        int new_group = MyGroups.addGroup(m_ctx, newGroupTitle, m_account, "");

                        for( long contact_id : mSelectId){

                            MyGroups.addGroupMembership( m_ctx, contact_id, new_group, true);// add & sync
                        }
                        MyGroups.loadGroupMemory();
                    }
                    break;
                }
                case rename_group: {
                    String newGroupTitle = Safe.safeString(value);

                    if ( newGroupTitle.isEmpty())
                        CrypServer.notify(uniqueId, "Enter a new group name", "warn");
                    else if ( MyGroups.groupNameInUse(newGroupTitle, m_account))
                        CrypServer.notify(uniqueId, "Group already exists", "warn");
                    else {
                        if( MyGroups.renameGroup(m_ctx, newGroupTitle, m_group_id, m_account)){// rename & sync

                            CrypServer.notify(uniqueId, "Group renamed", "success");
                        }else{

                            CrypServer.notify(uniqueId, "Group rename failed", "warn");
                        }
                    }
                    break;
                }
                case file_upload:{

                    try {
                        JSONObject jsonObject = new JSONObject( value );
                        Iterator<String> keys = jsonObject.keys();

                        // Suspend sync while import is in progress
                        SqlIncSync.getInstance().suspendSync();

                        while( keys.hasNext()){

                            String fileName = keys.next();
                            String filePath = jsonObject.getString(fileName);

                            /**
                             * The server stores the file in a unique path that does not include
                             * the original filename. Hence the need to also pass a fileName.
                             */
                            long contact_id = ImportVcard.importVcf(m_ctx, fileName, filePath, m_group_id);
                            if( contact_id == 0)
                                CrypServer.notify(uniqueId, "Import vCard invalid", "warn");
                        }
                        // Restore sync process
                        SqlIncSync.getInstance().resumeSync(m_ctx);

                        String url = WebUtil.getServerUrl(m_ctx);
                        m_single_shot_command = "<META http-equiv=\"refresh\" content=\"0;URL="+url+"\">";

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case theme:{

                    LogUtil.log(LogUtil.LogType.LIST_HTM,"theme selected: " + value);
                    SettingsActivity.setTheme(m_ctx, value);
                    break;
                }
                case uri:
                case queryParameterStrings:
                    break;
            }

            switch (link_enum) {

                case NIL:
                    break;
                case all_in_account:
                    /**
                     * Save the group_id, reset dependent session data
                     */
                    m_group_id = CConst.GROUP_ALL_IN_ACCOUNT;// special for all contacts in account
                    CrypServer.put(uniqueId, "group_id", m_group_id);

                    m_search = ""; // start with empty search
                    CrypServer.put(uniqueId, "search", m_search);

                    m_start_index = 0; // display from first contact
                    CrypServer.put(uniqueId, "start_index", m_start_index);
                    break;
                case starred_in_account:
                    /**
                     * Save the group_id, reset dependent session data
                     */
                    m_group_id = CConst.GROUP_STARRED_IN_ACCOUNT;// special for all contacts in account
                    CrypServer.put(uniqueId, "group_id", m_group_id);

                    m_search = ""; // start with empty search
                    CrypServer.put(uniqueId, "search", m_search);

                    m_start_index = 0; // display from first contact
                    CrypServer.put(uniqueId, "start_index", m_start_index);
                    break;
                case empty_trash://WEBAPP add confirmation dialog to empty trash
                    /**
                     * Delete all contacts in the account that are part of the group Trash
                     */
                    MyGroups.emptyTrash( m_ctx, m_account);
                    break;
                case new_contact:
                    break;
                case cb_all:
                    LogUtil.log("parse.mDisplayId.size, start: "+mDisplayId.size());
                    for(long id : mDisplayId)
                        mSelectId.add(id);
                    CrypServer.putSelectId(uniqueId, mSelectId);// Persist to session data
                    LogUtil.log("parse.mDisplayId.size, end: " + mDisplayId.size());
                    break;
                case cb_none:
                    mSelectId.clear();
                    CrypServer.putSelectId(uniqueId, mSelectId);// Persist to session data
                    break;
                case cb_starred:
                    mSelectId.clear();
                    mSelectId.addAll(mStarredId);
                    CrypServer.putSelectId(uniqueId, mSelectId);// Persist to session data
                    break;
                case cb_unstarred:
                    mSelectId.clear();
                    mSelectId.addAll(mUnStarredId);
                    CrypServer.putSelectId(uniqueId, mSelectId);// Persist to session data
                    break;
                case chevron_left:
                    m_start_index -= m_page_size;
                    if (m_start_index < 0)
                        m_start_index = 0;
                    CrypServer.put(uniqueId, "start_index", m_start_index);
                    break;
                case chevron_right:
                    m_start_index += m_page_size;
                    if (m_start_index > m_max_index) {
                        m_start_index -= m_page_size;
                    }
                    CrypServer.put(uniqueId, "start_index", m_start_index);
                    break;
                case merge_contacts:
                    break;
                case delete_contacts:
                    /**
                     * Move the selected contacts to Trash and clear the selected ID list
                     */
                    for( long id : mSelectId){

                        MyGroups.trashContact(m_ctx, m_account, id);
                    }
                    /**
                     * Contacts now deleted and selected array no longer needed
                     */
                    mSelectId.clear();
                    CrypServer.putSelectId(uniqueId, mSelectId);
                    MyGroups.loadGroupMemory();
                    break;
                case delete_group: {  // Delete the current group, assign current group as default
                    if (MyGroups.isBaseGroup(m_group_id)) {

                        // A base group that cannot be deleted, inform user
                        CrypServer.notify(uniqueId, "Cannot delete standard group", "warn");
                        log(LogUtil.LogType.LIST_HTM, "Cannot delete base group");
                    }else{
                        // Delete the group, assign default group and save in session data
                        boolean syncTransaction = true;
                        MyGroups.deleteGroup( m_ctx, m_group_id, syncTransaction);
                        MyGroups.loadGroupMemory();
                        m_group_id = MyGroups.getDefaultGroup(m_account);
                        CrypServer.put(uniqueId, "group_id", m_group_id);
                        CrypServer.notify(uniqueId, "Group deleted", "success");
                        log(LogUtil.LogType.LIST_HTM, "Group deleted");
                    }
                    break;
                }
                case group_edit_apply:
                    applyGroupEdits();
                    CrypServer.put(uniqueId, "group_edit_modal", "");// Clear group plan
                    break;
                case group_edit_cancel:
                    mGroupEdit.clear(); // Clear group edit plan
                    CrypServer.put(uniqueId, "group_edit_modal", "");// Clear model
                    CrypServer.putGroupEdit(uniqueId, new HashMap<Integer, String>());
                    break;
                case export_contacts: {

                    if( mSelectId.isEmpty())
                        CrypServer.notify(uniqueId, "Select at least one contact","warn");
                    else{
                        String fileName = "export.vcf";
                        File file = new File( m_ctx.getFilesDir()+"/"+fileName);
                        ExportVcf.writeContactVcard( mSelectId, file);
                        return "download:"+fileName;
                    }
                    break;
                }
                case print_contacts:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case find_merge:
                    CrypServer.notify(uniqueId, link_enum + " Not implemented yet", "warn");
                    break;
                case password_cancel:
                    CrypServer.put(uniqueId, "password_modal", "");
                    break;
                case restore_contacts:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case sort_first:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case sort_last:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case display_comfortable:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case display_cozy:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case display_compact:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case settings:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case dump_group:
                    Cursor c = MyGroups.getGroupContactsCursor(m_group_id, m_search);

                    log(LogUtil.LogType.LIST_HTM, "m_group_id  : " + m_group_id);
                    log(LogUtil.LogType.LIST_HTM, "m_account   : " + m_account);
                    log(LogUtil.LogType.LIST_HTM, "c.getCount(): " + c.getCount());
                    c.close();
                    MyGroups.dumpGroupTitleTable();
                    break;
                case file_upload_cancel:
                    CrypServer.put(uniqueId, "file_upload_modal", "");
                    break;
            }
        }
        return CConst.GENERATE_HTML;
    }

    /**
     * Apply the group modifications selected by the user
     */
    private static void applyGroupEdits() {

        log(LogUtil.LogType.LIST_HTM, "applyGroupModal()");
        /**
         * Iterate through the edit plan.
         */
        for( int gId : mGroupEdit.keySet()){

            int trashGroupId = MyGroups.getGroupId(m_account, CConst.TRASH);

            String command = mGroupEdit.get(gId);
            if( command.contains("1")){

                /**
                 * The plan only operates on the selected contacts, iterate selected contacts
                 */
                for( long cId : mSelectId){

                    MyGroups.addGroupMembership(m_ctx, cId, gId, true);// add and sync
                    MyGroups.deleteGroupRecords( m_ctx, cId, trashGroupId, true);// delete and sync
                }
            }else
            if( command.contains("0")){

                /**
                 * The plan only operates on the selected contacts, iterate selected contacts
                 */
                for( long cId : mSelectId){

                    MyGroups.deleteGroupRecords( m_ctx, cId, gId, true);// delete and sync
                }
            }
            // Else ignore indeterminate command "-"
        }
        /**
         * User will start with a new plan each time
         */
        mGroupEdit.clear();
        /**
         * Update in-memory data to match database
         */
        MyGroups.loadGroupMemory();
    }

    /**
     * Build the group edit model specific to the selected contacts and write it to a file.
     * The file will be injected into the DOM via javascript. Post a notification asking
     * the user to select at least one contact if they have not done so.
     */
    private static void buildGroupEditModal(){

        if( mSelectId.isEmpty()){

            buildNotificationFile("Please select at least one contact", "info",
                    CrypServer.group_edit_modal_filename);

        }else{

            HashMap<Integer, Integer> groupsThisPage = new HashMap<Integer, Integer>();
            int[] groupIds = MyGroups.getGroupIds(m_account);

            // Tally groups used with this page of contacts.
            for( long select_contact_id  : mSelectId){

                int[] groups = MyGroups.getGroups(select_contact_id);
                for( int group : groups){

                    // Bump counter for number of contacts in this group
                    int j = groupsThisPage.containsKey(group) ? groupsThisPage.get(group) : 0;
                    groupsThisPage.put(group, j + 1);
                }
            }

            try {

                MiniTemplator t = new MiniTemplator(WebService.assetsDirPath + "/group_edit_modal.htm");

                int trashGroupId = MyGroups.getGroupId(m_account, CConst.TRASH);

                //Build groups menu with dependency on all groups used on this page
                //Iterate through all the groups in this account
                for (int gId : groupIds) {

                    if( gId == trashGroupId)
                        continue;

                    int numberInGroup = groupsThisPage.containsKey(gId) ? groupsThisPage.get(gId) : 0;

                    String value_code = "value=\"0\""; // value="0"

                    if (numberInGroup == mSelectId.size())
                        value_code = "value=\"1\""; // value="1"
                    else if (numberInGroup > 0)
                        value_code = "value=\"\"";  // value=""

                    t.setVariable("cb_group_id", gId);
                    t.setVariable("value_code", value_code);
                    t.setVariable("group_select_title", MyGroups.mGroupTitle.get(gId));
                    t.addBlock("group_select");
                }

                String htm = t.generateOutput();

                File file = new File( m_ctx.getFilesDir()+"/"+CrypServer.group_edit_modal_filename);
                Util.writeFile( file, htm);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void buildNotificationFile(String message, String type, String filename) {

        String notifyJs = "notifyJs('"+message+"','"+type+"');";
        String wrapper = "<script>"+notifyJs+"</script>";
        File file = new File( m_ctx.getFilesDir()+"/"+filename);
        Util.writeFile( file, wrapper);
    }
}
