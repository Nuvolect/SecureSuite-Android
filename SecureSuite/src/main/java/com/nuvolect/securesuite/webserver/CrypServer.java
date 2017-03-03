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

package com.nuvolect.securesuite.webserver;//


import android.content.Context;
import android.content.Intent;

import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Passphrase;
import com.nuvolect.securesuite.util.Util;
import com.nuvolect.securesuite.webserver.connector.ServeCmd;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.nuvolect.securesuite.util.LogUtil.DEBUG;
import static com.nuvolect.securesuite.util.LogUtil.log;
import static com.nuvolect.securesuite.util.LogUtil.logException;

/**<pre>
 * Server for running webserver on a service or background thread.
 *
 * SECURITY AND AUTHENTICATION
 *
 * Three techniques are employed
 * 1. https self-signed certificate
 * 2. State/response: login allows minimal css, js files, others blocked
 * 3. Security token in header
 *
 * Each session uses a uniqueId to control security. Each session is authenticated independently.
 *
 * </pre>
 */
public class CrypServer extends NanoHTTPD {

    private static final long NOTIFY_DURATION = 3 * 1000L;  // How long to show a notification
    public static int mPort = 0;
    /**
     * Storage for session data
     */
    public static HashMap<String, String> sessionMap;
    private static String m_default_account;
    private static String m_default_group_id;
    public static HashMap<String, ArrayList<Long>> sessionMapSelected;
    public static HashMap<String, HashMap<Integer, String>> sessionMapGroupEdit;
    /**
     * System wide security token.
     */
    private static String m_sec_tok = "";
    private static boolean m_serverEnabled = false;

    public enum URI_ENUM {NIL, login_htm, logout_htm, list_htm, detail_htm, restful_htm, }
    public static URI_ENUM mCurrentPage = URI_ENUM.NIL;
    public static boolean mUnlocked;
    public static String password_modal_filename = "password_modal_filled.htm";
    public static String password_modal_apply_filename = "password_modal_apply_filled.htm";
    public static String group_edit_modal_filename = "group_edit_modal_filled.htm";

    private static Context m_ctx;

    /**
     * Common mime types for dynamic content
     */
    public static final String
            MIME_PLAINTEXT = "text/plain",
            MIME_HTML = "text/html",
            MIME_JS = "application/javascript",
            MIME_CSS = "text/css",
            MIME_PNG = "image/png",
            MIME_ICO = "image/x-icon",
            MIME_WOFF = "application/font-woff",
            MIME_DEFAULT_BINARY = "application/octet-stream",
            MIME_VCARD = "text/vcard",
            MIME_XML = "text/xml";

    Response.IStatus HTTP_OK = Response.Status.OK;

    /**
     * CrypServer constructor that is called when the service starts.
     * @param ctx
     * @param port
     */
    public CrypServer(Context ctx, int port) {
        super(port);//FUTURE allow server to bind to a named host? super(hostToBindTo, port)

        m_ctx = ctx;
        mPort = port;
        m_serverEnabled = Cryp.get(CConst.SERVER_ENABLED, CConst.FALSE).contentEquals(CConst.TRUE);

        // Initialize session data
        sessionMap =  new HashMap<String, String>();
        sessionMapSelected =  new HashMap<String, ArrayList<Long>>();
        sessionMapGroupEdit =  new HashMap<String, HashMap<Integer, String>>();
        m_default_account = Cryp.getCurrentAccount();
        m_default_group_id = String.valueOf(MyGroups.getDefaultGroup(m_default_account));

        /**
         * Configure the security token. The token is generated on first use and
         * cached in a member variable.
         */
        m_sec_tok = "";
        getSecTok(); // Configure security token and set member variable.
        /**
         * All RESTful access to this IP is blocked unless it is the companion device.
         */
        WebUtil.NullHostNameVerifier.getInstance().setHostVerifierEnabled(true);

        ListHtm.init(m_ctx);
    }

