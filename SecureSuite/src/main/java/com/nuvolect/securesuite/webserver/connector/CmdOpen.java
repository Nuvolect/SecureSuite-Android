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

package com.nuvolect.securesuite.webserver.connector;//


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nuvolect.securesuite.main.App;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Omni;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.webserver.WebUtil;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import java.io.InputStream;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * <pre>
 * Returns information about requested directory and its content,
 * optionally can return directory tree as files, and options for the current volume.
 *
 * volume(root) "files" object:
 *     {
 * +     "csscls": "elfinder-navbar-root-local",
 *       "dirs": 1,
 * +     "disabled": [ "chmod" ],
 *       "hash": "l1_Lw",
 *       "isowner": false,
 *       "locked": 1,
 *       "mime": "directory",
 *       "name": "Demo",
 *       "read": 1,
 *       "size": 0,
 *       "ts": 1453299075,
 * +     "tmbUrl": "http://hypweb.net/elFinder-nightly/demo/Demo/.tmb/"
 * +     "uiCmdMap": [],
 *       "volumeid": "l1_",
 *       "write": 0,
 *     },
 * cwd object is just like a volume file with the addition of "root"
 *   "cwd":
 *     {
 *       "csscls": "elfinder-navbar-root-local",
 *       "dirs": 1,
 *       "disabled": [ "chmod" ],
 *       "hash": "l1_Lw",
 *       "isowner": false,
 *       "locked": 1,
 *       "mime": "directory",
 *       "name": "Demo",
 *       "read": 1,
 * +     "root": "l1_Lw",
 *       "size": 0,
 *       "tmbUrl": "http://hypweb.net/elFinder-nightly/demo/Demo/.tmb/",
 *       "ts": 1453299075,
 *       "uiCmdMap": [],
 *       "volumeid": "l1_",
 *       "write": 0
 *     },
 * directory "files" object:
 *     {
 * +     "dirs": 1,
 *       "hash": "l1_V2VsY29tZQ",
 * +     "icon": "http://hypweb.net/elFinder-nightly/demo/Demo/Welcome/.diricon.png",
 *       "isowner": false,
 *       "locked": 1,
 *       "mime": "directory",
 *       "name": "Welcome",
 *       "phash": "l1_Lw",
 *       "read": 1,
 *       "size": 0,
 *       "ts": 1458097231,
 * +     "volumeid": "l1_",
 *       "write": 0
 *     },
 * file "files" object:
 *     {
 *       "hash": "l1_UkVBRE1FLm1k",
 *       "isowner": false,
 *       "locked": 1,
 *       "mime": "text/x-markdown",
 *       "name": "README.md",
 *       "phash": "l1_Lw",
 *       "read": 1,
 *       "size": "3683",
 *       "ts": 1418091234,
 *       "write": 0
 *     }
 * </pre>
 */
public class CmdOpen extends ConnectorJsonCommand {

    static boolean DEBUG = LogUtil.DEBUG;

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        long startTime = System.currentTimeMillis();
        /**
         init : (true|false|not set), optional parameter.
         If true indicates that this request is an initialization request and its response must
         include the value api (number or string >= 2) and should include the options object,
         but will still work without it.

         Also, this option affects the processing of parameter target hash value.
         If init == true and target is not set or that directory doesn't exist,
         then the data connector must return the root directory of the default volume.
         Otherwise it must return error "File not found".

         target : (string) Hash of directory to open. Required if init == false or init is not set.

         tree : (true|false), optional. If true, response must contain subfolders trees of roots directories.
         */
        boolean init = params.containsKey("init") && params.get("init").contentEquals("1");

        /**
         * target : (string) Hash of directory to open. Required if init == false or init is not set
         */
        OmniFile targetFile;
        String target = params.containsKey("target") ? params.get("target") : "";

        /**
         * tree : (true|false), optional. If true, response must contain subfolders trees of roots directories.
         */
        boolean tree = params.containsKey("tree") && params.get("tree").contentEquals("1");

