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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.NameUtil;
import com.nuvolect.securesuite.data.MyContacts;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlCipher.ATab;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.data.SqlCipher.KvTab;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.LogUtil.LogType;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.Safe;
import com.nuvolect.securesuite.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * <pre>
 * An editor activity for a single Contact. It is all done through code and no XML.
 * The page is a ScrollView layout containing a RelativeLayout with a series of TableLayouts.
 *
 * The top name section is a single TableLayout. Layout elements are all put in place and
 * most are initially GONE, that is they are there but do not take space and are not visible.
 * Remaining sections can contain a variable number of rows of like kind items, emails, etc.
 *
 * Each section is three TableLayout elements:
 * 1) the header and rule,
 * 2) the body and
 * 3) the Add new button.
 * The section is stored in a class to better manage data and control such as visibility.
 *
 * When the user selects "Done" the JSON data is written back to the database, otherwise if the
 * user exits the data is just tossed.
 *
 * JSON pairs are stored using the key as a label and value as the target data.
 * Example phone: Key: Home, Value: 123-456-7890
 * Example email: Key: Home, Value: john @ doe.com
 * </pre>
 */
public class ContactEditFragment extends Fragment {

    private static final boolean DEBUG = LogUtil.DEBUG;
    public static final String ITEM_ID_KEY = "item_id";
    public static final String DELETE_ME_KEY = "DeLeTe Me";
    public static long m_contact_id = -1;
    private static Activity m_act;
    private static boolean nameMenuExpand;
    private static boolean m_passwordFieldsShown;
    private static JSONObject kvJson;
    private static RelativeLayout m_relativeLayout;
    private static EditFullName editFullName;
    private static EditRow row_name_prefix;
    private static EditRow row_name_first;
    private static EditRow row_name_middle;
    private static EditRow row_name_last;
    private static EditRow row_name_suffix;
    //    private static EditRow row_note;
    private static EditRow row_phonetic_family;
    private static EditRow row_phonetic_middle;
    private static EditRow row_phonetic_given;
    private static EditRow row_nickname;
    private static EditRow row_organization;
    private static EditRow row_title;
    private static EditNotes section_notes;
    private static EditUsername section_username;
    private static EditPassword section_password;
    private PasswordFragment m_passwordFragment;
    private static EditSection section_phone;
    private static EditSection section_email;
    private static EditSection section_im;
    private static EditSection section_address;
    private static EditSection section_website;
    private static EditSection section_relation;
    private static EditSection section_date;
    private static EditSection section_internetcall;
    private static EditGroups section_groups;

    private static int menuArrowSizeDp = 50;
    private static int menuSize ;
    private static int imageSizeDp = 50;
    private static int imageSize ;
    private static int clearXSizeDp = 32;
    private static int clearXSize ;
    private static int sectionPaddingDp = 10;
    private static int sectionPadding ;

