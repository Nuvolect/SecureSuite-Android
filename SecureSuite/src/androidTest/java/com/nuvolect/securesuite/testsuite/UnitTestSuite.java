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

package com.nuvolect.securesuite.testsuite;

import com.nuvolect.securesuite.data.CryptoDbTest;
import com.nuvolect.securesuite.data.CryptoFilesystemTest;
import com.nuvolect.securesuite.data.MyGroupsTest;
import com.nuvolect.securesuite.util.CrypUtilTest;
import com.nuvolect.securesuite.util.KeystoreUtilTest;
import com.nuvolect.securesuite.util.OmniTest;
import com.nuvolect.securesuite.util.PersistTest;
import com.nuvolect.securesuite.webserver.connector.CmdArchiveTest;
import com.nuvolect.securesuite.webserver.connector.CmdExtractTest;
import com.nuvolect.securesuite.webserver.connector.CmdMkfileTest;
import com.nuvolect.securesuite.webserver.connector.CmdRmFileTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite to run multiple tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(
        {
                KeystoreUtilTest.class,
                PersistTest.class,
                MyGroupsTest.class, // Change testEnabled = true, to run full test
                CrypUtilTest.class,
                CryptoFilesystemTest.class,
                CryptoDbTest.class,
                OmniTest.class,
                CmdMkfileTest.class,
                CmdRmFileTest.class,
                CmdArchiveTest.class,
                CmdExtractTest.class,
        }
)
public class UnitTestSuite {
}
