package com.nuvolect.securesuite.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Base64;

import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Safe;
import com.nuvolect.securesuite.util.Util;

import net.sqlcipher.DatabaseUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Import Google cloud contacts.
 */
public class ImportContacts {

    //    private static final boolean DEBUG = LogUtil.DEBUG;
    private static final boolean DEBUG = false;
    private static Callbacks mListener;
    private static boolean mInterruptImport;
    private static ArrayList<String> mImportAccounts;

    /**
     * A callback interface that all activities containing this class must implement.
     */
    public interface Callbacks {

        public void reportProgresss(int progress);
    }

    public static void interruptImport(){

        mInterruptImport = true;
    }

    /**
     * Import a single contact.  A new contact will be created each time.
     * Duplicate contacts can be created.
     * @param ctx
     * @param cloud_c_id
     * @return
     */
    public static boolean importSingleContact(Context ctx, long cloud_c_id){

        SqlCipher.setupImport();

        ContentResolver contentResolver = ctx.getContentResolver();

        /// Get name.  Store as simple JSONObject key:value pairs
        String where = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
        String[] params = new String[] { cloud_c_id +"", ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE };
        Cursor c = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, where, params, null);

//            Util.dumpCursorDescription("Import single",c);

        c.moveToFirst();

        if( c.getCount() > 0){

            int account_name_column_index =        c.getColumnIndex( RawContacts.ACCOUNT_NAME);
            int account_type_column_index =        c.getColumnIndex( RawContacts.ACCOUNT_TYPE);
            int display_name_column_index =        c.getColumnIndex( RawContacts.DISPLAY_NAME_PRIMARY);
            int display_name_source_column_index = c.getColumnIndex( RawContacts.DISPLAY_NAME_SOURCE);
            int starred_column_index =             c.getColumnIndex( RawContacts.STARRED);
            int version_column_index =             c.getColumnIndex( RawContacts.VERSION);

            String account_name =                  c.getString( account_name_column_index).toLowerCase(Locale.US);
            String account_type =                  c.getString( account_type_column_index).toLowerCase(Locale.US);
            String starred =                       c.getString( starred_column_index);
            int cloud_version =                    c.getInt( version_column_index);
            String last_name = ""; // provide non-null value for now, replace in later step
            String display_name =                  s(c.getString( display_name_column_index));
            int display_name_source =              c.getInt( display_name_source_column_index);
            c.close();

            long contact_id = SqlCipher.getNextUnusedContactID();

            boolean success = SqlCipher.updateAccountRecord(
                    account_name, account_type,
                    cloud_c_id, cloud_version,
                    contact_id,
                    starred,
                    last_name, display_name, display_name_source);


            if( success )
                success = replaceDetailRecord(ctx, account_name, cloud_c_id, contact_id, false);// false==Don't import groups
            /**
             * Update account table record first and last name elements with KvTab data
             */
            if( success)
                success = 1 == SqlCipher.updateATabFirstLastName(contact_id);

            if( ! success )
                SqlCipher.deleteContact(ctx, contact_id, false);

            return success;
        }
        else{
            c.close();
            return false;
        }
    }

    /**
     * Import cloud accounts selected by user into local database.
     * @param ctx
     * @param cloudAccountTitle
     * @param userSelectedAccount
     * @param listener
     * @return
     */
    public static int importAccountContacts(
            Context ctx, CharSequence[] cloudAccountTitle, boolean[] userSelectedAccount,
            Callbacks listener){

        mListener = listener;
        mInterruptImport = false;
        mImportAccounts = new ArrayList<String>();

        /**
         * Convert the user choices into an ArrayList of selected accounts
         */
        for(int i =0; i < userSelectedAccount.length; i++){

            if( userSelectedAccount[i])
                mImportAccounts.add( cloudAccountTitle[i].toString());
        }

        ContentResolver contentResolver = ctx.getContentResolver();
        int importCount = 0;
        int count = 0;

        SqlCipher.setupImport();

        // Load all of the android group id and title data into secure sql and in-memory representations
        importGroups( ctx );

        // Get data on every android contact to determine how/if to process it
        String selectionA = ContactsContract.RawContacts.DELETED + " != 1";
        String selectionB = ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + " <> ''";
        String selection = DatabaseUtils.concatenateWhere( selectionA, selectionB);

        String [] projection = new String [] {
                RawContacts.ACCOUNT_NAME,
                RawContacts.ACCOUNT_TYPE,
                RawContacts.CONTACT_ID,
                RawContacts.DELETED,
                RawContacts.RAW_CONTACT_IS_USER_PROFILE,
                RawContacts.DISPLAY_NAME_PRIMARY,
                RawContacts.DISPLAY_NAME_SOURCE,
                RawContacts.STARRED,
                RawContacts.VERSION,
        };

        Cursor c = contentResolver.query( RawContacts.CONTENT_URI, projection, selection, null, null);
        if( DEBUG) Util.dumpCursorDescription( "ContactsContract.AUTHORITY_URI", c);

        // Get the index for each column one time, and use it each time in the loop
        // This will save 13-21% in performance to capture these indexes one time
        // http://stackoverflow.com/questions/9114086/optimizing-access-to-cursors-in-android-position-vs-column-names
        int account_name_column_index =        c.getColumnIndex( RawContacts.ACCOUNT_NAME);
        int account_type_column_index =        c.getColumnIndex( RawContacts.ACCOUNT_TYPE);
        int contact_id_column_index =          c.getColumnIndex( RawContacts.CONTACT_ID);
        //        int deleted_column_index =   c.getColumnIndex( RawContacts.DELETED);
//        int is_user_profile_column_index =     c.getColumnIndex( RawContacts.RAW_CONTACT_IS_USER_PROFILE);
        int display_name_column_index =        c.getColumnIndex( RawContacts.DISPLAY_NAME_PRIMARY);
        int display_name_source_column_index = c.getColumnIndex( RawContacts.DISPLAY_NAME_SOURCE);
        int starred_column_index =             c.getColumnIndex( RawContacts.STARRED);
        int version_column_index =             c.getColumnIndex( RawContacts.VERSION);

        while( c.moveToNext() && ! mInterruptImport){

            String account_name =  c.getString( account_name_column_index).toLowerCase(Locale.US);

            if( ! mImportAccounts.contains( account_name))
                continue;

            long cloud_c_id =        c.getLong( contact_id_column_index);
            String account_type =  c.getString( account_type_column_index);
            String last_name = ""; // provide non-null value for now, replace in later step
            String display_name =s(c.getString( display_name_column_index));
            int display_name_source = c.getInt( display_name_source_column_index);
            String starred =       c.getString( starred_column_index);
            int cloud_version =       c.getInt( version_column_index);

//            if( 1 == c.getInt( is_user_profile_column_index )){
//
//                LogUtil.log("user profile: "+display_name);
//            }

            long contact_id = SqlCipher.testIdTestVersion( account_name, cloud_c_id, cloud_version);

            if( contact_id > 0L){

                SqlCipher.updateAccountRecord(
                        account_name, account_type,
                        cloud_c_id, cloud_version,
                        contact_id,
                        starred,
                        last_name, display_name, display_name_source);

                replaceDetailRecord( ctx, account_name, cloud_c_id, contact_id, true);// true==import groups
                ++importCount;
            }
            ++count;
            if( mListener != null && count % 10 == 0)
                mListener.reportProgresss( count);
        }

        c.close();

        if( mListener != null )
            mListener.reportProgresss( count);

        // Add base groups to each account, no duplicates will be created
        for( String account : mImportAccounts)
            MyGroups.addBaseGroupsToNewAccount(ctx, account);

        MyGroups.removeNonBaseEmptyGroups(ctx);
        MyGroups.removeStarredInAndroidGroups(ctx);

        SqlCipher.validateGroups(ctx, true);

        if( mListener != null )
            mListener.reportProgresss( count+1);
        /**
         * Update all account table records first and last name elements with KvTab data
         */
        SqlCipher.updateATabFirstLastName();

        //FUTURE fix photo import, Arron Hutchison has a photo that does not get imported

        return importCount;
    }

    /**
     * Collect details of the contact record and write to the detail db.
     * Create group records for contact.
     * Return -1 if there was an error, otherwise return the _id (contact_id).
     * @param ctx
     * @param account_name, both the local and cloud account name, they are the same
     * @param cloud_c_id, cloud contact_id, if remapped, different than local contact_id
     * @param contact_id, local contact_id, different from cloud_c_id if remapped
     * @return
     */
    private static boolean replaceDetailRecord(
            Context ctx,
            String account_name,
            long cloud_c_id,
            long contact_id,
            boolean import_groups) {

        ContentValues cv = new ContentValues();
        JSONObject kv = new JSONObject();
        JSONArray jarray = new JSONArray();
        ContentResolver contentResolver = ctx.getContentResolver();

        try {
            cv.put(CConst.CONTACT_ID, contact_id);// Store locally contact_id

            /**
             * Start with a complete and empty key-value object.  This is for 1 name, 1 title, etc.
             */
            for( SqlCipher.KvTab kvObj : SqlCipher.KvTab.values())
                try {
                    kv.put(kvObj.toString(), "");
                } catch (JSONException e) { e.printStackTrace(); }

            /**
             * First load all of the single data elements.  After, section by section load arrays of similar elements
             */
            /// Get name.  Store as simple JSONObject key:value pairs
            String nameWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] nameNameParams = new String[] { cloud_c_id+"", ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE };
            Cursor nameCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, nameWhere, nameNameParams, null);

            String name_first=null, name_last=null, display_name=null, name_middle=null, name_prefix=null, name_suffix=null;
            String phonetic_family=null, phonetic_given=null, phonetic_middle=null;

            if (nameCursor != null && nameCursor.moveToFirst()) {
                display_name = s(nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)));
                name_first =   s(nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)));
                name_last =    s(nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)));
                name_middle =  s(nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME)));
                name_prefix =  s(nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PREFIX)));
                name_suffix =  s(nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)));
                phonetic_family = s(nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME)));
                phonetic_given =  s(nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME)));
                phonetic_middle = s(nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME)));
            }
            kv.put(CConst.NAME_PREFIX, name_prefix==null?"":name_prefix);
            kv.put(CConst.NAME_FIRST, name_first==null?"":name_first);
            kv.put(CConst.NAME_MIDDLE, name_middle==null?"":name_middle);
            kv.put(CConst.NAME_LAST, name_last==null?"":name_last);
            kv.put(CConst.NAME_SUFFIX, name_suffix==null?"":name_suffix);
            kv.put(CConst.PHONETIC_FAMILY, phonetic_family==null?"":phonetic_family);
            kv.put(CConst.PHONETIC_GIVEN, phonetic_given==null?"":phonetic_given);
            kv.put(CConst.PHONETIC_MIDDLE, phonetic_middle==null?"":phonetic_middle);
