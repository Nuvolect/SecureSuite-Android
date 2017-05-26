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
import android.support.annotation.NonNull;

import com.google.gson.JsonObject;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.util.OmniUtil;
import com.nuvolect.securesuite.util.OmniZip;
import com.nuvolect.securesuite.webserver.MimeUtil;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

/**
 * ZipDl - zip multiple files to "Archive.zip", download and delete.
 *
 1st request to make temporary archive file on server side

 Arguments:

 cmd : zipdl
 targets[] : array of hashed paths of the nodes
 Response:

 zipdl array data for 2nd request
 {
   file: "elfzdljKWIxU",        // temporary archive file name
   name: "Test here_Files.zip", // download file name
   mime: "application/zip"      // MIME type
 }

 2nd requset to download an archive

 Arguments:

 cmd : zipdl
 download : 1
 targets[0] : hash path for detect target volume drive (e.g. cwd hash)
 targets[1] : target temporary archive file name
 targets[2] : download file name
 targets[3] : MIME type

 Response: RAW data of archive file with HTTP headers for download

 {
   "zipdl": {
     "file": "elfzdlwjPURf",
     "name": "MIME-types_Files.zip",
     "mime": "application\/zip"
   },
   "debug": {
     "connector": "php",
     "phpver": "5.5.36",
     "time": 0.10684299468994,
     "memory": "2159Kb \/ 800Kb \/ 256M",
     "upload": "",
   "volumes": [{
     "id": "l1_",
     "name": "localfilesystem",
     "mimeDetect": "finfo",
     "imgLib": "imagick"
   }, {
     "id": "l2_",
     "name": "localfilesystem",
     "mimeDetect": "finfo",
     "imgLib": "imagick"
   }, {
     "id": "l3_",
     "name": "localfilesystem",
     "mimeDetect": "finfo",
     "imgLib": "imagick"
   }],
     "mountErrors": [],
     "phpErrors": []
   }
 }

 First call - request JSON object with "file", "name", "mime", make ZIP
 ~~~~
 GET '/connector'
 {
 queryParameterStrings=
 cmd=zipdl&
 targets%5B%5D=c0_L0tlcGxlci9zaWduYWwtMjAxNi0wNC0yNy0xOTI0MjEuanBn&
 targets%5B%5D=c0_L0tlcGxlci9zaWduYWwtMjAxNi0wNS0wMi0xMzIzNTkuanBn&
 _=1467751633659,
 _=1467751633659,
 cmd=zipdl,
 targets[]=c0_L0tlcGxlci9zaWduYWwtMjAxNi0wNS0wMi0xMzIzNTkuanBn
 }
 ~~~~

 Second call is POST, download ZIP
 ~~~~
 POST '/connector'
 {
 queryParameterStrings=
 cmd=zipdl&
 targets%5B%5D=c0_L0tlcGxlci9zaWduYWwtMjAxNi0wNC0yNy0xOTI0MjEuanBn&
 targets%5B%5D=c0_L0tlcGxlci9zaWduYWwtMjAxNi0wNS0wMi0xMzIzNTkuanBn&
 _=1467751633659
 }
 ~~~~
 */

public class CmdZipdl extends ConnectorJsonCommand {

    private Context context;

    public CmdZipdl(Context context) {
        this.context = context;
    }

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        if (params.containsKey("download") && params.get("download").equals("1")) {
            return downloadFile(params);
        }
        return archiveFiles(params);
    }

    private InputStream archiveFiles(@NonNull Map<String, String> params) {
        /**
         * Params only has the first element of the targets[] array.
         * This is fine if there is only one target but an issue for multiple file operations.
         * Manually parse the query parameter strings to get all targets.
         */
        String[] qps = params.get("queryParameterStrings").split("&");

        ArrayList<OmniFile> files = new ArrayList<>();
        for (String candidate : qps) {
            if (candidate.contains("targets")) {
                String[] parts = candidate.split("=");

                String target = parts[1];
                files.add(new OmniFile(target));

                if (LogUtil.DEBUG) {
                    LogUtil.log(LogUtil.LogType.CMD_ZIPDL, target);
                }
            }
        }

        /**
         * Create a file for the target archive in the directory of the first target file.
         */
        String volumeId = files.get(0).getVolumeId();
        String zipFileName = "Archive.zip";
        String zipPath = files.get(0).getParentFile().getPath() + File.separator + zipFileName;
        OmniFile zipFile = OmniUtil.makeUniqueName(new OmniFile(volumeId, zipPath));

        // Filename may have changed
        zipFileName = FilenameUtils.getName(zipFile.getPath());

        JsonObject zipdl = new JsonObject();
        JsonObject wrapper = new JsonObject();

        zipdl.addProperty("file", zipFile.getHash());
        zipdl.addProperty("name", zipFileName);
        zipdl.addProperty("mime", MimeUtil.MIME_ZIP);
        wrapper.add("zipdl", zipdl);

        if (!OmniZip.zipFiles(context, files, zipFile, 0)) {
            return null;
        }

        LogUtil.log(LogUtil.LogType.CMD_ZIPDL, "first request");
        LogUtil.log(LogUtil.LogType.CMD_ZIPDL, wrapper.toString());

        return getInputStream(wrapper);
    }

    private InputStream downloadFile(@NonNull Map<String, String> params) {
        OmniFile file = new OmniFile(params.get("targets[]_2"));
        LogUtil.log(LogUtil.LogType.CMD_ZIPDL, "second request");

        return file.getFileInputStream();
    }
}
