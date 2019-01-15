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
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.util.OmniHash;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import java.io.InputStream;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * info
 *
 * Returns information about places nodes
 *
 * Arguments:
 *
 * cmd : info
 * targets[] : array of hashed paths of the nodes
 * Response:
 *
 * files: (Array of data) places directories info data Information about File/Directory
 */
public class CmdInfo extends ConnectorJsonCommand {

    private static boolean DEBUG = LogUtil.DEBUG;

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        String url = params.get("url");

        String target = "";
        if (params.containsKey("targets[]")) {
            target = params.get("targets[]");
        }

        /**
         * A non-empty targets is a hashed path starting with with the volume
         * followed by a encoded relative path.
         */
        String segments[] = target.split("_");
        String volumeId = segments[0] + "_";

        String path = OmniHash.decode(segments[1]);
        OmniFile targetFile = new OmniFile(volumeId, path);

        LogUtil.log(LogUtil.LogType.CMD_INFO, "volumeId: " + volumeId + ", relativePath: " + path);

        JsonArray files = new JsonArray();

        files.add(FileObj.makeObj(volumeId, targetFile, url));

        LogUtil.log(LogUtil.LogType.INFO, "File " + targetFile.getName());

        JsonObject wrapper = new JsonObject();
        wrapper.add("files", files);

        if (DEBUG) {
            LogUtil.log(LogUtil.LogType.CMD_PARENTS, wrapper.toString());
        }

        return getInputStream(wrapper);
    }
}
