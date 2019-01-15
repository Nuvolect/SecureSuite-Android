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

package com.nuvolect.securesuite.util;//

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.nuvolect.securesuite.main.App;
import com.nuvolect.securesuite.main.CConst;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * Omni provides a layer between the app and native file utilities.
 * It allows common code to interact with volumes of different types such
 * as clear text volumes, encrypted volumes and (future) network volumes.
 * It provides methods that can operate between clear-text and encrypted files.
 * It provides an abstraction for the root path of a file system.
 *
 * A physical file path is derived from two parts, a volumeId and a path.
 * The volumeId represents root of the path upto the root '/';
 * The path is appended to the root to make the full physical path.
 *
 * A volume hash has two parts, the volume ID follwed by a path with
 * '_' underscore separator.
 *
 * The volumeId is one or more alpha characters followed by one more more
 * integer digits. When combined with a hashed path, the volume ID is followed
 * by an underscore '_' character.
 *
 * A volume can be an entire disk or a sub-tree inside a filesystem.
 * The volume root can be hidden and inaccessable from the user interface.
 *
 * For example on Linux or OSX userVolumeId "u0" can be positioned at:
 *     /Users/auser/.deep_dive/
 * The path /com.company.appname combined with the volume root makes:
 *     /Users/auser/.deep_dive/com.company.appname
 * Browsing the filesystem the user only sees /com.company.appname.
 *
 * On Android
 *   o localVolume is external storage and often on the sdcard
 *   o userVolume is maintained inside the app and is private to the app.
 *   o cryptoVolume is local to the app like the userVolume, but it is encrypted with IOCipher.
 *
 * Class organization
 * Omni.java - This is the base class for the Omni system with a few utilities to access Omni member data
 * OmniFile - Analogise to java 'File' class
 * OmniFiles - Analogise to java 'Files' class
 * OmniHash - Utilities dealing with encoding and decoding hashes
 * OmniImage - Utilities specific to imaging functions
 * OmniZip - Utilities for zip archive management
 * OmniUtil - All other Omni utilities
 */
public class Omni {

    private static JSONObject volRoot; // key: vId, value: path to root, ending in '/'
    private static JSONObject volName; // key: vId, value: volume name
    private static JSONObject volHash; // key: vHash, value: vId

    public static String localVolumeId = "l0"; // Local Volume 0, sdcard on Android, root on Linux
    public static String userVolumeId_0 = "u0"; // User Volume 0, restricted to user file
    public static String userVolumeId_1 = "u1"; // User Volume 1, restricted to user file
    public static String cryptoVolumeId = "c0"; // Encrypted Volume 0
    public static String externalVolumeId = "x0"; // Removable volume 0
    public static String APP_STORAGE_NAME_PREFIX = "app_";
    private static List<String> activeVolumeIds;
    public static String localRoot;
    public static String userRoot_0;
    public static String crypRoot;
    public static final String THUMBNAIL_FOLDER_PATH = "/.tmb/";

