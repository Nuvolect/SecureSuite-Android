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

package com.nuvolect.securesuite.main;//

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * This adapter backs the search dialog.
 */
public class SearchViewAdapter extends CursorAdapter {

    //    private static final boolean DEBUG = true;
    private final LayoutInflater m_inflater;
    private int m_itemLayout;
    private String m_search;
    private int[] m_cursor_index = new int[4];
    int m_contact_id=-1;
    int iconSizeDp = 64;
    int extraPaddingDp = 0;
    int extraPadding;
    int m_iconSize;
    private int m_imageSize;
    private int m_display_name_index;
    private int m_contact_id_index;
    private String[] kv_keys;
    private int m_kv_index;

    public SearchViewAdapter(Context ctx, Cursor c, int flags, int itemLayout, String search) {
        super(ctx, c, flags);

        //        if(DEBUG) Util.dumpCursorDescription( c, "GroupDetailCursorAdapter");
        this.m_inflater = LayoutInflater.from(ctx);
        this.m_itemLayout = itemLayout;
        this.m_search = search.toLowerCase(Locale.US);

        // This will save 13-21% in performance to capture these indexes one time
        // http://stackoverflow.com/questions/9114086/optimizing-access-to-cursors-in-android-position-vs-column-names
        m_display_name_index = c.getColumnIndex(SqlCipher.DTab.display_name.toString());
        m_contact_id_index = c.getColumnIndex(SqlCipher.DTab.contact_id.toString());

        m_cursor_index[0]  = c.getColumnIndex(SqlCipher.DTab.email.toString());
        m_cursor_index[1] = c.getColumnIndex(SqlCipher.DTab.phone.toString());
        m_cursor_index[2] = c.getColumnIndex(SqlCipher.DTab.address.toString());
        m_cursor_index[3] = c.getColumnIndex(SqlCipher.DTab.website.toString());
        m_kv_index = c.getColumnIndex(SqlCipher.DTab.kv.toString());

        kv_keys = new String[]{    //Keys in an optimal order for searching KV match
                SqlCipher.KvTab.note.toString(),
                SqlCipher.KvTab.organization.toString(),
                SqlCipher.KvTab.title.toString(),
                SqlCipher.KvTab.username.toString(),
                SqlCipher.KvTab.password.toString(),
                SqlCipher.KvTab.nickname.toString(),
        };

        m_iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                iconSizeDp, ctx.getResources().getDisplayMetrics());

        extraPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                extraPaddingDp, ctx.getResources().getDisplayMetrics());

        int screenWidth = (int) Util.screenWidth(ctx);
        int imagesFitMinimumSize = (screenWidth - extraPadding) / m_iconSize;
        this.m_imageSize = (screenWidth - extraPadding) / imagesFitMinimumSize;
    }

    @Override
    public void bindView(View view, Context ctx, Cursor cursor) {

        View contactTile = view;

        if( contactTile == null)
            contactTile = m_inflater.inflate(m_itemLayout, null, false);

        m_contact_id = cursor.getInt(m_contact_id_index);
        TextView displayNameTv = (TextView) contactTile.findViewById(R.id.displayNameTv);
        String display_name = cursor.getString( m_display_name_index );
        if( display_name.isEmpty())
            display_name = "(No name)";
        displayNameTv.setText(display_name);

        if( display_name.contains("Thomas"))
            display_name = "Thomas Knight";

        /**
         * Iterate and discover what matched, show it on the info line.
         * If the display_name matched, also show something interesting.
         */
        TextView contactInfoTv = (TextView) contactTile.findViewById(R.id.contactInfoTv);
        String contactInfo = "";

        if( display_name.toLowerCase().contains(m_search)) {  // Done, add email or phone, or leave blank

            if (contactInfo.isEmpty())
                contactInfo = SqlCipher.getFirstItem(m_contact_id, SqlCipher.DTab.email);

            if( contactInfo.isEmpty())
                contactInfo = SqlCipher.getFirstItem(m_contact_id, SqlCipher.DTab.phone);
        }else{

            /**
             * Look for a match in email, phone and address
             */
            try {
                for(int c_index : m_cursor_index){

                    String jsonArrayString = cursor.getString( c_index);

                    if( jsonArrayString.toLowerCase().contains(m_search)){

                        JSONArray jsonArray = new JSONArray( jsonArrayString);

                        for( int i = 0; i < jsonArray.length(); i++){

                            JSONObject jsonObject = jsonArray.getJSONObject( i );
                            String key = jsonObject.keys().next();// only one object per array element
                            String item = jsonObject.getString( key );

                            if( item.toLowerCase().contains( m_search)){

                                contactInfo = item;
                                break;
                            }else {
                                if (key.toLowerCase().contains(m_search)) {

                                    contactInfo = key + ": " + item;
                                    break;
                                }
                            }
                        }
                        if( ! contactInfo.isEmpty())
                            break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if( contactInfo.isEmpty()){

                String kv = cursor.getString(m_kv_index);

                if( kv.toLowerCase().contains(m_search)){

                    try {
                        JSONObject jsonObject = new JSONObject( kv );
                        String value="";

                        for( String key : kv_keys){

                            value = jsonObject.getString( key );
                            if( value.toLowerCase().contains(m_search))
                                break;
                        }
                        contactInfo = value;

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        /**
         * Display some relevant info below the display name, if any.
         * If the contact only has a display name, and that name is a match,
         * the contactInfo will be empty.
         */
        contactInfoTv.setText(contactInfo);

        String encodedImage = SqlCipher.get(m_contact_id, SqlCipher.DTab.photo);

        ImageView iv = (ImageView) contactTile.findViewById(R.id.tileImageIv);
        android.view.ViewGroup.LayoutParams layoutParams = iv.getLayoutParams();
        layoutParams.width = m_imageSize;
        layoutParams.height = m_imageSize;
        iv.setLayoutParams(layoutParams);

        if( encodedImage.isEmpty()){

            if( SqlCipher.get(m_contact_id, SqlCipher.ATab.last_name).isEmpty())
                iv.setImageDrawable( ctx.getResources().getDrawable(R.drawable.ic_social_location_city));
            else
                iv.setImageDrawable( ctx.getResources().getDrawable(R.drawable.ic_social_person));
        }
        else{
            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            iv.setImageBitmap(decodedBitmap);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        View contactTile = m_inflater.inflate(m_itemLayout, null);

        return contactTile;
    }

    public void setSearch(String search) {

        this.m_search = search.toLowerCase(Locale.US);
    }
}