        String url;
        if (params.containsKey("url")) {
            url = params.get("url");
        } else {
            url = WebUtil.getServerUrl(App.getContext());
        }

        String volumeId;
        JsonObject wrapper = new JsonObject();
        JsonArray uiCmdMap = new JsonArray();

        /**
         * files : (Array) array of objects - files and directories in current directory.
         * If parameter tree == true, then added to the folder of the directory tree to a given depth.
         * The order of files is not important.
         *
         * Note you must include the top-level volume objects here as well
         * (i.e. cwd is repeated here, in addition to other volumes) Information * about File/Directory
         */
        JsonArray fileObjects;
        if (init) {
            /**
             * api : (Number) The version number of the protocol, must be >= 2.1,
             * ATTENTION - return api ONLY for init request!
             */
            wrapper.addProperty("api", "2.1");
        }

        /**
         * An empty target defaults to the root of the default volume otherwise
         * a non-empty target uses a hashed file volume and path.
         * The path starts with the volume appended with an encoded path.
         */
        if (target.isEmpty()) {
            volumeId = Omni.getDefaultVolumeId();
            targetFile = new OmniFile(volumeId, CConst.ROOT);
            if (DEBUG) {
                LogUtil.log(LogUtil.LogType.CMD_OPEN,"Target empty: " + targetFile.getPath());
            }
        } else {
            /**
             * A non-empty target is a hashed path starting with with the volume
             * followed by a encoded relative path.
             */
            targetFile = new OmniFile(target);
            volumeId = targetFile.getVolumeId();
            if (DEBUG) {
                LogUtil.log(LogUtil.LogType.CMD_OPEN,"Target: " + targetFile.getPath());
            }
        }

        /**
         * Make sure the thumbnail directory exists
         */
        new OmniFile(volumeId, Omni.THUMBNAIL_FOLDER_PATH).mkdirs();
        /**
         * Add files that are in the target directory
         *
         * files : (Array) array of objects - files and directories in current directory.
         * If parameter tree == true, then added to the folder of the directory tree to a given depth.
         * The order of files is not important.
         *
         * Note you must include the top-level volume objects here as well (i.e. cwd is repeated here,
         * in addition to other volumes) Information about File/Directory
         */
        fileObjects = targetFile.listFileObjects(url);

        /**
         * The current working directory is always a directory and never a file.
         * If the target is a file the cwd is the file's parent directory.
         */
        JsonObject cwd;
        if (targetFile.isDirectory()) {
            cwd = FileObj.makeObj(volumeId, targetFile, url);
        } else {
            cwd = FileObj.makeObj(volumeId, targetFile.getParentFile(), url);
        }

        //cwd is like a volume file with the addition of the root element
        cwd.add("root", cwd.get("hash"));
        wrapper.add("cwd", cwd);

        if (DEBUG) {
            LogUtil.log(LogUtil.LogType.CMD_OPEN,
                    "CWD  name: "+targetFile.getName()
                            +", path: "+targetFile.getPath()
                            +", hash: "+targetFile.getHash());
        }

        /**
         * Add additional file volumes
         */
        if (tree) {
            String volumeIds[] = Omni.getActiveVolumeIds();

            for (String thisVolumeId : volumeIds) {
                OmniFile thisRootFile = new OmniFile( thisVolumeId, CConst.ROOT);
                JsonObject thisRootFileObject = thisRootFile.getFileObject(url);
                // Only the root objects get this
                thisRootFileObject.addProperty("csscls", "elfinder-navbar-root-local");

                // Add the root volume
                fileObjects.add(thisRootFileObject);

                /**
                 * For each volume, get objects for each directory 1 level deep
                 */
                OmniFile[] files = thisRootFile.listFiles();

                for (OmniFile file: files) {
                    if (file.isDirectory()) {
                        fileObjects.add(file.getFileObject(url));
                    }
                }
            }
        }

        wrapper.add("files", fileObjects);

