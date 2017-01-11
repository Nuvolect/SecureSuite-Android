package com.nuvolect.securesuite.data;

import android.content.ContentValues;
import android.content.Context;
import android.util.Base64;

import com.nuvolect.securesuite.data.SqlCipher.ATab;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.data.SqlCipher.KvTab;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.LogUtil.LogType;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.Safe;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ezvcard.VCard;
import ezvcard.io.text.VCardReader;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.FormattedName;
import ezvcard.property.Note;
import ezvcard.property.Organization;
import ezvcard.property.Photo;
import ezvcard.property.Telephone;
import ezvcard.property.Title;
import ezvcard.property.Url;
import ezvcard.util.TelUri;

public class ImportVcard {

    private static final boolean DEBUG = false;
    private static Context m_ctx;

    /**
     * Import vCard and associate it with a group.  If the group_id is <= 0, do not associate it
     * with any group.
     * @param ctx
     * @param vcf
     * @param group_id
     * @return
     */
    public static long importVcf(Context ctx, InputStream vcf, int group_id) {

        m_ctx = ctx;

        long contact_id = 0;
        try {
            VCardReader reader;
            reader = new VCardReader(vcf);
            VCard vcard = null;

            while ((vcard = reader.readNext()) != null){

                contact_id = vcfToContact( ctx, vcard);
                Persist.setCurrentContactId( ctx, contact_id);
                /**
                 * Inform the db sync system to update other device.
                 * The contact will sync along with all of its group assignments.
                 */
                SqlIncSync.getInstance().insertContact(ctx, contact_id);

                if( group_id > 0 ){

                    // Add current group if non-zero
                    // This method also bumps in memory group counts
                    MyGroups.addGroupMembership( ctx, contact_id, group_id, false);// Sync is already set
                }
            }
            reader.close();

        } catch (FileNotFoundException e) {
            LogUtil.logException(ctx, LogType.IMPORT_VCF, e);
        } catch (IOException e) {
            LogUtil.logException(ctx, LogType.IMPORT_VCF, e);
        } catch (Exception e) {
            LogUtil.logException(ctx, LogType.IMPORT_VCF, e);
        }
        return contact_id;
    }

    public interface ImportProgressCallbacks {

        public void progressReport(int importProgress);
    }
    /**
     * Import vCard and associate it with a group.  If the group_id is <= 0, do not associate it
     * with any group.  Spans time, do not call on UI thread.
     * @param ctx
     * @param path
     * @return
     */
    public static long importVcf(Context ctx, String path, int group_id, ImportProgressCallbacks callbacks) {

        LogUtil.log(LogType.IMPORT_VCF, "path: "+path);

        m_ctx = ctx;
        long contact_id = 0;
        try {
            File file = new File( path);
            if( file != null && file.isFile()){

                String fName = file.getName().toLowerCase(Locale.US);
                boolean validExtension = (fName.endsWith("vcf") || fName.endsWith("vcard"));
                if( ! validExtension )
                    return 0;
            }else
                return 0; // error, not a file

            VCardReader reader;
            reader = new VCardReader(file);
            VCard vcard = null;
            int importProgress = 0;

            while ((vcard = reader.readNext()) != null){

                contact_id = vcfToContact( ctx, vcard);
                Persist.setCurrentContactId( ctx, contact_id);
                SqlIncSync.getInstance().insertContact(ctx, contact_id);

                if( group_id > 0 ){

                    //Also add current group if non-zero
                    MyGroups.addGroupMembership( m_ctx, contact_id, group_id, false);
                    MyGroups.mGroupCount.put(group_id, 1 + MyGroups.mGroupCount.get(group_id));
                }
                if( callbacks != null)
                    callbacks.progressReport( ++importProgress);
            }
            reader.close();

        } catch (FileNotFoundException e) {
            LogUtil.logException(ctx, LogType.IMPORT_VCF, e);
        } catch (IOException e) {
            LogUtil.logException(ctx, LogType.IMPORT_VCF, e);
        } catch (Exception e) {
            LogUtil.logException(ctx, LogType.IMPORT_VCF, e);
        }
        return contact_id;
    }

