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

import android.content.Context;

import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.Persist;

import static com.nuvolect.securesuite.data.MyGroups.getContacts;

public class MyContacts {

    /**
     * Check if there is an existing empty contact and if so, delete it and set
     * the current contact to the first contact.  This is used when a user creates
     * a new contact then selects undo or hits the back button.  Hence the
     * empty contact is not necessary and is deleted.
     * @param ctx
     */
    public static void manageEmptyContact(Context ctx) {

        long empty_contact_id = Persist.getEmptyContactId(ctx);
        if( empty_contact_id != 0){

            // User is discarding an empty contact that was just created
            SqlCipher.deleteContact(ctx, empty_contact_id, true);
            Persist.setEmptyContactId(ctx, 0);
            Persist.setCurrentContactId(ctx, SqlCipher.getFirstContactID());
            MyGroups.loadGroupMemory();
        }
    }

    /**
     * Test and as necessary assign a default contact_id.
     * The user may have just deleted the contact.
     * If the current group does not have any contacts, set the contact_id to zero,
     * otherwise return the first contact in the group.
     */
    public static long setDefaultContactId(Context ctx) {

        long contact_id = Persist.getCurrentContactId(ctx);
        if( ! SqlCipher.validContactId( contact_id)){

            int group = Cryp.getCurrentGroup();
            long[] contacts = getContacts(group);
            if( contacts.length > 0)
                contact_id = contacts[ 0 ];
            else
                contact_id = 0;

            Persist.setCurrentContactId(ctx, contact_id);
        }
        return contact_id;
    }

    public static long setValidId(Context ctx, long contact_id) {

        if( ! SqlCipher.validContactId( contact_id)){

            int group = Cryp.getCurrentGroup();
            long[] contacts = getContacts(group);
            if( contacts.length > 0)
                contact_id = contacts[ 0 ];
            else
                contact_id = 0;
        }

        Persist.setCurrentContactId(ctx, contact_id);

        return contact_id;
    }

    public static boolean contactExists(long contact_id) {

        return SqlCipher.validContactId( contact_id);
    }

    /**
     * Get the current contact ID and validate it against the current account.
     * Provide a default contact ID that always matches the current account.
     *
     * @param ctx
     * @return
     */
    public static long getCurrrentContactId(Context ctx) {

        long contact_id = Persist.getCurrentContactId( ctx);
        String account = MyAccounts.getAccount( contact_id);
        return getCurrrentContactId( ctx, account);
    }

    public static long getCurrrentContactId(Context ctx, String account) {

        long contact_id = Persist.getCurrentContactId( ctx);
        String current_account = Cryp.getCurrentAccount();
        if( account.contentEquals( current_account))
            return contact_id;
        else
            return getFirstContact( ctx, account);
    }

    private static long getFirstContact(Context ctx, String account) {

        return SqlCipher.getFirstContactID( account);
    }

    public static long getFirstContactInGroup(int group_id){

        long[] contacts = MyGroups.getContacts(group_id);
        if( contacts.length > 0)
            return contacts[0];
        else
            return 0;
    }

    /**
     * Identify contacts given id range and testing that the contact
     * is not in the trash.
     * @param contact_id
     * @return
     */
    public static boolean invalidContact(long contact_id) {

        if( contact_id <= 0 || MyGroups.isInTrash( contact_id))
            return true;
        else
            return false;
    }

    /**
     * Identify contacts given id range and testing that the contact
     * is not in the trash.
     * @param contact_id
     * @return
     */
    public static boolean validContact(long contact_id) {

        return ! invalidContact( contact_id);
    }
}

