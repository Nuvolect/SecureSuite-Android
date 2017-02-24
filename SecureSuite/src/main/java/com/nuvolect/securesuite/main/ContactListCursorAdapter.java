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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.util.AppTheme;

public class ContactListCursorAdapter extends CursorAdapter {

    private final LayoutInflater m_inflater;
    private final int m_layout;
    protected ListView mListView;
    private int m_h1_color;

    public ContactListCursorAdapter(Context ctx, Cursor c, int flags, int layout) {
        super(ctx, c, flags);

        this.m_layout = layout;
        this.m_inflater=LayoutInflater.from(ctx);
        m_h1_color = AppTheme.getThemeColor(ctx, R.attr.h1_color);
    }

    @Override
    public void bindView(View view, Context ctx, Cursor cursor) {

        TextView name = (TextView) view.findViewById(R.id.contact_name);
        name.setText(cursor.getString(cursor.getColumnIndex(DTab.display_name.toString())));
        name.setTextColor( m_h1_color);
    }

    @Override
    public View newView(Context m_act, Cursor cursor, ViewGroup parent) {

        return m_inflater.inflate( m_layout, null);
    }
}
