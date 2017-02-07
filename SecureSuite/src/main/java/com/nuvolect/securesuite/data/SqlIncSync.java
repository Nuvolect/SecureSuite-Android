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

package com.nuvolect.securesuite.data;//

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.nuvolect.securesuite.webserver.WebUtil;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.Passphrase;
import com.nuvolect.securesuite.util.TimeUtil;
import com.nuvolect.securesuite.util.WorkerCommand;

/** Incremental sync object and methods */
public class SqlIncSync {

    //TODO sync in both directions
    //TODO test sync when WiFi event fires
    private static SqlIncSync instance;
    private Handler mHandler;

    private static final long SYNC_TIMER_DELAY = 10000;// Delay 10 seconds to batch updates
    private Context m_ctx;
    /**
     * Flag true when sync is suspended.  This is used when the user might upload a large
     * file with hundreds or thousands of contacts.
     */
    private boolean mSyncSuspended = false;
    /**
     * True when sync target device has an IP address assigned and sync is desirable.
     */
    private boolean mSyncEnabled = false;

    public static enum INC_SYNC_TYPE {NIL,
        INSERT_CONTACT, UPDATE_CONTACT, DELETE_CONTACT,
        DELETE_GROUP,
        PASSWORD_GEN_HISTORY, PASSWORD_TARGET, PASSWORD_LENGTH, PASSWORD_GEN_MODE
    };

    public static synchronized SqlIncSync getInstance() {
        if(instance == null) {
            instance = new SqlIncSync();
        }
        return instance;
    }

    public SqlIncSync(){

        /**
         * Use the main looper to create the handler.  Without the main looper
         * the handler will fail when initialized from a non-UI thread, such as when
         * called from the web server.
         */
        mHandler = new Handler(Looper.getMainLooper());

        mSyncSuspended = false;
        mSyncEnabled = WebUtil.companionServerAssigned();
    }

    /**
     * For long running processes, suspend the sync timer until resumed.
     * All sync data requests are still recorded.
     */
    public void suspendSync() {

        mHandler.removeCallbacks(m_incrementalSyncTimer);
        mSyncSuspended = true;
    }

    /**
     * By default sync is enabled when a target sync device is assigned.
     * This method disables it when it is unassigned, otherwise
     * it is defined when this singleton is initialized
     */
    public void setSyncEnabled(boolean state){
        mSyncEnabled = state;
    }

    /**
     * By default sync is enabled when a target sync device is assigned.
     * This method gets current sync state.
     */
    public boolean getSyncEnabled(){
        return mSyncEnabled;
    }

    /**
     * Resume sync timer after a long running process.
     */
    public void resumeSync(Context ctx) {

        if( mSyncEnabled){

        mSyncSuspended = false;
        setSyncTimer(ctx);
        }
    }


    public void insertContact(Context ctx, long contact_id) {

        if( mSyncEnabled){

        SqlCipher.putIncSync( INC_SYNC_TYPE.INSERT_CONTACT.ordinal(), contact_id);

        if( !mSyncSuspended)
            setSyncTimer(ctx);
        }
    }

    public void updateContact(Context ctx, long contact_id) {

        if( mSyncEnabled){

        SqlCipher.putIncSync( INC_SYNC_TYPE.UPDATE_CONTACT.ordinal(), contact_id);

        if( !mSyncSuspended)
            setSyncTimer(ctx);
        }
    }

    public void deleteContact(Context ctx, long contact_id) {

        if( mSyncEnabled) {
            SqlCipher.putIncSync(INC_SYNC_TYPE.DELETE_CONTACT.ordinal(), contact_id);

            if (!mSyncSuspended)
                setSyncTimer(ctx);
        }
    }

    public void deleteGroup(Context ctx, int group_id) {

        if( mSyncEnabled) {
            SqlCipher.putIncSync(INC_SYNC_TYPE.DELETE_GROUP.ordinal(), (long) group_id);

            if (!mSyncSuspended)
                setSyncTimer(ctx);
        }
    }

    /**
     * Save password related crypdata for synchronization
     *
     * @param ctx
     * @param key
     */
    public void crypSync(Context ctx, String key){

        if( mSyncEnabled) {
            if (key.contentEquals(Passphrase.PASSWORD_GEN_HISTORY))
                SqlCipher.putIncSync(INC_SYNC_TYPE.PASSWORD_GEN_HISTORY.ordinal(), 0);
            if (key.contentEquals(Passphrase.PASSWORD_TARGET))
                SqlCipher.putIncSync(INC_SYNC_TYPE.PASSWORD_TARGET.ordinal(), 0);
            if (key.contentEquals(Passphrase.PASSWORD_LENGTH))
                SqlCipher.putIncSync(INC_SYNC_TYPE.PASSWORD_LENGTH.ordinal(), 0);
            if (key.contentEquals(Passphrase.PASSWORD_GEN_MODE))
                SqlCipher.putIncSync(INC_SYNC_TYPE.PASSWORD_GEN_MODE.ordinal(), 0);

            if (!mSyncSuspended)
                setSyncTimer(ctx);
        }
    }

    /**
     * Set or modify a timer to synchronize a batch of updates.
     * If the timer is already enabled, push back the trigger time
     * @param ctx
     */
    private void setSyncTimer(Context ctx) {

        /**
         * Save context in order to kickoff worker service call
         */
        m_ctx = ctx;

        mHandler.removeCallbacks(m_incrementalSyncTimer);
        mHandler.postDelayed(m_incrementalSyncTimer, SYNC_TIMER_DELAY);
    }

    private Runnable m_incrementalSyncTimer = new Runnable() {
        public void run() {

            WorkerCommand.queStartIncSync(m_ctx);

            mHandler.removeCallbacks(m_incrementalSyncTimer);
        }
    };

    /**
     * Set a text string with the last time the device was updated as an incoming companion.
     */
    public void setIncomingUpdate(){

        Cryp.put(CConst.LAST_INCOMING_UPDATE, "Incoming update: "
                + TimeUtil.friendlyTimeMDYM(System.currentTimeMillis()));
    }
    /**
     * Return text string with the last time the device was updated as an incoming companion.
     * @return
     */
    public String getIncomingUpdate(){

        return Cryp.get(CConst.LAST_INCOMING_UPDATE, "Incoming update: never");
    }
    /**
     * Set a text string with the last time the device was updated as an outgoing companion.
     */
    public void setOutgoingUpdate(){

    Cryp.put(CConst.LAST_OUTGOING_UPDATE, "Outgoing update: "
            + TimeUtil.friendlyTimeMDYM(System.currentTimeMillis()));
    }
    /**
     * Return text string with the last time the device was updated as an outgoing companion.
     * @return
     */
    public String getOutgoingUpdate(){

        return Cryp.get(CConst.LAST_OUTGOING_UPDATE, "Outgoing update: never");
    }
}
