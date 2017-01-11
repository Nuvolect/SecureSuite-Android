package com.nuvolect.securesuite.main;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class EditGroupMember implements Parcelable{

    public long contact_id;
    public Bitmap thumbnail;
    public String display_name;

    public EditGroupMember(){
        super();
    }

    public EditGroupMember( long contact_id, String display_name, Bitmap thumbnail2){

        super();
        this.contact_id = contact_id;
        this.thumbnail = thumbnail2;
        this.display_name = display_name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {

        out.writeLong(contact_id);
        out.writeString(display_name);
        thumbnail.writeToParcel(out, 0);
    }
}
