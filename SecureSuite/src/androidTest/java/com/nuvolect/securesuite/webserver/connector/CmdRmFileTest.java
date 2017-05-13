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

package com.nuvolect.securesuite.webserver.connector;

import android.content.Context;

import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.Omni;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.webserver.WebUtil;

import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by serg on 13.05.17.
 */

/**
 * Exercise the connector "rm" command, {@link CmdRm}
 *
 * The test creates a directory called CmdRmFileTest and makes 10 subdirectories inside it.
 * Each directory contains 100 files, including the CmdRmFileTest.
 * Each subdirectory in turn contains other 10 subdirectories (and so on to the hierarchy of 3 levels deep).
 * There are 2121 files overall, including directories
 */
public class CmdRmFileTest {

    private int filesCount = 0;

    private void createDirRecursively(Context ctx, String rootPath, String name) throws Exception {
        createDir(ctx, rootPath, name);

        int level = rootPath.split("/").length;
        if (level < 3) {
            createDir(ctx, rootPath + name + "/", "subdir");
        }
    }

    private OmniFile createDir(Context ctx, String rootPath, String name) throws Exception {
        String volumeId = Omni.userVolumeId;

        OmniFile targetDir = new OmniFile(volumeId, rootPath + name);
        if (targetDir.exists()) {
            targetDir.delete();
        }

        Map<String, String> params = new HashMap<>();
        params.put(CConst.TARGET, targetDir.getParentFile().getHash());// root hash
        params.put(CConst.NAME, name);
        params.put(CConst.URL, WebUtil.getServerUrl( ctx ));

        InputStream inputStream = CmdMkdir.go(params);
        byte[] b = new byte[4096];
        int bytes = inputStream.read( b );
        assertThat( bytes > 0, is( true));

        assertThat( targetDir.exists(), is( true ));

        for (int i = 0; i < 100; i++) {
            createFile(ctx, rootPath + name, "testFile" + i);
        }

        filesCount++;

        return targetDir;
    }

    private void createFile(Context ctx, String rootPath, String name) throws Exception {
        String volumeId = Omni.userVolumeId;

        OmniFile targetFile = new OmniFile(volumeId, rootPath + "/" + name);
        if (targetFile.exists()) {
            targetFile.delete();
        }

        Map<String, String> params = new HashMap<>();
        params.put(CConst.TARGET, targetFile.getParentFile().getHash());// root hash
        params.put(CConst.NAME, name);
        params.put(CConst.URL, WebUtil.getServerUrl( ctx ));

        InputStream inputStream = CmdMkfile.go(params);
        byte[] b = new byte[4096];
        int bytes = inputStream.read( b );
        assertThat( bytes > 0, is( true));

        assertThat( targetFile.exists(), is( true ));

        filesCount++;
    }

    @Test
    public void go() throws Exception {

        Context ctx = getTargetContext();
        SqlCipher.getInstance( ctx );

        assertThat ( Omni.init( ctx), is( true ));

        OmniFile baseDir = createDir(ctx, "/", "CmdRmFileTest");
        for (int i = 0; i < 10; i++) {
            createDirRecursively(ctx, "/CmdRmFileTest/", "Subdir" + i);
        }

        long startTime = System.currentTimeMillis();
        Map<String, String> params = new HashMap<>();
        params.put("targets[]", baseDir.getHash());
        params.put("queryParameterStrings", "cmd=rm&targets%5B%5D=" + baseDir.getHash());
        params.put("cmd", "rm");
        params.put("uri", "/connector");
        params.put(CConst.URL, WebUtil.getServerUrl( ctx ));
        CmdRm.go(ctx, params);
        double timeTaken = (double)(System.currentTimeMillis() - startTime) / 1000;

        System.out.println(String.format("time taken to delete %d files: %2$,.2fs. (%3$,.2f per second)", filesCount, timeTaken, filesCount / timeTaken));
    }
}
