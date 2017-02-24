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

package com.nuvolect.securesuite.data;//

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

import static com.nuvolect.securesuite.data.SqlCipher.*;

/**
 * Name utility methods.
 */
public class NameUtil {


    /**
     * Iterate the database and set the Atab.display_name and Dtab.display name
     * to default values:
     *   first_name + space + last_name + comma space + organization
     *   If it is still blank try adding an email or phone number
     */
    public static void setAllDefaultDisplayNames(){


        HashSet<Long> contactIds = getContactIds();

        for( long contact_id : contactIds){

            setNamesFromKv(contact_id);
        }
    }

    /**
     * Set the default ATab.display_name & DTab.display_name for a specific contact.
     *   first_name + space + last_name + comma space + organization
     * @param contact_id
     */
    public static void setNamesFromKv(long contact_id){


        try {
            JSONObject kvJson = new JSONObject(get(contact_id, DTab.kv));

            String last_name = kvJson.getString(KvTab.name_last.toString());
            String first_name = kvJson.getString(KvTab.name_first.toString());

            String display_name = (first_name + " " + last_name).trim();

            if( display_name.isEmpty()){

                display_name = kvJson.getString(KvTab.organization.toString());
            }
            put(contact_id, ATab.last_name, last_name);
            put(contact_id, ATab.display_name, display_name);
            put(contact_id, DTab.display_name, display_name);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /** <pre>
     * Parse the name and save to name elements prefix, first, middle, last, suffix:
     * 1 word: KvTab.name_first
     * 2 words: name_first, name_last
     * 3 words: name_first, name_middle, name_last
     * 4 words: prefix, name_first, name_middle, name_last
     * 5 words: prefix, name_first, name_middle, name_last, suffix
     * > 5, extra words are part of middle name
     * Update display_name
     * </pre>
     */
    public static void parseFullNameToKv(Long contact_id, String full_name) {

        try {
            JSONObject kvJson = new JSONObject(get(contact_id, DTab.kv));
            String[] words = full_name.split("\\s+");
            int count = words.length;

            switch( count ){
                case 0:
                    kvJson.putOpt(KvTab.name_prefix.toString(), "");
                    kvJson.putOpt(KvTab.name_first.toString(), "");
                    kvJson.putOpt(KvTab.name_middle.toString(), "");
                    kvJson.putOpt(KvTab.name_last.toString(), "");
                    kvJson.putOpt(KvTab.name_suffix.toString(), "");
                    break;
                case 1:
                    kvJson.putOpt(KvTab.name_prefix.toString(), "");
                    kvJson.putOpt(KvTab.name_first.toString(), words[0]);
                    kvJson.putOpt(KvTab.name_middle.toString(), "");
                    kvJson.putOpt(KvTab.name_last.toString(), "");
                    kvJson.putOpt(KvTab.name_suffix.toString(), "");
                    break;
                case 2:
                    kvJson.putOpt(KvTab.name_prefix.toString(), "");
                    kvJson.putOpt(KvTab.name_first.toString(), words[0]);
                    kvJson.putOpt(KvTab.name_middle.toString(), "");
                    kvJson.putOpt(KvTab.name_last.toString(), words[1]);
                    kvJson.putOpt(KvTab.name_suffix.toString(), "");
                    break;
                case 3:
                    kvJson.putOpt(KvTab.name_prefix.toString(), "");
                    kvJson.putOpt(KvTab.name_first.toString(), words[0]);
                    kvJson.putOpt(KvTab.name_middle.toString(), words[1]);
                    kvJson.putOpt(KvTab.name_last.toString(), words[2]);
                    kvJson.putOpt(KvTab.name_suffix.toString(), "");
                    break;
                case 4:
                    kvJson.putOpt(KvTab.name_prefix.toString(), words[0]);
                    kvJson.putOpt(KvTab.name_first.toString(), words[1]);
                    kvJson.putOpt(KvTab.name_middle.toString(), words[2]);
                    kvJson.putOpt(KvTab.name_last.toString(), words[3]);
                    kvJson.putOpt(KvTab.name_suffix.toString(), "");
                    break;
                case 5:
                    kvJson.putOpt(KvTab.name_prefix.toString(), words[0]);
                    kvJson.putOpt(KvTab.name_first.toString(), words[1]);
                    kvJson.putOpt(KvTab.name_middle.toString(), words[2]);
                    kvJson.putOpt(KvTab.name_last.toString(), words[3]);
                    kvJson.putOpt(KvTab.name_suffix.toString(), words[4]);
                    break;
                default:
                    kvJson.putOpt( KvTab.name_prefix.toString(), words[0]);
                    kvJson.putOpt( KvTab.name_first.toString(), words[1]);

                    String middle = "";

                    for( int i =2; i < count-2; i++)
                        middle = middle +" "+ words[ i ];

                    kvJson.putOpt(KvTab.name_middle.toString(), middle);
                    kvJson.putOpt(KvTab.name_last.toString(), words[count - 2]);
                    kvJson.putOpt(KvTab.name_suffix.toString(), words[count - 1]);
                    break;
            }
            put(contact_id, DTab.kv, kvJson.toString());

        } catch (JSONException e) { e.printStackTrace(); }
    }

    /**
     * Return the full name from the name components.
     * @param contact_id
     * @return
     */
    public static String getFullName(Long contact_id) {

        String full_name = "";
        try {
        JSONObject kvJson = new JSONObject(get(contact_id, DTab.kv));
            String s = kvJson.getString( KvTab.name_prefix.toString()) + " ";
            s += kvJson.getString( KvTab.name_first.toString()) + " ";
            s += kvJson.getString( KvTab.name_middle.toString()) + " ";
            s += kvJson.getString( KvTab.name_last.toString()) + " ";
            s += kvJson.getString( KvTab.name_suffix.toString()) + " ";
            s = s.trim();// remove leading or trailing spaces
            full_name = s.replaceAll("\\s+", " ");// remove extra white space

        } catch (JSONException e) { e.printStackTrace(); }

        return full_name;
    }
}
