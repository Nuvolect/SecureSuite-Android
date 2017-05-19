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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * tree
 *
 * Return folder's subfolders.
 *
 * Arguments:
 *
 * cmd : tree
 * target : folder's hash
 * Response:
 *
 * tree : (Array) Folders list. Information about File/Directory
 */
public class CmdTree extends ConnectorJsonCommand {

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        String url = params.get("url");

        String target = params.get("target");
        OmniFile targetFile = new OmniFile( target );
        String volumeId = targetFile.getVolumeId();

        LogUtil.log(LogUtil.LogType.CMD_TREE, "volumeId: " + volumeId +
                ", path: " + targetFile.getPath());

        JsonArray tree = new JsonArray();
        tree.add(FileObj.makeObj(volumeId, targetFile, url));

        int i = 0;
        OmniFile[] files = targetFile.listFiles();

        if (files == null) {
            files = new OmniFile[0];
        }

        for (OmniFile file: files) {
            if (file.isDirectory()) {
                tree.add(FileObj.makeObj(volumeId, file, url));
                LogUtil.log(LogUtil.LogType.CMD_TREE, "File " + (++i) +
                        ", " + file.getName());
            }
        }

        JsonObject wrapper = new JsonObject();
        wrapper.add("tree", tree);

        return getInputStream(wrapper);
    }
}