    /**
     * Import vCard and associate it with a group.  If the group_id is <= 0, do not associate it
     * with any group. Check the filename ending for .vcf and .vcard, otherwise return 0.
     * Return 0 on any error otherwise return the id of the last contact created.
     * @param ctx
     * @param fName
     * @param path
     * @param group_id
     * @return
     */
    public static long importVcf(Context ctx, String fName, String path, int group_id) {

        LogUtil.log(LogType.IMPORT_VCF, "Filename: "+fName+", path: "+path);

        m_ctx = ctx;
        long contact_id = 0;
        try {
            File file = new File( path);
            if( ! fName.isEmpty() && file.isFile()){

                boolean validExtension = (fName.endsWith(".vcf") || fName.endsWith(".vcard"));
                if( ! validExtension )
                    return 0;
            }else
                return 0; // error, not a file

            VCardReader reader;
            reader = new VCardReader(file);
            VCard vcard = null;
            while ((vcard = reader.readNext()) != null){

                contact_id = vcfToContact( ctx, vcard);
                Persist.setCurrentContactId( ctx, contact_id);
                SqlIncSync.getInstance().insertContact(ctx, contact_id);

                LogUtil.log(LogType.IMPORT_VCF, "imported new contact_id: "+contact_id);

                if( group_id > 0 ){

                    // Also add current group if non-zero
                    // Bump in memory group count
                    MyGroups.addGroupMembership(m_ctx, contact_id, group_id, false);

                    // Add to My Contacts group
                    String account = MyGroups.mGroupAccount.get( group_id);
                    int my_contacts_id = MyGroups.getGroupId( account, CConst.MY_CONTACTS);
                    MyGroups.addGroupMembership(m_ctx, contact_id, my_contacts_id, false);
                }
            }
            reader.close();

        } catch (FileNotFoundException e) {
            LogUtil.logException(ctx, LogType.IMPORT_VCF, e);
        } catch (IOException e) {
            LogUtil.logException(ctx, LogType.IMPORT_VCF, e);
        } catch (Exception e) {
            LogUtil.logException(ctx, LogType.IMPORT_VCF, e);
        }
        return contact_id;
    }

