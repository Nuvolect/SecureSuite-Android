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
import com.nuvolect.securesuite.util.OmniUtil;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

/**
 * Create a new directory.
 *
 * Arguments:
 *
 * cmd : mkdir
 * target : hash of target directory,
 * name : New directory name
 * dirs[] : array of new directories path (requests at pre-flight of folder upload)
 * Response:
 *
 * added : (Array) Array with a single object - a new directory. Information about File/Directory
 * hashes : (Object) Object of the hash value as a key to the given path in the dirs[]
 */
public class CmdMkdir extends ConnectorJsonCommand {

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        String target = "";// Target is a hashed volume and path
        if (params.containsKey("target")) {
            target = params.get("target");
        }

        String url = params.get("url");

        OmniFile targetFile = OmniUtil.getFileFromHash(target);

        LogUtil.log(LogUtil.LogType.CMD_MKDIR, "Target " + targetFile.getPath());

        String name = "";
        if (params.containsKey("name")) {
            name = params.get("name");
        }

        String volumeId = targetFile.getVolumeId();
        String path = targetFile.getPath();

        OmniFile file = new OmniFile(volumeId, path + File.separator + name);

        JsonArray added = new JsonArray();
        JsonObject wrapper = new JsonObject();

        if (file.mkdir()) {
            JsonObject newDir = FileObj.makeObj(volumeId, file, url);
            added.add(newDir);
        }
        wrapper.add("added", added);

        return getInputStream(wrapper);
    }
}