    android.widget.TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);

    android.widget.TableLayout.LayoutParams tableLayoutParams = new TableLayout.LayoutParams(
            TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT);

    RelativeLayout.LayoutParams relativeLayoutParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

    // Keep track of group participation and use when user presses done
    public static int[] m_groupIds;
    public static CharSequence[] m_groupNames;
    public static String m_group_account;
    public static boolean[] m_groupCheckstate;
    private static boolean m_groupMembershipChanged;
    //    public static Button m_groupEditButton=null;
    public static AlertDialog m_groupsEditAlertDialog;
    public static String SELECT_GROUPS = "Select groups";
    public static String NOTES_HINT = "Enter notes";
    public static String USERNAME_HINT = "Enter username";
    public static String PASSWORD_HINT = "Generate or enter password";
    private int m_rule_color;
    private int m_h1_color;
    private int m_h2_color;
    private int m_h3_color;
    private int m_body_color;
    public EditText m_notesEt;
    public EditText m_usernameEt;
    public EditText m_passwordEt;
    private EditText full_nameEt;
    private ImageView m_photoMenu;
    private Bitmap m_importDecodedBitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG)LogUtil.log("ContactEditFragment onCreate");

        m_act = getActivity();

        setHasOptionsMenu( true );
        if( savedInstanceState == null){

            /*
             * Assign color constants from the theme.  For whatever reason
             * when this is done after a rotation, all colors resolve to zero.
             * Consequently, save values in the instance state to restore later.
             */

            menuSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    menuArrowSizeDp, getResources().getDisplayMetrics());
            imageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    imageSizeDp, getResources().getDisplayMetrics());
            clearXSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    clearXSizeDp, getResources().getDisplayMetrics());
            sectionPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    sectionPaddingDp, getResources().getDisplayMetrics());

            m_rule_color = AppTheme.getThemeColor(m_act, R.attr.rule_color);
            m_h1_color   = AppTheme.getThemeColor(m_act, R.attr.h1_color);
            m_h2_color   = AppTheme.getThemeColor(m_act, R.attr.h2_color);
            m_h3_color   = AppTheme.getThemeColor(m_act, R.attr.h3_color);
            m_body_color = AppTheme.getThemeColor(m_act, R.attr.body_color);
        }else{

            // Restore from the instance state
            menuSize     = savedInstanceState.getInt("menuSize");
            imageSize    = savedInstanceState.getInt("imageSize");
            clearXSize   = savedInstanceState.getInt("clearXSize");
            sectionPadding = savedInstanceState.getInt("sectionPadding");
            m_rule_color = savedInstanceState.getInt("m_rule_color");
            m_h1_color   = savedInstanceState.getInt("m_h1_color");
            m_h2_color   = savedInstanceState.getInt("m_h2_color");
            m_h3_color   = savedInstanceState.getInt("m_h3_color");
            m_body_color = savedInstanceState.getInt("m_body_color");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current instance state
        savedInstanceState.putInt("menuSize",       menuSize);
        savedInstanceState.putInt("imageSize",      imageSize);
        savedInstanceState.putInt("clearXSize",     clearXSize);
        savedInstanceState.putInt("sectionPadding", sectionPadding);
        savedInstanceState.putInt("m_rule_color",   m_rule_color);
        savedInstanceState.putInt("m_h1_color",     m_h1_color);
        savedInstanceState.putInt("m_h2_color",     m_h2_color);
        savedInstanceState.putInt("m_h3_color",     m_h3_color);
        savedInstanceState.putInt("m_body_color",   m_body_color);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(DEBUG)LogUtil.log("ContactEditFragment onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        if(DEBUG)LogUtil.log("ContactEditFragment onResume");
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if(DEBUG)LogUtil.log("ContactEditFragment onDestroy");

        // In case the user discards empty contact, it will be deleted
        MyContacts.manageEmptyContact(m_act);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(16)
    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        m_contact_id = Persist.getCurrentContactId(m_act);
        m_groupMembershipChanged = false;
        nameMenuExpand = false;
        m_passwordFieldsShown = false;

        // Dynamically build a layout of ScrollView containing a RelativeLayout
        ScrollView scrollView = new ScrollView(m_act);
        scrollView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // Set the image behind all of the content
        if( Build.VERSION.SDK_INT >= 16)
            scrollView.setBackground( AppTheme.backgroundDrawableImage(m_act));
        else
            scrollView.setBackgroundDrawable( AppTheme.backgroundDrawableImage(m_act));

        m_relativeLayout = new RelativeLayout(m_act);
        scrollView.addView( m_relativeLayout);

        if( Build.VERSION.SDK_INT >= 16)
            scrollView.setBackground( AppTheme.backgroundDrawableShape( m_act));
        else
            scrollView.setBackgroundDrawable( AppTheme.backgroundDrawableShape( m_act));

        TableLayout nameTable = new TableLayout( m_act);
        nameTable.setId( Util.generateViewId());

        nameTable.setLayoutParams( tableLayoutParams);
        m_relativeLayout.addView(nameTable);

        if (m_contact_id != -1) {  // Make sure a valid contact ID was passed

            kvJson = null;
            try {
                kvJson = new JSONObject( SqlCipher.get( m_contact_id, DTab.kv));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //FUTURE use dp to pixel unit conversion for consistency
            // Clear the display of any previous table views
            nameTable.setPadding( 15, 15, 15, 15);
            nameTable.setColumnStretchable(0, true);
            nameTable.setColumnShrinkable( 1, true);
            nameTable.setColumnShrinkable( 2, true);

            // Create the top row: name, expand arrow and mini profile photo
            TableRow row = new TableRow(m_act);
            row.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.WRAP_CONTENT));

            // Add the full name in large letters
            editFullName = new EditFullName( row );

            // Second column is the name menu arrow
            final ImageView nameMenu = new ImageView(m_act);
            TableRow.LayoutParams nameMenu_layoutParams = new TableRow.LayoutParams(menuSize, menuSize);
            nameMenu.setLayoutParams(nameMenu_layoutParams);
            AppTheme.colorBackgroundDrawable( m_act, nameMenu, R.drawable.expander_open, m_h1_color);
            nameMenu.setOnClickListener( new OnClickListener() {
                public void onClick(View v) {

                    nameMenuExpand = ! nameMenuExpand;
                    if( nameMenuExpand){
                        AppTheme.colorBackgroundDrawable(m_act, nameMenu, R.drawable.expander_close, m_h1_color);
                        row_name_prefix.setVisibile(true);
                        row_name_first.setVisibile(true);
                        row_name_middle.setVisibile( true );
                        row_name_last.setVisibile( true );
                        row_name_suffix.setVisibile(true);

                        // When editing details, disable editing full name
                        full_nameEt.setClickable(false);
                        full_nameEt.setEnabled(false);
                    }else{
                        AppTheme.colorBackgroundDrawable(m_act, nameMenu, R.drawable.expander_open, m_h1_color);
                        row_name_prefix.setVisibile(false);
                        row_name_first.setVisibile(false);
                        row_name_middle.setVisibile( false );
                        row_name_last.setVisibile( false );
                        row_name_suffix.setVisibile(false);
                        full_nameEt.setClickable(true);
                        full_nameEt.setEnabled(true);
                    }
                    return;
                }
            });
            row.addView( nameMenu );

            // Third column is the picture menu
            m_photoMenu = new ImageView(m_act);
            TableRow.LayoutParams photoMenu_layoutParams = new TableRow.LayoutParams(imageSize, imageSize);
            m_photoMenu.setLayoutParams(photoMenu_layoutParams);
            String encodedImage = SqlCipher.get(m_contact_id, DTab.photo);
            if( encodedImage.isEmpty()){

                if( SqlCipher.get(m_contact_id, ATab.last_name).isEmpty())
                    m_photoMenu.setBackgroundResource(R.drawable.ic_social_location_city);
                else
                    m_photoMenu.setBackgroundResource(R.drawable.ic_social_person);
                m_photoMenu.setImageDrawable( getResources().getDrawable(R.drawable.contact_picture_large));
            }
            else{
                byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                m_photoMenu.setImageBitmap(decodedBitmap);
            }
            m_photoMenu.setOnClickListener( new OnClickListener() {

                /*
                 * Start a photo browser with listener returning photo location.
                 * The photo is imported into a class member variable and
                 * saved when user hits save.
                 */
                public void onClick(View v) {
                    if(DEBUG)LogUtil.log( "photoMenuClick");

                    // Kickoff a browser activity here.
                    // When user selects file, onActivityResult called with the result.
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    m_act.startActivityForResult(
                            Intent.createChooser(intent, "Select Picture"), CConst.BROWSE_IMPORT_PHOTO_ACTION);
                    return;
                }
            });

            row.addView( m_photoMenu );

            // Add first row
            nameTable.addView(row);

            // Add rows for name details
            row_name_prefix = new EditRow( nameTable, KvTab.name_prefix, "Name prefix");
            row_name_first  = new EditRow( nameTable, KvTab.name_first,  "First name");
            row_name_middle = new EditRow( nameTable, KvTab.name_middle, "Middle name");
            row_name_last   = new EditRow( nameTable, KvTab.name_last,   "Last name");
            row_name_suffix = new EditRow( nameTable, KvTab.name_suffix, "Last suffix");

            row_phonetic_family = new EditRow( nameTable, KvTab.phonetic_family, "Phonetic family name");
            row_phonetic_middle = new EditRow( nameTable, KvTab.phonetic_middle, "Phonetic middle name");
            row_phonetic_given  = new EditRow( nameTable, KvTab.phonetic_given, "Phonetic given name");

            row_nickname        = new EditRow( nameTable, KvTab.nickname, "Nickname");

            row_organization    = new EditRow( nameTable, KvTab.organization, "Add organization");
            row_organization.setVisibile( true );
            row_title           = new EditRow( nameTable, KvTab.title, "Title");

            //            row_note            = new EditRow( nameTable, KvTab.note, "Notes");

            // Name, image and header is in place
            // Iterate through content for this contact
            //                                      Section to add     Add after this table
            section_phone =        new EditSection( DTab.phone,        nameTable);
            section_email =        new EditSection( DTab.email,        section_phone.lastTable);
            section_im =           new EditSection( DTab.im,           section_email.lastTable);
            section_address =      new EditSection( DTab.address,      section_im.lastTable);
            section_website =      new EditSection( DTab.website,      section_address.lastTable);
            section_username =     new EditUsername(                   section_website.lastTable);
            section_password =     new EditPassword(                   section_username.lastTable);

            section_internetcall = new EditSection( DTab.internetcall, section_password.lastTable);
            section_relation =     new EditSection( DTab.relation,     section_internetcall.lastTable);
            section_date =         new EditSection( DTab.date,         section_relation.lastTable);

            section_notes =        new EditNotes  ( /* KvTab.note */   section_date.lastTable);
            section_groups =       new EditGroups (                    section_notes.lastTable);
            TableLayout lastTable = section_groups.lastTable;

            // These sections start expanded, even if empty
            section_phone.setVisible( true);
            section_email.setVisible( true);
            section_groups.setVisible( true);

            // Add button at bottom "Add another field"
            TableLayout tableLayout = new TableLayout(m_act);
            tableLayout.setLayoutParams( tableLayoutParams );

            // Clear the display of any previous table views
            tableLayout.removeAllViews();
            tableLayout.setPadding( 15, 15, 15, 15);

            TableRow buttonRow = new TableRow(m_act);
            buttonRow.setLayoutParams( tableRowParams );

            Button button = new Button(m_act);
            button.setText("Add another field");
            button.setOnClickListener(new OnClickListener(){
                public void onClick(View arg0) {
                    addFieldDialog();
                }
            });
            buttonRow.addView(button);

            //            TableRow.LayoutParams buttonRowParams = (TableRow.LayoutParams) buttonRow.getLayoutParams();
            //            buttonRowParams.gravity = Gravity.CENTER;
            //            button.setLayoutParams(buttonRowParams);
            //FUTURE center button
            //            TableLayout.LayoutParams testLP = new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            //            testLP.gravity = Gravity.CENTER;
            //            buttonRow.setLayoutParams(testLP);

            tableLayout.addView(buttonRow);// Row added to a table

            RelativeLayout.LayoutParams buttonRowLayoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);

            int tid = lastTable.getId();
            buttonRowLayoutParams.addRule(RelativeLayout.BELOW, tid);

            m_relativeLayout.addView(tableLayout, buttonRowLayoutParams);// Table added to a view
        }
        return scrollView;
    }


    /**
     * Add another field menu: in menu order.  Replaces '_' with ' ' on menu.
     */
    public static enum AddFieldMenu {
        Phonetic_name,
        IM,
        Notes,
        Nickname,
        Website,
        Username,
        Password,
        Internet_call,
        Relationship,  // not Relation, to match Google
        Date
    };

    /**
     * Present dynamic menu with "Add another field" options
     */
    protected void addFieldDialog() {

        final List<String> stringMenuText = new ArrayList<String>();
        final List<AddFieldMenu> stringMenuEnum = new ArrayList<AddFieldMenu>();

        for( AddFieldMenu menuItem : AddFieldMenu.values()){

            boolean display = true;

            switch( menuItem){

                case Date:
                    if( section_date.isShown())
                        display = false;
                    break;
                case IM:
                    if( section_im.isShown())
                        display = false;
                    break;
                case Internet_call:
                    if( section_internetcall.isShown())
                        display = false;
                    break;
                case Nickname:
                    if( row_nickname.isShown())
                        display = false;
                    break;
                case Notes:
                    if( section_notes.isShown())
                        display = false;
                    break;
                case Password:
                    if( section_password.isShown())
                        display = false;
                    break;
                case Phonetic_name:
                    if( row_phonetic_family.isShown())
                        display = false;
                    break;
                case Relationship:
                    if( section_relation.isShown())
                        display = false;
                    break;
                case Username:
                    if( section_username.isShown())
                        display = false;
                    break;
                case Website:
                    if( section_website.isShown())
                        display = false;
                    break;

                default:
            }

            if(display){
                String item = menuItem.toString().replace('_', ' ');

                // Save menu text to display and the enum for switch statement that follows
                stringMenuText.add( item);
                stringMenuEnum.add( menuItem);
            }
        }
        final CharSequence[] items = stringMenuText.toArray(new CharSequence[stringMenuText.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(m_act);
        builder.setItems( items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                AddFieldMenu menuItem = stringMenuEnum.get(which);

                switch( menuItem){

                    case Phonetic_name:
                        row_phonetic_family.setVisibile( true );
                        row_phonetic_middle.setVisibile( true );
                        row_phonetic_given.setVisibile( true );
                        row_phonetic_family.requestFocus();
                        break;
                    case IM:
                        section_im.setVisible(true);
                        section_im.requestFocus();
                        break;
                    case Notes:
                        section_notes.setVisible( true);
                        section_notes.requestFocus();
                        break;
                    case Nickname:
                        row_nickname.setVisibile(true);
                        row_nickname.requestFocus();
                        break;
                    case Website:
                        section_website.setVisible(true);
                        section_website.requestFocus();
                        break;
                    case Username:
                        section_username.setVisible( true);
                        section_username.requestFocus();
                        break;
                    case Password:
                        section_password.setVisible( true);
                        section_password.requestFocus();
                        break;
                    case Internet_call:
                        section_internetcall.setVisible( true);
                        section_internetcall.requestFocus();
                        break;
                    case Relationship:
                        section_relation.setVisible( true);
                        section_relation.requestFocus();
                        break;
                    case Date:
                        section_date.setVisible( true);
                        section_date.requestFocus();
                        break;

                    default:
                        if(DEBUG)LogUtil.log( "Unknown: "+menuItem.toString());
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    /**
     * Configure the name row for editing
     */
    public class EditFullName {

        private TextWatcher m_textWatcher;
        public String full_name;

        private EditFullName(TableRow row){

            // Add the name and make it large
            full_name = NameUtil.getFullName( m_contact_id);
            full_nameEt = new EditText( m_act);
            full_nameEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            full_nameEt.setTextColor(m_h1_color);
            full_nameEt.setHint("Prefix first middle last suffix");
            if( ! full_name.isEmpty())
                full_nameEt.setText(full_name);
            full_nameEt.setWidth(1);// Wrap text
            full_nameEt.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.title_text_size));

            m_textWatcher = new TextWatcher() {
                public void afterTextChanged(Editable s) {
                    full_name = s.toString().trim();

                    // User edited the display name directly.  Parse it into parts.
                    parseFullName();
//                    if(DEBUG)LogUtil.log( "nameTextWatcher: "+display_name);
                }
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
                public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
            };
            full_nameEt.addTextChangedListener(m_textWatcher);
            row.addView(full_nameEt);
        }

        /**
         * Enable or disable the row text listener.  This is useful when the text
         * field is being edited by actions on another row, such as editing
         * elements of a name that can change the full name.
         * @param listener
         */
        private void enableListener(boolean listener){

            if( listener )
                full_nameEt.addTextChangedListener( m_textWatcher);
            else
                full_nameEt.removeTextChangedListener( m_textWatcher);
        }

        public void setText(String text){

            enableListener( false);
            full_nameEt.setText(text);
            enableListener( true);
        }

        /**
         * Combine all the elements of a name into the Display Name.
         */
        public void mergeDisplayName() {

            try {
                String s = kvJson.getString( KvTab.name_prefix.toString()) + " ";
                s += kvJson.getString( KvTab.name_first.toString()) + " ";
                s += kvJson.getString( KvTab.name_middle.toString()) + " ";
                s += kvJson.getString( KvTab.name_last.toString()) + " ";
                s += kvJson.getString( KvTab.name_suffix.toString()) + " ";
                s = s.trim();// remove leading or trailing spaces
                full_name = s.replaceAll("\\s+", " ");// remove extra white space
                setText(full_name);

            } catch (JSONException e) { e.printStackTrace(); }
        }

        /** <pre>
         * The name has been edited directly.
         * Parse the name and save to name elements prefix, first, middle, last, suffix:
         * 1 word: KvTab.name_first
         * 2 words: name_first, name_last
         * 3 words: name_first, name_middle, name_last
         * 4 words: prefix, name_first, name_middle, name_last
         * 5 words: prefix, name_first, name_middle, name_last, suffix
         * > 5, extra words are part of middle name
         * </pre>
         */
        protected void parseFullName() {

            try {
                String[] words = editFullName.full_name.split("\\s+");
                int count = words.length;

                switch( count ){
                    case 0:
                        kvJson.putOpt( KvTab.name_prefix.toString(), "");
                        kvJson.putOpt( KvTab.name_first.toString(), "");
                        kvJson.putOpt( KvTab.name_middle.toString(), "");
                        kvJson.putOpt( KvTab.name_last.toString(), "");
                        kvJson.putOpt( KvTab.name_suffix.toString(), "");
                        row_name_prefix.setText("");
                        row_name_first.setText("");
                        row_name_middle.setText("");
                        row_name_last.setText("");
                        row_name_suffix.setText("");
                        break;
                    case 1:
                        kvJson.putOpt( KvTab.name_prefix.toString(), "");
                        kvJson.putOpt( KvTab.name_first.toString(), words[0]);
                        kvJson.putOpt( KvTab.name_middle.toString(), "");
                        kvJson.putOpt( KvTab.name_last.toString(), "");
                        kvJson.putOpt( KvTab.name_suffix.toString(), "");
                        row_name_prefix.setText("");
                        row_name_first.setText(words[0]);
                        row_name_middle.setText("");
                        row_name_last.setText("");
                        row_name_suffix.setText("");
                        break;
                    case 2:
                        kvJson.putOpt( KvTab.name_prefix.toString(), "");
                        kvJson.putOpt( KvTab.name_first.toString(), words[0]);
                        kvJson.putOpt( KvTab.name_middle.toString(), "");
                        kvJson.putOpt( KvTab.name_last.toString(), words[1]);
                        kvJson.putOpt( KvTab.name_suffix.toString(), "");
                        row_name_prefix.setText("");
                        row_name_first.setText(words[0]);
                        row_name_middle.setText("");
                        row_name_last.setText(words[1]);
                        row_name_suffix.setText("");
                        break;
                    case 3:
                        kvJson.putOpt( KvTab.name_prefix.toString(), "");
                        kvJson.putOpt( KvTab.name_first.toString(), words[0]);
                        kvJson.putOpt( KvTab.name_middle.toString(), words[1]);
                        kvJson.putOpt( KvTab.name_last.toString(), words[2]);
                        kvJson.putOpt( KvTab.name_suffix.toString(), "");
                        row_name_prefix.setText("");
                        row_name_first.setText(words[0]);
                        row_name_middle.setText(words[1]);
                        row_name_last.setText(words[2]);
                        row_name_suffix.setText("");
                        break;
                    case 4:
                        kvJson.putOpt( KvTab.name_prefix.toString(), words[0]);
                        kvJson.putOpt( KvTab.name_first.toString(), words[1]);
                        kvJson.putOpt( KvTab.name_middle.toString(), words[2]);
                        kvJson.putOpt( KvTab.name_last.toString(), words[3]);
                        kvJson.putOpt( KvTab.name_suffix.toString(), "");
                        row_name_prefix.setText(words[0]);
                        row_name_first.setText(words[1]);
                        row_name_middle.setText(words[2]);
                        row_name_last.setText(words[3]);
                        row_name_suffix.setText("");
                        break;
                    case 5:
                        kvJson.putOpt( KvTab.name_prefix.toString(), words[0]);
                        kvJson.putOpt( KvTab.name_first.toString(), words[1]);
                        kvJson.putOpt( KvTab.name_middle.toString(), words[2]);
                        kvJson.putOpt( KvTab.name_last.toString(), words[3]);
                        kvJson.putOpt( KvTab.name_suffix.toString(), words[4]);
                        row_name_prefix.setText(words[0]);
                        row_name_first.setText(words[1]);
                        row_name_middle.setText(words[2]);
                        row_name_last.setText(words[3]);
                        row_name_suffix.setText(words[4]);
                        break;
                    default:
                        kvJson.putOpt( KvTab.name_prefix.toString(), words[0]);
                        kvJson.putOpt( KvTab.name_first.toString(), words[1]);

                        String middle = "";

                        for( int i =2; i < count-2; i++)
                            middle = middle +" "+ words[ i ];

                        kvJson.putOpt( KvTab.name_middle.toString(), middle);
                        kvJson.putOpt( KvTab.name_last.toString(), words[count-2]);
                        kvJson.putOpt( KvTab.name_suffix.toString(), words[count-1]);
                        row_name_prefix.setText(words[0]);
                        row_name_first.setText(words[1]);
                        row_name_middle.setText(middle);
                        row_name_last.setText(words[count-2]);
                        row_name_suffix.setText(words[count-1]);
                        break;
                }
            } catch (JSONException e) { e.printStackTrace(); }
        }
    }

    /**
     * Configure a row for editing.
     */
    public class EditRow {

        private TableRow m_row;
        private KvTab m_kvTab;
        private String m_hintText;
        private EditText m_nameEt;
        private TextWatcher m_textWatcher;
        private String rowContent;

        private EditRow(TableLayout table, KvTab kvTab, String hintText){

            this.m_hintText = hintText;
            this.m_kvTab = kvTab;

            m_row = new TableRow(m_act);
            m_row.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.WRAP_CONTENT));

            // Create and set the format to edit row elements
            m_nameEt = new EditText( m_act);
            if( kvTab == KvTab.note)
                m_nameEt.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            else
                m_nameEt.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            m_nameEt.setTextColor(m_h3_color);
            rowContent = "";
            try {
                rowContent = kvJson.getString( kvTab.toString());
            } catch (JSONException e) { e.printStackTrace(); }

            if( rowContent.isEmpty())
                m_nameEt.setHint( m_hintText );
            else
                m_nameEt.setText( rowContent );
            m_nameEt.setWidth( 1 );// Wrap text

            m_nameEt.setOnTouchListener(new OnTouchListener(){
                public boolean onTouch(View arg0, MotionEvent arg1) {
                    if( m_kvTab == KvTab.organization){
                        m_nameEt.setHint("Company");
                        row_title.setVisibile( true );
                    }
                    return false;
                }
            });

            m_textWatcher = new TextWatcher() {
                public void afterTextChanged(Editable s) {
                    try {

                        kvJson.put( m_kvTab.toString(), s.toString());
                        if(DEBUG)LogUtil.log( m_kvTab.toString()+" kvNameTextWatcher: "+kvJson.get(m_kvTab.toString()));
                        editFullName.mergeDisplayName();

                    } catch (JSONException e) { e.printStackTrace(); }
                }
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
                public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
            };
            m_nameEt.addTextChangedListener(m_textWatcher);
            m_row.addView( m_nameEt);
//            if( isEmpty())
                m_row.setVisibility(TableRow.GONE);
//            else
//                m_row.setVisibility(TableRow.VISIBLE);
            table.addView( m_row);
        }
        public void requestFocus() {
            if( m_nameEt != null)
                m_nameEt.requestFocus();
        }
        /**
         * Enable or disable the row text listener.  This is useful when the text
         * field is being edited by actions on another row, such as editing
         * elements of a name that can change the display name.
         * @param listener
         */
        private void enableListener(boolean listener){

            if( listener )
                m_nameEt.addTextChangedListener( m_textWatcher);
            else
                m_nameEt.removeTextChangedListener( m_textWatcher);
        }

        public boolean isShown() {

            return m_row.isShown();
        }

        public void setVisibile(boolean visible) {

            this.m_row.setVisibility( visible?TableRow.VISIBLE:TableRow.GONE);
        }

        public void setText(String text){

            enableListener( false);
            this.m_nameEt.setText(text);
            enableListener( true);
        }

        public boolean isEmpty() {
            return rowContent.isEmpty();
        }
        public boolean hasText() {
            return ! rowContent.isEmpty();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // Action bar items

        switch (item.getItemId()) {

            case R.id.menu_edit_save:{

                //At one point the save button is not displayed on an ICS sdk 14 device
                saveEdits();
                Persist.setEmptyContactId(m_act, 0);
                boolean contactModified = true;
                mCallbacks.onEditContactFinish( contactModified);
            }
            break;
            case R.id.menu_edit_discard:{
                MyContacts.manageEmptyContact(m_act);
                boolean contactModified = false;
                mCallbacks.onEditContactFinish( contactModified);
            }
            break;
            case R.id.menu_password_fields:
                toggleEditFields();
                if(m_passwordFieldsShown)
                   item.setTitle("Show contact fields");
                else
                    item.setTitle("Show password fields");
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Toggle edit fields for entering passwords or contacts
     */
    private void toggleEditFields() {

        m_passwordFieldsShown = !m_passwordFieldsShown;

        if(m_passwordFieldsShown){

            section_password.setVisible( true);// Show password fields
            section_username.setVisible( true);
            section_website.setVisible( true);

            if( section_phone.isEmpty())// Hide non password fields if empty
                section_phone.setVisible( false);
            if( section_email.isEmpty())
                section_email.setVisible( false);

        }else{

            section_phone.setVisible( true);// Show contact fields
            section_email.setVisible( true);

            if( section_password.isEmpty())// Hide non contact fields if empty
                section_password.setVisible( false);
            if( section_username.isEmpty())
                section_username.setVisible( false);
            if( section_website.isEmpty())
                section_website.setVisible( false);
        }
    }

    /**
     * Save edited JSON data to database.
     */
    private void saveEdits() {

        String last_name = "";
        //Save photo if changed
        if( m_importDecodedBitmap != null){

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m_importDecodedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            SqlCipher.put(m_contact_id, DTab.photo, encodedImage);
        }

        //Save group membership
        if( m_groupMembershipChanged)
            saveGroupMembership();

        // Get any updated KV items
        String updatedNotes = m_notesEt.getText().toString();
        String updatedUsername = m_usernameEt.getText().toString();
        String updatedPassword = m_passwordEt.getText().toString();
        try {
            last_name = kvJson.getString(KvTab.name_last.toString());
            kvJson.put( KvTab.note.toString(), updatedNotes);
            kvJson.put( KvTab.username.toString(), updatedUsername);
            kvJson.put( KvTab.password.toString(), updatedPassword);
        } catch (JSONException e) {
            LogUtil.logException(m_act, LogType.CONTACT_EDIT, e);
        }
        //Save all the detail single key-value pairs
        SqlCipher.put(m_contact_id, DTab.kv, kvJson.toString());

        // KvJson has name and organization updates, update display_name
        // Must happen after KvTab and last_name have been updated
        NameUtil.setNamesFromKv(m_contact_id);
        SqlCipher.put(m_contact_id, ATab.last_name, last_name);

        //Save all the detail collections
        cleanAndUpdate( DTab.phone, section_phone.sectionJarray);
        cleanAndUpdate( DTab.email, section_email.sectionJarray);
        cleanAndUpdate( DTab.im, section_im.sectionJarray);
        cleanAndUpdate( DTab.address, section_address.sectionJarray);
        cleanAndUpdate( DTab.website, section_website.sectionJarray);
        cleanAndUpdate( DTab.relation, section_relation.sectionJarray);
        cleanAndUpdate( DTab.date, section_date.sectionJarray);
        cleanAndUpdate( DTab.internetcall, section_internetcall.sectionJarray);
    }

    /**
     * Look for changes in group membership and issue necessary insert or delete calls
     * to the database.
     */
    private void saveGroupMembership() {

        // Get the set of pre-edit groups
        Set<Integer> groupSet = MyGroups.getGroupSet(m_contact_id);
        boolean groupChangeDetected = false;

        int i = 0;
        for( boolean inGroup : m_groupCheckstate){

            if( inGroup && ! groupSet.contains( m_groupIds[i])){

                // Detected an addition
                MyGroups.addGroupMembership( m_act, m_contact_id, m_groupIds[i], true );
                groupChangeDetected = true;
            }else
            if( ! inGroup && groupSet.contains( m_groupIds[i])){

                // Detected a deletion
                MyGroups.deleteGroupRecords( m_act, m_contact_id, m_groupIds[i], true);
                groupChangeDetected = true;
            }
            i++;
        }
        if( groupChangeDetected ) {
            int trashGroupId = MyGroups.getGroupId( m_group_account, CConst.TRASH);
            MyGroups.deleteGroupRecords(m_act, m_contact_id, trashGroupId, true);//sync is true
        }
    }

    /**
     * Remove deleted items and write section array to database
     * @param dbEnum
     * @param jArray
     */
    private void cleanAndUpdate( DTab dbEnum, JSONArray jArray){

        try {
            JSONArray cleanArray = new JSONArray();

            for( int i = 0; i < jArray.length(); i++){

                JSONObject item = jArray.getJSONObject( i );
                Iterator<?> item_keys = item.keys();
                String item_label = (String) item_keys.next();
                String item_value = item.getString(item_label);

                if( ! item_value.contentEquals(DELETE_ME_KEY))
                    cleanArray.put( item );
            }
            // Any deleted items have cleared
            SqlCipher.put(m_contact_id, dbEnum, cleanArray.toString());

        } catch (JSONException e) { e.printStackTrace(); }
    }

    public class EditSection {

        private TableLayout headerTable;   // Table for header title and rule
        private TableLayout bodyTable;     // Table for body content
        private TableLayout addNewButtonTable;
        public TableLayout lastTable;      // Last table in this section, ref for adding the next table
        private TextView addNewTv;         // Text view "Add New"
        private JSONArray sectionJarray;   // List of items to edit
        private String SPINNER_TYPES[] = {""};

        public boolean isShown(){

            return this.headerTable.isShown();
        }

        public boolean isEmpty(){

            return this.sectionJarray.length() == 0;
        }

        public void requestFocus() {

            bodyTable.requestFocus();
        }

        public void setVisible(boolean b){

            headerTable.setVisibility( b?TableLayout.VISIBLE:TableLayout.GONE);
            bodyTable.setVisibility( b?TableLayout.VISIBLE:TableLayout.GONE);
            addNewButtonTable.setVisibility( b?TableLayout.VISIBLE:TableLayout.GONE);
            addNewTv.setVisibility( b?TextView.VISIBLE:TextView.GONE);
        }
        /**
         * Build a section to edit.  Pass in the previous table to manage relative layout
         * placement. When the section is empty the table is hidden.
         * Section will be added after the previousTable.
         * @param dbEnum
         * @param previousTable
         * @return
         */
        public EditSection(final DTab dbEnum, TableLayout previousTable){

            switch( dbEnum){

                case address:
                    SPINNER_TYPES = CConst.ADDRESS_TYPES;
                    break;
                case date:
                    SPINNER_TYPES = CConst.DATE_TYPES;
                    break;
                case email:
                    SPINNER_TYPES = CConst.EMAIL_TYPES;
                    break;
                case im:
                    SPINNER_TYPES = CConst.IM_TYPES;
                    break;
                case internetcall:
                    SPINNER_TYPES = CConst.INTERNETCALL_TYPES;
                    break;
                case phone:
                    SPINNER_TYPES = CConst.PHONE_TYPES;
                    break;
                case relation:
                    SPINNER_TYPES = CConst.RELATION_TYPES;
                    break;
                case website:
                    SPINNER_TYPES = CConst.WEBSITE_TYPES;
                    break;
                default:
                    if(DEBUG)LogUtil.log("Edit: Invalid dbEnum");
            }
            try {
                String itemList = SqlCipher.get(m_contact_id, dbEnum);
                sectionJarray = new JSONArray( itemList);

                // Insert a category header and rule in a new table
                headerTable = newCategoryHeader(
                        dbEnum.toString().toUpperCase(Locale.US), previousTable);

                bodyTable = new TableLayout(m_act);
                bodyTable.setId( Util.generateViewId());
                bodyTable.setLayoutParams( tableLayoutParams );
                bodyTable.setVisibility(TableLayout.GONE);

                // Clear the display of any previous table views
                bodyTable.removeAllViews();
                bodyTable.setPadding( 15, 5, 15, 2);
                bodyTable.setColumnStretchable(0, true);
                bodyTable.setColumnShrinkable(1, true);
                bodyTable.setColumnShrinkable(2, true);

                // Fill the new table with rows
                for( int array_index = 0; array_index < sectionJarray.length(); array_index++){

                    JSONObject item = sectionJarray.getJSONObject(array_index);
                    Iterator<?> item_keys = item.keys();
                    final String item_label = (String) item_keys.next();
                    String item_value = item.getString(item_label);

                    // Construct the next row for editing
                    new EditSectionRow( dbEnum, item_label, item_value, array_index );
                }

                RelativeLayout.LayoutParams rlp1 = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

                int tid1 = headerTable.getId();
                rlp1.addRule(RelativeLayout.BELOW, tid1);

                m_relativeLayout.addView( bodyTable, rlp1);// Table added to a view

                // Add new button (text view)
                addNewButtonTable = new TableLayout(m_act);
                addNewButtonTable.setId( Util.generateViewId());
                addNewButtonTable.setLayoutParams( tableLayoutParams );
                addNewButtonTable.setVisibility(TableLayout.GONE);

                TableRow row = new TableRow(m_act);
                row.setLayoutParams( tableRowParams );

                addNewTv = new TextView( m_act);
                addNewTv.setText("Add new");
                addNewTv.setVisibility(TextView.GONE);
                addNewTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                addNewTv.setTypeface(null, Typeface.ITALIC);
                addNewTv.setTextColor(m_h3_color);
                addNewTv.setPadding(30, 1, 1, 3);
                addNewTv.setOnClickListener( new OnClickListener() {
                    public void onClick(View arg0) {
                        try {

                            // Add an empty item to the list
                            JSONObject blankObj = new JSONObject();
                            blankObj.put(SPINNER_TYPES[0], "");
                            sectionJarray.put(blankObj);

                            // Construct the new row for editing
                            new EditSectionRow( dbEnum, SPINNER_TYPES[0], "", sectionJarray.length()-1 );

                        } catch (JSONException e) { e.printStackTrace(); }

                        // Call method to engage new item

                        if(DEBUG)LogUtil.log( dbEnum.toString()+" Add new click: ");
                    }
                });

                row.addView( addNewTv );
                addNewButtonTable.addView(row);

                RelativeLayout.LayoutParams rlp2 = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

                int tid2 = bodyTable.getId();
                rlp2.addRule(RelativeLayout.BELOW, tid2);

                m_relativeLayout.addView( addNewButtonTable, rlp2);// Table added to a view

                //Keep a copy that is named to be obviously last table in the section
                lastTable = addNewButtonTable;

                if( itemList.contentEquals("[]"))
                    setVisible( false );
                else
                    setVisible( true );

            } catch (JSONException e1) { e1.printStackTrace(); }
        }

        private class EditSectionRow {

            // Index for this specific row in the JSON array
            private int mItemIndex;
            private String mItemKey;
            private String mItemValue;
            private String mSpinnerLabel;
            String[] spinList;
            final Spinner spinner;
            ArrayAdapter<String> spinnerArrayAdapter;

            private EditSectionRow( final DTab dbEnum, String pItemKey, String pItemValue, int pItemIndex ){

                this.mItemIndex = pItemIndex;
                this.mItemKey = pItemKey;
                this.mItemValue = pItemValue;

                TableRow row = new TableRow(m_act);
                row.setLayoutParams( tableRowParams );

                // Add next item value (i.e. phone number) followed by the descriptor name (home)
                EditText itemTextEt = new EditText( m_act);

                switch( dbEnum){

                    case email:
                        itemTextEt.setInputType( InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS );
                        break;
                    case phone:
                        itemTextEt.setInputType( InputType.TYPE_CLASS_PHONE );
                        break;

                    default :
                        itemTextEt.setInputType(
                                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                }
                itemTextEt.setText( mItemValue );
                itemTextEt.setWidth( 1 );
                itemTextEt.setSingleLine( false); // Multiple line addresses work
                itemTextEt.setPadding(5, 5, 0, 0);

                // Pass the array of values plus the index and key to replace changed number
                itemTextEt.addTextChangedListener( new TextWatcher( ) {
                    public void afterTextChanged(Editable e) {
                        JSONObject updatedObj = new JSONObject();
                        try {
                            LogUtil.log( dbEnum.toString()+" editRowTextWatcher.mSpinnerLabel: "+mSpinnerLabel.toString());
                            mItemValue = e.toString();
                            /**
                             * mItemKey has the first item in the spinner, not what we want.
                             * mSpinnerLabel has the current item selected.
                             */
                            updatedObj.put( mSpinnerLabel, mItemValue); // Build replacement object
                            sectionJarray.put(mItemIndex, updatedObj);          // Replace element of array
                            LogUtil.log( dbEnum.toString()+" editRowTextWatcher: "+sectionJarray.toString());

                        } catch (JSONException e1) { e1.printStackTrace(); }
                    }
                    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
                    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
                });
                row.addView( itemTextEt);
                row.setLayoutParams(new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,TableRow.LayoutParams.WRAP_CONTENT));

                spinner = new Spinner( m_act);
                String type = mItemKey.toUpperCase(Locale.US);
                spinList = Util.extendArray(SPINNER_TYPES,type);
                //selected item will look like a spinner set from XML
                spinnerArrayAdapter = new ArrayAdapter<String>(
                        m_act, android.R.layout.simple_spinner_item, spinList);
                spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(spinnerArrayAdapter);
                int nItem = spinnerArrayAdapter.getPosition( type);
                spinner.setSelection(nItem);
                spinner.setOnItemSelectedListener( new OnItemSelectedListener( ) {
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        try {
                            mSpinnerLabel = spinList[ position ];

                            if( mSpinnerLabel.contentEquals(CConst.CUSTOM)){

                                //Create a new dialog to capture a custom label
                                AlertDialog.Builder alert = new AlertDialog.Builder(m_act);
                                alert.setTitle("Custom label name");
                                final EditText input = new EditText(m_act);
                                alert.setView(input);

                                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int labelIndex) {

                                        String label = input.getText().toString();
                                        try {
                                            JSONObject updatedObj = new JSONObject();
                                            updatedObj.put( label, mItemValue);
                                            sectionJarray.put(mItemIndex, updatedObj); // Replace element of array
                                            spinList = Util.extendArray(SPINNER_TYPES,label);
                                            int length = spinList.length;
                                            spinnerArrayAdapter = new ArrayAdapter<String>(
                                                    m_act, android.R.layout.simple_spinner_item, spinList);
                                            spinner.setAdapter(spinnerArrayAdapter);
                                            spinner.setSelection( length-1);
                                            spinnerArrayAdapter.notifyDataSetChanged();

                                        } catch (JSONException e) { e.printStackTrace(); }

                                        // Build replacement object
                                        //                                        if(DEBUG)LogUtil.log( dbEnum.toString()+" custom spinner selected: "+sectionJarray.toString());
                                    }
                                });

                                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Canceled.
                                    }
                                });

                                alert.show();
                            }else{

                                JSONObject updatedObj = new JSONObject();
                                updatedObj.put( spinList[ position ], mItemValue); // Build replacement object
                                sectionJarray.put(mItemIndex, updatedObj);         // Replace element of array

                               LogUtil.log( dbEnum.toString()+" spinner selected: "+sectionJarray.toString());
                            }

                        } catch (JSONException e1) { e1.printStackTrace(); }
                    }
                    public void onNothingSelected(AdapterView<?> parentView) { }
                });

                row.addView( spinner);

                // Third column is the 'x' delete button
                ImageView deleteButton = new ImageView(m_act);
                deleteButton.setLayoutParams( new TableRow.LayoutParams( clearXSize, clearXSize));
                deleteButton.setImageResource(R.drawable.ic_clear_normal);
                deleteButton.setOnClickListener(
                        new CustomDeleteOnClickListener( itemTextEt, spinner, deleteButton) {
                            public void onClick(View v) {

                                mPhoneNum_et.setVisibility(EditText.GONE);
                                mSpinner.setVisibility(Spinner.GONE);
                                mDeleteButton.setVisibility(ImageView.GONE);

                                /* Replace deleted row with a unique identifiable string.
                                 * Even though the row will be hidden, it still occupies
                                 * an indexed spot in the array. To keep indexing consistent
                                 * it is given a unique identifiable string that can be
                                 * removed before saving to the database.
                                 */
                                JSONObject updatedObj = new JSONObject();
                                try {
                                    mItemValue = DELETE_ME_KEY;
                                    updatedObj.put( mItemKey, mItemValue); // Build replacement object
                                    sectionJarray.put(mItemIndex, updatedObj); // Replace element of array
                                    if(DEBUG)LogUtil.log( dbEnum.toString()+" deleteButtonClick: "+sectionJarray.toString());
                                } catch (JSONException e1) { e1.printStackTrace(); }

                                return;
                            }
                        });
                row.addView( deleteButton );
                bodyTable.addView(row);

            }
        }

        private class CustomDeleteOnClickListener implements OnClickListener {

            public EditText mPhoneNum_et;
            public Spinner mSpinner;
            public ImageView mDeleteButton;

            public CustomDeleteOnClickListener( EditText phoneNum_et, Spinner spinner, ImageView deleteButton) {
                mPhoneNum_et = phoneNum_et;
                mSpinner = spinner;
                mDeleteButton = deleteButton;
            }
            public void onClick(View arg0) { }
        }
    }
    public class EditUsername {

        public TableLayout lastTable;      // Last table in this section, ref for adding the next table
        private TableLayout headerTable;   // Table for header title and rule
        private TableLayout usernameTable; // Table to show the username as a button
        private String username;

        public boolean isShown(){

            return this.headerTable.isShown();
        }

        public boolean isEmpty(){

            return this.username.isEmpty();
        }

        public void requestFocus() {

            if( m_usernameEt != null)
                m_usernameEt.requestFocus();
        }

        public void setVisible(boolean b) {

            headerTable.setVisibility( b?TableLayout.VISIBLE:TableLayout.GONE);
            usernameTable.setVisibility(b ? TableLayout.VISIBLE : TableLayout.GONE);
        }

        @SuppressLint("NewApi")
        public EditUsername( TableLayout previousTable){

            // Insert a category header and rule in a new table
            String title = "USERNAME";
            headerTable = newCategoryHeader( title, previousTable);

            // Add edit text
            usernameTable = new TableLayout(m_act);
            usernameTable.setId(Util.generateViewId());
            usernameTable.setLayoutParams(tableLayoutParams);

            // Clear the display of any previous table views
            usernameTable.removeAllViews();
            usernameTable.setPadding(15, 15, 15, 15);

            // This is not what it appears, it allows the text to wrap
            usernameTable.setColumnShrinkable(0, true);

            TableRow tableRow = new TableRow(m_act);
            tableRow.setLayoutParams(tableRowParams);

            m_usernameEt = new EditText(m_act);
            this.username ="";
            try {
                this.username = kvJson.getString( KvTab.username.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            m_usernameEt.setHint(USERNAME_HINT);
            m_usernameEt.setTextColor(m_body_color);
            m_usernameEt.setSingleLine(true);
            m_usernameEt.setText(this.username);  // Username will be picked up in saveEdits();

            tableRow.addView(m_usernameEt);

            usernameTable.addView(tableRow);// Row added to a table

            RelativeLayout.LayoutParams rowLayoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

            int tid = headerTable.getId();
            rowLayoutParams.addRule(RelativeLayout.BELOW, tid);

            m_relativeLayout.addView(usernameTable, rowLayoutParams);// Table added to a view

            //Keep a copy that is named to be obviously last table in the section
            lastTable = usernameTable;

            if( this.username.isEmpty())
                setVisible( false );
            else
                setVisible( true );
        }
    }

    public class EditPassword {

        public TableLayout lastTable;      // Last table in this section, ref for adding the next table
        private TableLayout headerTable;   // Table for header title and rule
        private TableLayout passwordTable; // Table to show the username as a button
        private String password;

        public boolean isShown(){

            return this.headerTable.isShown();
        }

        public boolean isEmpty(){

            return this.password.isEmpty();
        }

        public void requestFocus() {

            if( m_passwordEt != null)
                m_passwordEt.requestFocus();
        }

        public void setVisible(boolean b) {

            headerTable.setVisibility( b?TableLayout.VISIBLE:TableLayout.GONE);
            passwordTable.setVisibility(b ? TableLayout.VISIBLE : TableLayout.GONE);
        }

        @SuppressLint("NewApi")
        public EditPassword( TableLayout previousTable){

            // Insert a category header and rule in a new table
            String title = "PASSWORD";
            headerTable = newCategoryHeader( title, previousTable);

            // Add edit text
            passwordTable = new TableLayout(m_act);
            passwordTable.setId(Util.generateViewId());
            passwordTable.setLayoutParams(tableLayoutParams);

            // Clear the display of any previous table views
            passwordTable.removeAllViews();
            passwordTable.setPadding(15, 15, 15, 15);

            // This is not what it appears, it allows the text to wrap
            passwordTable.setColumnShrinkable(0, true);

            TableRow tableRow = new TableRow(m_act);
            tableRow.setLayoutParams(tableRowParams);

            m_passwordEt = new EditText(m_act);
            this.password ="";
            try {
                this.password = kvJson.getString( KvTab.password.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            m_passwordEt.setHint(PASSWORD_HINT);
            m_passwordEt.setTextColor(m_body_color);
            m_passwordEt.setSingleLine(true);
            m_passwordEt.setText(this.password);  // Username will be picked up in saveEdits();
            tableRow.addView(m_passwordEt);

            ImageView spacer1 = new ImageView(m_act);
            spacer1.setLayoutParams(new TableRow.LayoutParams(clearXSize, clearXSize));
            tableRow.addView(spacer1);

            // Third column is the password button
            ImageView passwordButton = new ImageView(m_act);
            passwordButton.setLayoutParams(new TableRow.LayoutParams(clearXSize, clearXSize));
            passwordButton.setImageResource(R.drawable.ic_menu_login);
            passwordButton.setOnClickListener( passwordButtonOnClick);
            tableRow.addView(passwordButton);

            passwordTable.addView(tableRow);// Row added to a table

            RelativeLayout.LayoutParams rowLayoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

            int tid = headerTable.getId();
            rowLayoutParams.addRule(RelativeLayout.BELOW, tid);

            m_relativeLayout.addView(passwordTable, rowLayoutParams);// Table added to a view

            //Keep a copy that is named to be obviously last table in the section
            lastTable = passwordTable;

            if( this.password.isEmpty())
                setVisible( false );
            else
                setVisible( true );
        }
    }

    Button.OnClickListener passwordButtonOnClick = new OnClickListener() {
        @Override
        public void onClick(View v) {

            m_passwordFragment = PasswordFragment.newInstance(m_act);
            m_passwordFragment.start(passwordListener);
        }
    };

    PasswordFragment.Callbacks passwordListener = new PasswordFragment.Callbacks() {
        @Override
        public void passwordApply(String password) {

            m_passwordEt.setText( password);
            m_passwordFragment.dismiss();
        }
    };

    public class EditNotes {

        public TableLayout lastTable;      // Last table in this section, ref for adding the next table
        private TableLayout headerTable;   // Table for header title and rule
        private TableLayout notesTable;   // Table to show the list of groups as a button

        public boolean isShown(){

            return this.headerTable.isShown();
        }

        public void requestFocus() {

            if( m_notesEt != null)
                m_notesEt.requestFocus();
        }

        public void setVisible(boolean b) {

            headerTable.setVisibility( b?TableLayout.VISIBLE:TableLayout.GONE);
            notesTable.setVisibility( b?TableLayout.VISIBLE:TableLayout.GONE);
        }

        @SuppressLint("NewApi")
        public EditNotes( TableLayout previousTable){

            // Insert a category header and rule in a new table
            String title = "NOTES";
            headerTable = newCategoryHeader( title, previousTable);

            // Add edit text
            notesTable = new TableLayout(m_act);
            notesTable.setId( Util.generateViewId());
            notesTable.setLayoutParams( tableLayoutParams );

            // Clear the display of any previous table views
            notesTable.removeAllViews();
            notesTable.setPadding( 15, 15, 15, 15);

            // This is not what it appears, it allows the text to wrap
            notesTable.setColumnShrinkable(0, true);

            TableRow notesRow = new TableRow(m_act);
            notesRow.setLayoutParams( tableRowParams );

            m_notesEt = new EditText(m_act);
            String notes="";
            try {
                notes = kvJson.getString( KvTab.note.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            m_notesEt.setHint(NOTES_HINT);
            m_notesEt.setTextColor(m_body_color);
            m_notesEt.setSingleLine( false);
            m_notesEt.setText( notes);  // Notes will be picked up in saveEdits();

            notesRow.addView(m_notesEt);

            notesTable.addView(notesRow);// Row added to a table

            RelativeLayout.LayoutParams noteRowLayoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

            int tid = headerTable.getId();
            noteRowLayoutParams.addRule(RelativeLayout.BELOW, tid);

            m_relativeLayout.addView(notesTable, noteRowLayoutParams);// Table added to a view

            //Keep a copy that is named to be obviously last table in the section
            lastTable = notesTable;

            if( notes.isEmpty())
                setVisible( false );
            else
                setVisible( true );
        }
    }
    /**
     * Build the edit section for groups.  This section is always visible.
     */
    public class EditGroups {

        public TableLayout lastTable;      // Last table in this section, ref for adding the next table
        private TableLayout headerTable;   // Table for header title and rule
        private TableLayout groupButtonTable;   // Table to show the list of groups as a button
        private TextView groupsTv;

        public void setVisible(boolean b) {

            headerTable.setVisibility( b?TableLayout.VISIBLE:TableLayout.GONE);
            groupButtonTable.setVisibility( b?TableLayout.VISIBLE:TableLayout.GONE);
        }

        @SuppressLint("NewApi")
        public EditGroups( TableLayout previousTable){

            // Insert a category header and rule in a new table
            m_group_account = SqlCipher.get( m_contact_id, ATab.account_name);
            String title = "GROUPS ("+m_group_account+")";
            headerTable = newCategoryHeader( title, previousTable);

            // Add button at bottom that will show list of groups
            groupButtonTable = new TableLayout(m_act);
            groupButtonTable.setId( Util.generateViewId());
            groupButtonTable.setLayoutParams( tableLayoutParams );

            // Clear the display of any previous table views
            groupButtonTable.removeAllViews();
            groupButtonTable.setPadding( 15, 15, 15, 15);

            // This is not what it appears, it allows the text to wrap
            groupButtonTable.setColumnShrinkable(0, true);

            TableRow buttonRow = new TableRow(m_act);
            buttonRow.setLayoutParams( tableRowParams );

            groupsTv = new TextView(m_act);
            String groups = MyGroups.getGroupTitles(m_contact_id);
            if( groups.isEmpty())
                groups = SELECT_GROUPS;
            groupsTv.setTextColor(m_body_color);
            groupsTv.setTypeface(null, Typeface.ITALIC);
            groupsTv.setSingleLine( false);
            groupsTv.setText( groups);

            groupsTv.setOnClickListener(new OnClickListener(){
                public void onClick(View arg0) {
                    editGroupsDialog(m_act, m_group_account);
                }
            });
            buttonRow.addView(groupsTv);

            groupButtonTable.addView(buttonRow);// Row added to a table

            RelativeLayout.LayoutParams buttonRowLayoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

            int tid = headerTable.getId();
            buttonRowLayoutParams.addRule(RelativeLayout.BELOW, tid);

            m_relativeLayout.addView(groupButtonTable, buttonRowLayoutParams);// Table added to a view

            //Keep a copy that is named to be obviously last table in the section
            lastTable = groupButtonTable;
        }

        protected void editGroupsDialog(Activity act, String account) {

            // Only setup group membership one time unless user sets cancel
            // Persist to allow user to edit multiple times without resetting the data
            if( ! m_groupMembershipChanged ){

                m_groupNames =      MyGroups.getGroupTitlesExceptTrash(account);
                m_groupCheckstate = MyGroups.getGroupCheckStateExceptTrash(account, m_contact_id);

                // Keep for saving IDs back to database on done
                m_groupIds = MyGroups.getGroupIds( account);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder( act);
            builder.setMultiChoiceItems( m_groupNames, m_groupCheckstate, new DialogInterface.OnMultiChoiceClickListener() {

                public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                    m_groupCheckstate[ which ] = isChecked;
                    m_groupMembershipChanged = true;
                }
            })
                    .setPositiveButton("New group", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            newGroupDialog();
                        }
                    })
                    .setNeutralButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            resetGroupButtonText();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            m_groupMembershipChanged = false;
                        }
                    })
                    .setOnCancelListener(new Dialog.OnCancelListener() {

                        @Override
                        public void onCancel(DialogInterface arg0) {

                            resetGroupButtonText();
                        }

                    });

            m_groupsEditAlertDialog = builder.create();
            m_groupsEditAlertDialog.show();
        }

        private void resetGroupButtonText() {

            String groups = "";
            String commaSpace = "";
            int i=0;
            for( boolean checked : m_groupCheckstate){

                if( checked){
                    groups += commaSpace + MyGroups.mGroupTitle.get( m_groupIds[ i ] );
                    commaSpace = ", ";
                }
                i++;
            }
            if( groups.isEmpty())
                groups = SELECT_GROUPS;
            groupsTv.setText( groups);
        }

        protected void newGroupDialog() {

            AlertDialog.Builder alert = new AlertDialog.Builder(m_act);
            alert.setTitle("Create new group");
            final EditText input = new EditText(m_act);
            alert.setView(input);

            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int labelIndex) {

                    String newGroupName = Safe.safeString( input.getText().toString());
                    MyGroups.addGroup( m_act,
                            newGroupName, m_group_account, CConst.GROUP_ACCOUNT_TYPE);

                    // Force reload on data structures
                    //FUTURE down-side is if the user made any edits, they will be lost, fix this
                    m_groupMembershipChanged = false;
                    editGroupsDialog( m_act, m_group_account);
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        }
    }

    /**
     * Build a new table layout and populate two rows: title and rule.
     * Add new table below previous table.
     * @param title
     * @param previousTable
     */
    private TableLayout newCategoryHeader( String title, TableLayout previousTable){

        TableLayout table = new TableLayout(m_act);
        table.setId(Util.generateViewId());
        table.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
        table.setColumnStretchable(0, true);
        table.setVisibility(TableLayout.GONE);

        // Clear the display of any previous table views
        table.removeAllViews();
        table.setPadding(15, 5, 15, 0);

        TableRow row = new TableRow(m_act);
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));

        TextView tv = new TextView( m_act);// cat name
        tv.setText(title);
        tv.setTextColor(m_h2_color);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setWidth(1);
        tv.setPadding(5, sectionPadding, 0, 5);
        row.addView( tv);
        table.addView(row);

        row = new TableRow(m_act);
        row.setLayoutParams( new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));

        // Horizontal rule using one column
        View v = new View( m_act);
        v.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor( m_rule_color);
        row.addView( v );
        table.addView(row);

        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        int tid = previousTable.getId();
        rlp.addRule(RelativeLayout.BELOW, tid);

        m_relativeLayout.addView( table, rlp);// Table added to a view

        return table;
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
        public void onEditContactFinish(boolean contactModified);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onEditContactFinish(boolean contactModified) {
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

    /**
     * User has selected a photo.  Save it into the image drawable.
     * Access same drawable when user saves contact data.
     * @param path
     */
    public void readPhoto(String path) {
        if(DEBUG)LogUtil.log("path: "+path);

        if( path == null)
            Toast.makeText(m_act, "Photo error",Toast.LENGTH_SHORT).show();

        m_importDecodedBitmap = BitmapFactory.decodeFile( path );
        int height = m_importDecodedBitmap.getHeight();
        int width = m_importDecodedBitmap.getWidth();
        if(DEBUG)LogUtil.log("profile size WxH: "+width+", "+height);

        if( height > CConst.PHOTO_ICON_MAX_EDGE || width > CConst.PHOTO_ICON_MAX_EDGE){

            int newWidth = -1;
            int newHeight = -1;
            float multFactor = -1.0F;
            if( height > width) {
                newHeight = CConst.PHOTO_ICON_MAX_EDGE;
                multFactor = (float) width/(float) height;
                newWidth = (int) (newHeight*multFactor);
            } else if(width > height) {
                newWidth = CConst.PHOTO_ICON_MAX_EDGE;
                multFactor = (float) height/ (float)width;
                newHeight = (int) (newWidth*multFactor);
            } else if(height == width) {
                newHeight = CConst.PHOTO_ICON_MAX_EDGE;
                newWidth = CConst.PHOTO_ICON_MAX_EDGE;
            }
            Bitmap bm = Bitmap.createScaledBitmap(m_importDecodedBitmap, newWidth, newHeight, false);
            height = bm.getHeight();
            width = bm.getWidth();
            m_importDecodedBitmap = bm;

            //FUTURE use a popup to show the user the photo
            //FUTURE allow the user to crop a photo
        }
        if( height > 0 && width > 0)
            m_photoMenu.setImageBitmap(m_importDecodedBitmap);
    }
}
