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

package com.nuvolect.securesuite.webserver;//

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.nuvolect.securesuite.data.NameUtil;
import com.nuvolect.securesuite.data.ExportVcf;
import com.nuvolect.securesuite.data.MyAccounts;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.main.SettingsActivity;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Passphrase;
import com.nuvolect.securesuite.util.Safe;
import com.nuvolect.securesuite.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import static com.nuvolect.securesuite.data.SqlCipher.ATab;
import static com.nuvolect.securesuite.data.SqlCipher.DTab;
import static com.nuvolect.securesuite.data.SqlCipher.KvTab;
import static com.nuvolect.securesuite.data.SqlCipher.add;
import static com.nuvolect.securesuite.data.SqlCipher.deleteIndexedItem;
import static com.nuvolect.securesuite.data.SqlCipher.get;
import static com.nuvolect.securesuite.data.SqlCipher.getKv;
import static com.nuvolect.securesuite.data.SqlCipher.putKv;
import static com.nuvolect.securesuite.util.LogUtil.log;

/** Generate content and manage user interactions of the Detail HTML page. */
public class DetailHtm {

    private static Context m_ctx;

    private static ArrayList<Long> mDisplayId;
    private static ArrayList<MultiFieldData> mMultiFieldData;
    private static int m_group_id = 0; // 0 == all in group
    private static String m_account;
    private static String m_search = "";
    private static Long m_contact_id;
    private static String templateFile = "detail.htm";

    private static String full_name_placeholder       = "Prefix first middle last suffix";
    private static String note_placeholder            = "Enter note";
    private static String nickname_placeholder        = "Enter nickname";
    private static String organization_placeholder    = "Enter organization";
    private static String password_placeholder        = "Generate or enter password";
    private static String phonetic_given_placeholder  = "Phonetic given name";
    private static String phonetic_middle_placeholder = "Phonetic middle";
    private static String phonetic_family_placeholder = "Phonetic family";
    private static String title_placeholder           = "Enter title";
    private static String username_placeholder        = "Enter username";

    /**
     * List of the main keys to associate with a key value pair.  This is typically a button or
     * a menu item versus a more simple link.
     */
    private enum KEYS {NIL,
        // Common to all pages
        account,        // Account menu select, value is account email
        group_id,       // Value is the group_id
        item_star,      // Item star, value is the contact_id
        link,           // Second level enum, Typically an html link, value is the link source
        search,         // Search string, value is the string
        theme,          // Value is user selected theme
        spacing,        // Page element spacing: comfortable, cozy, compact

        uri,                    // Full uri for routing
        queryParameterStrings,  // Raw parameters
        unique_id,

        // Specific to this page
        add,            // Add a field, value is the field, index will hold multi-field index
        cb_group_id,    // Value is the group_id of selected checkbox
        contact_id,     // Value is the contact_id
        update,         // Value is field updated, content is new value, index for multi-field
        content,        // Value is the content of the field_update field
        delete,         // Value is the field to delete, index will hold multi-field index
        file_upload,
        new_group,      // Value is the new group name
        new_group_contacts, // Value is the new group name, contact is added
        index,          // Value is index of a multi-field, multiple emails, mobiles, etc.
        item,           // List item selected for details, value is the contact_id
        export_contact, // Value is contact_id to export
        password_update,// For a contact record, value is a the password KV value
        password,       // Archive, value is most recent password
        password_mode,  // Archive, value is the mode
        password_length,// Archive, value is the length
    }

    enum FIELD { NIL,
        // Single instance fields, i.e., only one on the page
        full_name,
        name,               // DTab.display_name
        nickname,           // KvTab.nickname
        note,
        organization,
        phoneticname,
        phonetic_given,
        phonetic_middle,
        phonetic_family,
        title,
        username,           // KvTab.username, Value is an updated username
        password,           // KvTab.password, Value is an updated password

        //Multi-instance-fields, indexed by zero based "index"
        address,            // DTab.address
        date,               // DTab.dates
        email,              // DTab.email
        phone,              // DTab.phone
        relation,           // DTab.relation
        website,            // DTab.website
    }