    /**
     * Check if the web server is enabled.  By default it is disabled.
     * Update the member variable used to ensure server requests are rejected
     * when the server is disabled.
     * @return
     */
    public static boolean isServerEnabled(){

        /**
         * Query the server state boolean, default to FALSE (disabled) if not defined.
         */
        m_serverEnabled = Cryp.get(CConst.SERVER_ENABLED, CConst.FALSE).contentEquals(CConst.TRUE);

        return m_serverEnabled;
    }

    public static void enableServer(Context ctx, boolean serverEnabled){

        m_serverEnabled = serverEnabled;
        Intent serverIntent = new Intent(ctx, WebService.class);

        if( serverEnabled){

            Cryp.put(CConst.SERVER_ENABLED, CConst.TRUE);
            ctx.startService(serverIntent); // Start LAN web server
        }else{
            Cryp.put(CConst.SERVER_ENABLED, CConst.FALSE);
            ctx.stopService(serverIntent); // Stop LAN web server
        }
    }

    @Override
    public Response serve(IHTTPSession session) {

        if( ! m_serverEnabled){

            return null;
        }

        Map<String, String> headers = session.getHeaders();

        CookieHandler cookies = session.getCookies();
        String uniqueId = cookies.read("uniqueId");

        if( uniqueId == null) {
            uniqueId = String.valueOf(System.currentTimeMillis());

            cookies.set("uniqueId", uniqueId, 30);
            mUnlocked = Cryp.getLockCode(m_ctx).isEmpty();

            if( mUnlocked )
                mCurrentPage = URI_ENUM.list_htm;
            else
                mCurrentPage = URI_ENUM.login_htm;

            CrypServer.put(uniqueId, "currentPage", mCurrentPage.toString());
        }else{

            mCurrentPage = URI_ENUM.valueOf(CrypServer.get(uniqueId, "currentPage"));
        }

        Method method = session.getMethod();
        Map<String, String> params = session.getParms();
        String uri = session.getUri();
        params.put("uri", uri);
        params.put("queryParameterStrings", session.getQueryParameterString());

        log(LogUtil.LogType.CRYP_SERVER, method + " '" + uri + "' " + params.toString());

        InputStream is = null;

        try {
            if (uri != null) {

                if (uri.endsWith(".vcf")) {
                    File file = new File( m_ctx.getFilesDir()+"/export.vcf");
                    is = new FileInputStream(file);
                    return new Response(HTTP_OK, MIME_VCARD, is);

                } else if (uri.endsWith(".js")) {
                    is = m_ctx.getAssets().open(uri.substring(1));
                    return new Response(HTTP_OK, MIME_JS, is);

                } else if (uri.endsWith(".css")) {
                    is = m_ctx.getAssets().open(uri.substring(1));
                    return new Response(HTTP_OK, MIME_CSS, is);

                } else if (uri.contentEquals("/favicon.ico")) {
                    uri = "/img"+uri;
                    is = m_ctx.getAssets().open(uri.substring(1));
                    return new Response(HTTP_OK, MIME_ICO, is);

                } else if (uri.endsWith(".png") || (uri.endsWith(".jpg"))) {
                    if( uri.startsWith("/img") || uri.startsWith("/css"))
                        is = m_ctx.getAssets().open(uri.substring(1));
                    else {
                        File request = new File(uri);
                        is = new FileInputStream(request);
                    }
                    // HTTP_OK = "200 OK" or HTTP_OK = Status.OK;(check comments)
                    return new Response(HTTP_OK, MIME_PNG, is);

                } else if (uri.endsWith(".ttf") || uri.endsWith("woff") || uri.endsWith("woff2")) {
                    is = m_ctx.getAssets().open(uri.substring(1));
                    // HTTP_OK = "200 OK" or HTTP_OK = Status.OK;(check comments)
                    return new Response(HTTP_OK, MIME_WOFF, is);

                } else if (uri.contentEquals("/")) {
                    mCurrentPage = URI_ENUM.valueOf(CrypServer.get(uniqueId, "currentPage"));

                } else if (uri.startsWith("/connector") && params.containsKey("cmd")) {

                    Map<String, String> files = new HashMap<String, String>();
                    try {
                        session.parseBody(files);
                    } catch (ResponseException e) {
                        e.printStackTrace();
                    }
                    String postBody = session.getQueryParameterString();
                    params.put("postBody", postBody);
                    params.put(CConst.UNIQUE_ID, uniqueId);

                    is = ServeCmd.process(m_ctx, params);

                    return new Response(HTTP_OK, MIME_HTML, is);

                }
                else {

                    if ( mCurrentPage != URI_ENUM.login_htm && (uri.endsWith(".htm") || uri.endsWith(".html"))) {

                        if( uri.startsWith("/files"))
                            is = m_ctx.openFileInput(uri.substring(7));// filename starts at 7
                        else
                            is = m_ctx.getAssets().open(uri.substring(1));

                        return new Response(HTTP_OK, MIME_HTML, is);
                    }
                }
            }

        } catch (IOException e) {
            log(LogUtil.LogType.CRYP_SERVER, "Error opening file: " + uri.substring(1));
            e.printStackTrace();
        }
        String generatedHtml = "";

        /**
         * Look for POST method and the case when passed the key for a new "page".
         */
        if (method == Method.POST ) {

            try {
                Map<String, String> files = new HashMap<String, String>();
                session.parseBody(files);

                if( DEBUG) {
                    log(LogUtil.LogType.CRYP_SERVER, "POST params: " + Util.trimAt(params.toString(), 50));
                }

                Set<String> fileSet = files.keySet();
                if( ! fileSet.isEmpty()){

                    JSONObject jsonObject = new JSONObject();

                    for( String key : fileSet){

                        String filePath = files.get(key);
                        String fileName = params.get(key);
                        jsonObject.put(fileName, filePath);
                        log(LogUtil.LogType.CRYP_SERVER, "POST file jsonObject: "+jsonObject.toString());
                    }
                    /**
                     * Save filenames and paths into params for processing by specific page
                     */
                    params.put(CConst.FILE_UPLOAD, jsonObject.toString());

                }
            } catch (IOException e) {
                e.printStackTrace();
                logException(m_ctx, LogUtil.LogType.CRYP_SERVER, e);
            } catch (ResponseException e) {
                e.printStackTrace();
                logException(m_ctx, LogUtil.LogType.CRYP_SERVER, e);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                if( ! uri.contentEquals("/")){

                    if( uri.startsWith("/") && uri.endsWith("_htm"))
                        uri = uri.substring(1);  // Removing leading '/' if template page
                    mCurrentPage = URI_ENUM.valueOf(uri);
                }

            } catch (Exception e) {
                log(LogUtil.LogType.CRYP_SERVER, "Error unknown key: " + uri);
                LogUtil.logException(m_ctx, LogUtil.LogType.CRYP_SERVER, e);
            }
        }
        log(LogUtil.LogType.CRYP_SERVER, "rendering page: " + mCurrentPage);
        long timeStart = System.currentTimeMillis();

        switch (mCurrentPage){

            case NIL:
                break;
            case login_htm:
                if( Cryp.getLockCode(m_ctx).isEmpty()){

                    CrypServer.put(uniqueId, "currentPage", URI_ENUM.list_htm.toString());
                    generatedHtml = ListHtm.render(m_ctx, uniqueId, params);
                }
                else {
                    CrypServer.put(uniqueId, "currentPage", URI_ENUM.login_htm.toString());
                    generatedHtml = LoginHtm.render(m_ctx, uniqueId, params);
                }
                break;
            case logout_htm:
                CrypServer.put(uniqueId, "currentPage", mCurrentPage.toString());
                generatedHtml = LogoutHtm.render(m_ctx, params);
                break;
            case list_htm:
                CrypServer.put(uniqueId, "currentPage", mCurrentPage.toString());
                generatedHtml = ListHtm.render(m_ctx, uniqueId, params);
                break;
            case detail_htm:
                CrypServer.put(uniqueId, "currentPage", mCurrentPage.toString());
                generatedHtml = DetailHtm.render(m_ctx, uniqueId, params);
                break;
            case restful_htm: {

                /**
                 * If it is a setup page, skip the security token check.
                 * This can only be done when host verification is disabled.
                 */
                boolean hostVerifierDisabled = ! WebUtil.NullHostNameVerifier.getInstance().m_hostVerifierEnabled;
                if( hostVerifierDisabled
                        && (params.containsKey(RestfulHtm.COMM_KEYS.register_companion_device.toString())
                        || params.containsKey(RestfulHtm.COMM_KEYS.companion_ip_test.toString()))){

                    log(LogUtil.LogType.CRYP_SERVER, "sec_tok test skipped");
                    generatedHtml = RestfulHtm.render(m_ctx, uniqueId, params);
                }
                else {

                    String sec_tok = headers.get(CConst.SEC_TOK);
                    if( sec_tok.contentEquals( m_sec_tok)){

                        log(LogUtil.LogType.CRYP_SERVER, "sec_tok match");
                        generatedHtml = RestfulHtm.render(m_ctx, uniqueId, params);
                    }
                    else
                        log(LogUtil.LogType.CRYP_SERVER, "sec_tok ERROR");
                }
                break;
            }
        }

        long timeElapsed = System.currentTimeMillis() - timeStart;

        log(LogUtil.LogType.CRYP_SERVER,
                "render done len: " + generatedHtml.length() + ", time (ms): " + timeElapsed);

        if( generatedHtml.startsWith("download:")){

            String fileName = generatedHtml.substring(9);// Filename starts after ':' in char 9

            try {
                log(LogUtil.LogType.CRYP_SERVER, "CrypServer downloading file: "+fileName);

                File file = new File( m_ctx.getFilesDir()+"/export.vcf");
                is = new FileInputStream(file);

                long fileLength = file.length();
                log(LogUtil.LogType.CRYP_SERVER, "CrypServer downloading file length: "+fileLength);

                Response response = new Response(HTTP_OK, MIME_VCARD, is);
                response.addHeader("Content-Disposition", "attachment; filename=\""+fileName+"\"");
                response.addHeader("Pragma","no-cache");
                response.addHeader("Cache-Control","no-cache, no-store, max-age=0, must-revalidate");
                response.addHeader("X-Content-Type-Options","nosniff");

                return response;

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                log(LogUtil.LogType.CRYP_SERVER, "CrypServer download file not found: " + fileName);
            }
        }

        return  new Response(Response.Status.OK, MIME_HTML, generatedHtml);
    }