//            if( DEBUG ) LogUtil.log("display_name:" + display_name);
//            if( DEBUG ) LogUtil.log("jName:" + kv.toString());
            cv.put(CConst.DISPLAY_NAME, display_name);
            nameCursor.close();

//            if( display_name.contains("Clemens"))
//                LogUtil.log("Found Clemens");

            // Get company and title.  Store as simple JSONObject key:value pairs
            String titleWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] titleWhereParams = new String[]{ cloud_c_id+"", ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE};
            Cursor titleCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, titleWhere, titleWhereParams, null);

            String title = null, organization = null;

            if (titleCursor !=null && titleCursor.moveToFirst()) {
                title = s(titleCursor.getString(titleCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)));
                organization = s(titleCursor.getString(titleCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)));
                if( DEBUG ) LogUtil.log("Title & company:" + title+ ", "+organization);
            }
            kv.put(CConst.TITLE, title==null?"":title);
            kv.put(CConst.ORGANIZATION, organization==null?"":organization);
            titleCursor.close();

            // Get note.  Store as simple JSONObject key:value pairs
            String noteWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] noteWhereParams = new String[]{ cloud_c_id+"", ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE};
            Cursor noteCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, noteWhere, noteWhereParams, null);

            String note = null;

            if (noteCursor !=null && noteCursor.moveToFirst()) {
                note = s(noteCursor.getString(noteCursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)));
                if( DEBUG ) LogUtil.log("Note:" + note);
            }
            noteCursor.close();
            kv.put(CConst.NOTE, note==null?"":note);

            // Get Nickname.  Store as simple JSONObject key:value pairs
            String nicknameWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] nicknameWhereParams = new String[]{ cloud_c_id+"", ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE};
            Cursor nicknameCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, nicknameWhere, nicknameWhereParams, null);

            String nickname = null;

            if (nicknameCursor !=null && nicknameCursor.moveToFirst()) {
                nickname = s(nicknameCursor.getString(nicknameCursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.DATA)));
                if( DEBUG ) LogUtil.log("Nickname:" + nickname);
            }
            nicknameCursor.close();
            kv.put(CConst.NICKNAME, nickname==null?"":nickname);

            /**
             * //FUTURE AAron Hutchison has an icon in the Google Contacts web app, yet InputStream input is null
             * The Android Contacts also does not display the icon.
             * Other contacts have contact images that download fine.
             * CS seems to operate just like the ANdroid Contacts app in other cases.
             */