    enum LINKS {NIL,
        all_in_account,     // Switch to list page, show all in account
        display_comfortable,// Update CSS
        display_compact,    // Update CSS
        display_cozy,       // Update CSS
        email,              // Start email with the first email address
        export_contact,
        find_merge,
        help,
        import_contacts,
        merge_contacts,
        new_contact,
        print_contacts,
        restore_contacts,
        settings,
        sort_first,
        sort_last,
        contact_photo,     //TODO implement edit/change of contact photo
    }

    public static void init(Context ctx){

        m_ctx = ctx;
        mDisplayId = new ArrayList<Long>();
        mMultiFieldData = new ArrayList<MultiFieldData>();
    }

    public static String render(Context ctx, String uniqueId, Map<String, String> params) {

        m_ctx = ctx;
        m_search = CrypServer.get( uniqueId, "search");
        m_account = CrypServer.get(uniqueId, "account");
        m_group_id = Integer.valueOf(CrypServer.get(uniqueId, "group_id"));

        /**
         * Parse parameters and process any updates.
         * Return an Action indicating what to do next
         */
        String action = parse(uniqueId, params);// set m_contact_id & others based on params

        if( action.startsWith("download:"))  // Check for download, action string includes filename
            return action;

        // else, action is GENERATE_HTML

        /**
         * Muilti-field data stores the field type and index for each field that
         * may have more than one entry such as email or mobile number.
         *
         * The element ids are unique for each document and are persisted between
         * page generations.  When a user interacts with a document element, the ID of
         * that element will be sent in POST data and managed in the previous parse() method.
         * For each new page generated the element ids are generated starting with _0 and
         * saved in an ArrayList.
         */
        mMultiFieldData = new ArrayList<MultiFieldData>();

        /**
         * Generate modals on another thread and continue generating html on this thread.
         * This way the html can be presented more quickly and modals will be ready
         * for use when needed. This assumes the modal is generated before the user presses
         * a button to use it.
         */
        new Thread(new Runnable() {
            @Override
            public void run() {

                //FIXME only call one time
                PasswordModal.buildPasswordModal(m_ctx, true);// With Use password button
                PasswordModal.buildPasswordModal(m_ctx, false);// no extra button
            }
        }).start();
        /**
         * Generate the page html and return it as the session response
         */
        return generateHtml( uniqueId, params);
    }

