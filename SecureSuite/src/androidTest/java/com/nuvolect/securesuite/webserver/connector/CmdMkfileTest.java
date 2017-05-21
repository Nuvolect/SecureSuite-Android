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
import com.nuvolect.securesuite.util.Omni;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.util.TestFilesHelper;

import org.junit.Test;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Exercise the connector "mkfile" command, {@link CmdMkfile}
 */
public class CmdMkfileTest {

    @Test
    public void go() throws Exception {

        Context ctx = getTargetContext();
        SqlCipher.getInstance(ctx);

        assertThat (Omni.init(ctx), is(true));

        OmniFile targetFile = TestFilesHelper.createFile(ctx, "/", ".filenameNeverGuessZez");
        assertThat(targetFile.delete(), is(true));
        assertThat( targetFile.exists(), is(false));
    }
}