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
import com.nuvolect.securesuite.util.Omni;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.util.OmniImage;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * image_query
 * <pre>
 *    Query the images of a volume
 * </pre>
 */
public class CmdImageQuery extends ConnectorJsonCommand {

    private static boolean DEBUG = false; //LogUtil.DEBUG;
    private static String NO_IMAGE_FILENAME = "no_image_found.jpg";

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        long startTime = System.currentTimeMillis();
        String url = params.get("url");

        String volumeId = Omni.getDefaultVolumeId();
        if (params.containsKey("volume_id")) {
            volumeId = params.get("volume_id");
        }

        OmniFile targetFile = new OmniFile( volumeId, "/DCIM/Camera");
        OmniFile[] files = targetFile.listFiles();
        JsonArray list = new JsonArray();
        /**
         * Iterate over all the files looking for intersects.
         * When an intersect is found, remove it from consideration.
         */
        for (OmniFile file : files) {
            if (OmniImage.isImage(file)) {
                JsonObject psObj = file.getPsObject(url);
                list.add(psObj);
            }
        }

        if (list.size() == 0) {
            JsonObject image = new JsonObject();
            image.addProperty("name", NO_IMAGE_FILENAME);
            image.addProperty("h", 270);
            image.addProperty("w", 270);
            image.addProperty("src", url + File.separator + "img" + File.separator + NO_IMAGE_FILENAME);

            list.add(image);
        }

        JsonObject wrapper = new JsonObject();
        wrapper.add("list", list);

        if (DEBUG) {
            String msg = "Elapsed time: "+String.valueOf(System.currentTimeMillis()-startTime);
            LogUtil.log(LogUtil.LogType.CMD_IMAGE_QUERY, msg);

            LogUtil.log(LogUtil.LogType.CMD_IMAGE_QUERY, wrapper.toString());
        }

        return getInputStream(wrapper);
    }
}
