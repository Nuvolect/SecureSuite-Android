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
 * put
 *
 * Stores text in a file.
 *
 * Arguments (passed in via HTTP POST):
 *
 * cmd : edit
 * target : hash of the file
 * content : new contents of the file
 * Response: An array of successfully uploaded files if success, an error otherwise.
 *
 * changed : (Array) of files that were successfully uploaded. Information about File/Directory
 *
 * Example:
 * cmd=put&target=l3_VEVTVC50eHQ&content=ZZZZZZZZ+and+more
 */
public class CmdPut extends ConnectorJsonCommand {

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        String target = params.containsKey("target") ? params.get("target") : "";
        String url = params.get("url");
        String content = params.containsKey("content") ? params.get("content") : "";

        OmniFile targetFile = new OmniFile(target);

        JsonArray changed = new JsonArray();
        JsonObject wrapper = new JsonObject();
        if (targetFile.writeFile(content)) {
            changed.add(targetFile.getFileObject(url));
        }
        LogUtil.log(LogUtil.LogType.CMD_PUT, targetFile.getName() +
                " updated: " + (changed.size() > 0));
        wrapper.add("changed", changed);

        return getInputStream(wrapper);
    }
}
