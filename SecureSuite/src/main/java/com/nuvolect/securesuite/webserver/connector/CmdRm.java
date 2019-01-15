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

import android.content.Context;
import android.media.MediaScannerConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.util.OmniImage;
import com.nuvolect.securesuite.util.OmniUtil;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;


/**
 *<pre>
 *
 * rm
 *
 * Recursively removes files and directories.
 *
 * Arguments:
 *
 * cmd : rm
 * targets : (Array) array of file and directory hashes to delete
 * Response:
 *
 * removed : (Array) array of file and directory 'hashes' that were successfully deleted
 *
 * Example:( 5B == '[', 5D == ']'
 * http://hypweb.net/elFinder-nightly/demo/2.1/php/connector.minimal.php?
 * cmd=rm
 * &targets%5B%5D=l2_TmV3RmlsZS50eHQ
 * &targets%5B%5D=l2_TmV3RmlsZSBjb3B5IDEudHh0
 * &_=1459218951979
 *
 *</pre>
 */
public class CmdRm extends ConnectorJsonCommand {

    private Context context;
    private ArrayList<OmniFile> removedFiles = new ArrayList<>();

    public CmdRm(Context context) {
        this.context = context;
    }

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        /**
         * Params only has the first element of the targets[] array.
         * This is fine if there is only one target but an issue for multiple file operations.
         * Manually parse the query parameter strings to get all targets.
         */
        String[] qps = params.get("queryParameterStrings").split("&");

        boolean success = true;
        for (String candidate : qps) {
            if (!candidate.contains("targets")) {
                continue;
            }
            String[] parts = candidate.split("=");

            OmniFile targetFile = OmniUtil.getFileFromHash(parts[1]);

            /**
             * Recursively delete files and folders adding each delete to an arraylist.
             */
            success = delete(targetFile, false);
            if (!success) {
                break;
            }
        }

        JsonObject wrapper = new JsonObject();

        if (success) {
            JsonArray removed = new JsonArray();
            ArrayList<String> pathsToScan = new ArrayList<>();
            for (OmniFile file: removedFiles) {
                removed.add(file.getHash());
                if (needScanFile(file)) {
                    pathsToScan.add(file.getAbsolutePath());
                }
            }
            if (pathsToScan.size() > 0) {
                MediaScannerConnection.scanFile(
                        context,
                        pathsToScan.toArray(new String[pathsToScan.size()]),
                        null,
                        null);
            }

            wrapper.add("removed", removed);
        }

        return getInputStream(wrapper);
    }

    public static boolean delete(Context context, OmniFile omniFile) {
        CmdRm instance = new CmdRm(context);
        return instance.delete(omniFile, true);
    }

    /**
     * Recursively deletes a directory and its contents.
     *
     * @param f The directory (or file) to delete
     * @return true if the delete succeeded, false otherwise
     */
    private boolean delete(OmniFile f, boolean scanFile) {
        if (f.isDirectory()) {
            for (OmniFile child : f.listFiles()) {
                if (!delete(child, scanFile)) {
                    return false;
                }
            }
        }

        /**
         * Delete thumbnail, if there is one
         */
        OmniImage.deleteThumbnail( f );

        boolean success = f.delete();

        if (success) {
            removedFiles.add(f);

            /**
             * The crypto storage does not use the media scanner.
             */
            if (scanFile && needScanFile(f)) {
                MediaScannerConnection.scanFile(
                        context,
                        new String[]{f.getAbsolutePath()},
                        null,
                        null);
            }

            return true;
        }

        return false;
    }

    private boolean needScanFile(OmniFile file) {
        //here you can add additional conditions if the file needs to be scanned
        return file.isStd();
    }
}
