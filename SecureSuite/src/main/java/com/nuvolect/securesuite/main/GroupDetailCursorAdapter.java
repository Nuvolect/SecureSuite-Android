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
import com.nuvolect.securesuite.data.SqlCipher.ATab;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.util.Util;

public class GroupDetailCursorAdapter extends CursorAdapter {

    //    private static final boolean DEBUG = true;
    private final LayoutInflater m_inflater;
    private int m_layout;
    private int m_imageSize;
    private int m_contact_id_index;
    private int m_display_name_index;
    private int m_last_name_index;

    int iconSizeDp = 120;
    int extraPaddingDp = 0;
    int extraPadding;
    int m_iconSize;

    public GroupDetailCursorAdapter(Context ctx, Cursor c, int flags, int layout) {
        super(ctx, c, flags);

        //        if(DEBUG) Util.dumpCursorDescription( c, "GroupDetailCursorAdapter");

        this.m_inflater=LayoutInflater.from(ctx);
        this.m_layout = layout;

        m_contact_id_index   = c.getColumnIndex( ATab.contact_id.toString());
        m_display_name_index = c.getColumnIndex( ATab.display_name.toString());
        m_last_name_index    = c.getColumnIndex( ATab.last_name.toString());

        m_iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                iconSizeDp, ctx.getResources().getDisplayMetrics());

        extraPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                extraPaddingDp, ctx.getResources().getDisplayMetrics());

        int screenWidth =  (int)Util.screenWidth(ctx);
        int imagesFitMinimumSize = (screenWidth - extraPadding) / m_iconSize;
        this.m_imageSize = (screenWidth - extraPadding) / imagesFitMinimumSize;
    }

    @Override
    public void bindView(View view, Context ctx, Cursor cursor) {

        View contactTile = view;

        if( contactTile == null){

            contactTile = m_inflater.inflate( m_layout, null);
        }
        TextView tv = (TextView) contactTile.findViewById(R.id.tile_name);
        String display_name = cursor.getString(m_display_name_index);
        tv.setText(display_name);

        long contact_id = cursor.getLong( m_contact_id_index );
        String encodedImage = SqlCipher.get(contact_id, DTab.photo);

        ImageView iv = (ImageView) contactTile.findViewById(R.id.tile_image);
        android.view.ViewGroup.LayoutParams layoutParams = iv.getLayoutParams();
        layoutParams.width = m_imageSize;
        layoutParams.height = m_imageSize;
        iv.setLayoutParams(layoutParams);

        if( encodedImage.isEmpty()){

            // Display a person or building icon based on last name field
            if( cursor.getString(m_last_name_index).isEmpty())
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
    public View newView(Context ctx, Cursor cursor, ViewGroup parent) {

        View contactTile = m_inflater.inflate( m_layout, null);

        return contactTile;
    }
}
