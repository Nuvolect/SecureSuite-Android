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

import android.support.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import java.io.InputStream;
import java.util.Map;

/**
 *
 * parents
 *
 *         Returns all parent folders and its subfolders on required (in connector options) deep.
 *         This command is invoked when a directory is reloaded in the client.
 *         Data provided by 'parents' command should enable the correct drawing of tree hierarchy
 *         directories.
 *
 *         Arguments:
 *
 *         cmd : parents
 *         target : folder's hash,
 *         Response:
 *
 *         tree : (Array) Folders list. Information about File/Directory
 *         Example:
 *
 *         With the present hierarchy
 *
 *         /root1
 *           /dir1
 *             /dir11
 *               /dir111
 *             /dir12
 *               /dir121
 *           /dir2
 *             /dir22
 *             /dir23
 *               /dir231
 *         /root2
 *
 *         Should 'dir111' be reloaded, 'parents' data should return:
 *         'dir111' parent directories
 *         for each parent directory, its subdirectories (no more depth is needed)
 *         should multiroot nodes be implemented, its root nodes (and optionally children)
 *         This way, client-side component will render the following reloaded hierarchy
 *
 *         /root1
 *           /dir1
 *             /dir11
 *               /dir111
 *             /dir12
 *           /dir2
 *         /root2
 */
public class CmdParents extends ConnectorJsonCommand {

    private static boolean DEBUG = false; //LogUtil.DEBUG;

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        String url = params.get("url");

        String target = params.get("target");
        OmniFile targetFile = new OmniFile(target);
        String volumeId = targetFile.getVolumeId();

        if (DEBUG) {
            LogUtil.log(LogUtil.LogType.CMD_PARENTS, "volumeId: " +
                    volumeId + ", path: " + targetFile.getPath());
        }
        JsonArray treeArray = new JsonArray();

        /**
         * Iterate upward and capture all parent and parent sibling
         * (aunts and uncles) directories
         */
        while (!targetFile.isRoot()) {
            targetFile = targetFile.getParentFile();
            if (targetFile == null) {
                break;
            }
            if (DEBUG) {
                LogUtil.log(LogUtil.LogType.CMD_PARENTS, "folder scan: " +
                        targetFile.getPath());
            }

            for (OmniFile file: targetFile.listFiles()) {
                if (file.isDirectory()) {
                    treeArray.add(FileObj.makeObj(volumeId, file, url));
                    if (DEBUG) {
                        LogUtil.log(LogUtil.LogType.CMD_PARENTS, "tree put: " +
                                file.getPath());
                    }
                }
            }
        }

        JsonObject tree = new JsonObject();
        tree.add("tree", treeArray);

        if (DEBUG) {
            LogUtil.log(LogUtil.LogType.CMD_PARENTS, tree.toString());
        }

        return getInputStream(tree);
    }
}
