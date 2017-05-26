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

package com.nuvolect.securesuite.util;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.webserver.WebUtil;
import com.nuvolect.securesuite.webserver.connector.CmdMkdir;
import com.nuvolect.securesuite.webserver.connector.CmdMkfile;
import com.nuvolect.securesuite.webserver.connector.CmdRm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestFilesHelper {

    private ArrayList<String> fileHashes = new ArrayList<>();
    private Context context;
    private OmniFile baseDir;

    private int level;
    private int filesInDir;

    public TestFilesHelper(Context context, int level, int filesInDir) {
        this.context = context;
        this.level = level;
        this.filesInDir = filesInDir;
    }

    public void createDirRecursively(String rootPath, String name) throws Exception {
        baseDir = createDir(rootPath, name);
    }

    public ArrayList<String> getFileHashes() {
        return fileHashes;
    }

    public OmniFile getBaseDir() {
        return baseDir;
    }

    public void createFile(String rootPath, String name) throws Exception {
        OmniFile targetFile = createFile(context, rootPath, name);
        fileHashes.add(targetFile.getHash());
    }

    public void removeBaseDir() {
        removeDir(context, baseDir);
    }

    public static void removeDir(Context context, OmniFile dir) {
        Map<String, String> params = new HashMap<>();
        params.put("targets[]", dir.getHash());
        params.put("queryParameterStrings", "cmd=rm&targets%5B%5D=" + dir.getHash());
        params.put("cmd", "rm");
        params.put("uri", "/connector");
        params.put(CConst.URL, WebUtil.getServerUrl(context));
        new CmdRm(context).go(params);
    }

    public static OmniFile createFile(Context context, String rootPath, String name) throws Exception {
        String volumeId = Omni.userVolumeId;
        OmniFile targetFile = new OmniFile(volumeId, rootPath + "/" + name);

        if (targetFile.exists()) {
            targetFile.delete();
        }

        Map<String, String> params = new HashMap<>();
        params.put(CConst.TARGET, targetFile.getParentFile().getHash());
        params.put(CConst.NAME, name);
        params.put(CConst.URL, WebUtil.getServerUrl( context ));
        JsonElement response = InputStreamAsJsonTest.convert(new CmdMkfile().go(params));

        assertThat(response.isJsonObject(), is(true));
        assertThat(response.getAsJsonObject().has("added"), is(true));
        JsonArray added = response.getAsJsonObject().get("added").getAsJsonArray();
        assertThat(added.size(), is(1));
        assertThat(added.get(0).isJsonObject(), is(true));
        JsonObject jsonFile = added.get(0).getAsJsonObject();
        assertThat(jsonFile.has("hash"), is(true));
        assertThat(jsonFile.get("hash").getAsString(), is(targetFile.getHash()));
        assertThat(jsonFile.has("name"), is(true));
        assertThat(jsonFile.get("name").getAsString(), is(name));

        assertThat(targetFile.exists(), is(true));

        return targetFile;
    }

    private OmniFile createDir(String rootPath, String name) throws Exception {
        String volumeId = Omni.userVolumeId;
        OmniFile targetDir = new OmniFile(volumeId, rootPath + name);

        if (targetDir.exists()) {
            targetDir.delete();
        }
        Map<String, String> params = new HashMap<>();
        params.put(CConst.TARGET, targetDir.getParentFile().getHash());// root hash
        params.put(CConst.NAME, name);
        params.put(CConst.URL, WebUtil.getServerUrl(context));

        new CmdMkdir().go(params);
        assertThat(targetDir.exists(), is(true));
        fileHashes.add(targetDir.getHash());

        for (int i = 0; i < filesInDir; i++) {
            createFile(rootPath + name, "testFile" + i);
        }

        int currentLevel = rootPath.split(File.separator).length;
        if (currentLevel < level) {
            createDir(rootPath + name + File.separator, "subdir");
        }

        return targetDir;
    }
}
