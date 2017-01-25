package com.nuvolect.securesuite.main;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlCipher.ATab;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.data.SqlCipher.KvTab;

public class CConst {

    public static final String FOLDER_NAME                        = "/securesuite/";
    public static final String TEMP_FOLDER                        = "/temp/";
    public static final String DEFAULT_ACCOUNT                    = "Default_account";
    public static final int PHOTO_ICON_MAX_EDGE                   = 192;
    //    public static final String DEFAULT_IP_PORT                    = "0.0.0.0:8000";
//    public static final int DEFAULT_PORT                          = 8000; // Different than other Nuvolect apps
    public static final String IP_PORT                            = "ip_port";
    public static final String PORT                               = "port";
    public static final String COMPANION_IP_PORT                  = "companion_ip_port";//Match settings.xml
    public static final int IP_TEST_TIMEOUT_MS                    = 8000;// Time for testing for companion device IP
    /** TIMEOUT delay when net up/down interrupt sync */
    public static final long FAILSAFE_SYNC_TIMEOUT                = 5 * 60 * 1000;

    public static final int RESPONSE_CODE_SUCCESS_100             = 100;
    public static final int RESPONSE_CODE_DONE_101                = 101;
    public static final int RESPONSE_CODE_USER_CANCEL_102         = 102;
    public static final int RESPONSE_CODE_EMPTY_MANIFEST_103      = 103;
    public static final int RESPONSE_CODE_JSON_ERROR_200          = 200;
    public static final int RESPONSE_CODE_SYNC_IN_PROCESS_201     = 201;
    public static final int RESPONSE_CODE_EXCEPTION_202           = 202;
    public static final int RESPONSE_CODE_MD5_FAIL_203            = 203;
    public static final int RESPONSE_CODE_ENCODING_EXCEPTION_204  = 204;
    public static final int RESPONSE_CODE_NO_BACKUP_SERVER_205    = 205;
    public static final int RESPONSE_CODE_PAYLOAD_EMPTY_206       = 206;
    public static final int RESPONSE_CODE_IP_NOT_REACHABLE_207    = 207;
    public static final int RESPONSE_CODE_WIFI_NOT_ENABLED_208    = 208;
    public static final int RESPONSE_CODE_REGISTRATION_ERROR_209  = 209;
    public static final int RESPONSE_CODE_COMPANION_TEST_FAIL_210 = 210;
    public static final int RESPONSE_CODE_SELF_TEST_FAIL_211      = 211;
    public static final int RESPONSE_CODE_SHARE_VCF               = 212;

    public enum NavMenu {contacts, groups, passwords, server }
    public enum NavChoice {Contacts, Groups, Passwords }

    /** for communicating between services and activities or web server to refresh */
    public static final String UI_TYPE_KEY          = "ui_type_key";
    public static final String CONTACTS             = "contacts";
    public static final String RECREATE             = "recreate";
    public static final String PASSWORDS            = "passwords";