    /**
     * Copy the VCard contents into a contact record, write it to
     * the database and return the contact_id;
     * @param ctx
     * @param vcard
     * @return
     */
    private static long vcfToContact( Context ctx, VCard vcard) {

        m_ctx = ctx;
        long contact_id = SqlCipher.getNextUnusedContactID();

        // Fetch name and don't allow null
        String first_name =vcard.getStructuredName().getGiven();
        String last_name  =vcard.getStructuredName().getFamily();
        first_name = first_name == null?"":first_name;
        last_name = last_name == null?"":last_name;

        String full_name = "";
        full_name = concat( full_name, concatArray( vcard.getStructuredName().getPrefixes()));
        full_name = concat( full_name, vcard.getStructuredName().getGiven());
        full_name = concat( full_name, vcard.getStructuredName().getFamily());
        full_name = concat( full_name, concatArray( vcard.getStructuredName().getSuffixes()));


        // If name details not provided, use the fully formatted name
        if( full_name.trim().isEmpty()){

            FormattedName formattedName = vcard.getFormattedName();
            full_name = formattedName.getValue();
        }

        String title = "";
        List<Title> titleList = vcard.getTitles();
        if( ! titleList.isEmpty())
            title = Safe.safeString( titleList.get(0).getValue());

        String organization = "";
        Organization org = vcard.getOrganization();
        if( org != null){

            List<String> values = org.getValues();
            if( ! values.isEmpty())
                organization = Safe.safeString( values.get(0));
        }
        //FUTURE Remove when confirmed organization/company keyword is cleared up
        if( organization.contentEquals("error"))
            organization = "";

        /*
         * Read note including newline characters.
         */
        String note = "";
        for( Note aNote : vcard.getNotes()){
            String value = aNote.getValue();
            note = note + conditionalAddNewline( value );  // Add the note, append newline if not empty
        }

        // FUTURE import dates
        // FUTURE import im
        // FUTURE import name by 5 parts + nickname
        // FUTURE import phonetic names
        // FUTURE import relation

        String address = addressToJsonArray( vcard.getAddresses()).toString();
        JSONArray emailsJsonArray = emailToJsonArray(vcard.getEmails());
        String emails = emailsJsonArray.toString();
        String phones = phoneToJsonArray(vcard.getTelephoneNumbers()).toString();
        String website = webToJsonArray( vcard.getUrls()).toString();
        String photo = encodePhoto( vcard );

        String log = full_name+", "+address+", "+emails+", "+phones+", "+website;
        if(DEBUG)
            LogUtil.log(LogType.IMPORT_VCF, log);

        // 1. Update detail db
        ContentValues detail_cv = new ContentValues();
        detail_cv.put( DTab.contact_id.toString(),   contact_id);
        detail_cv.put( DTab.display_name.toString(), "");// revise below
        detail_cv.put( DTab.comms_select.toString(),  "{}");
        detail_cv.put( DTab.address.toString(),      address);
        detail_cv.put( DTab.date.toString(),        "[]");
        detail_cv.put( DTab.email.toString(),        emails);
        detail_cv.put( DTab.im.toString(),           "[]");
        detail_cv.put( DTab.phone.toString(),        phones);
        detail_cv.put( DTab.photo.toString(),        photo);
        detail_cv.put( DTab.relation.toString(),     "[]");
        detail_cv.put( DTab.internetcall.toString(), "[]");
        detail_cv.put( DTab.website.toString(),      website);

        JSONObject kv = new JSONObject();

        try {
            for( KvTab kvObj : KvTab.values())  // Start with a complete kv set
                kv.put(kvObj.toString(), "");

            // Overwrite a few specific values
            kv.put(KvTab.name_first.toString(),   first_name);
            kv.put(KvTab.name_last.toString(),    last_name);
            kv.put(KvTab.title.toString(),        title);
            kv.put(KvTab.organization.toString(), organization);
            kv.put(KvTab.note.toString(),         note);

        } catch (JSONException e) { e.printStackTrace(); }

        detail_cv.put( DTab.kv.toString(),           kv.toString());

        SqlCipher.detail_db.beginTransaction();
        long row = SqlCipher.detail_db.insert( SqlCipher.DETAIL_TABLE, null, detail_cv);
        if( row > 0)
            SqlCipher.detail_db.setTransactionSuccessful();
        SqlCipher.detail_db.endTransaction();

        // 2. Update account db
        ContentValues account_cv = new ContentValues();
        account_cv.put( ATab.contact_id.toString(),          contact_id);
        account_cv.put( ATab.display_name.toString(),        "");// Revised below
        account_cv.put( ATab.display_name_source.toString(), "");
        account_cv.put( ATab.last_name.toString(),           last_name);
        account_cv.put( ATab.starred.toString(),             CConst.STARRED_0);
        account_cv.put( ATab.account_name.toString(),        Cryp.getCurrentAccount());
        account_cv.put( ATab.account_type.toString(),        Persist.getCurrentAccountType( ctx ));
        account_cv.put( ATab.version.toString(),             1);
        account_cv.put( ATab.cloud_version.toString(),       1);

        SqlCipher.account_db.beginTransaction();
        row = SqlCipher.account_db.insert( SqlCipher.ACCOUNT_TABLE, null, account_cv);
        if( row > 0)
            SqlCipher.account_db.setTransactionSuccessful();
        SqlCipher.account_db.endTransaction();

        NameUtil.setNamesFromKv(contact_id);

        return contact_id;
    }

    private static String encodePhoto(VCard vcard) {

        List<Photo> photos = vcard.getPhotos();
        if( photos.isEmpty())
            return "";

        Photo photo = photos.get(0);
        byte[] photoBytes = photo.getData();
        String encodedPhoto = Base64.encodeToString( photoBytes, Base64.DEFAULT);

        return encodedPhoto;
    }

    private static JSONArray webToJsonArray(List<Url> urls) {

        JSONArray jArray = new JSONArray();

        for(Url url : urls ){

            String string = Safe.safeString( url.getValue().toString());
            String type = url.getGroup();
            JSONObject jObject = new JSONObject();
            try {
                jObject.put(type==null?"OTHER":type, string);
            } catch (JSONException e) {
                LogUtil.logException(m_ctx, LogType.IMPORT_VCF, e);
            }
            jArray.put(jObject);
        }

        return jArray;
    }