    /**
     * Get session data for a unique key with default
     * @param uniqueId
     * @param key
     * @param defaultString
     * @return
     */
    public static String get(String uniqueId, String key, String defaultString) {

        String v = sessionMap.get(key+uniqueId);
        if( v == null){
            v = defaultString;
            sessionMap.put(key + uniqueId, defaultString);
        }
        if( key.contains("file_upload_modal"))
            log(LogUtil.LogType.CRYP_SERVER, "get key file_upload_modal, length: "+v.length());

        return v;
    }

    /**
     * Get session data for a unique key.  The default value is only referenced
     * when the key cannot be found.
     * @param uniqueId
     * @param key
     * @return
     */
    public static String get(String uniqueId, String key) {

        String v = sessionMap.get(key+uniqueId);
        if( v == null){
            String s = "";
            if( key.equals("account"))
                s = m_default_account;
            else if (key.equals("group_id"))
                s = m_default_group_id;
            else if (key.equals("start_index"))
                s = "0";
            else if (key.equals("search"))
                s = "";
            else if (key.equals("currentPage")) {
                if (mUnlocked)
                    s = URI_ENUM.list_htm.toString();
                else
                    s = URI_ENUM.login_htm.toString();
            }

            sessionMap.put(key + uniqueId, s);
            return s;
        }else{
            return v;
        }
    }

