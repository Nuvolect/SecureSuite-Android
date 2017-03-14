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
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.content.CookieHandler;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.nuvolect.securesuite.util.LogUtil.DEBUG;
import static com.nuvolect.securesuite.util.LogUtil.log;
import static com.nuvolect.securesuite.webserver.RestfulHtm.COMM_KEYS.uri;

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
    private static String m_current_account;
    private static String m_current_group_id;
    public static HashMap<String, ArrayList<Long>> sessionMapSelected;
    public static HashMap<String, HashMap<Integer, String>> sessionMapGroupEdit;
    private static String EMBEDDED_HEADER_KEY = "referer";
    private static String embedded_header_value = "";
    /**
     * System wide security token.
     */
    private static String m_sec_tok = "";
    private static boolean m_serverEnabled = false;
    private Set<String> assetSet;
    private Set<String> filesSet;
    private static IHTTPSession m_session = null;

    public static boolean mAuthenticated = false;

    private static Context m_ctx;

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
        m_current_account = Cryp.getCurrentAccount();
        m_current_group_id = String.valueOf(MyGroups.getDefaultGroup(m_current_account));

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

        try {

            String [] assetArray = ctx.getResources().getAssets().list("");
            assetSet = new HashSet<String>(Arrays.asList( assetArray));

            // Manually add folder and page, /elFinder-2.1.22/ss_finder.html
            assetSet.add(CConst.ELFINDER_PAGE.substring(1));

            // Manage a set of modals generated in the app:/files folder
            String [] filesArray = ctx.getFilesDir().list();
            filesSet = new HashSet<String>(Arrays.asList( filesArray));

        } catch (IOException e) {
            LogUtil.logException(CrypServer.class, e);
        }

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

        m_session = session;

        CookieHandler cookies = session.getCookies();
        String uniqueId = cookies.read("uniqueId");

        if( uniqueId == null ){

            if( embedded_header_value.isEmpty())
                embedded_header_value = WebUtil.getServerUrl(m_ctx);

            Map<String, String> headers = session.getHeaders();

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                if( entry.getKey().startsWith( EMBEDDED_HEADER_KEY) &&
                        entry.getValue().contains( embedded_header_value)){
                    uniqueId = CConst.EMBEDDED_USER;
                    break;
                }
            }
            if( DEBUG && uniqueId == null){

                LogUtil.log(LogUtil.LogType.CRYP_SERVER, "header value mismatch: "+embedded_header_value);
                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    LogUtil.log(LogUtil.LogType.CRYP_SERVER, "header: "+entry.getKey() + ":::" + entry.getValue());
                }
            }
        }

        if( uniqueId == null) {

            uniqueId = String.valueOf(System.currentTimeMillis());
            cookies.set("uniqueId", uniqueId, 30);
        }
        /**
         * Session is authenticated when authentication is wide open or
         * session has been previously authenticated.
         */
        mAuthenticated = Cryp.getLockCode( m_ctx).isEmpty()
                || uniqueId.contentEquals(CConst.EMBEDDED_USER)
                || get(uniqueId, CConst.AUTHENTICATED, "0").contentEquals( "1");

        Method method = session.getMethod();

        Map<String, String> files = new HashMap<String, String>();
        try {
            session.parseBody(files);
        } catch (ResponseException e) {
            LogUtil.logException(CrypServer.class, e);
        } catch (IOException e) {
            LogUtil.logException(CrypServer.class, e);
        }

        Map<String, List<String>> paramsMultiple = session.getParameters();
        Map<String, String> params = new HashMap<String, String>();

        /**
         * Parameters can now have multiple values for a single key.
         * Iterate over params and copy to a HashMap<String, String>.
         * This "old way" is simple and compatible with code base.
         * Duplicate keys are made unique { key, key_2, key_3, .. key_n }
         */
        Set<String> keySet = paramsMultiple.keySet();
        for( String key : keySet){
            List<String> values = paramsMultiple.get(key);
            int n = 0;
            for( String value : values){
                if( ++n == 1){
                    params.put( key, value);
                }else{
                    params.put( key+"_"+ n, value);
                }
            }
        }

        if( method == Method.POST)
            log(LogUtil.LogType.CRYP_SERVER, method + " '" + uri + "' " + params.toString());

        String uri = session.getUri();
        params.put("uri", uri);
        params.put("queryParameterStrings", session.getQueryParameterString());

        params.put(CConst.UNIQUE_ID, uniqueId);

        log(LogUtil.LogType.CRYP_SERVER, method + " '" + uri + "' " + params.toString());

        InputStream is = null;

        try {
            if (uri == null)
                return null;

//            if (uri.endsWith(".vcf")) {
//                File file = new File( m_ctx.getFilesDir()+"/export.vcf");//FIXME hardcoded vcf name
//                is = new FileInputStream(file);
//                return new Response(Status.OK, MimeUtil.MIME_VCARD, is, -1);
//
//            } else

                if (uri.endsWith(".js")) {
                is = m_ctx.getAssets().open(uri.substring(1));
                return new Response(Status.OK, MimeUtil.MIME_JS, is, -1);

            } else if (uri.endsWith(".css")) {
                is = m_ctx.getAssets().open(uri.substring(1));
                return new Response(Status.OK, MimeUtil.MIME_CSS, is, -1);

            } else if (uri.endsWith(".map")) {
                is = m_ctx.getAssets().open(uri.substring(1));
                return new Response(Status.OK, MimeUtil.MIME_JSON, is, -1);

            } else if (uri.contentEquals("/favicon.ico")) {
                uri = "/img"+uri;
                is = m_ctx.getAssets().open(uri.substring(1));
                return new Response(Status.OK, MimeUtil.MIME_ICO, is, -1);

            } else if (uri.endsWith(".png") || (uri.endsWith(".jpg"))) {
                if( uri.startsWith("/img") || uri.startsWith("/css") || uri.startsWith("/elFinder"))
                    is = m_ctx.getAssets().open(uri.substring(1));
                else {
                    File request = new File(uri);
                    is = new FileInputStream(request);
                }
                return new Response(Status.OK, MimeUtil.MIME_PNG, is, -1);

            } else if (uri.endsWith(".ttf") || uri.endsWith("woff") || uri.endsWith("woff2")) {
                is = m_ctx.getAssets().open(uri.substring(1));
                return new Response(Status.OK, MimeUtil.MIME_WOFF, is, -1);

            } else if (! mAuthenticated && uri.startsWith("/connector")
                    && params.containsKey("cmd") && params.get("cmd").contentEquals("login")) {

                params.put(CConst.UNIQUE_ID, uniqueId);

                is = ServeCmd.process(m_ctx, params);

                return new Response(Status.OK, MimeUtil.MIME_HTML, is, -1);

            } else if (mAuthenticated && uri.startsWith("/connector")) {

                params.put(CConst.UNIQUE_ID, uniqueId);

                is = ServeCmd.process(m_ctx, params);

                return new Response(Status.OK, MimeUtil.MIME_HTML, is, -1);

            } else {

                if( uri.contentEquals("/footer.htm")){

                    is = m_ctx.getAssets().open("footer.htm");
                    return new Response(Status.OK, MimeUtil.MIME_HTML, is, -1);
                }
                /**
                 * When locked only the login page is served, regardless of the uri
                 */
                if(! mAuthenticated){

                    log(LogUtil.LogType.CRYP_SERVER, "Serving login.htm");
                    is = m_ctx.getAssets().open("login.htm");
                    return new Response(Status.OK, MimeUtil.MIME_HTML, is, -1);
                }

                /**
                 * Pages are served from /templates or from /assets.
                 * Templates are populated via {@link MiniTemplator}.
                 * Assets are served more traditionally with Angularjs.
                 */
                if ( uri.endsWith(".htm") || uri.endsWith(".html")) {

                    /**
                     * Serve /assets pages here.
                     * Fall through to serve the {@Link MiniTemplator} pages below.
                     */
                    if( assetSet.contains( uri.substring(1))){

                        log(LogUtil.LogType.CRYP_SERVER, "Serving asset page: "+uri);
                        is = m_ctx.getAssets().open(uri.substring(1));
                        return new Response(Status.OK, MimeUtil.MIME_HTML, is, -1);
                    }
//                        if( uri.startsWith("/files")){
//
//                            is = m_ctx.openFileInput(uri.substring(7));// filename starts at 7
//                        }
//                        else{
//                            is = m_ctx.getAssets().open(uri.substring(1));
//                        }
//
//                        return new Response(Status.OK, MIME_HTML, is, -1);
                }
            }

        } catch (IOException e) {
            log(LogUtil.LogType.CRYP_SERVER, "Error opening file: " + uri.substring(1));
            LogUtil.logException(CrypServer.class, e);
        }
        String generatedHtml = "";

        /**
         * Look for POST method and the case when passed the key for a new "page".
         */
        if (method == Method.POST ) {

            try {
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
            } catch (JSONException e) {
                LogUtil.logException(CrypServer.class, e);
            }
        }

        long timeStart = System.currentTimeMillis();

        /**
         * Pages served via {@link MiniTemplator}.
         * Each page is generated in-memory and presented to the browser.
         */
        if( uri.contentEquals("/list.htm") || uri.contentEquals("/")) {
            generatedHtml = ListHtm.render(m_ctx, uniqueId, params);
        }
        else {
            if( uri.contentEquals("/detail.htm")) {
                generatedHtml = DetailHtm.render(m_ctx, uniqueId, params);
            }else {
                /**
                 * Modals are generated with {@link MiniTemplator} under app:/files
                 */
                String fileCandidate = uri.substring(7);

                if (filesSet.contains( fileCandidate )) {

                    File file = new File(m_ctx.getFilesDir() +"/"+ fileCandidate);
                    try {
                        is = new FileInputStream(file);
                        return new Response(Status.OK, MimeUtil.MIME_HTML, is, -1);

                    } catch (FileNotFoundException e) {
                        LogUtil.logException(CrypServer.class, e);
                    }
                }
                else{
                    log(LogUtil.LogType.CRYP_SERVER, "Page not found: " + uri);
                }
            }
        }

        long timeElapsed = System.currentTimeMillis() - timeStart;

        log(LogUtil.LogType.CRYP_SERVER,
                "render done len: " + generatedHtml.length() + ", time (ms): " + timeElapsed);

        if( generatedHtml.startsWith("download:")){

            String fileName = generatedHtml.substring(9);// Filename starts after ':' in char 9

            try {
                File file = new File( m_ctx.getFilesDir()+CConst.VCF_FOLDER+fileName);
                is = new FileInputStream(file);

                long fileLength = file.length();
                log(LogUtil.LogType.CRYP_SERVER, "CrypServer downloading file: "+fileName);
                log(LogUtil.LogType.CRYP_SERVER, "CrypServer downloading file length: "+fileLength);

                Response response = new Response(Status.OK, MimeUtil.MIME_VCARD, is, -1);
                response.addHeader("Content-Disposition", "attachment; filename=\""+fileName+"\"");
                response.addHeader("Pragma","no-cache");
                response.addHeader("Cache-Control","no-cache, no-store, max-age=0, must-revalidate");
                response.addHeader("X-Content-Type-Options","nosniff");

                return response;

            } catch (FileNotFoundException e) {
                LogUtil.logException(CrypServer.class, e);
                log(LogUtil.LogType.CRYP_SERVER, "CrypServer download file not found: " + fileName);
            }
        }

        return  new Response(Status.OK, MimeUtil.MIME_HTML, generatedHtml);
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
     * Get session data for a unique key.
     * The default value is only referenced when the key cannot be found.
     * @param uniqueId
     * @param key
     * @return
     */
    public static String get(String uniqueId, String key) {

        String v = sessionMap.get(key+uniqueId);
        if( v == null){
            String s = "";
            if( key.equals("account"))
                s = m_current_account;
            else if (key.equals("group_id"))
                s = m_current_group_id;
            else if (key.equals("start_index"))
                s = "0";
            else if (key.equals("search"))
                s = "";

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
     * Configure the security token.
     * @param ctx
     */
    private static void initSecTok(Context ctx) {

        setSecTok(
                Cryp.get(
                        CConst.SEC_TOK,
                        Passphrase.generateRandomString(32, Passphrase.SYSTEM_MODE))
        );
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

    /**
     * Clear user credentials.
     * This will disable all network access with the exception of login.
     *
     * @param ctx
     * @return
     */
    public static boolean clearCredentials(Context ctx){

        boolean success = false;

        if( m_session != null){

            CookieHandler cookies = m_session.getCookies();
            String uniqueId = cookies.read(CConst.UNIQUE_ID);
            put(uniqueId, CConst.AUTHENTICATED, "0");

            success = true;
        }
        mAuthenticated = false;
        setSecTok( "");

        return success;
    }

    /**
     * Configure the security token and default page for a new user
     * @param ctx
     * @param uniqueId
     */
    public static void setValidUser(Context ctx, String uniqueId) {

        initSecTok( ctx);

        put(uniqueId, CConst.AUTHENTICATED, "1");

        LogUtil.log(LogUtil.LogType.CRYP_SERVER, "cookie set authenticated: "+uniqueId);
    }
}
