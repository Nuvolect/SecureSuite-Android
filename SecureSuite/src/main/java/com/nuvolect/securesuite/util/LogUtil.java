package com.nuvolect.securesuite.util;

import android.content.Context;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LogUtil {

    public static boolean VERBOSE = true;// 1. Auto set for build variant. 2. Developer menu option
    public static boolean DEBUG = true;

    public static String TAG = "SecureSuite";

    public enum LogType { NIL,
        ACA_UTIL,
        BETTER_CRYPTO,
        BOOT_RECEIVER,
        CALL_RECEIVER,
        CDA,
        CLA,
        CLF,
        CMD_DEBUG,
        COMM,
        CONTACT_DETAIL,
        CONTACT_EDIT,
        CRYPT,
        CRYP_SERVER,
        DETAIL_HTM,
        DEVELOPER_DIALOG,
        EDIT_GROUP,
        EXPORT_VCF,
        GDA,
        GLA,
        GROUP_COMMS,
        IMPORT_VCF,
        IN_APP_PAYMENT,
        JSON,
        LICENSE_MANAGER,
        LICENSE_UTIL,
        LIST_HTM,
        LOCK_ACTIVITY,
        LOGIN_HTM,
        MY_GROUPS,
        NANOHTTPD,
        NDEF_READER,
        NFC_ACTIVITY,
        NFC_SESSION,
        PASSWORD,
        REST,
        RESTFUL_HTM,
        RESTORE_DB,
        SECURE,
        SEND_SMS,
        SERVE,
        SETTINGS,
        SETTINGS_ACTIVITY,
        SQLCIPHER,
        SQL_FULL_SYNC_SRC,
        SQL_FULL_SYNC_TARGET,
        SQL_INC_SYNC_SRC,
        SQL_INC_SYNC_TARGET,
        SQL_SYNC_TEST,
        USER_MANAGER,
        UTIL,
        VOLLEY,
        WEB_SERVER,
        WEB_SERVICE,
        WEB_UTIL,
        WHATS_NEW,
        WIFI_BROADCAST_RECEIVER,
        WORKER,
        }

    public static void setVerbose(boolean verbose){

        VERBOSE = verbose;
        DEBUG = verbose;
    }
    /**
     * Post a message to the developer console if VERBOSE is enabled.
     * @param log
     */
    public static void log(String log){

        if(LogUtil.VERBOSE)
            Log.v(LogUtil.TAG, log);
    }

    public static void log(LogType tag, String log){

        if(LogUtil.VERBOSE)
            Log.v(LogUtil.TAG+":"+tag.toString(), log);
    }
    public static void log(Class<?> clazz, String log) {

        if(LogUtil.VERBOSE)
            Log.v( TAG+":"+clazz.toString(), log);
    }

    /**
     * Put exception in Android LogCat and logDB.
     * @param ctx
     * @param clazz
     * @param e
     */
    public static void logException(Context ctx, Class<?> clazz, Exception e) {

        e.printStackTrace(System.err);
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log( clazz,  "ERROR Exception: "+sw.toString());
    }
    /**
     * Put exception in Android LogCat and logDB.
     * @param clazz
     * @param e
     */
    public static void logException( Class<?> clazz, Exception e) {

        e.printStackTrace(System.err);
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log( clazz,  "ERROR Exception: "+sw.toString());
    }
    /**
     * Put exception in Android LogCat and logDB.
     * @param clazz
     * @param e
     */
    public static void logException( Class<?> clazz, String note, Exception e) {

        e.printStackTrace(System.err);
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log( clazz,  "ERROR Exception: "+note+sw.toString());
    }
    /**
     * Put exception in Android LogCat and logDB.
     * @param ctx
     * @param logType
     * @param e
     */
    public static void logException(Context ctx, LogType logType, Exception e) {

        e.printStackTrace(System.err);
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log( logType,  "ERROR Exception: "+sw.toString());
    }
    public static void logException(LogType logType, Exception e) {

        e.printStackTrace(System.err);
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log( logType,  "ERROR Exception: "+sw.toString());
    }

    public static void logException(LogType logType, String s, Exception e) {

        e.printStackTrace(System.err);
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log( logType,  "ERROR Exception: "+s+sw.toString());
    }

    /**
     * Print simple log
     * @param tag
     * @param string
     */
    public static void e(String tag, String string) {

        if(LogUtil.VERBOSE)
            Log.e( tag, string);
    }
}
