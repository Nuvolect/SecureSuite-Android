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
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.util.OmniHash;
import com.nuvolect.securesuite.util.Passphrase;
import com.nuvolect.securesuite.util.Util;
import com.nuvolect.securesuite.webserver.connector.CmdUpload;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.nuvolect.securesuite.util.LogUtil.DEBUG;
import static com.nuvolect.securesuite.util.LogUtil.log;
import static com.nuvolect.securesuite.webserver.MimeUtil.MIME_GIF;
import static com.nuvolect.securesuite.webserver.MimeUtil.MIME_ICO;
import static com.nuvolect.securesuite.webserver.MimeUtil.MIME_JPG;
import static com.nuvolect.securesuite.webserver.MimeUtil.MIME_JSON;
import static com.nuvolect.securesuite.webserver.MimeUtil.MIME_PNG;
import static com.nuvolect.securesuite.webserver.MimeUtil.MIME_TTF;
import static com.nuvolect.securesuite.webserver.MimeUtil.MIME_WOFF;
import static java.util.Locale.US;

/**<pre>
 * Server for running webserver on a service or background thread.
 *
 * Two types of pages are served, traditional html/angular and MiniTemplator.class pages.
 *
 * Services are guided by a set of rules
 *   without authentication
 *      assets: { js, css, map, png, jpg, ico, ttf, woff, woff2}
 *      assets: { login.htm, footer.htm}
 *      REST: { admin?cmd=login}
 *   with authentication
 *      MiniTemplator: { list.htm, detail.htm}
 *      assets: { logout.htm, navbar.htm, developer.htm, crypto_performance.htm, keystore.htm}
 *      files: { password_modal*.htm, group_edit_modal*.htm}
 *      REST: { connector?cmd=* }
 *      REST: { admin?cmd=* }
 *      REST: { sync?* }
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

    private static Context m_ctx;
    private static final long NOTIFY_DURATION = 3 * 1000L;  // How long to show a notification
    public static int m_port = 0;
    private static String m_serverUrl;
    /**
     * Storage for session data
     */
    public static HashMap<String, String> m_sessionMap;
    private static String m_current_account;
    private static String m_current_group_id;
    public static HashMap<String, ArrayList<Long>> m_sessionMapSelected;
    public static HashMap<String, HashMap<Integer, String>> m_sessionMapGroupEdit;
    private static String EMBEDDED_HEADER_KEY = "referer";
    private static String embedded_header_value = "";
    /**
     * System wide security token.
     */
    private static String m_sec_tok = "";
    private static boolean m_serverEnabled = false;
    private static IHTTPSession m_session = null;
    public static boolean mAuthenticated = false;

    private enum EXT {
        js, css, map, png, jpg, gif, ico, ttf, woff, woff2, invalid, htm, html,
        // RESTFull services
        admin, calendar, connector, sync, omni,
    }

    private enum PAGE {
        // Served from /files, generated with MiniTemplate
        list,
        detail,
        password_modal_filled,
        password_modal_apply_filled,
        group_edit_modal_filled,
        // Served from assets
<<<<<<< Updated upstream
=======
        calendar,
        event_edit,
        spa,
>>>>>>> Stashed changes
        crypto_performance,
        developer,
        footer,
        keystore,
        login,
        logout,
        navbar,
        finder,

        invalid,
    }

    /**
     * CrypServer constructor that is called when the service starts.
     * @param ctx
     * @param port
     */
    public CrypServer(Context ctx, int port) {
        super(port);//FUTURE allow server to bind to a named host? super(hostToBindTo, port)

        m_ctx = ctx;
        m_port = port;
        m_serverUrl = WebUtil.getServerUrl(m_ctx);
        m_serverEnabled = Cryp.get(CConst.SERVER_ENABLED, CConst.FALSE).contentEquals(CConst.TRUE);

        // Initialize session data
        m_sessionMap =  new HashMap<String, String>();
        m_sessionMapSelected =  new HashMap<String, ArrayList<Long>>();
        m_sessionMapGroupEdit =  new HashMap<String, HashMap<Integer, String>>();
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
        Map<String, String> headers = session.getHeaders();
        String uniqueId = cookies.read(CConst.UNIQUE_ID);

        if( uniqueId == null ){

            if( embedded_header_value.isEmpty())
                embedded_header_value = WebUtil.getServerUrl(m_ctx);

            for (Map.Entry<String, String> entry : headers.entrySet()) {

                if( entry.getKey().startsWith( EMBEDDED_HEADER_KEY) &&
                        entry.getValue().contains( embedded_header_value)){
                    uniqueId = CConst.EMBEDDED_USER;
                    break;
                }
            }
            if( DEBUG && uniqueId == null){

                LogUtil.log(LogUtil.LogType.CRYP_SERVER, "header value mismatch: "+embedded_header_value);
                for (Map.Entry<String, String> entry : headers.entrySet()) {

                    LogUtil.log(LogUtil.LogType.CRYP_SERVER, "header: "+entry.getKey() + ":::" + entry.getValue());
                }
            }
        }

        if( uniqueId == null) {

            uniqueId = String.valueOf(System.currentTimeMillis());
            cookies.set( CConst.UNIQUE_ID, uniqueId, 30);
        }
        /**
         * Session is authenticated when authentication is wide open or
         * session has been previously authenticated.
         */
        mAuthenticated = Cryp.getLockCode( m_ctx).isEmpty()
                || uniqueId.contentEquals(CConst.EMBEDDED_USER)
                || get(uniqueId, CConst.AUTHENTICATED, "0").contentEquals( "1");

        Method method = session.getMethod();
        Map<String, List<String>> paramsMultiple = session.getParameters();
        Map<String, String> params = new HashMap<String, String>();


        /**
         * Get files associated with a POST method
         */
        Map<String, String> files = new HashMap<String, String>();
        try {
            session.parseBody(files);
        } catch (ResponseException e) {
            LogUtil.logException(CrypServer.class, e);
        } catch (IOException e) {
            LogUtil.logException(CrypServer.class, e);
        }
        /**
         * {
         *    "data": {
         *        "EventID": 0,
         *        "StartAt": "2017/04/13 12:00 AM",
         *        "EndAt": "2017/04/14 12:00 AM",
         *        "IsFullDay": false,
         *        "Title ": "Sample title",
         *        "Description": "Something about the event"
         *    }
         * }
         */
        if( method.equals( Method.POST) && files.size() > 0){

            if( files.containsKey("postData")){

                try {
                    JSONObject postData = new JSONObject( files.get("postData"));
                    JSONObject data = postData.getJSONObject("data");
                    params.put("data", data.toString());

                    Iterator<String> keys = data.keys();

                    while ( keys.hasNext()){

                        String key = keys.next();
                        String value = data.getString( key);
                        params.put( key, value );
                    }
                } catch (JSONException e) {
                    LogUtil.logException(CrypServer.class, e);
                }
            }
        }

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

        String uri = session.getUri();
        params.put(CConst.URI, uri);
        params.put(CConst.URL, m_serverUrl);
        params.put("queryParameterStrings", session.getQueryParameterString());

        params.put(CConst.UNIQUE_ID, uniqueId);

        log(LogUtil.LogType.CRYP_SERVER, method + " '" + uri + "' " + params.toString());

        InputStream is = null;
        EXT ext = null;

        String fileExtension = FilenameUtils.getExtension( uri ).toLowerCase(US);
        if( fileExtension.isEmpty()){
            if( uri.contentEquals("/")){
                ext = EXT.htm;
                if( mAuthenticated)
                    uri = "/list.htm";
                else
                    uri = "/login.htm";
            }else{
                ext = determineServiceEnum( uri );
            }
        }
        else{
            try {
                ext = EXT.valueOf( fileExtension );
            } catch (IllegalArgumentException e) {
                log(LogUtil.LogType.CRYP_SERVER, "ERROR invalid extension "+ uri + fileExtension);
                ext = EXT.invalid;
            }
        }

        try {

            if (uri == null)
                return null;

            switch (ext) {

                case js:
                    is = m_ctx.getAssets().open(uri.substring(1));
                    return new Response(Status.OK, MimeUtil.MIME_JS, is, -1);
                case css:
                    is = m_ctx.getAssets().open(uri.substring(1));
                    return new Response(Status.OK, MimeUtil.MIME_CSS, is, -1);
                case map:
                    is = m_ctx.getAssets().open(uri.substring(1));
                    return new Response(Status.OK, MIME_JSON, is, -1);
                case png:
                    if( uri.startsWith("/img") || uri.startsWith("/css") || uri.startsWith("/elFinder")){

                        is = m_ctx.getAssets().open(uri.substring(1));
                        return new Response(Status.OK, MIME_PNG, is, -1);
                    }
                    else if ( uri.startsWith("/files/")){
                        String fileName = FilenameUtils.getName( uri);
                        File file = new File( m_ctx.getFilesDir() + "/" + fileName);
                        is = new FileInputStream(file);
                        return new Response(Status.OK, MIME_PNG, is, -1);
                    }
                    log(LogUtil.LogType.CRYP_SERVER, "ERROR not found: "+uri);
                    return new Response(Status.NOT_FOUND, MIME_PLAINTEXT, "Not found: " + uri);
                case jpg:
                    is = m_ctx.getAssets().open(uri.substring(1));
                    return new Response(Status.OK, MIME_JPG, is, -1);
                case gif:
                    is = m_ctx.getAssets().open(uri.substring(1));
                    return new Response(Status.OK, MIME_GIF, is, -1);
                case ico:
                    is = m_ctx.getAssets().open(uri.substring(1));
                    return new Response(Status.OK, MIME_ICO, is, -1);
                case ttf:
                    is = m_ctx.getAssets().open(uri.substring(1));
                    return new Response(Status.OK, MIME_TTF, is, -1);
                case woff:
                case woff2:
                    is = m_ctx.getAssets().open(uri.substring(1));
                    return new Response(Status.OK, MIME_WOFF, is, -1);
                case htm:
                case html: {
                    if (uri.contentEquals("/login.htm")) {
                        log(LogUtil.LogType.CRYP_SERVER, "Serving login.htm");
                        is = m_ctx.getAssets().open("login.htm");
                        return new Response(Status.OK, MimeUtil.MIME_HTML, is, -1);
                    }
                    if (mAuthenticated) {

                        return serveAuthenticatedHtml(uri, uniqueId, params);
                    } else {
                        return new Response(Status.UNAUTHORIZED, MIME_PLAINTEXT, "Invalid authentication: " + uri);
                    }
                }
                case omni:{
                    String mime = "";
                    OmniFile omniFile = new OmniFile( uri);
                    if( omniFile.getPath().startsWith(CConst.TMB_FOLDER)){
                        /**
                         * Request for a thumbnail file.
                         * The file name is hashed and mime type is png.
                         */
                        mime = MIME_PNG;
                    }
                    else
                        mime = omniFile.getMime();

                    is = omniFile.getFileInputStream();
                    return new Response(Status.OK, mime, is, -1);
                }
                case admin: {
                    /**
                     * GET/POST /admin?cmd=login works with or without validation.
                     * All other REST services require authentication.
                     */
                    if (params.containsKey("cmd") && params.get("cmd").contentEquals("login")) {

                        is = com.nuvolect.securesuite.webserver.admin.ServeCmd.process(m_ctx, params);
                        return new Response(Status.OK, MIME_JSON, is, -1);
                    }
                }
                case calendar:
                case connector:
                case sync: {
                        if ( passSecurityCheck( uri, headers )) {

                            switch (ext) {
                                case admin:
                                    is = com.nuvolect.securesuite.webserver.admin.ServeCmd.process(m_ctx, params);
                                    return new Response(Status.OK, MIME_JSON, is, -1);
                                case calendar:{
                                    String json = CalendarRest.process(m_ctx, params);
                                    return new Response(Status.OK, MIME_JSON, json);
                                }
                                case connector:{

                                    String mime = MIME_JSON;
                                    if( params.get("cmd").contentEquals("upload")){
                                        loadUploadParams( files, params);
                                    }
                                    else if( params.get("cmd").contentEquals("file")){

                                        OmniFile omniFile = new OmniFile( params.get("target") );
                                        mime = omniFile.getMime();
                                    }
                                    is = com.nuvolect.securesuite.webserver.connector.ServeCmd.process(m_ctx, params);
                                    return new Response(Status.OK, mime, is, -1);
                                }
                                case sync:
                                    String json = SyncRest.process(m_ctx, params);
                                    return new Response(Status.OK, MIME_PLAINTEXT, json);
                            }
                        } else {
                            /**
                             * The security token can be temporarily disabled during companion pairing.
                             */
                            boolean hostVerifierDisabled = !WebUtil.NullHostNameVerifier.getInstance().m_hostVerifierEnabled;
                            if (ext == EXT.sync && hostVerifierDisabled &&  params.containsKey(CConst.CMD)
                                    && (params.get(CConst.CMD).contentEquals(SyncRest.CMD.register_companion_device.toString())
                                    || params.get(CConst.CMD).contentEquals(SyncRest.CMD.companion_ip_test.toString()))) {

                                log(LogUtil.LogType.CRYP_SERVER, "sec_tok test skipped");
                                String json = SyncRest.process(m_ctx, params);
                                return new Response(Status.OK, MIME_PLAINTEXT, json);
                            } else {

                                log(LogUtil.LogType.CRYP_SERVER, "Authentication ERROR: "+params);
                                return new Response(Status.UNAUTHORIZED, MIME_PLAINTEXT, "Authentication error: " + uri);
                            }
                    }
                }
                case invalid:
                    log(LogUtil.LogType.CRYP_SERVER, "ERROR invalid extension " + uri);
                    return new Response(Status.NOT_ACCEPTABLE, MIME_PLAINTEXT, "Invalid request " + uri);
                default:
                    log(LogUtil.LogType.CRYP_SERVER, "ERROR unmanaged extension " + ext);
                    return new Response(Status.NOT_FOUND, MIME_PLAINTEXT, "Not found: " + uri);
            }

        } catch ( Exception e) {
            log(LogUtil.LogType.CRYP_SERVER, "ERROR exception " + uri);
            LogUtil.logException(CrypServer.class, e);
        }
        return new Response(Status.NOT_FOUND, MIME_PLAINTEXT, "Unmanaged request: " + uri);
    }

    /**
     * Load paramameters passed via POST method to be processed by {@link CmdUpload }
     * @param files
     * @param params
     */
    private void loadUploadParams(Map<String, String> files, Map<String, String> params) {

        try {
            Set<String> fileSet = files.keySet();
            JSONArray array = new JSONArray();

            if( ! fileSet.isEmpty()){

                for( String key : fileSet){

                    JSONObject jsonObject = new JSONObject();
                    String filePath = files.get(key);
                    String fileName = params.get(key);
                    jsonObject.put(CConst.FILE_PATH, filePath);
                    jsonObject.put(CConst.FILE_NAME, fileName);
                    array.put(jsonObject);
                    log(LogUtil.LogType.CRYP_SERVER, "POST file jsonObject: "+jsonObject.toString());
                }
                /**
                 * Save filenames and paths into params for processing by specific page
                 */
                params.put(CConst.POST_UPLOADS, array.toString());

                if( DEBUG) {
                    log(LogUtil.LogType.CRYP_SERVER, "upload params: " + Util.trimAt(array.toString(), 50));
                }
            }
        } catch (JSONException e) {
            LogUtil.logException(CrypServer.class, e);
        }
    }

    /**
     * Perform a series of security checks for a REST service.
     * @param uri
     * @param headers
     */
    private boolean passSecurityCheck(String uri, Map<String, String> headers) {

        if( mAuthenticated)
            return true;

        if (headers.containsKey(CConst.SEC_TOK) && headers.get(CConst.SEC_TOK).contentEquals(m_sec_tok)) {
            return true;
        }

        if( headers.containsKey("referer")){

            String referer = headers.get("referer");
            String serverIpPort = WebUtil.getServerUrl(m_ctx);

            if( referer.startsWith( serverIpPort)){

                LogUtil.log(LogUtil.LogType.CRYP_SERVER, "Referer approved");
                return true;
            }else{
                LogUtil.log(LogUtil.LogType.CRYP_SERVER, "ERROR referer DENIED: "+uri);
                return false;
            }
        }
        return false;
    }

    /**
     * Convert a REST uri into an enum
     * @param uri
     * @return
     */
    private EXT determineServiceEnum(String uri) {

        if( uri.startsWith("/calendar"))
            return EXT.calendar;
        else
        if( uri.startsWith("/admin"))
            return EXT.admin;
        else
        if( uri.startsWith("/connector"))
            return EXT.connector;
        else
        if( uri.startsWith("/sync"))
            return EXT.sync;
        else
        if(OmniHash.isHash( uri))
            return EXT.omni;
        else
            return EXT.invalid;
    }

    /**
     * Serve HTML files:
     *     MiniTemplator: { list.htm, detail.htm}
     *     assets: { logout.htm, navbar.htm, developer.htm, crypto_performance.htm, keystore.htm}
     *     files: { password_modal*.htm, group_edit_modal*.htm}
     * @param uri
     * @param uniqueId
     * @param params
     */
    private Response serveAuthenticatedHtml(String uri, String uniqueId, Map<String, String> params) {

        String baseName = FilenameUtils.getBaseName( uri ).toLowerCase(US);

        log(LogUtil.LogType.CRYP_SERVER, "Serve authenticated HTML: " + baseName);

        PAGE page = null;
        try {
            page = PAGE.valueOf( baseName);
        } catch (IllegalArgumentException e) {
            log(LogUtil.LogType.CRYP_SERVER, "ERROR invalid page "+ uri + baseName);
            page = PAGE.invalid;
        }

        switch ( page ){

            /**
             * MiniTemplator pages generated real-time, served from app:/files
             */
            case list:
            case detail:{
                log(LogUtil.LogType.CRYP_SERVER, "Serving page: "+uri);
                String htmlOrJson;
                if( page == PAGE.list)
                    htmlOrJson = ListHtm.render(m_ctx, uniqueId, params);
                else
                    htmlOrJson = DetailHtm.render(m_ctx, uniqueId, params);

                if( htmlOrJson.startsWith("{")){
                    return getFileDownload( htmlOrJson);
                }
                else{
                    return new Response(Status.OK, MimeUtil.MIME_HTML, htmlOrJson);
                }
            }
            /**
             * MiniTemplator files served from app:/files, generated by listHtm and detailHtm
             */
            case password_modal_filled:
            case password_modal_apply_filled:
            case group_edit_modal_filled: {
                log(LogUtil.LogType.CRYP_SERVER, "Serving file: "+uri);
                try {
                    File file = new File(m_ctx.getFilesDir() + uri);
                    InputStream is = new FileInputStream(file);
                    return new Response(Status.OK, MimeUtil.MIME_HTML, is, -1);

                } catch (FileNotFoundException e) {
                    LogUtil.logException(CrypServer.class, e);
                }
                break;
            }
            /**
             * Pages served from assets
             */
<<<<<<< Updated upstream
=======
            case calendar:
            case event_edit:
            case spa:
>>>>>>> Stashed changes
            case crypto_performance:
            case developer:
            case footer:
            case keystore:
            case login:
            case logout:
            case navbar:
            case finder:{
                log(LogUtil.LogType.CRYP_SERVER, "Serving asset: "+uri.substring(1));
                try {
                    InputStream is = m_ctx.getAssets().open(uri.substring(1));
                    return new Response(Status.OK, MimeUtil.MIME_HTML, is, -1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }

            case invalid:
                log(LogUtil.LogType.CRYP_SERVER, "ERROR invalid page "+ uri);
                return new Response(Status.NOT_ACCEPTABLE, MIME_PLAINTEXT, "Invalid request "+uri);
            default:
                log(LogUtil.LogType.CRYP_SERVER, "ERROR unmanaged page "+ uri);
                return new Response(Status.NOT_FOUND, MIME_PLAINTEXT, "Not found: "+uri);
        }
        log(LogUtil.LogType.CRYP_SERVER, "ERROR serving page"+ uri);
        return new Response(Status.NOT_FOUND, MIME_PLAINTEXT, "Error serving page: "+uri);
    }

    private Response getFileDownload(String jsonString) {

        String fileName = "";
        long fileLength = -1;
        try {
            JSONObject object = new JSONObject( jsonString );
            if( object.getString("error").isEmpty()) {
                fileName = object.getString("filename");
                fileLength = object.getLong("length");
            }
            else
                return null;
        } catch (JSONException e) {
            e.printStackTrace();//TODO handle exception
        }

        try {
            File file = new File( m_ctx.getFilesDir()+CConst.VCF_FOLDER+fileName);
            InputStream is = new FileInputStream(file);

            log(LogUtil.LogType.CRYP_SERVER, "CrypServer downloading file: "+fileName);
            log(LogUtil.LogType.CRYP_SERVER, "CrypServer downloading file length: "+fileLength);

            Response response = new Response(Status.OK, MimeUtil.MIME_BIN, is, fileLength);
            response.addHeader("Content-Disposition", "attachment; filename=\""+fileName+"\"");
            return response;

        } catch (FileNotFoundException e) {
            LogUtil.logException(CrypServer.class, e);
            log(LogUtil.LogType.CRYP_SERVER, "CrypServer download file not found: " + fileName);
        }
        return null;//TODO Return 404?
    }

    /**
     * Get session data for a unique key with default
     * @param uniqueId
     * @param key
     * @param defaultString
     * @return
     */
    public static String get(String uniqueId, String key, String defaultString) {

        String v = m_sessionMap.get(key+uniqueId);
        if( v == null){
            v = defaultString;
            m_sessionMap.put(key + uniqueId, defaultString);
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

        String v = m_sessionMap.get(key+uniqueId);
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

            m_sessionMap.put(key + uniqueId, s);
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

        ArrayList<Long> arrayList = m_sessionMapSelected.get(uniqueId);
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

        m_sessionMapGroupEdit.put( uniqueId, plan);
    }
    /**
     * Get hash map of group edit plan from session data.
     * The list is unique to each session/user.
     * @param uniqueId
     * @return
     */
    public static HashMap<Integer, String> getGroupEdit(String uniqueId) {

        HashMap<Integer, String> hashMap = m_sessionMapGroupEdit.get(uniqueId);
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

        m_sessionMapSelected.put( uniqueId, selected);
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

        m_sessionMap.put(key + uniqueId, value);
    }

    /**
     * Save session data to the hashmap
     * @param uniqueId
     * @param key
     * @param intValue
     */
    public static void put(String uniqueId, String key, int intValue){

        m_sessionMap.put(key + uniqueId, String.valueOf(intValue));
    }
    /**
     * Save session data to the hashmap
     * @param uniqueId
     * @param key
     * @param longValue
     */
    public static void put(String uniqueId, String key, long longValue){

        m_sessionMap.put(key + uniqueId, String.valueOf(longValue));
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

        setSecTok( Cryp.get( CConst.SEC_TOK,
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