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

package com.nuvolect.securesuite.data;//

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.main.SettingsActivity;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.DialogUtil;
import com.nuvolect.securesuite.util.JsonUtil;
import com.nuvolect.securesuite.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Various methods to cleanup and maintain the database.
 */
public class CleanupFragment extends DialogFragment {

    private final boolean DEBUG = false;
    private static Activity m_act;
    private static CleanupFragment m_fragment;
    private View m_rootView;

    public static CleanupFragment newInstance(Activity act){

        m_act = act;
        m_fragment = new CleanupFragment();

        // Supply no input as an argument, not required, left as example
        Bundle args = new Bundle();
        m_fragment.setArguments(args);

        return m_fragment;
    }


    public void startFragment(){

        FragmentTransaction ft = m_act.getFragmentManager().beginTransaction();
        Fragment prev = m_act.getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        this.show(ft, "dialog");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        m_rootView = inflater.inflate(R.layout.cleanup_fragment, container, false);

        // Add the theme background outline and fill color behind fragment
        AppTheme.applyDrawableShape(m_act, m_rootView);
        m_rootView.findViewById(R.id.formatNumbersTr).setOnClickListener(onClickFormatNumbers);
        m_rootView.findViewById(R.id.removeEmptyGroupsTr).setOnClickListener(onClickRemoveEmptyGroups);
        m_rootView.findViewById(R.id.removeStarredInAndroidGroupsTr).setOnClickListener(onClickRemoveStarredInAndroidGroups);
        m_rootView.findViewById(R.id.inspectRepairDbTr).setOnClickListener(onClickInspectRepairDb);
        m_rootView.findViewById(R.id.cancelFl).setOnClickListener(cancelButtonOnClick);

        return m_rootView;
    }

