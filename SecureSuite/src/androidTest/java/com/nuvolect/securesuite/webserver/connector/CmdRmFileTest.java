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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.InputStreamAsJsonTest;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Omni;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.util.TestFilesHelper;
import com.nuvolect.securesuite.webserver.WebUtil;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Exercise the connector "rm" command, {@link CmdRm}
 *
 * The test creates a directory called CmdRmFileTest and makes 10 subdirectories inside it.
 * Each directory contains 100 files, including the CmdRmFileTest.
 * Each subdirectory in turn contains other 10 subdirectories (and so on to the hierarchy of 3 levels deep).
 * There are 2121 files overall, including directories
 */
public class CmdRmFileTest {

    @Test
    public void go() throws Exception {
        Context ctx = getTargetContext();

        SqlCipher.getInstance(ctx);

        assertThat(Omni.init(ctx), is(true));

        TestFilesHelper testFilesCreator = new TestFilesHelper(ctx, 3, 100);
        testFilesCreator.createDirRecursively("/", "CmdRmFileTest");

        OmniFile baseDir = testFilesCreator.getBaseDir();
        ArrayList<String> fileHashes = testFilesCreator.getFileHashes();
        int filesCount = fileHashes.size();

        Map<String, String> params = new HashMap<>();
        params.put("targets[]", baseDir.getHash());
        params.put("queryParameterStrings", "cmd=rm&targets%5B%5D=" + baseDir.getHash());
        params.put("cmd", "rm");
        params.put("uri", "/connector");
        params.put(CConst.URL, WebUtil.getServerUrl(ctx));

        long startTime = System.currentTimeMillis();

        JsonObject response = InputStreamAsJsonTest.convert(new CmdRm(ctx).go(params))
                .getAsJsonObject();

        /**
         * Expect to get json object with json array "removed", containing all the deleted
         * file hashes
         */
        assertThat(response.has("removed"), is(true));
        assertThat(response.get("removed").isJsonArray(), is(true));
        assertThat(response.get("removed").getAsJsonArray().size(), is(filesCount));
        for (JsonElement element: response.get("removed").getAsJsonArray()) {
            assertThat(element.isJsonPrimitive(), is(true));
            String hash = element.getAsString();
            assertThat(fileHashes.contains(hash), is(true));
        }

        double timeTaken = (double)(System.currentTimeMillis() - startTime) / 1000;

        LogUtil.log(String.format(Locale.getDefault(), "time taken to delete %d files: " +
                        "%2$,.2fs. (%3$,.2f per second)",
                filesCount, timeTaken, filesCount / timeTaken));

        /**
         * Run the command with invalid params (the directory has already been deleted)
         * Expect to get empty json object
         */
        response = InputStreamAsJsonTest.convert(new CmdRm(ctx).go(params))
                .getAsJsonObject();
        assertThat(response.isJsonObject(), is(true));
        assertThat(response.getAsJsonObject().entrySet().size(), is(0));
    }
}
