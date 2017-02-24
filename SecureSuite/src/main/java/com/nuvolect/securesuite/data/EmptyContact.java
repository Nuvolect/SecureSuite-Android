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

import org.json.JSONException;
import org.json.JSONObject;

import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.data.SqlCipher.DTab;
import com.nuvolect.securesuite.data.SqlCipher.KvTab;

public class EmptyContact {

    public static JSONObject get() {

        JSONObject emptyContact = new JSONObject();
        try {

            emptyContact.put(DTab.display_name.toString(), "");
            emptyContact.put(DTab.photo.toString(), "");

            JSONObject kv = new JSONObject();
            
            for( KvTab kvObj : KvTab.values())
                kv.put(kvObj.toString(), "");
                
            emptyContact.put(CConst.KV, kv);

            emptyContact.put( DTab.address.toString(),      "[]");// Empty JSONArray
            emptyContact.put( DTab.date.toString(),        "[]");
            emptyContact.put( DTab.email.toString(),        "[]");
            emptyContact.put( DTab.im.toString(),           "[]");
            emptyContact.put( DTab.phone.toString(),        "[]");
            emptyContact.put( DTab.relation.toString(),     "[]");
            emptyContact.put( DTab.internetcall.toString(), "[]");
            emptyContact.put( DTab.website.toString(),      "[]");

        } catch (JSONException e) { e.printStackTrace(); }

        return emptyContact;
    }
}