    private static String generateHtml(String uniqueId, Map<String, String> params) {

        String generatedHtml = "";

        try {
            MiniTemplator t = new MiniTemplator(WebService.assetsDirPath + "/" + templateFile);

            int theme = SettingsActivity.getThemeNumber(m_ctx);
            t.setVariable("theme", AppTheme.getThemeName(m_ctx, theme));

            t.setVariable("full_name_placeholder",       full_name_placeholder);
            t.setVariable("note_placeholder",            note_placeholder);
            t.setVariable("nickname_placeholder",        nickname_placeholder);
            t.setVariable("organization_placeholder",    organization_placeholder);
            t.setVariable("password_placeholder",        password_placeholder);
            t.setVariable("phonetic_given_placeholder",  phonetic_given_placeholder);
            t.setVariable("phonetic_middle_placeholder", phonetic_middle_placeholder);
            t.setVariable("phonetic_family_placeholder", phonetic_family_placeholder);
            t.setVariable("title_placeholder",           title_placeholder);
            t.setVariable("username_placeholder",        username_placeholder);

            t.setVariable("mSearch", m_search);
            t.setVariable("contact_id", String.valueOf(m_contact_id));

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
            // Show contact photo if they have one
            String encodedImage = SqlCipher.get(m_contact_id, DTab.photo);
            if( encodedImage.isEmpty())
                t.setVariable("contact_photo", "/img/contact_picture_large.png");
            else{
                String contact_photo = "contact_photo.png";
                FileOutputStream out = null;
                try {
                    //Save the photo and then load it
                    byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                    Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    out = m_ctx.openFileOutput(contact_photo, Context.MODE_PRIVATE);
                    decodedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //Load the photo
                t.setVariable("contact_photo", m_ctx.getFilesDir() + "/" + contact_photo);
            }

            /**
             * Format the left side list of groups Title(count)
             * proto: <a href="" onclick="post('group_link=1234')">Family(3)</a>
             */
            int allContacts = MyAccounts.getContactCount( m_account);
            String link_title = "All in account ("+ allContacts +")";
            if( m_group_id == 0)// Make the current group BOLD
                t.setVariable("all_in_account", "<strong>" + link_title + "</strong>");
            else
                t.setVariable("all_in_account", link_title);

            int starContacts = MyAccounts.getStarredCount(m_account);
            String star_title = "Starred ("+ starContacts +")";
            if( m_group_id == CConst.GROUP_STARRED_IN_ACCOUNT)// Make the current group BOLD
                t.setVariable("starred_in_account", "<strong>" + star_title + "</strong>");
            else
                t.setVariable("starred_in_account", star_title);

            int[] groupIds = MyGroups.getGroupIds(m_account);
            String[] groupTitles = MyGroups.getGroupTitlesStringArray(m_account);

            for( int i=0; i < groupIds.length; i++) {

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

            /**
             * Build the contact group pull-down checkbox list
             */
            boolean[] groupCheckState = MyGroups.getGroupCheckState(m_account, m_contact_id);

            for( int i =0; i < groupCheckState.length; i++){

                String value_code = (groupCheckState[i] ? "value=\"1\"" : "value=\"0\"");
                t.setVariable("cb_group_id", groupIds[i]);
                t.setVariable("value_code", value_code);
                t.setVariable("group_select_title", groupTitles[i]);
                t.addBlock("group_select");
            }

            // Used for the browser tag description
            String display_name = get(m_contact_id, DTab.display_name);
            t.setVariable("display_name", display_name);

            String full_name = NameUtil.getFullName(m_contact_id);
            t.setVariable("full_name", full_name.isEmpty() ? full_name_placeholder : full_name);

            String star = SqlCipher.get(m_contact_id, ATab.starred);
            if( star.startsWith("0")){
                t.setVariable("star_state", "0");
                t.setVariable("star_icon", "glyphicon-star-empty");
            }
            else{
                t.setVariable("star_state", "1");
                t.setVariable("star_icon", "glyphicon-star");
            }

            String title = getKv(m_contact_id, KvTab.title);
            t.setVariable("title", title.isEmpty() ? title_placeholder : title);

            String organization = getKv( m_contact_id, KvTab.organization);
            t.setVariable("organization", organization.isEmpty() ? organization_placeholder : organization);

            String phonetic_given = getKv( m_contact_id, KvTab.phonetic_given);
            String phonetic_middle = getKv( m_contact_id, KvTab.phonetic_middle);
            String phonetic_family = getKv( m_contact_id, KvTab.phonetic_family);
            if( ! phonetic_given.isEmpty() || ! phonetic_middle.isEmpty() || ! phonetic_family.isEmpty()){

                if( phonetic_given.contentEquals("."))phonetic_given = "";
                t.setVariable("phonetic_given",  phonetic_given.isEmpty()  ? phonetic_given_placeholder  : phonetic_given);
                t.setVariable("phonetic_middle", phonetic_middle.isEmpty() ? phonetic_middle_placeholder : phonetic_middle);
                t.setVariable("phonetic_family", phonetic_family.isEmpty() ? phonetic_family_placeholder : phonetic_family);
                t.addBlock("phonetic_block");
            }

            String nickname = getKv(m_contact_id, KvTab.nickname);
            if( ! nickname.isEmpty()){

                t.setVariable("nickname", nickname);
                t.addBlock("nickname_block");
            }

            /**
             * List of contact groups displayed under contact name, organization and title
             */
            String[] groupArray = MyGroups.getGroupTitleArray(m_contact_id);
            for( String groupTitle : groupArray){
                t.setVariable("contact_group_title", groupTitle);
                t.addBlock("contact_group");
            }

            fillDetail(t, "email",   m_contact_id, DTab.email, true);
            fillDetail(t, "phone",   m_contact_id, DTab.phone, true);
            fillDetail(t, "address", m_contact_id, DTab.address, true);
            fillDetail(t, "date",    m_contact_id, DTab.date, false);
            fillDetail(t, "website", m_contact_id, DTab.website, false);
            fillUsername(t, m_contact_id);
            fillPassword(t, m_contact_id);
            fillDetail(t, "relation", m_contact_id, DTab.relation, false);

            String note= getKv(m_contact_id, KvTab.note);
            t.setVariable("note", note.trim().isEmpty() ? note_placeholder : note);
            t.setVariable("notify_js", CrypServer.getNotify(uniqueId));

            generatedHtml = t.generateOutput();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return generatedHtml;
    }

    /**
     * Fill in username details if username is not empty
     * @param t  - Template
     * @param id - contact id
     */
    static void fillUsername(MiniTemplator t, long id){

        String username = SqlCipher.getKv( id, KvTab.username);

        if( ! username.isEmpty()){

            t.setVariable("username", username);
            t.addBlock("username_block");
        }
    }

    /**
     * Fill in password details if password is not empty
     * @param t  - Template
     * @param id - contact id
     */
    static void fillPassword(MiniTemplator t, long id){

        String password = SqlCipher.getKv( id, KvTab.password);

        if( ! password.isEmpty()){

            t.setVariable("password", password);
            t.addBlock("password_block");
        }
    }


    /**
     * Fill a detail block in the contact detail page.  If the detail is absent, fill nothing.
     * @param t    : template
     * @param tVar : dettail table field string
     * @param id   : contact ID
     * @param dTab : detail table field
     */
    public static void fillDetail(
            MiniTemplator t, String tVar, Long id, SqlCipher.DTab dTab, boolean forceAdd ) {

        try {
            JSONArray jsonArray = new JSONArray( SqlCipher.get(id, dTab));
            int len = jsonArray.length();
            if( len > 0){

                for( int i = 0; i < len; i++){

                    JSONObject item = jsonArray.getJSONObject(i);
                    Iterator<?> item_keys = item.keys();
                    String item_label = (String) item_keys.next();
                    String item_value = item.getString(item_label);

                    t.setVariable("id_l", nextId(dTab, DType.label, i));  // Document element ID
                    t.setVariable(tVar+"_cat", item_label);

                    t.setVariable("id_v", nextId(dTab, DType.value, i));  // Document element ID
                    t.setVariable(tVar+"_value", item_value);


                    t.setVariable("del_i", String.valueOf(i));

                    switch (dTab){
                        case website:

                            if( item_value.startsWith("http"))
                                t.setVariable("website_http_value", item_value);
                            else
                                t.setVariable("website_http_value", "http://"+item_value);
                            break;
                        case address:
                            String addressUri = item_value.replaceAll("[\\t\\n\\r]",", ");
                            t.setVariable("address_uri_value", addressUri);
                            break;
                        default:
                            break;
                    }

                    t.addBlock(tVar);
                }
                t.addBlock(tVar+"_post");
            }else{

                if( forceAdd)
                    t.addBlock(tVar+"_post");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
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

        String key="";
        String value="";

        for (Map.Entry<String, String> entry : params.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();

            /**
             * The inistantedit.js fieldBlur method sends [object Object] as a key with no value.
             * It is not used here so just skip it.
             */
            if( key.startsWith("[object"))
                continue;

            KEYS key_enum = KEYS.NIL;
            try {
                key_enum = KEYS.valueOf(key);
            } catch (Exception e) {
                log(LogUtil.LogType.DETAIL_HTM, "Error unknown key: " + key);
                LogUtil.logException(m_ctx, LogUtil.LogType.DETAIL_HTM, e);
            }
            LINKS link_enum = LINKS.NIL;

            switch(key_enum) {

                case NIL: {
                    log(LogUtil.LogType.DETAIL_HTM, "Error NIL key: " + key);
                    break;
                }
                case account:

                    throw new RuntimeException( "Account change handled by ListHtm");
                case group_id: {
                    /**
                     * Save the group_id, reset dependent session data
                     */
                    m_group_id = Integer.valueOf(value);
                    CrypServer.put(uniqueId, "group_id", m_group_id);

                    m_search = ""; // start with empty search
                    CrypServer.put(uniqueId, "search", m_search);
                    break;
                }
                case search:
                    m_search = value;
                    CrypServer.put(uniqueId, "search", m_search);
                    break;
                case item:{
                    m_contact_id = Long.valueOf(value);
                    log(LogUtil.LogType.DETAIL_HTM, "item m_contact_id: " + m_contact_id);
                    break;
                }
                case item_star: {
                    long contactId = Long.valueOf(value);
                    String star = get(contactId, ATab.starred);// 0 == unstarred
                    if( star.startsWith("0"))
                        SqlCipher.put(contactId, ATab.starred, "1");
                    else
                        SqlCipher.put(contactId, ATab.starred, "0");
                    // Group star count has changed
                    // Load group data into memory, used for group titles and people counts
                    MyGroups.loadGroupMemory();
                    break;
                }
                case link:
                    try {
                        link_enum = LINKS.valueOf(value);
                    } catch (Exception e) {
                        log(LogUtil.LogType.DETAIL_HTM, "Error unknown link: " + value);
                        LogUtil.logException(m_ctx, LogUtil.LogType.DETAIL_HTM, e);
                    }
                    break;
                case contact_id:
                    break;
                case update:// value == field name, content has the content

                    if( value.startsWith("_")) {  // Look for an element ID
                        int eid = Integer.valueOf(value.substring(1));
                        updateMultiField(eid, params.get("content"));// Update field based on unique element ID
                    }else{

                        updateField(value, params.get("content"));
                    }
                    break;
                case index:
                    break;
                case add:{
                    String itemType = value;
                    addItem(itemType);
                    break;
                }
                case delete:{
                    String itemType = value;
                    String itemIndex = params.get("index");
                    deleteItem( itemType, itemIndex);
                    break;
                }
                case cb_group_id:{

                    MyGroups.invertGroupSelect( m_ctx, m_contact_id, Integer.valueOf(value));
                    break;
                }
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
                case new_group_contacts:{ // Create new group and add user to the new group
                    String newGroupTitle = Safe.safeString(value);

                    if ( newGroupTitle.isEmpty())
                        CrypServer.notify(uniqueId, "Enter a new group name", "warn");
                    else if ( MyGroups.groupNameInUse(newGroupTitle, m_account))
                        CrypServer.notify(uniqueId, "Group already exists","warn");
                    else{
                        int newGroupId = MyGroups.addGroup(m_ctx, newGroupTitle, m_account, "");
                        MyGroups.addGroupMembership( m_ctx, m_contact_id, newGroupId, true);
                        MyGroups.loadGroupMemory();
                    }
                    break;
                }
                case file_upload:
                    break;
                case export_contact: {
                    m_contact_id = Long.valueOf( value );
                    String userName = SqlCipher.get( m_contact_id, ATab.display_name);
                    String fileName = Safe.safeString( userName.replaceAll("\\W+", ""));
                    if( fileName.isEmpty())
                        fileName = "contact";
                    fileName = fileName + ".vcf";

                    // Create a folder for temporary use if necessary
                    new File( m_ctx.getFilesDir()+CConst.VCF_FOLDER).mkdirs();

                    File file = new File( m_ctx.getFilesDir()+CConst.VCF_FOLDER+fileName);
                    ExportVcf.writeContactVcard(m_contact_id, file);
                    return "download:"+fileName;
                }
                case password_update:{

                    //FUTURE run all user input through Safe to avoid SQL injection
                    SqlCipher.putKv(m_contact_id, KvTab.password, value.trim());
                    break;
                }
                case password_mode:
                    Passphrase.setPasswordGenMode(m_ctx, Integer.valueOf(value));
                    break;
                case password_length:
                    Passphrase.setPasswordLength(m_ctx, Integer.valueOf(value.trim()));
                    break;
                case password:
                    Passphrase.appendPasswordHistory(m_ctx, value);
                    break;
                case theme:{

                    log(LogUtil.LogType.DETAIL_HTM,"theme selected: " + value);
                    SettingsActivity.setTheme(m_ctx, value);
                    break;
                }
                case uri:
                case queryParameterStrings:
                case unique_id:
                    break;
                default:
                    log(LogUtil.LogType.DETAIL_HTM, "ERROR, unmanaged key_enum: " + key_enum);
            }

            switch (link_enum){

                case NIL:
                    break;
                case all_in_account:
                    /**
                     * Save the group_id, reset dependent session data
                     */
                    m_group_id = 0;// special for all contacts in account
                    CrypServer.put(uniqueId, "group_id", m_group_id);

                    m_search = ""; // start with empty search
                    CrypServer.put(uniqueId, "search", m_search);
                    break;
                case display_comfortable:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case display_compact:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case display_cozy:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case find_merge:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case import_contacts:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case merge_contacts:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case new_contact:
                    m_contact_id = SqlCipher.createEmptyContact(m_ctx, m_group_id);
                    CrypServer.put(uniqueId, "contact_id", String.valueOf(m_contact_id));
                    break;
                case print_contacts:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case restore_contacts:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case settings:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case help:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case sort_first:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                case sort_last:
                    CrypServer.notify(uniqueId, link_enum+" Not implemented yet","warn");
                    break;
                default:
                    log(LogUtil.LogType.DETAIL_HTM, "ERROR, unmanaged link_enum: " + link_enum);
            }
        }
        return CConst.GENERATE_HTML ;// Default to generate html as next step
    }

    private static void addItem(String fieldString) {

        FIELD field = FIELD.valueOf(fieldString);

        switch (field){

            case address:
                add(m_contact_id, DTab.address, "Home", "Enter address");
                break;
            case date:
                add(m_contact_id, DTab.date, "Birthday", "Enter date");
                break;
            case email:
                add(m_contact_id, DTab.email, "Home", "Enter email");
                break;
            case phoneticname:
                putKv(m_contact_id, KvTab.phonetic_given, phonetic_given_placeholder );
                break;
            case nickname:
                putKv(m_contact_id, KvTab.nickname, nickname_placeholder);
                break;
            case phone:
                add(m_contact_id, DTab.phone, "Home", "Enter phone");
                break;
            case relation:
                add(m_contact_id, DTab.relation, "Wife", "Enter relationship");
                break;
            case website:
                add(m_contact_id, DTab.website, "Business", "Enter website");
                break;
            case username:
                putKv(m_contact_id, KvTab.username, username_placeholder);
                break;
            case password:
                putKv(m_contact_id, KvTab.password, password_placeholder);
                break;
            default:
                log(LogUtil.LogType.DETAIL_HTM, "addItem.Default unhandled key: " + field);
        }
    }

    private static void deleteItem(String fieldString, String indexString) {

        FIELD field = FIELD.valueOf(fieldString);
        int index = 0;

        switch (field){

            case address:
                index = Integer.valueOf(indexString);
                deleteIndexedItem( m_contact_id, DTab.address, index);
                break;
            case date:
                index = Integer.valueOf(indexString);
                deleteIndexedItem( m_contact_id, DTab.date, index);
                break;
            case email:
                index = Integer.valueOf(indexString);
                deleteIndexedItem( m_contact_id, DTab.email, index);
                break;
            case nickname:
                break;
            case phone:
                index = Integer.valueOf(indexString);
                deleteIndexedItem( m_contact_id, DTab.phone, index);
                break;
            case relation:
                index = Integer.valueOf(indexString);
                deleteIndexedItem( m_contact_id, DTab.relation, index);
                break;
            case website:
                index = Integer.valueOf(indexString);
                deleteIndexedItem( m_contact_id, DTab.website, index);
                break;
            case username:
                SqlCipher.putKv(m_contact_id, KvTab.username, "");
                break;
            case password:
                SqlCipher.putKv( m_contact_id, KvTab.password, "");
                break;
            default:
                log(LogUtil.LogType.DETAIL_HTM, "deleteItem.Default unhandled key: " + fieldString);
        }
    }

    enum DType { value, label }

    private static class MultiFieldData {
        DTab dTab;
        DType dType;
        int index;

        MultiFieldData(DTab dTab, DType dType, int index){

            this.dTab = dTab;
            this.dType = dType;
            this.index = index;
        }
    };

    /**
     * Save the element details and return the reference ID used for the html onClick;
     * @param dTab The specific field defined by DTAB, email, mobile, relation, etc.
     * @param dType The type/label of field or category, Work, Home, etc.
     * @param index Which specific instance, 0 == first email, etc.
     * @return
     */
    private static String nextId(DTab dTab, DType dType, int index){

        mMultiFieldData.add(new MultiFieldData(dTab, dType, index));
        return "_"+(mMultiFieldData.size()-1);
    }

    private static void updateMultiField(int eid, String eid_value) {

        MultiFieldData fieldData;

        if( eid < mMultiFieldData.size())
            fieldData = mMultiFieldData.get(eid);
        else{
            log(LogUtil.LogType.WEB_SERVER, "eid out of bounds: "+eid);
            return;
        }

        switch ( FIELD.valueOf(fieldData.dTab.toString())){

            case NIL:
                break;
            case address:
                if( fieldData.dType == DType.value)
                    SqlCipher.updateValue(m_contact_id, DTab.address, fieldData.index, eid_value);
                else
                    SqlCipher.updateLabel(m_contact_id, DTab.address, fieldData.index, eid_value);
                break;
            case date:
                if( fieldData.dType == DType.value)
                    SqlCipher.updateValue(m_contact_id, DTab.date, fieldData.index, eid_value);
                else
                    SqlCipher.updateLabel(m_contact_id, DTab.date, fieldData.index, eid_value);
                break;
            case email:
                if( fieldData.dType == DType.value)
                    SqlCipher.updateValue(m_contact_id, DTab.email, fieldData.index, eid_value);
                else
                    SqlCipher.updateLabel(m_contact_id, DTab.email, fieldData.index, eid_value);
                break;
            case phone:
                if( fieldData.dType == DType.value)
                    SqlCipher.updateValue(m_contact_id, DTab.phone, fieldData.index, eid_value);
                else
                    SqlCipher.updateLabel(m_contact_id, DTab.phone, fieldData.index, eid_value);
                break;
            case relation:
                if( fieldData.dType == DType.value)
                    SqlCipher.updateValue(m_contact_id, DTab.relation, fieldData.index, eid_value);
                else
                    SqlCipher.updateLabel(m_contact_id, DTab.relation, fieldData.index, eid_value);
                break;
            case website:
                if( fieldData.dType == DType.value)
                    SqlCipher.updateValue(m_contact_id, DTab.website, fieldData.index, eid_value);
                else
                    SqlCipher.updateLabel(m_contact_id, DTab.website, fieldData.index, eid_value);
                break;
            default:
                log(LogUtil.LogType.DETAIL_HTM, "updateMultiField.Default unhandled key: " + eid_value);
        }
    }

    private static void updateField(String fieldName, String content) {

        log(LogUtil.LogType.DETAIL_HTM, "fieldName: " + fieldName+", content: "+content);

        FIELD field_enum = FIELD.NIL;
        try {
            field_enum = FIELD.valueOf(fieldName);
        } catch (Exception e) {
            log(LogUtil.LogType.DETAIL_HTM, "Error unknown key: " + fieldName);
            LogUtil.logException(m_ctx, LogUtil.LogType.DETAIL_HTM, e);
        }

        switch(field_enum) {

            case NIL: {
                log(LogUtil.LogType.DETAIL_HTM, "Error NIL key: " + fieldName);
                break;
            }
            case full_name:
                // Parse name into parts and save to both databases
                NameUtil.parseFullNameToKv(m_contact_id, content.trim());
                NameUtil.setNamesFromKv(m_contact_id);
                break;
            case organization:
                SqlCipher.putKv(m_contact_id, KvTab.organization, content.trim());
                NameUtil.setNamesFromKv(m_contact_id);
                break;
            case title:
                SqlCipher.putKv(m_contact_id, KvTab.title, content.trim());
                break;
            case note:
                SqlCipher.putKv(m_contact_id, KvTab.note, content);
                break;
            case username:
                SqlCipher.putKv(m_contact_id, KvTab.username, content.trim());
                break;
            case password:
                SqlCipher.putKv(m_contact_id, KvTab.password, content.trim());
                break;
            case phonetic_given:
                SqlCipher.putKv(m_contact_id, KvTab.phonetic_given, content.trim());
                break;
            case phonetic_middle:
                SqlCipher.putKv(m_contact_id, KvTab.phonetic_middle, content.trim());
                break;
            case phonetic_family:
                SqlCipher.putKv(m_contact_id, KvTab.phonetic_family, content.trim());
                break;
            case nickname:
                SqlCipher.putKv(m_contact_id, KvTab.nickname, content.trim());
                break;
            default:
                log(LogUtil.LogType.DETAIL_HTM, "updateField.Default unhandled key: " + fieldName);
        }
    }
}

