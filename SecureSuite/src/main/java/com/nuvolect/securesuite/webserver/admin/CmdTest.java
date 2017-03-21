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

package com.nuvolect.securesuite.webserver.admin;//

import android.content.Context;

import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Omni;
import com.nuvolect.securesuite.util.OmniFile;
import com.nuvolect.securesuite.util.OmniFiles;
import com.nuvolect.securesuite.webserver.connector.CmdRm;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * test
 *
 * Run a specific test and return the result
 */
public class CmdTest {

    private static Context m_ctx;
    private static long testFileSize = 10 * 1024 * 1024;

    enum TEST_ID {
        write_sdcard_test_file,
        write_crypto_test_file,
        read_sdcard_test_file,
        read_crypto_test_file,
        duplicate_crypto_test_file,
        duplicate_sdcard_test_file,
        copy_crypto_file_to_sdcard,
        copy_sdcard_file_to_crypto,
        delete_sdcard_test_file,
        delete_crypto_test_file,
        delete_sdcard_test_folder,
        delete_crypto_test_folder,
    }

    public static ByteArrayInputStream go(Context ctx, Map<String, String> params) {

        m_ctx = ctx;

        try {
            JSONObject wrapper = new JSONObject();

            String error = "";

            TEST_ID test_id = null;
            try {
                test_id = TEST_ID.valueOf(params.get("test_id"));
            } catch (IllegalArgumentException e) {
                error = "Error, invalid command: "+params.get("cmd");
            }
            long timeStart = System.currentTimeMillis();

            assert test_id != null;

            try {
                switch ( test_id ){

                    case write_sdcard_test_file:
                        write_sdcard_test_file();
                        break;
                    case write_crypto_test_file:
                        write_crypto_test_file();
                        break;
                    case read_sdcard_test_file:
                        read_sdcard_test_file();
                        break;
                    case read_crypto_test_file:
                         read_crypto_test_file();
                        break;
                    case duplicate_crypto_test_file:
                         duplicate_crypto_test_file();
                        break;
                    case duplicate_sdcard_test_file:
                         duplicate_sdcard_test_file();
                        break;
                    case copy_crypto_file_to_sdcard:
                         copy_crypto_file_to_sdcard();
                        break;
                    case copy_sdcard_file_to_crypto:
                         copy_sdcard_file_to_crypto();
                        break;
                    case delete_sdcard_test_file:
                        delete_sdcard_test_file();
                        break;
                    case delete_crypto_test_file:
                        delete_crypto_test_file();
                        break;
                    case delete_sdcard_test_folder:
                        delete_sdcard_test_folder();
                        break;
                    case delete_crypto_test_folder:
                        delete_crypto_test_folder();
                        break;
                    default:
                        error = "Invalid test: "+test_id;
                }
            } catch (Exception e) {
                error = "Exception";
                LogUtil.logException( CmdTest.class, e);
            }

            wrapper.put("error", error);
            wrapper.put("test_id", test_id.toString());
            wrapper.put("delta_time",
                    String.valueOf(System.currentTimeMillis() - timeStart) + " ms");

            return new ByteArrayInputStream(wrapper.toString(2).getBytes("UTF-8"));

        } catch (JSONException e) {
            LogUtil.logException( CmdTest.class, e);
        } catch (UnsupportedEncodingException e) {
            LogUtil.logException( CmdTest.class, e);
        }

        return null;
    }

    private static void delete_crypto_test_folder() {
        String path = "/.sstest";
        OmniFile file = new OmniFile(Omni.cryptoVolumeId, path);
        CmdRm.delete(m_ctx, file);
    }

    private static void delete_sdcard_test_folder() {
        String path = "/.sstest";
        OmniFile file = new OmniFile(Omni.localVolumeId, path);
//        CmdRm.delete(m_ctx, file);
    }

    private static void delete_crypto_test_file() {
        String path = "/.sstest/test.bin";
        OmniFile file = new OmniFile(Omni.cryptoVolumeId, path);
        file.delete();
    }