    /**
     * Get array list of selected long IDs from session data.
     * The list is unique to each session/user.
     * @param uniqueId
     * @return
     */
    public static ArrayList<Long> getSelectId(String uniqueId) {

        ArrayList<Long> arrayList = sessionMapSelected.get(uniqueId);
        if( arrayList == null)
            arrayList = new ArrayList<Long>();

        return arrayList;
    }

    /**
     * Save hash map of group edit plan elements
     * The list is unique to each session/user.
     * @param uniqueId
     * @param plan
     */
    public static void putGroupEdit(String uniqueId, HashMap<Integer, String> plan) {

        sessionMapGroupEdit.put( uniqueId, plan);
    }
    /**
     * Get hash map of group edit plan from session data.
     * The list is unique to each session/user.
     * @param uniqueId
     * @return
     */
    public static HashMap<Integer, String> getGroupEdit(String uniqueId) {

        HashMap<Integer, String> hashMap = sessionMapGroupEdit.get(uniqueId);
        if( hashMap == null)
            hashMap = new HashMap<Integer, String>();

        return hashMap;
    }

    /**
     * Save ArrayList of selected long IDs into session data.
     * The list is unique to each session/user.
     * @param uniqueId
     * @param selected
     */
    public static void putSelectId(String uniqueId, ArrayList<Long> selected) {

        sessionMapSelected.put( uniqueId, selected);
    }

