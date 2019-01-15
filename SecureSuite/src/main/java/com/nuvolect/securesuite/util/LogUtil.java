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
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility class for logging. All logcat info goes through these methods.
 * By default the development build enables verbose logging and the release
 * build is quiet.
 */
public class LogUtil {

    /**
     * Set each time the app starts from App.class with reference
     * to debug/values/strings.xml and main/values/strings.xml.
     *
     * VERBOSE and DEBUG are also toggled from the developer menu.
     */
    public static boolean VERBOSE = false;
    public static boolean DEBUG = false;// Used to enable blocks of debugging code.

    public static String TAG = "SecureSuite";

    public enum LogType { NIL,
        ACA_UTIL,
        ADMIN_SERVE_CMD,
        BETTER_CRYPTO,
        BOOT_RECEIVER,
        CAL_REST,
        CALL_RECEIVER,
        CDA,
        CERTIFICATE,
        CLA,
        CLF,
        CMD_DEBUG,
        CMD_DUPLICATE,
        CMD_EXTRACT,
        CMD_FILE,
        CMD_GET,
        CMD_IMAGE_QUERY,
        CMD_INFO,
        CMD_LOGIN,
        CMD_LS,
        CMD_MKDIR,
        CMD_MKFILE,
        CMD_OPEN,
        CMD_PARENTS,
        CMD_PASTE,
        CMD_PUT,
        CMD_RENAME,
        CMD_RESIZE,
        CMD_SEARCH,
        CMD_TREE,
        CMD_UPLOAD,
        CMD_ZIPDL,
        COMM,
        CONNECTOR_SERVE_CMD,
        CONTACT_DETAIL,
        CONTACT_EDIT,
        CRYP_SERVER,
        CRYPT,
        DETAIL_HTM,
        DEVELOPER_DIALOG,
        EDIT_GROUP,
        EXPORT_VCF,
        FILE_OBJ,
        FINDER_ACTIVITY,
        GDA,
        GLA,
        GROUP_COMMS,
        IMPORT_VCF,
        IN_APP_PAYMENT,
        INFO,
        JSON,
        LICENSE_MANAGER,
        LICENSE_UTIL,
        LIST_HTM,
        LOCK_ACTIVITY,
        LOGIN_HTM,
        MIME_UTIL,
        MY_GROUPS,
        MY_WEB_VIEW_CLIENT,
        NANOHTTPD,
        NDEF_READER,
        NFC_ACTIVITY,
        NFC_SESSION,
        OMNI,
        OMNI_FILE,
        OMNI_FILES,
        OMNI_IMAGE,
        OMNI_ZIP,
        PASSWORD,
        PERSIST,
        REST,
        RESTORE_DB,
        SECURE,
        SEND_SMS,
        SERVE_CMD,
        SETTINGS,
        SETTINGS_ACTIVITY,
        SERVER_FRAGMENT,
        SIZE,
        SQL_FULL_SYNC_SRC,
        SQL_FULL_SYNC_TARGET,
        SQL_INC_SYNC_SRC,
        SQL_INC_SYNC_TARGET,
        SQL_SYNC_TEST,
        SQLCIPHER,
        SSL_UTIL,
        SYNC_REST,
        USER_MANAGER,
        UTIL,
        VOLLEY,
        WEB_FRAGMENT,
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
