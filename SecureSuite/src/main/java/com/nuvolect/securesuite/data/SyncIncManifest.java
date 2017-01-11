package com.nuvolect.securesuite.data;//

import java.util.HashSet;

/**
 * Provide data and methods to incrementally sync two sqlite databases.
 * HashSets are provided for inserts, updates, deletes and cryp password data.
 * The data is converted to JSON using GSON.  As sync requirements are satisfied
 * they are removed from the manifest.
 */
public class SyncIncManifest {

    HashSet<Long> contactInserts;
    HashSet<Long> contactUpdates;
    HashSet<Long> contactDeletes;
    //TODO add groupInserts, group will not sync until a contact is added to it
    /**
     * A new empty group is currently not synced.  Only when a contact is added to a
     * new group is the group synced and created on the companion device.
     */
    HashSet<Integer> groupDeletes;
    HashSet<Integer> crypSync;

    public SyncIncManifest() {

        init();
    }

    public void init() {

        contactInserts = new HashSet<Long>();
        contactUpdates = new HashSet<Long>();
        contactDeletes = new HashSet<Long>();
        groupDeletes = new HashSet<Integer>();
        crypSync = new HashSet<Integer>();
    }

    public void loadDbIncSync() {

        SqlCipher.loadSyncManifest(this);
    }

    public String report(){

        String report = ""
                +"\ncontact inserts: "+ contactInserts
                +"\ncontact updates: "+ contactUpdates
                +"\ncontact deletes: "+ contactDeletes
                +"\ngroup deletes: "+ groupDeletes
                +"\ncrypSync: "+ crypSync
                ;
        return report;
    }

    public boolean isEmpty() {

        if( ! contactInserts.isEmpty())
            return false;
        if( ! contactUpdates.isEmpty())
            return false;
        if( ! contactDeletes.isEmpty())
            return false;
        if( ! groupDeletes.isEmpty())
            return false;
        if( ! crypSync.isEmpty())
            return false;

        return true;
    }

    /**
     * <pre>
     * The manifest may have inserts, updates or deletes with the same contact_id.
     * A contact_id should only be on a single list.
     * There are two specific conditions:
     * 1. For each contact_id on the deletes list, make sure it is removed from the
     * inserts or updates lists.
     * 2. For each contact_id on the updates list, make sure it is removed from the inserts list.
     * </pre>
     */
    public void optimize() {

        for( long contact_id : contactDeletes){  // condition 1

            contactInserts.remove(contact_id);
            contactUpdates.remove(contact_id);
        }
        for( long contact_id : contactUpdates){  // condition 2

            contactInserts.remove(contact_id);
        }
    }
}
