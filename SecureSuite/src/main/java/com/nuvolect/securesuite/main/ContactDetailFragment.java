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

package com.nuvolect.securesuite.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.MyContacts;
import com.nuvolect.securesuite.data.NameUtil;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlCipher.ATab;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.data.SqlCipher.KvTab;
import com.nuvolect.securesuite.graphics.RoundedImageView;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.LogUtil.LogType;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

/**
 * A fragment representing a single Contact detail screen. This fragment is
 * either contained in a {@link ContactListActivity} in two-pane mode (on
 * tablets) or a {@link ContactDetailActivity} on handsets.
 */
public class ContactDetailFragment extends Fragment {

    public long m_contact_id = -1;
    private final boolean DEBUG = LogUtil.DEBUG;
    private Activity m_act ;
    private boolean starred;

    private int starSizeDp = 40;
    private int starSize ;
    private int smsSizeDp = 40;
    private int smsSize ;
    private int imageSizeDp = 190;
    private int imageSize ;
    private int sectionPaddingDp = 10;
    private int sectionPadding ;
    private int cornerRadiusDp = 10;
    private int cornerRadius ;
    private int borderWidthDp = 2;
    private int borderWidth ;
    private float m_bodyTextSizeSp = 20f;//This size is adjusted based on the current density and user font size preference
    private float m_fullNameTextSizeSp = 28f;//This size is adjusted based on the current density and user font size preference
    private int m_rule_color;
    private int m_h1_color;
    private int m_h2_color;
    private int m_h3_color;
    private int m_body_color;
    private JSONObject mCommsSelectJson;// Communications select
    private TextView passwordTv;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ContactDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG)LogUtil.log("CDF onCreate");

        m_act = getActivity();

        if( savedInstanceState == null){

            /*
             * Assign color constants from the theme.  For whatever reason
             * when this is done after a rotation, all colors resolve to zero.
             * Consequently, save values in the instance state to restore later.
             */
            starSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    starSizeDp, getResources().getDisplayMetrics());
            smsSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    smsSizeDp, getResources().getDisplayMetrics());
            imageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    imageSizeDp, getResources().getDisplayMetrics());
            sectionPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    sectionPaddingDp, getResources().getDisplayMetrics());
            cornerRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    cornerRadiusDp, getResources().getDisplayMetrics());
            borderWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    borderWidthDp, getResources().getDisplayMetrics());

            m_rule_color = AppTheme.getThemeColor(m_act, R.attr.rule_color);
            m_h1_color   = AppTheme.getThemeColor(m_act, R.attr.h1_color);
            m_h2_color   = AppTheme.getThemeColor(m_act, R.attr.h2_color);
            m_h3_color   = AppTheme.getThemeColor(m_act, R.attr.h3_color);
            m_body_color = AppTheme.getThemeColor(m_act, R.attr.body_color);
        }else{
            // Restore from the instance state
            starSize       = savedInstanceState.getInt("starSize");
            smsSize        = savedInstanceState.getInt("smsSize");
            imageSize      = savedInstanceState.getInt("imageSize");
            sectionPadding = savedInstanceState.getInt("sectionPadding");
            cornerRadius   = savedInstanceState.getInt("cornerRadius");
            borderWidth    = savedInstanceState.getInt("borderWidth");
            m_rule_color   = savedInstanceState.getInt("m_rule_color");
            m_h1_color     = savedInstanceState.getInt("m_h1_color");
            m_h2_color     = savedInstanceState.getInt("m_h2_color");
            m_h3_color     = savedInstanceState.getInt("m_h3_color");
            m_body_color   = savedInstanceState.getInt("m_body_color");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current instance state
        savedInstanceState.putInt("starSize",       starSize);
        savedInstanceState.putInt("smsSize",        smsSize);
        savedInstanceState.putInt("imageSize",      imageSize);
        savedInstanceState.putInt("sectionPadding", sectionPadding);
        savedInstanceState.putInt("cornerRadius",   cornerRadius);
        savedInstanceState.putInt("borderWidth",    borderWidth);
        savedInstanceState.putInt("m_rule_color",   m_rule_color);
        savedInstanceState.putInt("m_h1_color",     m_h1_color);
        savedInstanceState.putInt("m_h2_color",     m_h2_color);
        savedInstanceState.putInt("m_h3_color",     m_h3_color);
        savedInstanceState.putInt("m_body_color", m_body_color);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(DEBUG)LogUtil.log("CDF onPause");
    }

    @Override
    public void onResume() {
        super.onResume();

        if(DEBUG)LogUtil.log("CDF onResume: "+SqlCipher.contactInfo(m_contact_id));
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(DEBUG)LogUtil.log("CDF onDestroy");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        m_contact_id = MyContacts.getCurrrentContactId( m_act);
        if(DEBUG)LogUtil.log("CDF onCreateView: "+SqlCipher.contactInfo(m_contact_id));

        View rootView=null;
        TableLayout table;
        RoundedImageView iv;

        if (m_contact_id > 0 && SqlCipher.validContactId(m_contact_id)) {

            int currentOrientation = getResources().getConfiguration().orientation;

            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Landscape
                rootView = inflater.inflate(R.layout.contact_detail_fragment_landscape, container, false);
                table = (TableLayout) rootView.findViewById(R.id.contact_detail_table_landscape);
                iv = (RoundedImageView) rootView.findViewById(R.id.contact_detail_image_landscape);
            }
            else {
                // Portrait
                rootView = inflater.inflate(R.layout.contact_detail_fragment_portrait, container, false);
                table = (TableLayout) rootView.findViewById(R.id.contact_detail_table_portrait);
                iv = (RoundedImageView) rootView.findViewById(R.id.contact_detail_image_portrait);
            }
            // Add the theme background outline and fill color behind fragment
            AppTheme.applyDrawableShape(m_act, rootView);

            table.setColumnStretchable(0, true);
            table.setColumnShrinkable( 1, true);
            table.setColumnShrinkable( 2, true);

            // Start with the contact image in its own view
            String encodedImage = SqlCipher.get( m_contact_id, DTab.photo);
            LayoutParams params = (LayoutParams) iv.getLayoutParams();
            params.width = imageSize;
            params.height = imageSize;
            iv.setLayoutParams(params);
            iv.setCornerRadius( (float)cornerRadius );
            iv.setBorderWidth( (float)borderWidth );
            iv.setBorderColor(m_rule_color);

            if( encodedImage.isEmpty()){

                if( SqlCipher.get( m_contact_id, ATab.last_name).isEmpty())
                    iv.setImageDrawable( getResources().getDrawable(R.drawable.ic_social_location_city));
                else
                    iv.setImageDrawable( getResources().getDrawable(R.drawable.ic_social_person));
            }
            else{
                byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                iv.setImageBitmap(decodedBitmap);
            }

            // Clear the display of any previous table views
            table.removeAllViews();
            table.setPadding( 15, 15, 15, sectionPadding);

            // Create the top row
            TableRow row = new TableRow(m_act);
            row.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.WRAP_CONTENT));

            // Add the name and make it large
            String full_name = NameUtil.getFullName(m_contact_id);
            TextView titleTv = new TextView( m_act);
            titleTv.setText(full_name);
            titleTv.setTextColor(m_h1_color);
            titleTv.setWidth( 1 );// Wrap text
            titleTv.setTextSize(m_fullNameTextSizeSp);
            row.addView( titleTv);

            // Next column is empty for the display_name row
            ImageView emptyIv = new ImageView(m_act);
            row.addView( emptyIv);

            // Last column is the favorite star
            starred = SqlCipher.get(m_contact_id, ATab.starred).contains(CConst.STARRED_1);
            final ImageView favoriteIv = new ImageView(m_act);
            TableRow.LayoutParams favorate_layoutParams = new TableRow.LayoutParams(starSize, starSize);
            favoriteIv.setLayoutParams(favorate_layoutParams);
            if( starred )
                favoriteIv.setBackgroundResource(R.drawable.btn_rating_star_on_normal);
            else
                favoriteIv.setBackgroundResource(R.drawable.btn_rating_star_off_normal);

            favoriteIv.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View arg0) {
                    starred = ! starred;
                    SqlCipher.invertStarred( m_contact_id);
                    if( starred ){
                        favoriteIv.setBackgroundResource(R.drawable.btn_rating_star_on_normal);
                    }
                    else{
                        favoriteIv.setBackgroundResource(R.drawable.btn_rating_star_off_normal);
                    }
                    mCallbacks.refreshGroupList();
                }});
            row.addView( favoriteIv );

            // Add first row, name and star
            table.addView(row);

            // Add the company and title if there is one
            String title = SqlCipher.getKv(m_contact_id, KvTab.title);
            String organization = SqlCipher.getKv(m_contact_id, KvTab.organization);

            String separator = "";
            if( ! title.isEmpty() && ! organization.isEmpty())
                separator = ", ";

            String titleAndCompany = title + separator + organization;

            if( ! titleAndCompany.trim().isEmpty())
                addTextRow( titleAndCompany, table, false);

            // Accumulate the phonetic name and add it if there is one
            String phoneticName = SqlCipher.getKv(m_contact_id, KvTab.phonetic_family)+" "+
                    SqlCipher.getKv(m_contact_id, KvTab.phonetic_middle)+" "+
                    SqlCipher.getKv(m_contact_id, KvTab.phonetic_given);

            if( ! phoneticName.trim().isEmpty()){
                addSectionTitle( table, "PHONETIC NAME");
                addRule( table);

                addTextRow( phoneticName, table, false);
            }

            // Load user communication selections
            try {
                mCommsSelectJson = new JSONObject( SqlCipher.get(m_contact_id, DTab.comms_select ));
            } catch (JSONException e) {
                LogUtil.logException(m_act, LogType.CONTACT_DETAIL, e);
            }

            newSection( DTab.phone, table);
            newSection( DTab.email, table);
            newSection( DTab.im, table);
            newSection( DTab.address, table);
            newSection( DTab.website, table);
            newKvSection("USERNAME", KvTab.username, table, false);
            newKvSection("PASSWORD", KvTab.password, table, true);
            newSection( DTab.relation, table);
            newSection( DTab.date, table);
            newKvSection("NOTES", KvTab.note, table, false);

            String account = SqlCipher.get( m_contact_id, ATab.account_name);
            addSectionTitle( table, "GROUPS ("+account+")");
            addRule( table);

            String groupList = MyGroups.getGroupTitles( m_contact_id);
            addTextRow( groupList, table, false);
        }
        else{
            rootView = inflater.inflate(R.layout.contact_detail_fragment_empty, container, false);

            // Add the theme background outline and fill color behind fragment
            AppTheme.applyDrawableShape( getActivity(), rootView);
        }

        return rootView;
    }

    /**
     * Add a new KV section if the KV value is not empty.
     * @param sectionTitle
     * @param kvItem
     * @param table
     */
    private void newKvSection( String sectionTitle, KvTab kvItem, TableLayout table, boolean startHidden){

        String value = SqlCipher.getKv(m_contact_id, kvItem);

        if( ! value.trim().isEmpty()){

            addSectionTitle( table, sectionTitle);
            addRule( table);
            addTextRow(value, table, startHidden);
        }
    }

    private void addTextRow(String textToAdd, TableLayout table, boolean startHidden) {

        TableRow row = new TableRow(m_act);
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,TableRow.LayoutParams.WRAP_CONTENT));

        TextView tv = new TextView( m_act);
        tv.setText( textToAdd );
        tv.setTextColor( m_body_color );
        tv.setTextSize( m_bodyTextSizeSp );
        tv.setWidth( 1 );
        tv.setPadding(5, 5, 0, 12);
        tv.setLongClickable(true);
        tv.setOnLongClickListener(onLongClickListener);

        if( startHidden){

            passwordTv = tv;
            passwordTv.setOnClickListener(toggleViewOnClickListener);
            passwordTv.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        row.addView( tv);
        table.addView(row);
    }

    /**
     * Invert the password view.  Short press to toggle, long press to copy
     */
    OnClickListener toggleViewOnClickListener = new OnClickListener() {

        boolean passwordHidden = true;

        @Override
        public void onClick(View v) {

            passwordHidden = ! passwordHidden;

            if( passwordHidden)
                passwordTv.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            else
                passwordTv.setInputType(InputType.TYPE_CLASS_TEXT);
        }
    };

    @SuppressLint("NewApi")
    private void newSection(final DTab dbEnum, TableLayout table){

        TableRow row;

        try {

            String itemList = SqlCipher.get(m_contact_id, dbEnum);
            if( ! itemList.contentEquals("[]")){

                JSONArray itemArray = new JSONArray(itemList);

                addSectionTitle( table, dbEnum.toString().toUpperCase(Locale.US));
                addRule( table);

                // Iterate through the json array and display the value on a row followed by the key on a row

                for( int item_index = 0; item_index < itemArray.length(); item_index++){

                    JSONObject item = itemArray.getJSONObject(item_index);
                    Iterator<?> item_keys = item.keys();
                    String item_label = (String) item_keys.next();
                    final String item_value = item.getString(item_label);

                    // Add next item value (i.e., phone number, etc.)
                    row = new TableRow(m_act);
                    row.setLayoutParams(new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));

                    TextView value_tv = new TextView( m_act);
                    value_tv.setText( item_value );
                    value_tv.setTextColor(m_body_color);
                    value_tv.setTextSize(m_bodyTextSizeSp);
                    value_tv.setWidth(1);
                    value_tv.setPadding(5, 5, 0, 0);

                    /**
                     * Set short press listeners for everything that can be operated on.
                     */
                    setListener(dbEnum, value_tv);

                    /**
                     * Set long press listener to copy all values
                     */
                    value_tv.setLongClickable( true );
                    value_tv.setOnLongClickListener( onLongClickListener);
                    row.addView( value_tv);

                    // If it is a phone number, add the SMS icon and click listener
                    if( dbEnum == DTab.phone){

                        final ImageView smsIv = new ImageView(m_act);
                        TableRow.LayoutParams favorate_layoutParams = new TableRow.LayoutParams(smsSize, smsSize);
                        smsIv.setLayoutParams(favorate_layoutParams);
                        AppTheme.colorBackgroundDrawable( m_act, smsIv, R.drawable.sms, m_body_color);

                        smsIv.setOnClickListener(new OnClickListener(){
                            public void onClick(View arg0) {

//                                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
//                                smsIntent.addCategory(Intent.CATEGORY_DEFAULT);
//                                smsIntent.setType("vnd.android-dir/mms-sms");
//                                smsIntent.putExtra("address", item_value);

                                // http://stackoverflow.com/questions/9798657/send-sms-via-intent
                                Uri uri = Uri.parse("smsto:"+item_value);
                                Intent smsIntent = new Intent(Intent.ACTION_SENDTO, uri);

                                if(Util.isIntentAvailable(m_act, smsIntent))
                                    startActivity( smsIntent );
                                else
                                    Toast.makeText(getActivity(), "No SMS app", Toast.LENGTH_SHORT).show();

                            }});
                        row.addView( smsIv );

                        final ImageView commSelectIv = new ImageView(m_act);
                        TableRow.LayoutParams commSelectLayoutParams = new TableRow.LayoutParams(starSize, starSize);
                        commSelectIv.setLayoutParams(commSelectLayoutParams);

                        if( mCommsSelectJson.has( item_value ))
                            commSelectIv.setBackgroundResource(R.drawable.btn_check_buttonless_on);
                        else
                            commSelectIv.setBackgroundResource(R.drawable.btn_check_buttonless_off);

                        commSelectIv.setTag( item_value );
                        commSelectIv.setOnClickListener(new OnClickListener(){

                            @Override
                            public void onClick(View v) {

                                try {
                                    String item_value = (String) v.getTag();
                                    if( mCommsSelectJson.has( item_value)){

                                        mCommsSelectJson.remove( item_value );
                                        v.setBackgroundResource(R.drawable.btn_check_buttonless_off);
                                    }
                                    else{

                                        mCommsSelectJson.put( item_value, CConst.COMMS_SELECT_MOBILE);
                                        v.setBackgroundResource(R.drawable.btn_check_buttonless_on);
                                    }

                                    // Save comms selection state
                                    SqlCipher.put(m_contact_id, DTab.comms_select, mCommsSelectJson.toString());

                                } catch (JSONException e) {
                                    LogUtil.logException(m_act, LogType.CONTACT_DETAIL, e);
                                }

                            }});
                        row.addView( commSelectIv );
                    }
                    if( dbEnum == DTab.email){

                        // Skip the second column
                        final ImageView emptyIv = new ImageView(m_act);
                        row.addView( emptyIv );

                        final ImageView commSelectIv = new ImageView(m_act);
                        TableRow.LayoutParams commSelectLayoutParams = new TableRow.LayoutParams(starSize, starSize);
                        commSelectIv.setLayoutParams(commSelectLayoutParams);

                        if( mCommsSelectJson.has( item_value ))
                            commSelectIv.setBackgroundResource(R.drawable.btn_check_buttonless_on);
                        else
                            commSelectIv.setBackgroundResource(R.drawable.btn_check_buttonless_off);

                        commSelectIv.setTag( item_value );
                        commSelectIv.setOnClickListener(new OnClickListener(){

                            @Override
                            public void onClick(View v) {

                                try {
                                    String item_value = (String) v.getTag();
                                    if( mCommsSelectJson.has( item_value)){

                                        mCommsSelectJson.remove( item_value );
                                        v.setBackgroundResource(R.drawable.btn_check_buttonless_off);
                                    }
                                    else{

                                        mCommsSelectJson.put( item_value, CConst.COMMS_SELECT_EMAIL);
                                        v.setBackgroundResource(R.drawable.btn_check_buttonless_on);
                                    }

                                    // Save comms selection state
                                    SqlCipher.put(m_contact_id, DTab.comms_select, mCommsSelectJson.toString());

                                } catch (JSONException e) {
                                    LogUtil.logException(m_act, LogType.CONTACT_DETAIL, e);
                                }

                            }});
                        row.addView( commSelectIv );
                    }

                    table.addView(row);

                    // Add the descriptor name (home)
                    row = new TableRow(m_act);
                    row.setLayoutParams(new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,TableRow.LayoutParams.WRAP_CONTENT));

                    TextView name_tv = new TextView( m_act);
                    name_tv.setText( item_label.toUpperCase(Locale.US));
                    name_tv.setTextColor( m_h3_color );
                    name_tv.setWidth( 1 );
                    name_tv.setPadding(5, 0, 0, 0);
                    row.addView( name_tv);
                    table.addView(row);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addSectionTitle(TableLayout table, String title) {

        TableRow row = new TableRow(m_act);
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,TableRow.LayoutParams.WRAP_CONTENT));

        TextView tv = new TextView( m_act);
        tv.setText( title );
        tv.setTextColor( m_h2_color);
        tv.setTypeface( null, Typeface.BOLD);
        tv.setWidth( 1 );
        tv.setPadding(5, sectionPadding, 0, 5);
        row.addView( tv);
        table.addView(row);
    }

    /**
     * Add a horizontal rule
     * @param table
     */
    private void addRule(TableLayout table) {

        TableRow row = new TableRow(m_act);
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,TableRow.LayoutParams.WRAP_CONTENT));

        // Horizontal rule uses two columns
        View v = new View( m_act);
        v.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor( m_rule_color);
        row.addView( v );

        View v2 = new View( m_act);
        v2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
        v2.setBackgroundColor( m_rule_color);
        row.addView( v2 );

        View v3 = new View( m_act);
        v3.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
        v3.setBackgroundColor( m_rule_color);
        row.addView( v3 );

        table.addView(row);
    }

    /**
     * Copy value with a long press, othewise try to operate with it.
     */
    View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {

            TextView tv = (TextView)v;
            CharSequence item = tv.getText();

            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    m_act.getSystemService(Context.CLIPBOARD_SERVICE);

            // Creates a new text clip to put on the clipboard
            ClipData clip = ClipData.newPlainText("SecureSuite", item);

            // Set the clipboard's primary clip.
            clipboard.setPrimaryClip(clip);

            if( tv == passwordTv)
                Toast.makeText(getActivity(), "Copied password", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getActivity(), "Copied: "+item, Toast.LENGTH_SHORT).show();

            return true;// consume the click event
        }
    };

    private void setListener(DTab dbEnum, TextView value_tv) {

        value_tv.setClickable(true);

        switch( dbEnum){

        case phone:
            value_tv.setOnClickListener( phoneClickListener);
            break;
        case im:
            value_tv.setOnClickListener( imClickListener);
            break;
        case email:
            value_tv.setOnClickListener( emailClickListener);
            break;
        case address:
            value_tv.setOnClickListener( addressClickListener);
            break;
        case website:
            value_tv.setOnClickListener( websiteClickListener);
            break;
        default:
            value_tv.setClickable(false);
        }
    }

    OnClickListener phoneClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            TextView tv = (TextView)v;
            String phoneNumber = tv.getText().toString();

            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));

            if(Util.isIntentAvailable(m_act, intent))
                startActivity(intent);
            else
                Toast.makeText(getActivity(), "No phone app", Toast.LENGTH_SHORT).show();
        }
    };

    OnClickListener imClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            TextView tv = (TextView)v;
            String imAddress = tv.getText().toString();

            Intent sendIntent = new Intent(Intent.ACTION_VIEW);
            sendIntent.putExtra("address", imAddress);
            sendIntent.setData(Uri.parse("sms:"));

            Toast.makeText(getActivity(), "sms: "+imAddress, Toast.LENGTH_SHORT).show();
        }
    };

    OnClickListener emailClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            TextView tv = (TextView)v;
            String emailAddress = tv.getText().toString();

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", emailAddress, null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "");

            if(Util.isIntentAvailable(m_act, emailIntent))
                startActivity(Intent.createChooser(emailIntent, "Send email..."));
            else
                Toast.makeText(getActivity(), "No email app", Toast.LENGTH_SHORT).show();
        }
    };

    OnClickListener addressClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            TextView tv = (TextView)v;
            CharSequence item = tv.getText();

            String address = item.toString().replaceAll("[\\t\\n\\r]", ", ");

            Uri gmmIntentUri = Uri.parse("geo:0,0?q="+ address);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if(Util.isIntentAvailable(m_act, mapIntent))
                startActivity(mapIntent);
            else
                Toast.makeText(getActivity(), "No mapping app", Toast.LENGTH_SHORT).show();

            Toast.makeText(getActivity(), "address: "+item, Toast.LENGTH_SHORT).show();
        }
    };

    OnClickListener websiteClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            TextView tv = (TextView)v;
            String url = tv.getText().toString();

            if (!url.startsWith("http://") && !url.startsWith("https://"))
                url = "http://" + url;

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse( url ));

            if(Util.isIntentAvailable(m_act, browserIntent))
                startActivity(browserIntent);
            else
                Toast.makeText(getActivity(), "No browser app", Toast.LENGTH_SHORT).show();
        }
    };


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.contact_detail_single_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // Action bar items

        switch (item.getItemId()) {

        case R.id.menu_edit_contact:{
            if( m_contact_id <= 0){

                // No contact select yet, default to the first contact record
                m_contact_id = SqlCipher.getFirstContactID();
                Persist.setCurrentContactId(m_act, m_contact_id);
            }

            Intent i = new Intent( m_act, ContactEditActivity.class);
            startActivity(i);

            return true;
        }
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;
    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        public void refreshGroupList();
    }
    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void refreshGroupList() {
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }
}
