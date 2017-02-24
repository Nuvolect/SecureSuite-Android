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

package com.nuvolect.securesuite.data;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.provider.ContactsContract;

public class ImportUtil {

    /**
     * Get the RawContacts data summary for each account
     * @return
     */
    public static Bundle generateCloudSummary(Context ctx){

        Bundle bundle = new Bundle();

        String selectionA = ContactsContract.RawContacts.DELETED + " != 1";
        String selectionB = ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + " <> ''";
        String selection = DatabaseUtils.concatenateWhere( selectionA, selectionB);

        HashMap<String, Integer> rawContactsMap = new HashMap<String, Integer>();

        Cursor c = ctx.getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts.ACCOUNT_NAME,
                        ContactsContract.RawContacts.CONTACT_ID, },
                        selection, null, null );

        //Tally all of the raw contacts by account

        while( c.moveToNext()){

            String account = c.getString( 0 );
            // Build contact data sums for each account
            if( rawContactsMap.containsKey(account)){

                Integer rawContactTotal = rawContactsMap.get(account);
                rawContactsMap.put(account, ++rawContactTotal);

            }else
                rawContactsMap.put(account, 1);
        }
        c.close();

        final CharSequence[] accountList = new CharSequence[ rawContactsMap.size()];
        final int[] countList = new int[ rawContactsMap.size()];
        int i=0;

        for( Map.Entry<String, Integer> anAccount : rawContactsMap.entrySet()){

            String currentAccount = anAccount.getKey().toLowerCase(Locale.US);
            int rawContactsThisAccount = anAccount.getValue();

            accountList[ i ] = currentAccount+"\n"+rawContactsThisAccount+" contacts";
            countList[ i++] = rawContactsThisAccount;
        }

        bundle.putCharSequenceArray("accountList", accountList);
        bundle.putIntArray("countList", countList);

        return bundle;
    }
}
