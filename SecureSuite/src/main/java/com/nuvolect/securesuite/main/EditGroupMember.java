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
