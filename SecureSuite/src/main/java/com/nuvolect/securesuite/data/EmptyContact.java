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
