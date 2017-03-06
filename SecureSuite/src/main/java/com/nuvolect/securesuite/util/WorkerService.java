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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.ContactsContract;

import com.nuvolect.securesuite.data.ImportContacts;
import com.nuvolect.securesuite.data.ImportContacts.Callbacks;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlFullSyncSource;
import com.nuvolect.securesuite.data.SqlFullSyncTarget;
import com.nuvolect.securesuite.data.SqlIncSyncSource;
import com.nuvolect.securesuite.data.SqlIncSyncTarget;
import com.nuvolect.securesuite.data.SqlSyncTest;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil.LogType;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class WorkerService extends Service {

    private static final boolean DEBUG = LogUtil.DEBUG;
    MyContentObserver observer;
    private static Context m_ctx;
    private int mContactCount;
    private static Handler mHandler;

    // Target we publish for clients to send messages to IncomingHandler.
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    // Keeps track of all current registered clients.
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    public static enum WorkTask {
        CHECK_ANDROID_UPDATES,
        EXPORT_VCF,
        IMPORT_VCF,
        NOTIFY_ANDROID_CHANGE,
        REGISTER_ANDROID_OBSERVER,
        INTERRUPT_PROCESSING,
        INTERRUPT_PROCESSING_AND_STOP,
        IMPORT_CLOUD_CONTACTS,
        IMPORT_CLOUD_CONTACTS_COMPLETE,
        REFRESH_USER_INTERFACE,
        IMPORT_CLOUD_CONTACTS_UPDATE,
        VALIDATE_DB_COUNTS,
        VALIDATE_DB_GROUPS,
        /**
         * The broadcast receiver is started with a Manifest definition.
         * These two states enable and disable it.
         */
        START_WIFI_BROADCAST_RECEIVER,
        STOP_WIFI_BROADCAST_RECEIVER,

        ///////////////////////////////////////////////////////////////////////////////
        /**
         * Kickoff the sync process.  Send _id of each db record to sync.
         * Other server will make a plan.
         */
        FULL_SYNC_SEND_SOURCE_MANIFEST,

        /**
         * Use the JSON data to build an optimized sync plan
         */
        FULL_SYNC_OPTIMIZE_PLAN,

        /**
         * Send sync data to the companion SecureSuite device
         */
        FULL_SYNC_SEND_DATA,

        /**
         * Process the sync data just received
         */
        FULL_SYNC_PROCESS_DATA,
        ///////////////////////////////////////////////////////////////////////////////

        /**
         * Kickoff incremental sync
         */
        INC_SYNC_START,

        /**
         * Do final prep and call for first data increment
         */
        INC_SYNC_OPTIMIZE_PLAN,

        /**
         * Send data based on request
         */
        INC_SYNC_SEND_DATA,
       ///////////////////////////////////////////////////////////////////////////////
        /**
         * ping pong data between SecureSuite devices.
         */
        PONG_TEST,
        PING_TEST, INC_SYNC_PROCESS_DATA, INC_SYNC_DATA_REQ,

    }

    @Override
    public void onCreate() {
        super.onCreate();

        m_ctx = this.getApplicationContext();

        WorkerServiceThread looper = new WorkerServiceThread();
        looper.start();
        try {
            looper.ready.acquire();
        } catch (InterruptedException e) {
            LogUtil.log(LogType.WORKER,
                    "Interrupted during wait for the CommServiceThread to start, prepare for trouble!");
            LogUtil.logException(m_ctx, LogType.WORKER, e);
        }
    }


    private class WorkerServiceThread extends Thread {
        public Semaphore ready = new Semaphore(0);

        WorkerServiceThread() {
            this.setName("commServiceThread");
        }

        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    _handleMessage(msg);
                }
            };
            ready.release(); // Signal the looper and handler are created
            Looper.loop();
        }
    }

    // internally queue a command
    @SuppressWarnings("unused")
    private void queueCommand(WorkTask cmd) {

        Bundle bundle = new Bundle();
        //        bundle.putLong(GConst.ANGEL_ID, angel_id);

        Message msg = Message.obtain();
        msg.setData(bundle);
        msg.what = cmd.ordinal();
        mHandler.sendMessage(msg);
    }

    /**
     * Put a command in the queue to be executed about timeMillis
     * with reference to uptimeMillis()
     * @param cmd
     * @param atTimeMillis
     */
    @SuppressWarnings("unused")
    private void queueCommand(WorkTask cmd, long atTimeMillis) {
        Message msg = Message.obtain();
        msg.what = cmd.ordinal();
        mHandler.sendMessageAtTime(msg, atTimeMillis);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            Bundle extras = null;
            if (intent != null)
                extras = intent.getExtras();

            if( extras != null && extras.containsKey("command")){

                /**
                 * Package the command defined in the intent and queue it along
                 * with it's parameters for processing. Most commands are executed
                 * in FIFO order with the exception of high priority commands pushed
                 * onto the front of the queue and executed in LIFO order.
                 */
                Message msg = Message.obtain();
                Bundle bundle = new Bundle();

                int cmdIndex = extras.getInt("command");
                msg.what = cmdIndex;
                WorkTask cmd = WorkTask.values()[cmdIndex];

                switch (cmd) {

                    // No parameters, sync data process
                    case PING_TEST:
                    case PONG_TEST:

                        // No parameters
                    case CHECK_ANDROID_UPDATES:
                    case EXPORT_VCF:
                    case IMPORT_VCF:
                    case NOTIFY_ANDROID_CHANGE:
                    case IMPORT_CLOUD_CONTACTS_COMPLETE:
                    case START_WIFI_BROADCAST_RECEIVER:
                    case STOP_WIFI_BROADCAST_RECEIVER:
                    case REGISTER_ANDROID_OBSERVER:
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;

                    // No parameters, delay before sending
                    case INC_SYNC_START:
                    case INC_SYNC_OPTIMIZE_PLAN:
                    case INC_SYNC_DATA_REQ:
                    case INC_SYNC_SEND_DATA:
                    case INC_SYNC_PROCESS_DATA:

                    case FULL_SYNC_SEND_SOURCE_MANIFEST:
                    case FULL_SYNC_OPTIMIZE_PLAN:
                    case FULL_SYNC_SEND_DATA:
                    case FULL_SYNC_PROCESS_DATA:
                        long upTimeMillis = SystemClock.uptimeMillis() + 100;
                        msg.setData(bundle);
                        mHandler.sendMessageAtTime(msg, upTimeMillis);
                        break;

                    // Single integer parameter
                    case REFRESH_USER_INTERFACE: {
                        bundle.putString(CConst.UI_TYPE_KEY, extras.getString(CConst.UI_TYPE_KEY));
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;
                    }

                    // Single boolean parameter
                    case IMPORT_CLOUD_CONTACTS_UPDATE:
                        break;
                    case VALIDATE_DB_GROUPS:
                    case VALIDATE_DB_COUNTS:{

                        boolean fix_errors = extras.getBoolean(CConst.FIX_ERRORS);
                        bundle.putBoolean(CConst.FIX_ERRORS, fix_errors);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;
                    }
                    case IMPORT_CLOUD_CONTACTS:{

                        // List of accounts and  boolean list showing user selections
                        CharSequence[] accountList = extras.getCharSequenceArray(CConst.IMPORT_ACCOUNT_LIST);
                        bundle.putCharSequenceArray(CConst.IMPORT_ACCOUNT_LIST, accountList);
                        boolean[] selectList = extras.getBooleanArray(CConst.IMPORT_SELECT_LIST);
                        bundle.putBooleanArray(CConst.IMPORT_SELECT_LIST, selectList);

                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;
                    }

                    // No parameters, take immediate action.
                    // Clear the queue of all messages and callbacks, next command will be only command in queue.
                    // These next two commands are the only commands that we receive that do not also get executed from the queue.
                    case INTERRUPT_PROCESSING:

                        mHandler.removeCallbacksAndMessages(null);
                        LogUtil.log(LogType.WORKER, "CLEAR_QUEUE");
                        break;

                    case INTERRUPT_PROCESSING_AND_STOP:

                        mHandler.removeCallbacksAndMessages(null);
                        ImportContacts.interruptImport();
                        if( DEBUG) LogUtil.log(LogType.WORKER, "CLEAR_QUEUE, STOPPING");
                        stopSelf();
                        break;
                    default:
                        LogUtil.log(LogType.WORKER,
                                "CommService ERROR not recognized: " + cmd.toString());
                }
                extras.clear();

            }
        } catch (Exception e) {
            LogUtil.logException(m_ctx, LogType.WORKER, e);
        }

        return START_STICKY;
    }

    private void _handleMessage(Message msg) {

        Bundle bundle = msg.getData();
        WorkTask cmd = WorkTask.values()[msg.what];

        Callbacks m_importProgressListener = new Callbacks(){

            @Override
            public void reportProgresss(int progress) {

                Bundle tinyBundle = new Bundle();
                tinyBundle.putInt(CConst.IMPORT_PROGRESS, progress);
                notifyListeners( WorkTask.IMPORT_CLOUD_CONTACTS_UPDATE.ordinal(), tinyBundle);
            }
        };

        try {

            switch (cmd) {

                case CHECK_ANDROID_UPDATES:
                    break;
                case EXPORT_VCF:
                    break;

                case INC_SYNC_START:
                    SqlIncSyncSource.getInstance().startIncSyncSource(m_ctx);
                    break;
                case INC_SYNC_OPTIMIZE_PLAN:
                    SqlIncSyncTarget.getInstance().incSyncOptimizeAndStart(m_ctx);
                    break;
                case INC_SYNC_DATA_REQ:
                    SqlIncSyncTarget.getInstance().incSyncDataReq(m_ctx);
                    break;
                case INC_SYNC_SEND_DATA:
                    SqlIncSyncSource.getInstance().incSyncDataSend(m_ctx);
                    break;
                case INC_SYNC_PROCESS_DATA:
                    SqlIncSyncTarget.getInstance().incSyncProcessData(m_ctx);
                    break;

                case FULL_SYNC_SEND_SOURCE_MANIFEST:
                    SqlFullSyncSource.getInstance().fullSyncSourceManifest(m_ctx);
                    break;
                case FULL_SYNC_OPTIMIZE_PLAN:
                    SqlFullSyncTarget.getInstance().fullSyncStart(m_ctx);
                    break;
                case FULL_SYNC_SEND_DATA:
                    SqlFullSyncSource.getInstance().fullSyncDataSend(m_ctx);
                    break;
                case FULL_SYNC_PROCESS_DATA:
                    SqlFullSyncTarget.getInstance().fullSyncDataProcess(m_ctx);
                    break;

                case PING_TEST:
                    SqlSyncTest.getInstance().ping_test(m_ctx);
                    break;
                case PONG_TEST:
                    SqlSyncTest.getInstance().pong_test(m_ctx);
                    break;
                case INTERRUPT_PROCESSING:
                    break;
                case INTERRUPT_PROCESSING_AND_STOP:
                    break;
                case IMPORT_CLOUD_CONTACTS:{
                    if(DEBUG)LogUtil.log("WorkerService, importRefresh start");

                    CharSequence[] accountList = bundle.getCharSequenceArray(CConst.IMPORT_ACCOUNT_LIST);
                    boolean[] selectList = bundle.getBooleanArray(CConst.IMPORT_SELECT_LIST);

                    int imported = ImportContacts.importAccountContacts(m_ctx, accountList, selectList, m_importProgressListener);

                    if(DEBUG)LogUtil.log("WorkerService, importRefresh complete: "+imported);

                    Bundle tinyBundle = new Bundle();
                    notifyListeners(WorkTask.IMPORT_CLOUD_CONTACTS_COMPLETE.ordinal(), tinyBundle);
                    break;
                }
                case IMPORT_CLOUD_CONTACTS_COMPLETE:
                    break;
                case IMPORT_VCF:
                    break;
                case NOTIFY_ANDROID_CHANGE:{
                    break;
                }
                case VALIDATE_DB_COUNTS:{
                    boolean fix_errors = bundle.getBoolean(CConst.FIX_ERRORS);
                    SqlCipher.validateFixDb(m_ctx, fix_errors);
                    break;
                }
                case VALIDATE_DB_GROUPS:{
                    boolean fix_errors = bundle.getBoolean(CConst.FIX_ERRORS);
                    SqlCipher.validateGroups(m_ctx, fix_errors);
                    break;
                }
                case REGISTER_ANDROID_OBSERVER:{
                    // Record current count to determine if changes are insert, delete or modify
                    mContactCount = getAndroidContactCount();

                    observer = new MyContentObserver();
                    m_ctx.getContentResolver()
                            .registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer);

                    if(DEBUG)LogUtil.log("WorkerService, ContactMonitorService registered");

                    notifyListeners(cmd.ordinal(), bundle);
                    break;
                }
                case REFRESH_USER_INTERFACE:{

                    //FUTURE push message to web clients
                    /**
                     * We need a node.js push type setup.  There may be multiple web clients and each
                     * needs to be sent a message.
                     * A generalized solution would allow any type of message with control over
                     * appearance and duration.
                     */
//                    WebService.userMessage("Contacts have changed, please refresh", "notice");
                    LogUtil.log("WorkerService " + cmd + " command to listeners("+activeListeners()+"): "+bundle);

                    notifyListeners( cmd.ordinal(), bundle);
                    break;
                }

                case IMPORT_CLOUD_CONTACTS_UPDATE:
                    break;

                case START_WIFI_BROADCAST_RECEIVER: {

                    LogUtil.log(LogType.WORKER, "Starting wifi broadcast receiver");

                    ComponentName receiver = new ComponentName(m_ctx, WifiBroadcastReceiver.class);

                    PackageManager pm = m_ctx.getPackageManager();

                    pm.setComponentEnabledSetting(receiver,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                    break;
                }
                case STOP_WIFI_BROADCAST_RECEIVER: {

                    LogUtil.log(LogType.WORKER, "Stopping wifi broadcast receiver");

                    ComponentName receiver = new ComponentName(m_ctx, WifiBroadcastReceiver.class);

                    PackageManager pm = m_ctx.getPackageManager();

                    pm.setComponentEnabledSetting(receiver,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                    break;
                }

                default:
                    break;
            }

        }
        catch (Exception e) {
            LogUtil.logException( m_ctx, LogType.WORKER, e);
        }
    }

    private int getAndroidContactCount() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI, null, null, null,
                    null);
            if (cursor != null) {
                return cursor.getCount();
            } else {
                return 0;
            }
        } catch (Exception ignore) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    public class MyContentObserver extends ContentObserver {

        public MyContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            final int currentCount = getAndroidContactCount();
            if (currentCount < mContactCount) {
                // DELETE HAPPEN.
                if(DEBUG)LogUtil.log("ContactMonitorService, delete: "+currentCount);
            } else if (currentCount == mContactCount) {
                // UPDATE HAPPEN.
                if(DEBUG)LogUtil.log("ContactMonitorService, update: "+currentCount);
            } else {
                // INSERT HAPPEN.
                if(DEBUG)LogUtil.log("ContactMonitorService, insert: "+currentCount);
            }
            mContactCount = currentCount;
        }
    }

    /**
     * Command to the service to register a client, receiving callbacks from the
     * service. The Message's replyTo field must be a Messenger of the client
     * where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, to stop receiving
     * callbacks from the service. The Message's replyTo field must be a
     * Messenger of the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Pass message to all active listeners.
     * @param bundle
     */
    private void notifyListeners(int whatCmd, Bundle bundle) {

        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                Message msg = Message.obtain();
                msg.setData(bundle);
                msg.what = whatCmd;
                mClients.get(i).send(msg);// Send a message to the registered handler

                if( DEBUG )
                    LogUtil.log("WorkerService notifyListeners: "+ WorkTask.values()[whatCmd]+" "+bundle);

            } catch (RemoteException e) {
                // The client is dead. Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    /**
     * Check if we have active listeners. Generally this will be an Activity that
     * is interested to know the state of this service.
     * @return boolean
     */
    public int activeListeners(){
        return mClients.size();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}