//            if( display_name.contains("Aaron"))
//                LogUtil.log("Found Aaron");

            /*
             * Import the contact photo
             */
            Uri photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, cloud_c_id);
            InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, photoUri);

            byte[] photoBytes = null;
            Bitmap photo = null;
            String encodedImage = "";

            if (input != null) {
                photo = BitmapFactory.decodeStream(input);
                if( DEBUG ) LogUtil.log("Photo: "+photo.getWidth()+"x"+photo.getHeight());
                try {
                    input.close();
                } catch (IOException e) { e.printStackTrace(); }
            }
            else {
                if( DEBUG ) LogUtil.log("#1, openContactPhotoInputStream failed to load photo");

                String [] projection = { ContactsContract.Contacts.PHOTO_ID };
                Cursor c = contentResolver.query( photoUri, projection ,null, null, null);

                if( DEBUG )Util.dumpCursorDescription("photo cursor", c);

                long photo_id = 0;
                if(c.getCount() > 0){

                    c.moveToFirst();
                    photo_id = c.getLong( 0 );
                }
                c.close();

                if( photo_id != 0) {

                    boolean data15exists = true; //data15 is ContactsContract.CommonDataKinds.Photo.PHOTO
                    try
                    {
                        c = contentResolver.query( photoUri,
                                new String[] {ContactsContract.CommonDataKinds.Photo.PHOTO}, null, null, null);
                    } catch (Exception e) {
                        data15exists = false;
                    }
                    if( data15exists){

                        try
                        {
                            if (c.moveToFirst())
                                photoBytes = c.getBlob(0);

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            c.close();
                        }
                    }

                    if (photoBytes != null){
                        photo = BitmapFactory.decodeByteArray(photoBytes,0,photoBytes.length);
                        if( DEBUG ) LogUtil.log("Photo: "+photo.getWidth()+"x"+photo.getHeight());
                    }
                    else
                    if( DEBUG ) LogUtil.log("#2, failed, blob decode from CommonDataKinds.Photo");
                }
                else
                if( DEBUG ) LogUtil.log("#2, photo_id == 0, failed to load photo");
            }
            if( photo != null){

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                photo.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] imageBytes = baos.toByteArray();
                encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            }
            cv.put( CConst.PHOTO, encodedImage);

            // Write group data directly to the account group table
            Cursor groupCursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[]{ // projection
                            ContactsContract.Data.DATA1,
                    },
                    ContactsContract.Data.MIMETYPE+"=? AND "+ContactsContract.Data.CONTACT_ID+" =?",  // selection
                    new String[]{ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE, cloud_c_id+""}, null
            );