    public final static int LARGE_ICON              = R.drawable.app_high_res_icon;
    public final static int SMALL_ICON              = R.drawable.icon_64;
    public static final String _ID                  = "_id";
    public static final String ACCOUNT_CRYP         = "account_cryp";
    public static final String ACCOUNT_DATA         = "account_data";
    public static final String ACCOUNT_ID           = "account_id";
    public static final String ACCOUNT_NAME         = ATab.account_name.toString();
    public static final String ACCOUNT_TYPE         = ATab.account_type.toString();
    public static final String ADDRESS              = DTab.address.toString();
    public static final String BLOG_URL             = "https://nuvolect.com/blog";
    public static final String COMMS_SELECT         = DTab.comms_select.toString();
    public static final String CONTACT              = "contact";
    public static final String CONTACT_ID           = SqlCipher.ADTab.contact_id.toString(); // DTab has the same field
    public static final String COUNTRY_CODE         = "country_code";
    public static final String COUNTS               = "counts";
    public static final String CRYP_SYNC            = "cryp_sync";
    public static final String DATES                = DTab.date.toString();
    public static final String DISPLAY_NAME         = DTab.display_name.toString();
    public static final String EMBEDDED_USER                      = "embedded_user";
    public static final String EMAIL                = DTab.email.toString();
    public static final String FIX_ERRORS           = "fix_errors";
    public static final String GENERATE_HTML        = "generate_html";
    public static final String GROUP_TITLE          = "group_title";
    public static final String ID                   = "id";
    public static final String IM                   = DTab.im.toString();
    public static final String INSERTS              = "inserts";
    public static final String INTERNATIONAL_NOTATION = "international_notation";
    public static final String INTERNETCALL         = DTab.internetcall.toString();
    public static final String KV                   = DTab.kv.toString();
    public static final String LAST_INCOMING_UPDATE = "last_outgoing_update";
    public static final String LAST_LOGIN_STATUS    = "last_login_status";
    public static final String LAST_OUTGOING_UPDATE = "last_incoming_update";
    public static final String MD5_PAYLOAD          = "md5_payload";
    public static final String NAME_FIRST           = KvTab.name_first.toString();
    public static final String NAME_LAST            = KvTab.name_last.toString();
    public static final String NAME_MIDDLE          = KvTab.name_middle.toString();
    public static final String NAME_PREFIX          = KvTab.name_prefix.toString();
    public static final String NAME_SUFFIX          = KvTab.name_suffix.toString();
    public static final String NICKNAME             = KvTab.nickname.toString();
    public static final String NOTE                 = KvTab.note.toString();
    public static final String NOTIFY_INCOMING_CALL = "notify_incoming_call";
    public static final String OK                   = "ok";
    public static final String ORGANIZATION         = KvTab.organization.toString();
    public static final String PAYLOAD              = "payload";
    public static final String PERMISSION_MANAGER   = "permission_manager";
    public static final String PHONE                = DTab.phone.toString();
    public static final String PHONETIC_FAMILY      = KvTab.phonetic_family.toString();
    public static final String PHONETIC_GIVEN       = KvTab.phonetic_given.toString();
    public static final String PHONETIC_MIDDLE      = KvTab.phonetic_middle.toString();
    public static final String PHOTO                = DTab.photo.toString();
    public static final String PREMIUM_FEATURE      = "premium_feature";
    public static final String RELATION             = DTab.relation.toString();
    public static final String RESPONSE_CODE        = "response_code";
    public static final String SEC_TOK              = "sec_tok";
    public static final String SERVER_ENABLED       = "server_enabled";
    public static final String SUBSCRIBER           = "subscriber";
    public static final String TITLE                = KvTab.title.toString();
    public static final String TOTAL                = "total";
    public static final String UNIQUE_ID                          = "unique_id";
    public static final String UPDATES              = "updates";
    public static final String VERSION              = ATab.version.toString();
    public static final String WEBSITE              = DTab.website.toString();

    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String STARRED_1 = "1";
    public static final String STARRED_0 = "0";
    public static final String CUSTOM = "CUSTOM";

    public static final String ADDRESS_TYPES[] = {"HOME","WORK","OTHER", "CUSTOM"};
    public static final String DATE_TYPES[] = {"BIRTHDAY","ANNIVERSARY","OTHER","CUSTOM"};
    public static final String EMAIL_TYPES[] = {"HOME","WORK","OTHER", "CUSTOM"};
    public static final String IM_TYPES[] = {"AIM","WINDOWS LIVE","YAHOO","SKYPE","QQ","HANGOUTS","ICQ","JABBER","CUSTOM"};
    public static final String INTERNETCALL_TYPES[] = {"HOME","WORK","OTHER", "CUSTOM"};
    public static final String PHONE_TYPES[] = {"MOBILE","WORK","HOME","MAIN","WORK FAX", "HOME FAX","PAGER","OTHER","CUSTOM"};
    public static final String RELATION_TYPES[] = {"ASSISTANT","BROTHER","CHILD", "DOMESTIC PARTNER","FATHER","FRIEND","MANAGER","MOTHER","PARENT","PARTNER","REFERRED BY","RELATIVE","SISTER","SPOUSE","CUSTOM"};
    public static final String WEBSITE_TYPES[] = {"WORK","HOME","OTHER", "CUSTOM"};

    public static final int CONTACT_LIST_LOADER = 1;
    public static final int CONTACT_DETAIL_LOADER = 2;
    public static final int CONTACT_EDIT_LOADER = 3;
    public static final String ACCOUNT = "account";
    public static final String SEARCH = "search";
    public static final String GROUP_ID = "group_id";
    /**
     * The fragment argument representing the item ID that this fragment represents.
     */
    public static final String CONTACT_ID_KEY = "contact_id";
    public static final String GROUP_ID_KEY = "group_id";

