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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.util.AppTheme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class EditGroupListAdapter extends ArrayAdapter <EditGroupMember>{

    private final LayoutInflater m_inflater;
    private final int m_layout;
    private int m_h1_color;
    static ArrayList<EditGroupMember> m_members;
    static Set<Long> m_deleteMemberSet;

    public EditGroupListAdapter(Context ctx, int flags, int layout) {
        super( ctx, flags);

        this.m_layout = layout;
        this.m_inflater=LayoutInflater.from(ctx);
        m_h1_color = AppTheme.getThemeColor(ctx, R.attr.h1_color);
        m_members = new ArrayList<EditGroupMember>();
        m_deleteMemberSet = new HashSet<Long>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        MemberHolder holder = null;

        if( row == null ){
            row = m_inflater.inflate(m_layout, parent, false);

            holder = new MemberHolder();
            holder.displayNameTv= (TextView)row.findViewById(R.id.displayNameTv);
            holder.thumbnailIv = (ImageView)row.findViewById(R.id.thumbnailIv);
            holder.deleteIv = (ImageView)row.findViewById(R.id.deleteIv);

            row.setTag(holder);
        }
        else{
            holder = (MemberHolder)row.getTag();
        }

        EditGroupMember member = m_members.get( position);

        holder.displayNameTv.setText( member.display_name);
        holder.displayNameTv.setTextColor( m_h1_color);
        holder.thumbnailIv.setImageBitmap( member.thumbnail);
        holder.deleteIv.setTag( member.contact_id);
        holder.deleteIv.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View view) {

                long contact_id = (Long) view.getTag();
                removeMember( contact_id);
                notifyDataSetChanged();
            }});

        return row;
    }

    static class MemberHolder {

        public ImageView deleteIv;
        long contact_id;
        TextView displayNameTv;
        ImageView thumbnailIv;
    }

    @Override
    public int getCount() {

        int size = m_members.size();

        return size;
    }

    public static void addMember( long contact_id, String display_name, Bitmap thumbnail){


        for( EditGroupMember egm : m_members){

            if( egm.contact_id == contact_id) // look for duplicates
                return;
        }

        m_members.add(new EditGroupMember( contact_id, display_name, thumbnail));

        /*
         * Look for delete/add condition, i.e, remove from delete list if user changed mind
         */
        if( m_deleteMemberSet.contains(contact_id))
            m_deleteMemberSet.remove(contact_id);
    }

    /**
     * Create a new member object and add it to the arraylist.
     * @param ctx
     * @param contact_id
     */
    public static void addMember( Context ctx, long contact_id){

        for( EditGroupMember egm : m_members){

            if( egm.contact_id == contact_id) // look for duplicates
                return;
        }

        String display_name = SqlCipher.get(contact_id, DTab.display_name);

        Bitmap thumbnail = null;
        String encodedImage = SqlCipher.get( contact_id, DTab.photo);

        if( encodedImage.isEmpty()){

            Drawable drawable;

            if( SqlCipher.get(contact_id, SqlCipher.ATab.last_name).isEmpty())
                drawable = ctx.getResources().getDrawable(R.drawable.ic_social_location_city);
            else
                drawable = ctx.getResources().getDrawable(R.drawable.ic_social_person);

            thumbnail = ((BitmapDrawable)drawable).getBitmap();
        }
        else{
            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
            thumbnail = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        }
        m_members.add(new EditGroupMember( contact_id, display_name, thumbnail));
        /*
         * Make sure an added use is not also on the delete list, i.e,
         * remove from delete list if user changed mind
         */
        if( m_deleteMemberSet.contains(contact_id))
            m_deleteMemberSet.remove(contact_id);
    }

    public static void removeMember( long contact_id ){

        // Keep track of deletes to be executed when user saves group
        m_deleteMemberSet.add( contact_id);

        ArrayList<EditGroupMember> tmp = new ArrayList<EditGroupMember>();

        for( EditGroupMember egm : m_members){  // new API can do this in 1 line

            if( egm.contact_id != contact_id)
                tmp.add(egm);
        }
        m_members = tmp;
        tmp = null; // release to gc
    }

    /**
     * Commit the group by adding or deleting contact records.
     * @param group_id
     */
    public static void commitGroupMembershipUpdates(Context ctx, int group_id) {

        Set<Long> startingContactSet = MyGroups.getContactSet(group_id);

        /*
         * Add member records while avoiding duplicates
         */
        for( EditGroupMember egm : m_members){

            if( !startingContactSet.contains(egm.contact_id))
                MyGroups.addGroupMembership( ctx, egm.contact_id, group_id, true);
        }

        /*
         * Commit group member deletes
         */
        Iterator<Long> iterator = m_deleteMemberSet.iterator();

        while( iterator.hasNext()){

            long contact_id = iterator.next();
            MyGroups.deleteGroupRecords( ctx, contact_id, group_id, true);
        }
    }
}
