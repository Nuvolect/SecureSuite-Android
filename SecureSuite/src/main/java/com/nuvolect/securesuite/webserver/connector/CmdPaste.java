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
import com.nuvolect.securesuite.util.OmniFiles;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import org.apache.commons.io.FilenameUtils;

import java.io.InputStream;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * <pre>
 * paste

 Copies or moves a directory / files

 Arguments:

 cmd : paste
 src : hash of the directory from which the files will be copied / moved (the source)
 dst : hash of the directory to which the files will be copied / moved (the destination)
 targets : An array of hashes for the files to be copied / moved
 cut : 1 if the files are moved, missing if the files are copied
 renames : Filename list of rename request
 suffix : Suffixes during rename (default is "~")
 Response:

 If the copy / move is successful:

 added : (Array) array of file and directory objects pasted. Information about File/Directory
 removed : (Array) array of file and directory 'hashes' that were successfully deleted
 {
 "added": [{
 "mime": "text\/plain",
 "ts": 1380910690,
 "read": 1,
 "write": 1,
 "size": 51,
 "hash": "l2_dW50aXRsZWQgZm9sZGVyL1JlYWQgVGhpcyBjb3B5IDEudHh0",
 "name": "Read This copy 1.txt",
 "phash": "l2_dW50aXRsZWQgZm9sZGVy"
 }],
 "removed": ["l2_UmVhZCBUaGlzIGNvcHkgMS50eHQ"]
 }
 Caution

 If the file name of the rename list exists in the directory,
 The command should rename the file to "filename + suffix"
 The command should stop copying at the first error. Is not allowed to overwrite
 files / directories with the same name. But the behavior of this command depends on
 some options on connector (if the user uses the default one). Please, take look the options:
 https://github.com/Studio-42/elFinder/wiki/Connector-configuration-options-2.1#copyoverwrite
 https://github.com/Studio-42/elFinder/wiki/Connector-configuration-options-2.1#copyjoin

 * Example:
 * GET '/servlet/connector'
 * {
 *   cmd=paste,
 *   cut=0,
 *   targets[]=l0_L3N0b3JhZ2UvZW11bGF0ZWQvMC9Eb3dubG9hZC9mcm96ZW4gcm9zZSBjb3B5IDEuanBn,
 *   dst=l0_L3N0b3JhZ2UvZW11bGF0ZWQvMC90bXA,
 *   suffix=~, _=1459348591420,
 *   src=l0_L3N0b3JhZ2UvZW11bGF0ZWQvMC9Eb3dubG9hZA,
 * }
 * Example 2, copy /Music from standard volume to paste crypto volume
 * {
 * targets[]=l0_L3N0b3JhZ2UvZW11bGF0ZWQvMC9NdXNpYw,
 * suffix=~,
 * _=1459527851332,
 * src=l0_L3N0b3JhZ2UvZW11bGF0ZWQvMA,
 * cmd=paste,
 * dst=c0_Lw,
 * queryParameterStrings=cmd=paste
 *   &dst=c0_Lw
 *   &targets%5B%5D=l0_L3N0b3JhZ2UvZW11bGF0ZWQvMC9NdXNpYw
 *   &cut=0
 *   &src=l0_L3N0b3JhZ2UvZW11bGF0ZWQvMA
 *   &suffix=~
 *   &_=1459527851332,
 * cut=0
 * }
 *
 * </pre>
 */
public class CmdPaste extends ConnectorJsonCommand {

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        String url = params.get("url");
        OmniFile dst = new OmniFile( params.get("dst"));

        boolean cut = params.containsKey("cut") && params.get("cut").contentEquals("1");

        String suffix = params.get("suffix");
        JsonArray added = new JsonArray();
        JsonArray removed = new JsonArray();

        /**
         * Params only has the first element of the targets[] array.
         * This is fine if there is only one target but an issue for multiple file operations.
         * Manually parse the query parameter strings to get all targets.
         */
        String[] qps = params.get("queryParameterStrings").split("&");

        /**
         * Iterate over the source files and copy each to the destination folder.
         * Files can be single files or directories and cut/copy/paste works across
         * different volume types, clear to encrypted, etc.
         * If the cut flag is set also delete each file.
         */
        for (String candidate : qps) {
            if(!candidate.contains("targets")) {
                continue;
            }
            String[] parts = candidate.split("=");

            OmniFile fromFile = new OmniFile(parts[1]);
            String toPath = dst.getPath()+"/"+fromFile.getName();
            OmniFile toFile = null;

            // add no more than 10 tilda
            for (int dupCount = 0; dupCount < 10; dupCount++) {
                toFile = new OmniFile(dst.getVolumeId(), toPath);

                if (!toFile.exists()) {
                    break;
                }

                // add ~ to filename, keep extension
                String extension = FilenameUtils.getExtension( toPath);
                if (!extension.isEmpty()) {
                    extension = "." + extension;
                }
                toPath = FilenameUtils.removeExtension( toPath) + suffix;
                toPath = toPath + extension;
            }

            boolean success;
            if (fromFile.isDirectory()) {
                success = OmniFiles.copyDirectory(fromFile, toFile);
            } else {
                success = OmniFiles.copyFile(fromFile, toFile);
            }

            if (success) {
                //note: full depth of directory not added
                added.add(FileObj.makeObj(toFile, url));
            }

            if (success && cut) {
                if (fromFile.delete()) {
                    removed.add(FileObj.makeObj(fromFile, url));
                } else {
                    LogUtil.log(LogUtil.LogType.CMD_PASTE, "File delete failure: " +
                            fromFile.getPath());
                }
            }
        }

        JsonObject wrapper = new JsonObject();

        wrapper.add("added", added);
        wrapper.add("removed", removed);

        return getInputStream(wrapper);
    }
}