    /**
     * Save session data to the hashmap
     * @param uniqueId
     * @param key
     * @param value
     */
    public static void put(String uniqueId, String key, String value){

        if( key.contains("file_upload_modal"))
            log(LogUtil.LogType.CRYP_SERVER, "put key file_upload_modal, length: "+value.length());

        sessionMap.put(key + uniqueId, value);
    }

    /**
     * Save session data to the hashmap
     * @param uniqueId
     * @param key
     * @param intValue
     */
    public static void put(String uniqueId, String key, int intValue){

        sessionMap.put(key + uniqueId, String.valueOf(intValue));
    }
    /**
     * Save session data to the hashmap
     * @param uniqueId
     * @param key
     * @param longValue
     */
    public static void put(String uniqueId, String key, long longValue){

        sessionMap.put(key + uniqueId, String.valueOf(longValue));
    }

    /**
     * Save a notification message.  The getNotify method is called to get the
     * javaScript call that is injected into each page.
     * @param uniqueId
     * @param message
     * @param type {base, success, info, warn, error}  color of the warning
     */
    public static void notify(String uniqueId, String message, String type) {

        String js = "notifyJs('"+message+"','"+type+"');";
        CrypServer.put(uniqueId, "notify_js", js);

        long msgExpire = System.currentTimeMillis()+ NOTIFY_DURATION;
        CrypServer.put(uniqueId, "notify_js_duration", msgExpire);
    }

    /**
     * Return a notification message.  If the message duration has expired, clear the
     * message from the system and return an empty string.
     * @param uniqueId
     * @return
     */
    public static String getNotify(String uniqueId){

        String js = CrypServer.get(uniqueId, "notify_js");
        if( js.isEmpty())
            return "";

        long msgExpire = Long.valueOf(CrypServer.get(uniqueId, "notify_js_duration"));
        if( msgExpire > System.currentTimeMillis())
            return js;
        else {
            CrypServer.put(uniqueId, "notify_js", "");
            return "";
        }
    }
    /**
     * Get the system wide security token
     * @return
     */
    public static String getSecTok() {

        if( m_sec_tok.isEmpty())
            m_sec_tok = Cryp.get(CConst.SEC_TOK, Passphrase.generateRandomString(32, Passphrase.SYSTEM_MODE));

        return m_sec_tok;
    }

    /**
     * Set the system wide security token.
     * @param sec_tok String
     */
    public static void setSecTok(String sec_tok) {

        m_sec_tok = sec_tok;
        Cryp.put(CConst.SEC_TOK, sec_tok);
    }
}
