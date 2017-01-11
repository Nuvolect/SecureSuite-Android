package com.nuvolect.securesuite.main;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.util.AppTheme;

public class EditGroupSearchAdapter extends CursorAdapter {

    private final LayoutInflater m_inflater;
    private final int m_layout;
    private int m_h1_color;
    private int m_h2_color;
    private Callbacks m_listener;

    public interface Callbacks {

        public void contactSelected(long contact_id, View v);
    }

    public EditGroupSearchAdapter(Context ctx, Cursor c, int flags, int layout, Callbacks listener) {
        super( ctx, c, flags);

        this.m_layout = layout;
        this.m_listener = listener;
        this.m_inflater=LayoutInflater.from(ctx);
        m_h1_color = AppTheme.getThemeColor(ctx, R.attr.h1_color);
        m_h2_color = AppTheme.getThemeColor(ctx, R.attr.h2_color);
    }

    @Override
    public void bindView(View view, Context ctx, Cursor c) {

        TextView display_name = (TextView) view.findViewById(R.id.displayNameTv);
        display_name.setText(c.getString(c.getColumnIndex(DTab.display_name.toString())));
        display_name.setTextColor( m_h1_color);

        long contact_id = c.getLong(c.getColumnIndex(DTab.contact_id.toString()));

        TextView contactInfoTv = (TextView) view.findViewById(R.id.contactInfoTv);
        String info = SqlCipher.getContactSecondaryInfo( contact_id);

        if( info.isEmpty())
            contactInfoTv.setVisibility(TextView.GONE);
        else{
            contactInfoTv.setVisibility(TextView.VISIBLE);
            contactInfoTv.setText( info );
            contactInfoTv.setTextColor( m_h2_color);
        }

        ImageView iv = (ImageView) view.findViewById(R.id.thumbnailIv);
        String encodedImage = SqlCipher.get( contact_id, DTab.photo);

        if( encodedImage.isEmpty())

            if( SqlCipher.get(contact_id, SqlCipher.ATab.last_name).isEmpty())
                iv.setImageDrawable(
                        ctx.getResources().getDrawable(R.drawable.ic_social_location_city));
            else
                iv.setImageDrawable(
                        ctx.getResources().getDrawable(R.drawable.ic_social_person));
        else{
            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            iv.setImageBitmap(decodedBitmap);
        }

        RelativeLayout searchItemRl = (RelativeLayout) view.findViewById(R.id.searchItemRl);
        searchItemRl.setTag(contact_id+"");

        searchItemRl.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v) {

                String tag = v.getTag().toString();
                long contact_id = Long.parseLong(tag);
                m_listener.contactSelected(contact_id, v);
            }});
    }

    @Override
    public int getCount() {

        int size = getCursor().getCount();

        return size ;//>= 4 ? 4 : size;
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return m_inflater.inflate( m_layout, null);
    }
}