    private static void delete_sdcard_test_file() {
        String path = "/.sstest/test.bin";
        OmniFile file = new OmniFile(Omni.localVolumeId, path);
//        file.delete();
    }

    private static void copy_sdcard_file_to_crypto() {

        String inPath = "/.sstest/test.bin";
        OmniFile in = new OmniFile(Omni.localVolumeId, inPath);

        String outPath = "/.sstest/~test.bin";
        OmniFile out = new OmniFile(Omni.cryptoVolumeId, outPath);

        OmniFiles.copyFile(in, out);
    }

    private static void copy_crypto_file_to_sdcard() {

        String inPath = "/.sstest/test.bin";
        OmniFile in = new OmniFile(Omni.cryptoVolumeId, inPath);

        String outPath = "/.sstest/~test.bin";
        OmniFile out = new OmniFile(Omni.localVolumeId, outPath);

        OmniFiles.copyFile(in, out);
    }

    private static void duplicate_sdcard_test_file() {
        String inPath = "/.sstest/test.bin";
        OmniFile in = new OmniFile(Omni.localVolumeId, inPath);
        String outPath = "/.sstest/duplicate.bin";
        OmniFile out = new OmniFile(Omni.localVolumeId, outPath);
        OmniFiles.copyFile( in, out);
    }

    private static void duplicate_crypto_test_file() {
        String inPath = "/.sstest/test.bin";
        OmniFile in = new OmniFile(Omni.cryptoVolumeId, inPath);
        String outPath = "/.sstest/duplicate.bin";
        OmniFile out = new OmniFile(Omni.cryptoVolumeId, outPath);
        OmniFiles.copyFile( in, out);
    }

    private static void read_crypto_test_file() throws IOException {
        String path = "/.sstest/test.bin";
        OmniFile file = new OmniFile(Omni.cryptoVolumeId, path);
        file.getParentFile().mkdirs();
        OmniFiles.countBytes( file );
    }

    private static void read_sdcard_test_file() throws IOException {
        String path = "/.sstest/test.bin";
        OmniFile file = new OmniFile(Omni.localVolumeId, path);
        OmniFiles.countBytes( file );
    }
    private static void write_crypto_test_file() throws IOException {
        String path = "/.sstest/test.bin";
        OmniFile file = new OmniFile(Omni.cryptoVolumeId, path);
        file.getParentFile().mkdirs();
        OmniFiles.createFile( file, testFileSize);
    }

    private static void write_sdcard_test_file() throws IOException {
        String path = "/.sstest/test.bin";
        OmniFile file = new OmniFile(Omni.localVolumeId, path);
        try {
            OmniFile localRoot = new OmniFile( Omni.localVolumeId, CConst.ROOT);
            String absRoot = localRoot.getAbsolutePath();
            boolean localRootExists = localRoot.exists();
            boolean localRootIsRoot = localRoot.isRoot();
            boolean localRootIsDir = localRoot.isDirectory();
            boolean localRootIsStd = localRoot.isStd();
            OmniFile localRootFile = new OmniFile( Omni.localVolumeId, "/.rootFile");
            boolean localRootFileExists = localRootFile.exists();
            boolean localRootFileIsRoot = localRootFile.isRoot();
            boolean localRootFileIsDir = localRootFile.isDirectory();
            boolean localRootFileIsStd = localRootFile.isStd();
            boolean writeLocalRootSuccess = localRootFile.writeFile("0123456789");
            localRootFileExists = localRootFile.exists();
            long localRootFileSize = localRootFile.length();


            boolean fileExists = file.exists();
            OmniFile parent = file.getParentFile();
            boolean parentExists = parent.exists();
            boolean createParent = parent.mkdirs();
            boolean isDir = parent.isDirectory();
            boolean result = file.getParentFile().mkdirs();
            boolean writeSuccess = file.writeFile("add something");
            boolean isFile = file.isFile();
            long size = file.length();
            OmniFile[] lst = parent.listFiles();
            OmniFiles.createFile( file, testFileSize);
        } catch (IOException e) {
            LogUtil.logException( CmdTest.class, e);
        }
    }
}