    public static final int    GROUP_ALL_IN_ACCOUNT           = -1;
    public static final int    GROUP_STARRED_IN_ACCOUNT       = -2;
    public static final String ALL_IN_ACCOUNT                 = "All in account";
    public static final String STARRED                        = "Starred";
    public static final String STARRED_IN_ANDROID             = "Starred in Android";
    public static final String MY_CONTACTS                    = "My Contacts";
    public static final String TRASH                          = "Trash";
    public static final String FRIENDS                        = "Friends";
    public static final String COWORKERS                      = "Coworkers";
    public static final String CUSTOM_ACCOUNT                 = "custom_account";
    public static final String GROUP_ACCOUNT_TYPE             = "user_custom";

    public static final int
            NO_ACTION                                         = 100,
            BROWSE_RESTORE_FOLDER_ACTION                      = 110,
            BROWSE_IMPORT_PHOTO_ACTION                        = 130,
            CHANGE_LOCK_CODE_ACTION                           = 140,
            VALIDATE_LOCK_CODE_ACTION                         = 150,
            CHANGE_LOCK_CODE_TEST_ACTION                      = 160,
            VALIDATE_LOCK_CODE_TEST_ACTION                    = 170,

            CALLER_ID_REQUEST_READ_PHONE_STATE                = 180,

            IMPORT_ACCOUNT_CONTACTS_REQUEST_READ_CONTACTS     = 190,
            IMPORT_ACCOUNT_CONTACTS_REQUEST_GET_ACCOUNTS      = 200,

            IMPORT_VCARD_REQUEST_EXTERNAL_STORAGE             = 210,
            IMPORT_VCARD_BROWSE_ACTION                        = 120,

            IMPORT_SINGLE_CONTACT_REQUEST_READ                = 230,
            IMPORT_SINGLE_CONTACT_PICKER                      = 220 ;

    public static final String RESTORE_BACKUP_PATH            = "restore_backup_path";
    public static final String IMPORT_VCF_PATH                = "import_vcf_path";
    public static final String IMPORT_PHOTO_PATH              = "import_photo_path";

    public static final String CONTACT_DETAIL_FRAGMENT_TAG    = "contact_detail_fragment_tag";
    public static final String CONTACT_EDIT_FRAGMENT_TAG      = "contact_edit_fragment_tag";
    public static final String CONTACT_LIST_FRAGMENT_TAG      = "contact_list_fragment_tag";
    public static final String EDIT_GROUP_FRAGMENT_TAG        = "edit_group_fragment_tag";
    public static final String GROUP_DETAIL_FRAGMENT_TAG      = "group_detail_fragment_tag";
    public static final String GROUP_EDIT_FRAGMENT_TAG        = "group_edit_fragment_tag";
    public static final String GROUP_LIST_FRAGMENT_TAG        = "group_list_fragment_tag";
    public static final String PASSWORD_FRAGMENT_TAG          = "password_fragment_tag";

    public static final String APP_VCF                        = "securesuite.vcf";

    public static final String DATABASE_PASSPHRASE            = "database_passphrase";
    public static final String RANDOM_EDGE                    = "h0!U9#Wfnx";
    public static final String THEME_SETTINGS                 = "theme_settings";

    public static final String DEFAULT_PASSPHRASE             = "WhenAllElseFails";
    public static final String MEMBERS                        = "members";
    public static final String MEMBERS_DELETED                = "members_deleted";

    public static final String IMPORT_PROGRESS                = "import_progress";
    public static final String IMPORT_ACCOUNT_LIST            = "import_account_list";
    public static final String IMPORT_SELECT_LIST             = "import_select_list";

    public static final String COMMS_SELECT_MOBILE            = "mobile";
    public static final String COMMS_SELECT_EMAIL             = "email";

    public static final String PASSPHRASE                     = "passphrase";
    public static final String LOCK_CODE                      = "lock_code";
    public static final String LOCK_INPUT_MODE                = "lock_input_mode";
    public static final String LOCK_INPUT_MODE_KEYBOARD       = "lock_input_mode_keyboard";
    public static final String LOCK_INPUT_MODE_NUMERIC        = "lock_input_mode_numeric";
    public static final String VALIDATE_LOCK_CODE             = "validate_lock_code";
    public static final String CHANGE_LOCK_CODE               = "change_lock_code";

    public static final String FILE_UPLOAD                    = "file_upload";
    public static final String RECORDS                        = "records";

    public static final String RESTFUL_HTM                    = "restful_htm";
    public static final String YUBIKEY                        = "yubikey";
    public static final String YUBIKEY_SERIAL1                = "yubikey_serial1";
    public static final String YUBIKEY_SERIAL2                = "yubikey_serial2";

}