        /**
         * Optional
         */
        wrapper.addProperty("uplMaxSize", "100M");
        wrapper.addProperty("uplMaxFile", "100");

        //Remove leading slash from the path
        JsonObject options = new JsonObject();
        String path = targetFile.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        options.addProperty("path", path);

        /**
         * Using the optional 'url' creates odd results.
         * From elFinder, Get info, link: produces a link combining the hashed path and a clear text filename
         *
         * Omitting the 'url' prodduces a link that is only a hashed url and this is desired,
         * So don't use the 'url' option.
         */
        //options.addProperty("url", url + "/" + targetFile.getHash() + "/");
        /**
         * Normally the client uses this URL as a base to fetch thumbnails,
         * a clear text filename would be added to perform a GET.
         * This works fine for single volume, however to support multiple volumes
         * the volumeId is required. We just set the url to root and use the
         * same volumeId+hash system to fetch thumbnails.
         */
        options.addProperty("tmbUrl", url + "/");

        options.addProperty("separator", CConst.SLASH);
        //options.put("dispInlineRegex","^(?:(?:image|text)|application/x-shockwave-flash$)");
        options.addProperty("dispInlineRegex","^(?:image|text/plain$)");
        JsonArray disabled = new JsonArray();
        options.add("disabled",disabled);
        options.addProperty("copyOverwrite",1);
        options.addProperty("uploadOverwrite",1);
        options.addProperty("uploadMaxSize", 2000000000); // 2GB
        options.addProperty("jpgQuality",100);

        /**
         * Of the archivers, "create" and "extract" are JSONArray.
         * "createext" is a JSONObject that also provides a file extension.
         */
        JsonObject archivers = new JsonObject();
        JsonArray create = new JsonArray();

            /*create.add("application/x-tar");
            create.add("application/x-gzip");
            create.add("application/x-bzip2");
            create.add("application/x-xz");*/

        create.add("application/zip");
        //create.add("application/x-7z-compressed");

        JsonObject createext = new JsonObject();

            /*createext.addProperty("application/x-tar", "tar");
            createext.addProperty("application/x-gzip", "tgz");
            createext.addProperty("application/x-bzip2", "tbz");
            createext.addProperty("application/x-xz", "xz");*/

        createext.addProperty("application/zip", "zip");
        //createext.put("application/x-7z-compressed", "7z");

        JsonArray extract = new JsonArray();

            /*extract.add("application/x-tar");
            extract.add("application/x-gzip");
            extract.add("application/x-bzip2");
            extract.add("application/x-xz");*/

        extract.add("application/zip");
        //extract.add("application/x-7z-compressed");

        archivers.add("create", create);
        archivers.add("createext", createext);
        archivers.add("extract", extract);
        options.add("archivers", archivers);


        options.add("uiCmdMap",uiCmdMap);
        options.addProperty("syncChkAsTs",1);
        options.addProperty("syncMinMs", 30000);
        wrapper.add("options", options);

        if (init) {

            JsonArray netDriversArray = new JsonArray();
            //netDriversArray.put("ftp");//TODO add netdrivers for FTP, others?
            wrapper.add("netDrivers", netDriversArray);

            JsonObject debug = new JsonObject();
            debug.addProperty("connector","java");
            debug.addProperty("time",(System.currentTimeMillis() - startTime)/1000.0);
            debug.addProperty("memory","3348Kb / 2507Kb / 128M");// FIXME user real memory figures

            JsonArray volumes = new JsonArray();
            JsonObject volume = new JsonObject();
            volume.addProperty("id", volumeId);
            volume.addProperty("driver", "localfilesystem");
            volume.addProperty("mimeDetect", "internal");
            debug.add("volumes", volumes);

            JsonArray mountErrors = new JsonArray();
            debug.add("mountErrors", mountErrors);

            wrapper.add("debug",debug);
        }

//        if (DEBUG) {
//            LogUtil.log(LogUtil.LogType.CMD_OPEN, wrapper.toString());
//        }

        return getInputStream(wrapper);
    }
}
