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

import com.google.gson.JsonObject;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import java.io.InputStream;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * size

 Returns the size of a directory or file.

 Arguments:

 cmd : size
 targets[] : hash paths of the nodes
 Response:

 size: The total size for all the supplied targets.
 */
public class CmdSize extends ConnectorJsonCommand {

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        String target = "";

        String relativePath = "";

        if (params.containsKey("targets[]")) {
            target = params.get("targets[]");
        } else if (params.containsKey("targets[0]")) {
            target = params.get("targets[0]");
        }

        OmniFile targetFile = new OmniFile(target);

        JsonObject size = new JsonObject();

        long sizeBytes = calcSize(targetFile);
        LogUtil.log(LogUtil.LogType.SIZE, "Target " +
                relativePath + ", size: " + sizeBytes);

        size.addProperty("size", sizeBytes);
        return getInputStream(size);
    }

    private long calcSize(OmniFile targetFile) {
        if (targetFile == null) return 0;
        if (targetFile.isFile()) return targetFile.length();
        if (!targetFile.isDirectory()) return targetFile.length();

        long size = 0;

        OmniFile[] tmp = targetFile.listFiles();
        if (tmp != null) {
            for (OmniFile file: targetFile.listFiles()) { // NPE gone
                if(file == null) continue;
                if (file.isFile()) {
                    size += file.length();
                }
                else {
                    size += calcSize(file);
                }
            }
        }
        return size;
    }
}
