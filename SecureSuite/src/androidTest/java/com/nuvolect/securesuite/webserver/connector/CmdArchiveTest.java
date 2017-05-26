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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.InputStreamAsJsonTest;
import com.nuvolect.securesuite.util.Omni;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.util.TestFilesHelper;
import com.nuvolect.securesuite.webserver.WebUtil;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CmdArchiveTest {

    @Test
    public void go() throws Exception {
        Context ctx = getTargetContext();

        SqlCipher.getInstance(ctx);

        assertThat (Omni.init(ctx), is(true));

        TestFilesHelper testFilesCreator = new TestFilesHelper(ctx, 3, 100);
        testFilesCreator.createDirRecursively("/", "CmdArchiveTest");
        OmniFile baseDir = testFilesCreator.getBaseDir();

        String archiveName = "CmdArchiveTest.zip";

        Map<String, String> params = new HashMap<>();
        params.put("target", baseDir.getHash());
        params.put("name", archiveName);
        params.put("cmd", "archive");
        params.put("uri", "/connector");
        params.put("type", "application/zip");
        params.put("queryParameterStrings",
                "cmd=archive&name=" + archiveName + "&target=" +
                        baseDir.getParentFile().getHash() + "&targets%5B%5D=" +
                        baseDir.getHash() + "&type=application%2Fzip&_=1495169664811");
        params.put(CConst.URL, WebUtil.getServerUrl(ctx));

        JsonObject response = InputStreamAsJsonTest.convert(new CmdArchive(ctx).go(params))
                .getAsJsonObject();

        assertThat(response.has("added"), is(true));
        assertThat(response.get("added").isJsonArray(), is(true));
        JsonArray added = response.get("added").getAsJsonArray();
        assertThat(added.size(), is(1));
        assertThat(added.get(0).isJsonObject(), is(true));
        JsonObject jsonFile = added.get(0).getAsJsonObject();
        assertThat(jsonFile.has("hash"), is(true));
        assertThat(jsonFile.has("name"), is(true));
        assertThat(jsonFile.has("mime"), is(true));

        assertThat(jsonFile.get("name").getAsString(), is(archiveName));
        assertThat(jsonFile.get("mime").getAsString(), is("application/zip"));

        /**
         * Cleaning out
         */
        OmniFile archiveFile = new OmniFile(jsonFile.get("hash").getAsString());
        archiveFile.delete();
        assertThat(!archiveFile.exists(), is(true));

        testFilesCreator.removeBaseDir();
        assertThat(testFilesCreator.getBaseDir().exists(), is(false));
    }
}