    TextView.OnClickListener onClickFormatNumbers = new View.OnClickListener(){

        @Override
        public void onClick(View view) {

            String cc = SettingsActivity.getCountryCode(m_act);
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber localPrototype = phoneUtil.getExampleNumber(cc);

            String nationalNumber ="Invalid format";

            if( SettingsActivity.getInternationalNotation(m_act))
                nationalNumber = phoneUtil.format( localPrototype, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
            else
                nationalNumber = phoneUtil.format( localPrototype, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);

            DialogUtil.confirmDialog(m_act,
                    "Confirm re-format all phone numbers",
                    "All of your phone numbers will be formatted to look like " + nationalNumber,
                    new DialogUtil.DialogUtilCallbacks() {
                        @Override
                        public void confirmed(boolean confirmed) {

                            if( confirmed)
                                formatNumbers();
                        }
                    });
        }
    };

    /**
     * Iterate through all of the phone number fields and update the format to match national standard.
     * National standard is set in Settings and default to US.
     * National standard is the format used for comparing incoming calls.
     * Phone numbers with extra stuff such as a comment will not be modified..
     * Provide metrics on total numbers, numbers updated, numbers unchanged and numbers that are invalid.
     * Provide a log of contacts with invalid numbers.
     */
    private void formatNumbers() {

        new AsyncTask<Void, Integer, String>() {

            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                progressDialog = new ProgressDialog(m_act);
                progressDialog.setMessage("Converting...");
                progressDialog.setIndeterminate(false);
                progressDialog.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(true);
                progressDialog.setMax(SqlCipher.getDbSize());
                progressDialog.setProgress(0);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(Void... voids) {

                String cc = SettingsActivity.getCountryCode(m_act);
                StringBuilder log = new StringBuilder();
                String nl = System.getProperty("line.separator");

                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                PhoneNumberUtil.PhoneNumberFormat notation = PhoneNumberUtil.PhoneNumberFormat.NATIONAL;
                if( SettingsActivity.getInternationalNotation(m_act))
                    notation = PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL;

                long [] contactIds = SqlCipher.getContactIds( null );
                int updated = 0;
                int invalid = 0;
                int unchanged = 0;
                int progress = 0;
                String number = "";

                for( long contactId : contactIds){

                    String s = SqlCipher.get( contactId, SqlCipher.DTab.phone);
                    try {
                        JSONArray numbers = new JSONArray( s );
                        boolean modified = false;

                        for( int i = 0; i < numbers.length(); i++){

                            // Fetch key value pair for this number and extract the number from it.
                            JSONObject kvPair = new JSONObject( numbers.getString( i ));
                            String key = JsonUtil.getKey( kvPair );
                            number = JsonUtil.getValue( kvPair );

                            Phonenumber.PhoneNumber num = phoneUtil.parse( number, cc);

                            if( phoneUtil.isPossibleNumber( num )){

                                String formattedNumber = phoneUtil.format(num, notation);

                                if( ! number.contentEquals( formattedNumber)){

                                    kvPair.put( key, formattedNumber);
                                    numbers.put(i, kvPair);
                                    modified = true;
                                    ++updated;
                                }
                                else{
                                    ++unchanged;
                                }
                            }else{

                                ++invalid;
                                if( invalid < 5){
                                    log.append( nl + SqlCipher.get(contactId, SqlCipher.DTab.display_name)+", ");
                                    log.append( number );
                                }
                            }
                        }
                        if( modified){
                            SqlCipher.put( contactId, SqlCipher.DTab.phone, numbers.toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (NumberParseException e) {
                        ++invalid;
                        if( invalid < 5){
                            log.append( nl + SqlCipher.get(contactId, SqlCipher.DTab.display_name)+", ");
                            log.append( number );
                        }
                    } catch (Exception e) {
                        ++invalid;
                    }
                    publishProgress( ++progress);
                }

                return " Contacts: "+contactIds.length + nl
                        +" Total numbers: "+String.valueOf( updated+unchanged+invalid) + nl
                        +" Unchanged numbers: "+unchanged + nl
                        +" Updated numbers: "+updated + nl
                        +" Invalid numbers: "+invalid + nl
                        +log;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);

                progressDialog.setProgress( values[0]);
            }

            @Override
            protected void onPostExecute(String summary) {
                super.onPostExecute(summary);

                progressDialog.cancel();
                DialogUtil.dismissMlDialog(m_act, "Conversion Summary", summary);
            }

        }.execute();
    }

    TextView.OnClickListener onClickRemoveEmptyGroups = new View.OnClickListener(){

        @Override
        public void onClick(View view) {

            DialogUtil.confirmDialog(m_act,
                    "Removed Empty Android Groups",
                    "No contacts will be removed, only groups not associated with any contacts",
                    new DialogUtil.DialogUtilCallbacks() {
                        @Override
                        public void confirmed(boolean confirmed) {

                            if( confirmed){

                                String result = MyGroups.removeNonBaseEmptyGroups(m_act);
                                Toast.makeText(m_act, result, Toast.LENGTH_SHORT).show();
                            }else
                                Toast.makeText(m_act, "Canceled", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    };
    TextView.OnClickListener onClickRemoveStarredInAndroidGroups = new View.OnClickListener(){

        @Override
        public void onClick(View view) {

            DialogUtil.confirmDialog(m_act,
                    "Removed Starred In Android Groups",
                    "No contacts will be removed, only groups",
                    new DialogUtil.DialogUtilCallbacks() {
                        @Override
                        public void confirmed(boolean confirmed) {

                            if( confirmed){

                                String result = MyGroups.removeStarredInAndroidGroups(m_act);
                                Toast.makeText(m_act, result, Toast.LENGTH_SHORT).show();
                            }else
                                Toast.makeText(m_act, "Canceled", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    };
    TextView.OnClickListener onClickInspectRepairDb = new View.OnClickListener(){

        @Override
        public void onClick(View view) {

            inspectDb();
        }
    };

    private void inspectDb() {

        final ArrayList<Long> errorIds = new ArrayList<>();

        new AsyncTask<Void, Integer, String>() {

            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                progressDialog = new ProgressDialog(m_act);
                progressDialog.setMessage("Inspecting...");
                progressDialog.setIndeterminate(false);
                progressDialog.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(true);
                progressDialog.setMax(SqlCipher.getDbSize());
                progressDialog.setProgress(0);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(Void... voids) {

                StringBuilder log = new StringBuilder();
                String nl = System.getProperty("line.separator");

                long [] contactIds = SqlCipher.getContactIds( null );

                int valid = 0;
                int invalid = 0;
                int progress = 0;

                for( long contactId : contactIds){

                    String display_name = "";

                    boolean jsonSuccess = true, exceptionSuccess = true;
                    try {
                        display_name = SqlCipher.get( contactId, SqlCipher.ATab.display_name);
                        String kvString = SqlCipher.get( contactId, SqlCipher.DTab.kv);
                        JSONObject kvJobj = new JSONObject( kvString);
                    } catch (JSONException e) {
                        LogUtil.log(LogUtil.LogType.SQLCIPHER, "Invalid JSON DTab.kv for ID: "+contactId);
                        jsonSuccess = false;
                    } catch ( RuntimeException e){
                        LogUtil.log(LogUtil.LogType.SQLCIPHER, "RuntimeException for ID: "+contactId);
                        exceptionSuccess = false;
                        ++invalid;
                    }
                    if(jsonSuccess)
                        ++valid;
                    else
                        ++invalid;

                    if( ! jsonSuccess || ! exceptionSuccess){

                        errorIds.add( contactId);
                        log.append("Error: " + display_name + nl);
                    }

                    publishProgress( ++progress);
                }

                String repairNotice = (invalid == 0 ? "":
                        nl+"If you choose Repair, there may be data loss");
                log.append( repairNotice);

                return " Contacts: "+contactIds.length + nl
                        +" Valid: "+valid + nl
                        +" Invalid: "+invalid + nl
                        + nl
                        +log;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);

                progressDialog.setProgress( values[0]);
            }

            @Override
            protected void onPostExecute(String summary) {
                super.onPostExecute(summary);

                progressDialog.cancel();
                DialogUtil.twoButtonMlDialog(
                        m_act, "Inspection Summary", summary,
                        "Repair", new DialogUtil.DialogUtilCallbacks() {
                            @Override
                            public void confirmed(boolean repairConfirmed) {

                                if( repairConfirmed ){

                                    String s = repairDb( errorIds)?"Repair successful":"Repair failed";
                                        Toast.makeText(m_act, s ,Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }

        }.execute();
    }

    /**
     * Given a list of error contact IDs, generate new contacts and
     * populate each with the display_name and all contact name fields.
     * @param error_contact_ids
     */
    private boolean repairDb(ArrayList<Long> error_contact_ids) {

        try {
            for( long contact_id : error_contact_ids){

                long new_contact_id = SqlCipher.createEmptyContact(m_act, 0);

                /**
                 * Get the display name from the corrupt contact id and
                 * use it to create a new contact.  Assumes the name is not corrupt.
                 */
                String display_name = SqlCipher.get(contact_id, SqlCipher.ATab.display_name);
                NameUtil.parseFullNameToKv(new_contact_id, display_name);
                NameUtil.setNamesFromKv(new_contact_id);

                int[]  groups = MyGroups.getGroups(contact_id);

                for( int group : groups){

                    MyGroups.addGroupMembership( m_act, new_contact_id, group, true);
                    MyGroups.mGroupCount.put(group, 1+ MyGroups.mGroupCount.get(group));
                }
                SqlCipher.deleteContact( m_act, contact_id, true);
            }
        } catch (Exception e) {
            LogUtil.logException( CleanupFragment.class, e);
            return false;
        }
        return true;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Request a window without a title
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    Button.OnClickListener cancelButtonOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            m_fragment.dismiss();
        }
    };
}