//            if( display_name.contains("Clemens"))
//                LogUtil.log("Found Clemens");

            int group_id_index = groupCursor.getColumnIndex( ContactsContract.Data.DATA1);

            while ( import_groups && groupCursor.moveToNext()) {

                int group_id = (int)groupCursor.getLong( group_id_index );  //Data1

                // Handle mapping of group IDs that may collide with existing IDs
                group_id = MyGroups.remapGroupId(group_id);

                String account = MyGroups.mGroupAccount.get(group_id);
                if( account == null || ! account_name.contentEquals( account ))
                    continue;// skip over if accounts don't match

                MyGroups.addGroupMembership( ctx, contact_id, group_id, false);// false==don't sync
            }
            groupCursor.close();

            MyGroups.addMyContactsGroup(ctx, contact_id, account_name);

            // Get list of phone numbers and store as a JSONArray
            final Uri PHONE_CONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            final String PHONE_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
            final String PHONE_DATA = ContactsContract.CommonDataKinds.Phone.NUMBER;

            // Query and loop for every phone number of the contact
            Cursor phoneCursor = contentResolver.query(PHONE_CONTENT_URI, null, PHONE_CONTACT_ID + " = ?", new String[] { cloud_c_id+"" }, null);

            // Save the first mobile number as the default for this user
            String comm_select_phone = "";

            while (phoneCursor != null && phoneCursor.moveToNext()) {

                String phoneNumber = s(phoneCursor.getString(phoneCursor.getColumnIndex(PHONE_DATA)));
                int phonetype = phoneCursor.getInt(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                String customLabel = s(phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)));
                String phoneLabel = s((String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(ctx.getResources(), phonetype, customLabel));

                JSONObject o = singleObject( phoneLabel, phoneNumber);
                jarray.put( o );
                if( DEBUG ) LogUtil.log("Phone number:" + phoneNumber + " Label: " + phoneLabel);

                /**
                 * Set the first number as the default selected number, this is used for group SMS text.
                 * If a number is tagged "mobile", make it the default. This way a mobile number
                 * will be the default instead of a home-tone line number.
                 */
                if( comm_select_phone.isEmpty())
                    comm_select_phone = phoneNumber;
                if( phoneLabel.toLowerCase().contains( "mobile"))
                    comm_select_phone = phoneNumber;
            }
            phoneCursor.close();
            cv.put( CConst.PHONE, jarray.toString());
            jarray = new JSONArray();

            final Uri EmailCONTENT_URI =  ContactsContract.CommonDataKinds.Email.CONTENT_URI;
            final String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
            final String EMAIL_DATA = ContactsContract.CommonDataKinds.Email.DATA;

            // Query and loop for every email of the contact
            Cursor emailCursor = contentResolver.query(EmailCONTENT_URI, null, EmailCONTACT_ID+ " = ?", new String[] { cloud_c_id+"" }, null);

            // Save the first email as the default for this user
            String comm_select_email = "";

            while (emailCursor != null && emailCursor.moveToNext()) {

                String email = s(emailCursor.getString(emailCursor.getColumnIndex(EMAIL_DATA)));
                int emailtype = emailCursor.getInt(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
                String customLabel = s(emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL)));
                String emailLabel = s((String) ContactsContract.CommonDataKinds.Email.getTypeLabel(ctx.getResources(), emailtype, customLabel));

                JSONObject o = singleObject( emailLabel, email);
                jarray.put( o );
                if( DEBUG ) LogUtil.log("Email:" + email+ " Label: "+emailLabel);

                if( comm_select_email.isEmpty())
                    comm_select_email = email;
            }
            emailCursor.close();
            cv.put( CConst.EMAIL, jarray.toString());

            jarray = new JSONArray();

            final Uri ADDRESS_CONTENT_URI =  ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI;
            final String ADDRESS_CONTACT_ID = ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID;
            final String FORMATTED_ADDRESS = ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS;

            // Query and loop for every address of the contact
            Cursor addressCursor = contentResolver.query(ADDRESS_CONTENT_URI, null, ADDRESS_CONTACT_ID+ " = ?", new String[] { cloud_c_id+"" }, null);

            while (addressCursor != null && addressCursor.moveToNext()) {

                String address = s(addressCursor.getString(addressCursor.getColumnIndex(FORMATTED_ADDRESS)));
                int addressType = addressCursor.getInt(addressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));
                String customLabel = s(addressCursor.getString(addressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.LABEL)));
                String addressLabel = s((String) ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(ctx.getResources(), addressType, customLabel));

                JSONObject o = singleObject( addressLabel, address);
                jarray.put( o );
                if( DEBUG ) LogUtil.log("Address:" + address+ " Label: "+addressLabel);
            }
            addressCursor.close();
            cv.put( CConst.ADDRESS, jarray.toString());

            jarray = new JSONArray();

            String websiteWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] websiteWhereParams = new String[]{ cloud_c_id+"", ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE};
            Cursor websiteCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, websiteWhere, websiteWhereParams, null);
            String websiteLabel = "WORK";

            if (websiteCursor !=null && websiteCursor.moveToFirst()) {
                String website = s(websiteCursor.getString(websiteCursor.getColumnIndex(ContactsContract.CommonDataKinds.Website.URL)));
                JSONObject o = singleObject( websiteLabel, website);
                jarray.put( o );
                if( DEBUG ) LogUtil.log("Website:" + website+ " Label: "+websiteLabel);
            }
            websiteCursor.close();
            cv.put( CConst.WEBSITE, jarray.toString());

            // Get IM, possibly more than one, store as JSONArray
            jarray = new JSONArray();
            String imWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] imWhereParams = new String[]{ cloud_c_id+"", ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE};
            Cursor imCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, imWhere, imWhereParams, null);

            while (imCursor !=null && imCursor.moveToNext()) {
                String im = s(imCursor.getString(imCursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)));
                String im_enum = s(imCursor.getString( imCursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA5)));
                String im_type = "";
                switch( Integer.parseInt(im_enum)){
                    case ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM:         im_type = "AIM"; break;
                    case ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM:      im_type = "CUSTOM"; break;
                    case ContactsContract.CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK: im_type = "GOOGLE TALK"; break;
                    case ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ:         im_type = "ICQ"; break;
                    case ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER:      im_type = "JABBER"; break;
                    case ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN:         im_type = "MSN"; break;
                    case ContactsContract.CommonDataKinds.Im.PROTOCOL_NETMEETING:  im_type = "NETMEETING"; break;
                    case ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ:          im_type = "QQ"; break;
                    case ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE:       im_type = "SKYPE"; break;
                    case ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO:       im_type = "YAHOO"; break;
                    default:
                        im_type = "OTHER";
                        break;
                }
                JSONObject o = singleObject( im_type, im);
                jarray.put( o );
                if( DEBUG ) LogUtil.log("IM:" + im+", Type:"+im_type);
            }
            imCursor.close();
            cv.put( CConst.IM, jarray.toString());

            // Get relation and relation type.  Store as simple JSONObjects key:value pairs
            jarray = new JSONArray();
            String relationWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] relationWhereParams = new String[]{ cloud_c_id+"", ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE};
            Cursor relationCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, relationWhere, relationWhereParams, null);

            String relation = "";
            String relation_type = "";
            String relation_enum = "";

            while (relationCursor !=null && relationCursor.moveToNext()) {
                relation = s(relationCursor.getString(relationCursor.getColumnIndex(ContactsContract.CommonDataKinds.Relation.NAME)));
                relation_enum = s(relationCursor.getString(relationCursor.getColumnIndex(ContactsContract.CommonDataKinds.Relation.DATA2)));
                switch( Integer.parseInt(relation_enum)){
                    case ContactsContract.CommonDataKinds.Relation.TYPE_ASSISTANT:        relation_type = "ASSISTANT"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_BROTHER:          relation_type = "BROTHER"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_CHILD:            relation_type = "CHILD"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_DOMESTIC_PARTNER: relation_type = "DOMESTIC PARTNER"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_FATHER:           relation_type = "FATHER"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_FRIEND:           relation_type = "FRIEND"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_MANAGER:          relation_type = "MANAGER"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER:           relation_type = "MOTHER"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_PARENT:           relation_type = "PARENT"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_PARTNER:          relation_type = "PARTNER"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_REFERRED_BY:      relation_type = "REFERRED BY"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_RELATIVE:         relation_type = "RELATIVE"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_SISTER:           relation_type = "SISTER"; break;
                    case ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE:           relation_type = "SPOUSE"; break;
                    default: relation_type = "CUSTOM"; break;
                }
                JSONObject o = singleObject( relation_type, relation);
                jarray.put( o );
                if( DEBUG ) LogUtil.log("Relation:" + relation+", "+relation_type);
            }
            cv.put(CConst.RELATION, jarray.toString());
            relationCursor.close();

            // Save key-value single element data.  Each only has 1, 1 name, 1 title, etc.
            cv.put(CConst.KV, kv.toString());

            //Save the detault communications selections for this user
            JSONObject comms_select = new JSONObject();
            if( ! comm_select_phone.isEmpty())
                comms_select.put(comm_select_phone, CConst.COMMS_SELECT_MOBILE);
            if( ! comm_select_email.isEmpty())
                comms_select.put(comm_select_email, CConst.COMMS_SELECT_EMAIL);
            cv.put(CConst.COMMS_SELECT, comms_select.toString());

            // Add placeholder for future
            jarray = new JSONArray();
            cv.put(CConst.DATES, jarray.toString());
            cv.put(CConst.INTERNETCALL, jarray.toString());

            if( DEBUG ) LogUtil.log("\n\n");

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // Write content values to detail record
        return SqlCipher.updateDetailRecord( contact_id, cv);
    }

    /**<pre>
     * Import all android groups into the database and memory representations.
     * Cloud group IDs can collide with local group IDs.
     * An ID remapping data structure is used to reassign group IDs.
     *
     * Import can be called multiple times to pull in updates from the Cloud.  It is assumed
     * that the group_id and contact_ids will diverge.  This is solved by two main concepts.
     *
     * First, as long as the local database is consistent with itself, then database integrity
     * will be preserved.
     *
     * Second, imported group records must match exactly in order to declare them redundant.
     * A group is tested for the group_id, group_title and account_name.  All three must match exactly
     * or it is assumed that the group is different.
     * In the same way, a contact record is tested for the contact_id and display_name, both must
     * match exactly or they will be assumed different.
     *
     * During import, data structures for groupMap and contactMap are maintained that maintain records
     * that collide with existing records.  The map provides a non-colliding replacement value.
     *
     * Each import is assumed to be consistent with itself and when complete the entire database has
     * integrity.
     *
     *
     * @param ctx
     * @return int number of groups imported
     * </pre>
     */
    public static int importGroups(Context ctx){

        int count = 0;

        Cursor groupCursor = ctx.getContentResolver().query(
                ContactsContract.Groups.CONTENT_URI,
                new String[]{
                        ContactsContract.Groups._ID,
                        ContactsContract.Groups.SYSTEM_ID,
                        ContactsContract.Groups.TITLE,
                        ContactsContract.Groups.DELETED,
                        ContactsContract.Groups.ACCOUNT_NAME,
                        ContactsContract.Groups.ACCOUNT_TYPE,
                }, null, null, null
        );

        int group_id_column_index =      groupCursor.getColumnIndex( ContactsContract.Groups._ID);
//        int system_id_column_index =     groupCursor.getColumnIndex( ContactsContract.Groups.SYSTEM_ID);
        int title_column_index =         groupCursor.getColumnIndex( ContactsContract.Groups.TITLE);
        int deleted_column_index =       groupCursor.getColumnIndex( ContactsContract.Groups.DELETED);
        int account_name_column_index =  groupCursor.getColumnIndex( ContactsContract.Groups.ACCOUNT_NAME);
        int account_type_column_index =  groupCursor.getColumnIndex( ContactsContract.Groups.ACCOUNT_TYPE);

        while( groupCursor.moveToNext()){

            String account = s(groupCursor.getString( account_name_column_index )).toLowerCase(Locale.US);

            if( ! mImportAccounts.contains( account))
                continue;

            boolean deleted = 1 ==  groupCursor.getInt( deleted_column_index );
            int cloud_group_id =  (int)   groupCursor.getLong( group_id_column_index );
            String group_title =  s(groupCursor.getString( title_column_index ));
            String account_type = s(groupCursor.getString(account_type_column_index));

            if( !deleted && !group_title.isEmpty()){

                MyGroups.addGroup(ctx, cloud_group_id, group_title, account, account_type);
                ++count;
            }
            else{
                if( deleted)
                    LogUtil.log("group deleted: "+cloud_group_id+", "+group_title+", "+account);
                if( group_title.isEmpty())
                    LogUtil.log("group title empty: "+cloud_group_id+", "+group_title+", "+account);
            }
        }
        groupCursor.close();

        return count;
    }

    public static JSONObject singleObject(String key, String value){

        JSONObject o = new JSONObject();
        try {
            o.put(key, value);
        } catch (JSONException e) {
        }
        return o;
    }
    /**
     * Make a safe string by making pairs of single quotes, removing non-printing characters
     * and replacing the null string with an empty string.
     * @param unsafeString
     * @return safeString
     */
    public static String s(String unsafeString){

        return Safe.safeString( unsafeString);
    }
}