    /**
     * Build data structures for managing volumes.
     * Probe the device to get current volumes.
     * @param ctx
     */
    public static boolean init(Context ctx) {

        // Create if necessary the main application encryption key.
        KeystoreUtil.createKeyNotExists( ctx, CConst.APP_KEY_ALIAS);

        activeVolumeIds = new ArrayList<String>();

        if( App.hasPermission( WRITE_EXTERNAL_STORAGE)) {

            activeVolumeIds.add( localVolumeId);
        }else{
            String s = "Access to local storage denied";
            if( ! LogUtil.DEBUG)
                Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show();
            LogUtil.log( LogUtil.LogType.OMNI, s + " : "+localVolumeId);
        }
        activeVolumeIds.add( userVolumeId_0);

        /**
         * Create the virtual file system and keep a reference too it.
         */
        boolean cryptoMounted = false;

        try {
            cryptoMounted = CryptoVolume.mountStorage( ctx);
        } catch (Exception e) {
            LogUtil.logException( LogUtil.LogType.OMNI, e);
        }

        /**
         * Each root starts and ends with SLASH
         */
        crypRoot = CConst.ROOT;
        localRoot = Environment.getExternalStorageDirectory().getAbsolutePath()+"/";
        userRoot_0 = ctx.getApplicationInfo().dataDir+"/omni/";

        boolean success = true;
        volRoot = new JSONObject();
        volName = new JSONObject();
        volHash = new JSONObject();

        try {
            volRoot.put( localVolumeId,  localRoot);
            volRoot.put( userVolumeId_0, userRoot_0);

            volName.put( localVolumeId,  "sdcard");
            volName.put( userVolumeId_0, APP_STORAGE_NAME_PREFIX +"0");

            volHash.put( localVolumeId  + "_" + OmniHash.encode( CConst.ROOT), localVolumeId);
            volHash.put( userVolumeId_0 + "_" + OmniHash.encode( CConst.ROOT), userVolumeId_0);

            if( cryptoMounted){

                volRoot.put( cryptoVolumeId, crypRoot);
                volName.put( cryptoVolumeId, "crypto");
                volHash.put( cryptoVolumeId + "_" + OmniHash.encode( CConst.ROOT), cryptoVolumeId);
                activeVolumeIds.add( cryptoVolumeId);
            }

            JSONArray privateStorage = AppStorage.getAppStorage(ctx);
            if( privateStorage.length() > 1){

                JSONObject priv_1 = privateStorage.getJSONObject(1);
                String userRoot_1 = priv_1.getString( "path")+"/omni/";
                volRoot.put( userVolumeId_1, userRoot_1);
                volName.put( userVolumeId_1, APP_STORAGE_NAME_PREFIX+"1");
                volHash.put( userVolumeId_1 + "_" + OmniHash.encode( CConst.ROOT), userVolumeId_1);

                activeVolumeIds.add( userVolumeId_1);
            }

            File removableStorage = StorageUtil.getRemovableStorage( ctx);
            if( removableStorage != null) {

                volRoot.put( externalVolumeId, removableStorage.getAbsolutePath()+"/");
                volName.put( externalVolumeId, "ext");
                volHash.put( externalVolumeId + "_" + OmniHash.encode( CConst.ROOT), externalVolumeId);
                activeVolumeIds.add( externalVolumeId);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            success = false;
        }

        /**
         * Iterate over writable volumes and create thumbnail folders
         */
        for( String volumeId : activeVolumeIds){

            OmniFile root = new OmniFile( volumeId, CConst.ROOT);
            if( ! root.canWrite())
                continue;

            OmniFile f = new OmniFile( volumeId, THUMBNAIL_FOLDER_PATH);
            boolean folderCreated = f.mkdirs();
            if( folderCreated)
                LogUtil.log( LogUtil.LogType.OMNI, "Thumbnail folder created: "+volumeId + f.getPath());
            else
                LogUtil.log( LogUtil.LogType.OMNI, "Thumbnail folder exists: " +volumeId+ f.getPath());

            if( ! f.exists()){
                success = false;
                break;
            }
        }

        return success;
    }

    /**
     * Return a string array of active volume Ids.
     * @return
     */
    public static String[] getActiveVolumeIds() {

        String[] stringArray = new String[ activeVolumeIds.size()];
        stringArray = activeVolumeIds.toArray( stringArray);

        return stringArray;
    }

    /**
     * Test if a volumeId is among the active volume IDs
     * @param volumeId
     * @return
     */
    public static boolean isActiveVolume( String volumeId){

        if( volumeId == null || volumeId.isEmpty())
            return false;

        for( String vol : activeVolumeIds) {

            if( vol.contentEquals( volumeId))
                return true;
        }
        return false;
    }

    /**
     * Test of the uri references a managed volume.
     *
     * @param uri
     * @return
     */
    public static boolean matchVolume(String uri) {

        String segments[] = uri.split("_");
        String part1 = segments[0] + "_";

        // Strip off leading slash if there is one
        String possibleVolumeId = part1;
        if( possibleVolumeId.startsWith("/"))
            possibleVolumeId = part1.substring(1);

        return isActiveVolume( possibleVolumeId);
    }

    /**
     * <pre>
     * Get the root path of a volume terminated with '/'.
     * Examples:
     * l0 /storage/emulated/0/
     * c0 /
     * @param volumeId
     * @return
     * </pre>
     */
    public static String getRoot(String volumeId) {
        try {
            return volRoot.getString(volumeId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "volumeId error";
    }

    /**
     * Return the volumeId of a hash if it has one, otherwise
     * return an empty string;
     * @param hash
     * @return
     */
    public static String getVolumeId(String hash) {

        String segments[] = hash.split("_");
        if( segments.length == 0)
            return "";

        String volumeId = segments[0];

        /**
         * Identify the volumeId by testing if it is a key in the map.
         */
        if( isActiveVolume( volumeId))
            return volumeId;
        else
            return "";
    }

    public static String getVolumeName( String volumeId){

        try {
            return volName.getString( volumeId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "bad volumeId";
    }

    public static InputStream getFileInputStreamFromHash(String hash) {

        OmniFile file = new OmniFile(hash);
        return file.getFileInputStream();
    }

    /**
     * Determine if a volume hash is a root directory.
     * Examples for root: l0_Lw, m0_lw
     *
     * @param volumeHash : volumeId followed by the encoded short path
     * @return
     */
    public static boolean isRoot(String volumeHash) {

        return volHash.has( volumeHash);
    }

    /**
     * Determine if a file is a root directory.
     *
     * @param volumeId
     * @param path
     * @return
     */
    public static boolean isRoot(String volumeId, String path) {

        return volRoot.has( volumeId) && path.contentEquals(CConst.ROOT);
    }

    /**
     * Return volumeId of a of a mixed hash/path uri.
     * Example:
     * http://10.0.1.25:8218/l0_L3N0b3JhZ2UvZW11bGF0ZWQvMC9Eb3dubG9hZA/Download/frozen%20rose.jpg
     * returns: l0
     * @param uri
     * @return
     */
    public static String getVolumeIdFromUri(String uri) {

        String segments[] = uri.split("/");
        if( segments.length == 0 || segments[1].isEmpty())
            return "";

        /**
         * Get the volumeId from the segment and confirm it is an active volume.
         */
        String volumeId = getVolumeId(segments[1]);
        return volumeId;
    }

    /**
     * Return path of a mixed hash/path uri.
     * Examples:
     * 1. volumeId + hash + path
     *    /l0_L3N0b3JhZ2UvZW11bGF0ZWQvMC9Eb3dubG9hZA/Download/frozen%20rose.jpg
     * returns: /storage/emulated/0/Download/frozen rose.jpg
     * 2. volumeId + hash
     *    /l0_L3N0b3JhZ2UvZW11bGF0ZWQvMC9QaWN0dXJlcy9mcm96ZW4gcm9zZS5qcGc
     * returns: /storage/emulated/0/Pictures/frozen rose.jpg
     * @param uri
     * @return
     */
    public static String getPathFromUri(String uri) {

        String uriSegments[] = uri.split("/");
        if( uriSegments.length == 0)
            return "";

        String volumeHash = uriSegments[1];
        String hashSegments[] = volumeHash.split("_");

        if( hashSegments.length == 0)
            return "";

        String path;

        if( uriSegments.length > 2){

            String p2 = getVolumeId( uri );
            for( int i = 2; i < uriSegments.length; i++)
                p2 += uriSegments[i];

            path = OmniHash.decode(hashSegments[1])+ "/"+uriSegments[uriSegments.length-1];
        }
        else{

            path = OmniHash.decode(hashSegments[1]);
        }

        return path;
    }

    /**
     * Return a default volume ID, the volume that the app uses for its storage.
     * @return
     */
    public static String getDefaultVolumeId() {
        return userVolumeId_0;
    }
}
