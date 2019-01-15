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
import com.nuvolect.securesuite.util.OmniImage;
import com.nuvolect.securesuite.util.OmniUtil;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import androidx.annotation.NonNull;


/**
 * resize

 * Change the size of an image.

 * Arguments:
 *
 * cmd : resize
 * mode : 'resize' or 'crop' or 'rotate'
 * target : hash of the image path
 * width : new image width
 * height : new image height
 * x : x of crop (mode='crop')
 * y : y of crop (mode='crop')
 * degree : rotate degree (mode='rotate')
 * quality
 * Response:
 *
 * changed : (Array) of files that were successfully resized. Information about File/Directory
 * To be able to resize the image, cdc record file must be specified and resize dim.
 * Resize must be in true dim and contain a line with dimensions of height and width (like "600x400").
 * If specified without resize dim resize the dialog will not work correctly.
 *
 * GET '/servlet/connector'
 * {
 * cmd=resize,
 * mode=crop,
 * target=l0_L3N0b3JhZ2UvZW11bGF0ZWQvMC9Eb3dubG9hZC9mcm96ZW4gcm9zZSBjb3B5IDEuanBn,
 * x=86, y=35, height=504, width=512 quality=100,
 * _=1459286749082,
 * }
 */
public class CmdResize extends ConnectorJsonCommand {

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        String url = params.get("url");
        // Target is a hashed volume and path
        String target = params.containsKey("target") ? params.get("target") : null;

        OmniFile targetFile = OmniUtil.getFileFromHash(target);
        LogUtil.log(LogUtil.LogType.CMD_RESIZE, "Target " + targetFile.getPath());

        String mode = params.containsKey("mode") ? params.get("mode") : "";
        int x = params.containsKey("x") ? Integer.valueOf( params.get("x")) : 0;
        int y = params.containsKey("y") ? Integer.valueOf(params.get("y")) : 0;
        float degree = params.containsKey("degree") ? Float.valueOf(params.get("degree")) : 0.0f;
        int height = params.containsKey("height") ? Integer.valueOf(params.get("height")) : 0;
        int width = params.containsKey("width") ? Integer.valueOf(params.get("width")) : 0;
        int quality = params.containsKey("quality") ? Integer.valueOf(params.get("quality")) : 100;
        JsonArray changed = new JsonArray();

        try {
            OmniFile image = null;
            if (mode.contains("rotate")) {
                image = OmniImage.rotateImage(targetFile, degree, quality);
                LogUtil.log(LogUtil.LogType.CMD_RESIZE,
                        "rotation complete degree: " + degree + ", quality: " + quality );
            } else if (mode.contains("crop")) {
                image = OmniImage.cropImage(targetFile, x, y, width, height, quality);
                LogUtil.log(LogUtil.LogType.CMD_RESIZE,
                        "crop complete: " + x + ", " + y + ", " + width + ", "+height );
            } else if (mode.contains("resize")) {
                image = OmniImage.resizeImage(targetFile, width, height, quality);
                LogUtil.log(LogUtil.LogType.CMD_RESIZE,
                        "resize complete: " + x + ", " + y + ", " + width + ", " + height);
            }
            if (image != null) {
                changed.add(image.getFileObject(url));
            }
        } catch (IOException e) {
            LogUtil.log(LogUtil.LogType.CMD_RESIZE, "resize failed: " + e.getMessage());
        }
        JsonObject wrapper = new JsonObject();
        wrapper.add("changed", changed);

        return getInputStream(wrapper);
    }
}
