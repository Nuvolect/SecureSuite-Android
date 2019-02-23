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
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Omni;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.webserver.WebUtil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static com.nuvolect.securesuite.main.App.getContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Exercise the connector "mkfile" command, {@link CmdMkfile}
 */
public class CmdMkfileTest {

    @Test
    public void go() throws Exception {

        Context ctx = getContext();

        SqlCipher.getInstance( ctx );
        assertThat ( ServerInit.init( ctx ), is( true ));

        String volumeId = Omni.userVolumeId_0;
        String rootPath = "/";
        String uniqueFilename = ".filenameNeverGuessZez";

        OmniFile targetFile = new OmniFile( volumeId, rootPath + uniqueFilename);
        boolean deleted = targetFile.delete();

        Map<String, String> params = new HashMap<String, String>();
        params.put(CConst.TARGET, targetFile.getParentFile().getHash());// root hash
        params.put(CConst.NAME, uniqueFilename);
        params.put(CConst.URL, WebUtil.getServerUrl( ctx ));

        InputStream inputStream = new CmdMkfile().go( params );

        try {

            byte[] bytes = new byte[4096];
            int numBytes = inputStream.read(bytes);
            assertThat( numBytes > 0, is( true));

            JSONObject jsonWrapper = new JSONObject( new String(bytes));
            JSONArray jsonArray = jsonWrapper.getJSONArray("added");
            assertThat( jsonArray.length() > 0, is( true ));
            JSONObject jsonObject = jsonArray.getJSONObject( 0 );

            boolean hasName = jsonObject.has("name");
            assertThat( hasName, is( true ));
            boolean nameMatch = jsonObject.getString("name").contentEquals(uniqueFilename);
            assertThat(nameMatch, is( true ));

            assertThat( targetFile.exists(), is( true ));
            assertThat( targetFile.delete(), is( true ));
            assertThat( targetFile.exists(), is( false ));

        } catch (IOException e) {
            LogUtil.logException(CmdMkfileTest.class, e);
        }
    }
}