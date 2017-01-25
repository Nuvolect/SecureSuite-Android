package com.nuvolect.securesuite.data;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.widget.Toast;

import com.nuvolect.securesuite.data.SqlCipher.ATab;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.data.SqlCipher.KvTab;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.LogUtil.LogType;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.Util;

import net.sqlcipher.Cursor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.ImageType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Kind;
import ezvcard.property.Note;
import ezvcard.property.Photo;
import ezvcard.property.StructuredName;

public class ExportVcf {


    private static Activity m_act;
    private static Cursor m_cursor;

    public static void emailVcf( Activity act, long contact_id){

        String messageTitle = "vCard for ";
        String messageBody = "\n\n\nContact from SecureSuite, a secure contacts manager";

        try {
            String displayName = SqlCipher.get(contact_id, ATab.display_name);
            String fileName = displayName.replaceAll("\\W+", "");
            if( fileName.isEmpty())
                fileName = "contact";
            fileName = fileName + ".vcf";

            new File( act.getFilesDir() +CConst.TEMP_FOLDER).mkdirs();
            File vcf_file = new File( act.getFilesDir() + CConst.TEMP_FOLDER + fileName);

            writeContactVcard(contact_id, vcf_file);

            Uri uri = FileProvider.getUriForFile( act, "com.nuvolect.securesuite.files", vcf_file);
//            act.grantUriPermission( act.getPackageName(), uri, act.MODE_PRIVATE);

            //convert from paths to Android friendly Parcelable Uri's
            ArrayList<Uri> uris = new ArrayList<Uri>();
            uris.add( uri);

            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, messageTitle+ displayName);
            intent.putExtra(Intent.EXTRA_TEXT, messageBody);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra("path", vcf_file.getAbsolutePath());

            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            act.startActivityForResult(Intent.createChooser(intent, "Share with..."),
                    CConst.RESPONSE_CODE_SHARE_VCF);

        } catch (Exception e) {
            LogUtil.logException(act, LogType.EXPORT_VCF, e);
        }
    }
    /**
     * Create a vcard for a single contact record
     * @param contact_id
     * @param f1 to save vcard
     */
    public static void writeContactVcard(long contact_id, File f1) {

        VCard vcard = makeVcard( contact_id);

        try {
            Ezvcard.write(vcard).go(f1);

        } catch (IOException e) {
            LogUtil.logException(m_act, LogType.EXPORT_VCF, e);
        }
    }

    /**
     * Create a vCard file from a collection of contacts.
     * @param arrayListId
     * @param outputFile
     * @return
     */
    public static int writeContactVcard(ArrayList<Long> arrayListId, File outputFile) {


        int count = 0;

        try {
            for(long id : arrayListId){

                VCard vcard = makeVcard( id) ;
                if( count == 0)
                    Ezvcard.write(vcard).go(outputFile);
                else
                    Ezvcard.write(vcard).go(outputFile, true);
                ++count;
            }

        } catch (IOException e) {
            LogUtil.logException(m_act, LogType.EXPORT_VCF, e);
            count = -1;
        }
        String summary = "Exported "+count+" contacts to "+outputFile.getName();
        LogUtil.log( summary);

        return count;
    }

    /**
     * Create a single vcard from a contact record
     * @param contact_id
     * @return appended vcard
     */
    public static VCard makeVcard( long contact_id) {

        VCard vcard = new VCard();
        try {
            vcard.setKind(Kind.individual());
            vcard.addLanguage("en-US");

            String full_name = NameUtil.getFullName(contact_id);
            vcard.setFormattedName(full_name);
            {
                StructuredName n = new StructuredName();
                String prefix = SqlCipher.getKv( contact_id, KvTab.name_prefix);
                n.addPrefix(prefix);
                String first = SqlCipher.getKv( contact_id, KvTab.name_first);
                n.setGiven( first );
                String last = SqlCipher.getKv( contact_id, KvTab.name_last);
                n.setFamily( last );
                String suffix = SqlCipher.getKv( contact_id, KvTab.name_suffix);
                n.addSuffix( suffix );
                vcard.setStructuredName(n);

//                if( prefix.isEmpty() && first.isEmpty() && last.isEmpty() && suffix.isEmpty())//TODO remove
//                    n.setFamily(full_name);
            }
            {
                JSONArray itemArray = new JSONArray( SqlCipher.get( contact_id, DTab.address));
                for( int i =0; i< itemArray.length(); i++){

                    JSONObject item = itemArray.getJSONObject(i);
                    Iterator<?> item_keys = item.keys();
                    String item_label = (String) item_keys.next();
                    final String item_value = item.getString(item_label);

                    Address adr = new Address();
                    adr.setStreetAddress( item_value);
                    if( item_label.contentEquals("HOME"))
                        adr.addType(AddressType.HOME);
                    else
                    if( item_label.contentEquals("WORK"))
                        adr.addType(AddressType.WORK);

                    vcard.addAddress(adr);
                }
            }
            {
                JSONArray itemArray = new JSONArray( SqlCipher.get( contact_id, DTab.phone));
                for( int i =0; i< itemArray.length(); i++){

                    JSONObject item = itemArray.getJSONObject(i);
                    Iterator<?> item_keys = item.keys();
                    String item_label = (String) item_keys.next();
                    final String item_value = item.getString(item_label);

                    TelephoneType type = null;
                    if( item_label.contentEquals("HOME"))
                        type = TelephoneType.HOME;
                    else
                    if( item_label.contentEquals("WORK"))
                        type = TelephoneType.WORK;

                    if( type == null)
                        vcard.addTelephoneNumber(item_value);
                    else
                        vcard.addTelephoneNumber(item_value, type);
                }
            }
            {
                JSONArray itemArray = new JSONArray( SqlCipher.get( contact_id, DTab.email));
                for( int i =0; i< itemArray.length(); i++){

                    JSONObject item = itemArray.getJSONObject(i);
                    Iterator<?> item_keys = item.keys();
                    String item_label = (String) item_keys.next();
                    final String item_value = item.getString(item_label);

                    EmailType type = null;
                    if( item_label.contentEquals("HOME"))
                        type = EmailType.HOME;
                    else
                    if( item_label.contentEquals("WORK"))
                        type = EmailType.WORK;

                    if( type == null)
                        vcard.addEmail( item_value);
                    else
                        vcard.addEmail( item_value, type);
                }
            }
            {
                JSONArray itemArray = new JSONArray( SqlCipher.get( contact_id, DTab.website));
                for( int i =0; i< itemArray.length(); i++){

                    JSONObject item = itemArray.getJSONObject(i);
                    Iterator<?> item_keys = item.keys();
                    String item_label = (String) item_keys.next();
                    final String item_value = item.getString(item_label);

                    vcard.addUrl( item_value);
                }
            }
            {
                String photoStr = SqlCipher.get( contact_id,  DTab.photo);
                byte[] b = null;
                b = Base64.decode( photoStr.getBytes(), Base64.DEFAULT);
                Photo photo = new Photo( b, ImageType.PNG );
                vcard.addPhoto(photo);
            }
            {
                String noteStr = SqlCipher.getKv( contact_id, KvTab.note);
                Note note = vcard.addNote(noteStr); // can contain newlines
                note.setLanguage("en-us");
            }
            {
                String organization = SqlCipher.getKv(contact_id, KvTab.organization);
                vcard.setOrganization(organization, "");// second parameter is department
            }
            {
                String title = SqlCipher.getKv(contact_id, KvTab.title);
                vcard.addTitle(title);
            }
            //FUTURE export im
            //FUTURE export dates
            //FUTURE export relation

        } catch (JSONException e) {
            LogUtil.logException(m_act, LogType.EXPORT_VCF, e);
        }
        return vcard;
    }

    /** Export all contacts to a single vcard file */
    public static void exportAllVcardAsync( Activity act){

        m_act = act;
        m_act.setProgressBarIndeterminateVisibility( true );

        // Cursor is closed in async task
        m_cursor = SqlCipher.getATabCursor();
        new ExportGroupVcardAsync( ).execute();
    }

    /** Export an account to a single vcard file */
    public static void exportAccountVcardAsync( Activity act, String account){

        m_act = act;
        m_act.setProgressBarIndeterminateVisibility( true );

        // Cursor is closed in async task
        m_cursor = MyAccounts.getAccountCursor( account);
        new ExportGroupVcardAsync( ).execute();
    }

    /** Export a group to a single vcard file */
    public static void exportGroupVcardAsync( Activity act, int group_id){

        m_act = act;
        m_act.setProgressBarIndeterminateVisibility( true );

        // Cursor is closed in async task
        m_cursor = MyGroups.getGroupContactsCursor(group_id);
        new ExportGroupVcardAsync( ).execute();
    }

    private static class ExportGroupVcardAsync extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void...groups) {

            Util.createAppPublicFolder();
            String basePath = Environment.getExternalStorageDirectory()+CConst.FOLDER_NAME;

            int c_id_index = m_cursor.getColumnIndex("contact_id");

            String aVcf = Persist.getNextVcfFilename(m_act);
            File f1 = new File( basePath + "/" + aVcf);
            int count = 0;

            VCard vcard;
            while(m_cursor.moveToNext()){

                long contact_id = m_cursor.getLong(c_id_index);
                if( contact_id <=0 )
                    break;

                vcard = makeVcard( contact_id);

                try {
                    Ezvcard.write(vcard).go(f1, true); // true == append
                    ++count;

                } catch (IOException e) {
                    LogUtil.logException(m_act, LogType.EXPORT_VCF, e);
                }
            }
            m_cursor.close();

            String summary = "Exported "+count+" contacts to "+aVcf;
            LogUtil.log( summary);

            return summary;
        }

        @Override
        protected void onPostExecute(String exportMsg) {

            Toast.makeText(m_act, exportMsg, Toast.LENGTH_LONG).show();
            m_act.setProgressBarIndeterminateVisibility( false );
        }
    }
}
