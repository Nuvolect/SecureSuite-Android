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

import com.nuvolect.securesuite.main.App;
import com.nuvolect.securesuite.main.CConst;

import org.junit.Before;
import org.junit.Test;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static androidx.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * {@link Omni} utility tests.
 * Assumes android.permission.WRITE_EXTERNAL_STORAGE
 */
public class OmniTest {

    @Before
    public void setup(){
        boolean setupOk = Omni.init( getTargetContext());
        assertThat( setupOk, is( true ));
    }

    @Test
    public void testUserStorage() throws  Exception {

        testStorage( Omni.userVolumeId_0);
    }

    @Test
    public void testCryptoStorage() throws  Exception {

        testStorage( Omni.cryptoVolumeId);
    }

    @Test
    public void testExternalStorage() throws Exception {

        if(App.hasPermission( WRITE_EXTERNAL_STORAGE)){

            testStorage( Omni.localVolumeId);
        }else
        {
            assertThat( true, is( true));
        }
    }
    
    public void testStorage(String volumeId) throws Exception {

        OmniFile rootFolder = new OmniFile( volumeId, CConst.ROOT);
        rootFolder.mkdirs();
        String absRoot = rootFolder.getAbsolutePath()+"/";
        absRoot = absRoot.replace("//","/");
        String getRoot = Omni.getRoot( volumeId);
        assertThat(absRoot.contentEquals( getRoot), is( true));
        String path = rootFolder.getPath();
        assertThat(path, is( CConst.ROOT));

        assertThat( rootFolder.exists(), is( true ));
        assertThat( rootFolder.isRoot(), is( true));
        assertThat( rootFolder.isDirectory(), is( true));
        boolean isCrypt = volumeId.contentEquals( Omni.cryptoVolumeId);
        assertThat( rootFolder.isCryp(), is( isCrypt));

        OmniFile fileInRootDir = new OmniFile( volumeId, "/.rootFileZz");

        if( fileInRootDir.exists())
            fileInRootDir.delete();

        assertThat( fileInRootDir.exists(), is( false ));
        assertThat( fileInRootDir.isRoot(), is( false ));
        assertThat( fileInRootDir.isDirectory(), is( false ));
        assertThat( fileInRootDir.isCryp(), is( isCrypt));
        assertThat( fileInRootDir.writeFile("0123456789"), is( true));
        assertThat( fileInRootDir.exists(), is( true));

        String readString = fileInRootDir.readFile();
        assertThat( readString.contentEquals("0123456789"), is( true ));

        long localRootFileSize = fileInRootDir.length();
        assertThat( localRootFileSize, is( 10L ));

        assertThat( fileInRootDir.delete(), is( true ));
        assertThat( fileInRootDir.exists(), is( false ));

        String[] vIds = Omni.getActiveVolumeIds();
        for( String vId : vIds){
            assertThat( vId.contains("_"), is( false));
        }
    }
}
