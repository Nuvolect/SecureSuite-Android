package com.nuvolect.securesuite.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.LogUtil.LogType;
import com.nuvolect.securesuite.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class GroupSendSms extends Activity {


    /**
     * Give the current group in Persist, launch SMS text with the selected mobile numbers.
     * @param ctx
     */
    public static void startGroupSms(Context ctx){

        // Get all the contacts in the group
        // Iterate through all the contacts and build sms list
        int group_id = Cryp.getCurrentGroup();
        long[] contacts = MyGroups.getContacts(group_id);

        // Build a string with all of the numbers then start an SMS activity
        StringBuilder uri = new StringBuilder("sms:");
        String comma = "";

        for(long c_id : contacts){  // next contact

            try {
                JSONObject commsSelect = new JSONObject( SqlCipher.get( c_id, DTab.comms_select));
                Iterator<?> keysIterator = commsSelect.keys();

                /**
                 * Note that the key is the phone number, i.e., it is always unique.
                 * The value is the number attribute, home, work, custom, etc.
                 * A user may have multiple phone numbers with the same attribute but
                 * all the numbers are unique.
                 */

                while( keysIterator.hasNext()){  // walk through comms selections

                    String key = (String)keysIterator.next();
                    String value = commsSelect.getString(key);
                    if( value.contains( CConst.COMMS_SELECT_MOBILE)){

                        uri.append(comma);
                        uri.append(key);

                        // After first number, precede each number with a comma
                        comma = ", ";
                    }
                }
            } catch (JSONException e) {
                LogUtil.logException(ctx, LogType.GROUP_COMMS, e);
            }
        }
        boolean haveAtLeastOne = ! comma.isEmpty();

        if( haveAtLeastOne){

            Intent smsIntent = new Intent(Intent.ACTION_VIEW);
            if( Util.isIntentAvailable(ctx, smsIntent)){

                smsIntent.setType("vnd.android-dir/mms-sms");
                smsIntent.setData(Uri.parse(uri.toString()));
//            smsIntent.putExtra("sms_body", "Body of Message");
                ctx.startActivity(smsIntent);
            }else{
                Toast.makeText(ctx, "SMS Text app not found", Toast.LENGTH_SHORT).show();
            }
        }else{

            Toast.makeText(ctx, "No contact mobile numbers selected", Toast.LENGTH_SHORT).show();
            Toast.makeText(ctx, "Select mobile numbers with green checkmarks", Toast.LENGTH_SHORT).show();
        }
    }
}
