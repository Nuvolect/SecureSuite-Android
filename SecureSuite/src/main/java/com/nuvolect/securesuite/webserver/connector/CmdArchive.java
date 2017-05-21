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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.util.OmniZip;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

/**
 * archive

 Packs directories / files into an archive.

 Arguments:

 cmd : archive
 type : mime-type for the archive
 current : hash of the directory that are added to the archive directory / files
 targets : an array of hashes of the directory / files
 Response: Client-Server-API-2.1#open, select - hash of the new archive

 Example:
 http://hypweb.net/elFinder-nightly/demo/2.1/php/connector.minimal.php?
   cmd=archive
   &name=Archive.zip
   &target=l2_TmV3Rm9sZGVyL3Rlc3Q
   &targets%5B%5D=l2_TmV3Rm9sZGVyL3Rlc3QvZW1wdHkgZm9sZGVy
   &targets%5B%5D=l2_TmV3Rm9sZGVyL3Rlc3QvY29sb3Vyc29mZmFsbC5qcGc
   &targets%5B%5D=l2_TmV3Rm9sZGVyL3Rlc3Qv0YLQtdGB0YIuanBn
   &type=application%2Fzip
   &_=1460671443521
 */
public class CmdArchive extends ConnectorJsonCommand {

    private Context context;

    public CmdArchive(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        String url = params.get("url");
        String target = params.get("target");
        String name = params.get("name");

        /**
         * Params only has the first element of the targets[] array.
         * This is fine if there is only one target but an issue for multiple file operations.
         * Manually parse the query parameter strings to get all targets.
         */
        String[] qps = params.get("queryParameterStrings").split("&");


        /**
         * Create a file for the target archive.
         * Assume the file does not already exist.
         * "target" is the folder containing the zip
         * "name" is the name of the zip file
         */
        OmniFile parentFile = new OmniFile(target);
        String zipPath = parentFile.getPath() + File.separator + name;
        OmniFile zipFile = new OmniFile(parentFile.getVolumeId(), zipPath);

        ArrayList<OmniFile> files = new ArrayList<>();
        for (String candidate: qps) {
            if (candidate.contains("targets")) {
                String[] parts = candidate.split("=");
                files.add(new OmniFile(parts[1]));
            }
        }

        JsonObject wrapper = new JsonObject();
        if (!OmniZip.zipFiles(context, files, zipFile, 0)) {
            JsonArray warning = new JsonArray();
            warning.add("errPerm");
            wrapper.add("warning", warning);

            return getInputStream(wrapper);
        }
        JsonArray added = new JsonArray();
        added.add(zipFile.getFileObject(url));
        wrapper.add("added", added);

        return getInputStream(wrapper);
    }
}
