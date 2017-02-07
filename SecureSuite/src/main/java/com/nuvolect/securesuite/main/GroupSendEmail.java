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

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.LogUtil.LogType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class GroupSendEmail {

    public static void emailGroup(Context ctx) {

        // Get all the contacts in the group
        // Iterate through all the contacts and build email intent
        int group_id = Cryp.getCurrentGroup();
        long[] contacts = MyGroups.getContacts(group_id);
        ArrayList<String> emailList = new ArrayList<String>();

        for(long c_id : contacts){  // next contact

            try {
                JSONObject commsSelect = new JSONObject( SqlCipher.get( c_id, DTab.comms_select));
                Iterator<?> keysIterator = commsSelect.keys();

                while( keysIterator.hasNext()){  // walk through comms selections

                    String key = (String)keysIterator.next();
                    String value = commsSelect.getString(key);
                    if( value.contains( CConst.COMMS_SELECT_EMAIL))
                        emailList.add( key );
                }
            } catch (JSONException e) {
                LogUtil.logException(ctx, LogType.GROUP_COMMS, e);
            }
        }
        if( emailList.isEmpty()){

            Toast.makeText(ctx, "No emails selected in group", Toast.LENGTH_SHORT).show();
            Toast.makeText(ctx, "Select with checkmark by email address", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] emails = emailList.toArray( new String[ emailList.size()]);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, emails);
        intent.putExtra(Intent.EXTRA_SUBJECT, "");    // Email subject title
        intent.putExtra(Intent.EXTRA_TEXT, "");       // Body text

        ctx.startActivity(Intent.createChooser(intent, "Send email..."));
    }

}