    private static JSONArray phoneToJsonArray(List<Telephone> telephoneNumbers) {

        JSONArray jArray = new JSONArray();

        for(Telephone phone : telephoneNumbers ){

            String value = Safe.safeString( phone.getText());
            TelUri uri = phone.getUri();
            if( value.isEmpty()){

                if( uri == null){
                    continue;  // Nothing here, skip and repeat the loop
                }
                value = uri.toString();
            }
            Set<TelephoneType> set = phone.getTypes();
            String type = "MOBILE";
            if( ! set.isEmpty()){

                Iterator<TelephoneType> iter = set.iterator();
                type = iter.next().toString().toUpperCase(Locale.US);

                if( type.equals("PREF") && iter.hasNext())
                    type = iter.next().toString().toUpperCase(Locale.US);
            }
            JSONObject jObject = new JSONObject();
            try {
                jObject.put(type, value);
            } catch (JSONException e) {
                LogUtil.logException(m_ctx, LogType.IMPORT_VCF, e);
            }
            jArray.put(jObject);
        }

        return jArray;
    }

    private static JSONArray emailToJsonArray(List<Email> emails) {

        JSONArray jArray = new JSONArray();

        for(Email email : emails ){

            String value = Safe.safeString( email.getValue());
            if( value.isEmpty()){
                continue;
            }
            Set<EmailType> set = email.getTypes();
            String type = "HOME";
            if( ! set.isEmpty()){

                Iterator<EmailType> iter = set.iterator();
                type = iter.next().toString().toUpperCase(Locale.US);

                if( type.equals("PREF") && iter.hasNext())
                    type = iter.next().toString().toUpperCase(Locale.US);
            }

            JSONObject jObject = new JSONObject();
            try {
                jObject.put( type, value);
            } catch (JSONException e) {
                LogUtil.logException(m_ctx, LogType.IMPORT_VCF, e);
            }
            jArray.put(jObject);
        }

        return jArray;
    }

    private static JSONArray addressToJsonArray(List<Address> addresses) {

        JSONArray jArray = new JSONArray();

        for(Address address : addresses ){

            String street = conditionalAddNewline( address.getStreetAddress());// + newline
            String poBox  = conditionalAddNewline( address.getPoBox());        // + newline
            String city   = Safe.safeString( address.getLocality());
            city = city.isEmpty()?"":city+" ";             // Space between city and state
            String state  = Safe.safeString( address.getRegion());
            state = state.isEmpty()?"":state+", ";         // comma after state
            String zipCode= conditionalAddNewline( address.getPostalCode());   // + newline
            String country= Safe.safeString( address.getCountry());

            String value = street + poBox + city + state +zipCode + country;

            Set<AddressType> set = address.getTypes();
            String type = "HOME";
            if( ! set.isEmpty()){

                Iterator<AddressType> iter = set.iterator();
                type = iter.next().toString().toUpperCase(Locale.US);

                if( type.equals("PREF") && iter.hasNext())
                    type = iter.next().toString().toUpperCase(Locale.US);
            }

            JSONObject jObject = new JSONObject();
            try {
                jObject.put( type, value);
            } catch (JSONException e) {
                LogUtil.logException(m_ctx, LogType.IMPORT_VCF, e);
            }
            jArray.put(jObject);
        }

        return jArray;
    }

    /**
     * Add a return to the string if it is not empty
     * @param string
     * @return
     */
    public static String conditionalAddNewline(String string){

        string = Safe.safeString( string );
        if( string.isEmpty())
            return string;
        else
            return string + "\n";
    }

    /**
     * Concatenate the strings.  Use a separator space if both are not empty
     * @param string1
     * @param string2
     */
    private static String concat(String string1, String string2) {

        string1 = string1==null?"":string1;
        string2 = string2==null?"":string2;

        if( string1.isEmpty())
            return string2;

        if( string2.isEmpty())
            return string1;

        return string1+ " " +string2;
    }

    /**
     * Return a space separated concatenated string of array items
     * @param stringList
     * @return
     */
    private static String concatArray(List<String> stringList) {

        String concat = "";
        String separator = "";

        for(String item : stringList){

            concat += separator + item;
            separator = " ";
        }

        return concat;
    }
}
