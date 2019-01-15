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
import com.nuvolect.securesuite.util.OmniUtil;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import java.io.InputStream;
import java.util.Map;

import info.guardianproject.iocipher.File;

/**
 * Create a new blank file.
 *
 * Arguments:
 *
 * cmd : mkfile
 * target : hash of target directory,
 * name : New file name
 * Response:
 *
 * added : (Array) Array with a single object - a new file. Information about File/Directory
 */
public class CmdMkfile extends ConnectorJsonCommand {

    public CmdMkfile() {
    }

    @Override
    public InputStream go( Map<String, String> params) {
        // Target is a hashed volume and directory path
        String targetDirHash = params.containsKey("target") ? params.get("target") : "";

        String serverUrl = params.get("url");

        /**
         * TargetFile is the new file.
         */
        OmniFile targetDir = OmniUtil.getFileFromHash(targetDirHash);
        String volumeId = targetDir.getVolumeId();
        LogUtil.log(LogUtil.LogType.CMD_MKFILE, "Target dir" + targetDir.getPath());

        String name = params.containsKey("name") ? params.get("name") : "";

        String path = targetDir.getPath();

        OmniFile targetFile = new OmniFile(volumeId, path + File.separator + name);
        boolean deleted = targetFile.delete();

        JsonArray added = new JsonArray();
        JsonObject wrapper = new JsonObject();

        if (targetFile.createNewFile()) {
            added.add(FileObj.makeObj( volumeId, targetFile, serverUrl));
        }
        wrapper.add("added", added);

        return getInputStream(wrapper);
    }
}
