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

package com.nuvolect.securesuite.util;

import android.content.Context;
import android.content.Intent;

import com.nuvolect.securesuite.main.CConst;

public class WorkerCommand {

    public static void registerAndroidObserver(Context ctx){

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.REGISTER_ANDROID_OBSERVER.ordinal());
        ctx.startService( i );
    }

    public static void importCloudContacts(Context ctx, CharSequence[] importAccountList, boolean[] importSelectList){

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.IMPORT_CLOUD_CONTACTS.ordinal());
        i.putExtra(CConst.IMPORT_ACCOUNT_LIST, importAccountList);
        i.putExtra(CConst.IMPORT_SELECT_LIST, importSelectList);
        ctx.startService( i );
    }
    public static void interruptProcessingAndStop(Context ctx) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.INTERRUPT_PROCESSING_AND_STOP.ordinal());
        ctx.startService( i );
    }

    /**
     * Dispatch a message to each activity that is listening.
     * @param ctx
     * @param uiType Hint for what to refresh, CConst.CONTACTS
     */
    public static void refreshUserInterface(Context ctx, String uiType){

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.REFRESH_USER_INTERFACE.ordinal());
        i.putExtra(CConst.UI_TYPE_KEY, uiType);
        ctx.startService( i );
    }

    public static void fullSyncSendSourceManifest(Context ctx) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.FULL_SYNC_SEND_SOURCE_MANIFEST.ordinal());
        ctx.startService( i );
    }

    public static void queOptimizeFullSyncPlan(Context ctx) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.FULL_SYNC_OPTIMIZE_PLAN.ordinal());
        ctx.startService( i );
    }

//    public static void queSyncDataReq(Context ctx) {
//
//        Intent i = new Intent( ctx, WorkerService.class);
//        i.putExtra("command", WorkerService.Command.FULL_SYNC_DATA_REQ.ordinal());
//        ctx.startService( i );
//    }

    public static void queFullSyncSendData(Context ctx) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.FULL_SYNC_SEND_DATA.ordinal());
        ctx.startService( i );
    }

    public static void queFullSyncProcessData(Context ctx) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.FULL_SYNC_PROCESS_DATA.ordinal());
        ctx.startService( i );
    }

    public static void quePingTest(Context ctx) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.PING_TEST.ordinal());
        ctx.startService( i );
    }
    public static void quePongTest(Context ctx) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.PONG_TEST.ordinal());
        ctx.startService( i );
    }

    public static void queValidateDbCounts(Context ctx, boolean fixErrors) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.VALIDATE_DB_COUNTS.ordinal());
        i.putExtra(CConst.FIX_ERRORS, fixErrors);
        ctx.startService( i );
    }

    public static void queValidateDbGroups(Context ctx, boolean fixErrors) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.VALIDATE_DB_GROUPS.ordinal());
        i.putExtra(CConst.FIX_ERRORS, fixErrors);
        ctx.startService( i );
    }

    public static void queStartIncSync(Context ctx) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.INC_SYNC_START.ordinal());
        ctx.startService( i );
    }

    public static void queOptimizeIncSyncPlan(Context ctx) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.INC_SYNC_OPTIMIZE_PLAN.ordinal());
        ctx.startService( i );
    }

    public static void queIncSyncSendData(Context ctx) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.INC_SYNC_SEND_DATA.ordinal());
        ctx.startService( i );
    }

    public static void queIncSyncProcessData(Context ctx) {

        Intent i = new Intent( ctx, WorkerService.class);
        i.putExtra("command", WorkerService.WorkTask.INC_SYNC_PROCESS_DATA.ordinal());
        ctx.startService( i );
    }

    public static void startWifiBroadcastReceiver(Context ctx) {

//        Intent i = new Intent( ctx, WorkerService.class);
//        i.putExtra("command", WorkerService.WorkTask.START_WIFI_BROADCAST_RECEIVER.ordinal());
//        ctx.startService( i );
    }

    public static void stopWifiBroadcastReceiver(Context ctx) {

//        Intent i = new Intent( ctx, WorkerService.class);
//        i.putExtra("command", WorkerService.WorkTask.STOP_WIFI_BROADCAST_RECEIVER.ordinal());
//        ctx.startService( i );
    }

}
