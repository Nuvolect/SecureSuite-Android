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
import com.google.gson.JsonElement;
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

import info.guardianproject.iocipher.File;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by serg on 19.05.17.
 */

public class CmdExtractTest {

    @Test
    public void go() throws Exception {
        Context ctx = getTargetContext();

        SqlCipher.getInstance(ctx);

        assertThat(Omni.init(ctx), is(true));

        TestFilesHelper testFilesHelper = new TestFilesHelper(ctx, 3, 100);
        testFilesHelper.createDirRecursively("/", "CmdExtractTest");

        OmniFile baseFilesDir = testFilesHelper.getBaseDir();

        String dirName = "CmdExtractTest";
        String archiveName = dirName + ".zip";

        Map<String, String> params = new HashMap<>();
        params.put("target", baseFilesDir.getParentFile().getHash());
        params.put("name", archiveName);
        params.put("cmd", "archive");
        params.put("uri", "/connector");
        params.put("type", "application/zip");
        params.put("queryParameterStrings",
                "cmd=archive&name=" + archiveName + "&target=" +
                        baseFilesDir.getParentFile().getHash() + "&targets%5B%5D=" +
                        baseFilesDir.getHash() + "&type=application%2Fzip&_=1495169664811");
        params.put(CConst.URL, WebUtil.getServerUrl(ctx));

        JsonObject response = InputStreamAsJsonTest.convert(new CmdArchive(ctx).go(params))
                .getAsJsonObject();

        assertThat(baseFilesDir.exists(), is(true));
        testFilesHelper.removeBaseDir();
        assertThat(baseFilesDir.exists(), is(false));

        JsonArray added = response.get("added").getAsJsonArray();

        JsonObject jsonFile = added.get(0).getAsJsonObject();

        OmniFile archiveFile = new OmniFile(jsonFile.get("hash").getAsString());
        assertThat(archiveFile.exists(), is(true));

        params = new HashMap<>();
        params.put("target", archiveFile.getHash());
        params.put("makedir", "1");
        params.put("cmd", "extract");
        params.put("uri", "/connector");
        params.put("queryParameterStrings",
                "cmd=extract&name=" + archiveName + "&target=" +
                        archiveFile.getHash() + "&makedir=0&unique_id=embedded_user");
        params.put(CConst.URL, WebUtil.getServerUrl(ctx));

        response = InputStreamAsJsonTest.convert(new CmdExtract().go(params))
                .getAsJsonObject();

        archiveFile.delete();

        assertThat(response.has("added"), is(true));
        added = response.get("added").getAsJsonArray();

        for (JsonElement element: added) {
            assertThat(element.isJsonObject(), is(true));
            JsonObject jsonObject = element.getAsJsonObject();

            assertThat(jsonObject.get("isowner").getAsJsonPrimitive().isBoolean(), is(true));
            assertThat(jsonObject.get("locked").getAsJsonPrimitive().isNumber(), is(true));
            assertThat(jsonObject.get("name").getAsJsonPrimitive().isString(), is(true));
            assertThat(jsonObject.get("mime").getAsJsonPrimitive().isString(), is(true));
            assertThat(jsonObject.get("phash").getAsJsonPrimitive().isString(), is(true));
            assertThat(jsonObject.get("read").getAsJsonPrimitive().isNumber(), is(true));
            assertThat(jsonObject.get("size").getAsJsonPrimitive().isNumber(), is(true));
            assertThat(jsonObject.get("ts").getAsJsonPrimitive().isNumber(), is(true));
            assertThat(jsonObject.get("write").getAsJsonPrimitive().isNumber(), is(true));
            assertThat(jsonObject.get("tmbUrl").getAsJsonPrimitive().isString(), is(true));
            assertThat(jsonObject.get("disabled").isJsonArray(), is(true));

            String hash = jsonObject.get("hash").getAsString();
            assertThat(new OmniFile(hash).exists(), is(true));
        }

        OmniFile unpackDir = new OmniFile(Omni.userVolumeId, File.separator + dirName);
        assertThat(unpackDir.exists(), is(true));
        TestFilesHelper.removeDir(ctx, unpackDir);
        assertThat(unpackDir.exists(), is(false));
    }
}
