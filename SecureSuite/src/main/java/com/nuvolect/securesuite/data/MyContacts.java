package com.nuvolect.securesuite.data;

import android.content.Context;

import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.Persist;

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
            long[] contacts = MyGroups.getContacts(group);
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
            long[] contacts = MyGroups.getContacts(group);
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
}

