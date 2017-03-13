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
//
//TODO create class description
//

import android.content.Context;

import org.apache.commons.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {

    public static String readFile(Context ctx, File file) {

        return readFile( file);
    }

    public static String readFile( File file){

        String fileContents = "";
        StringBuilder sb = new StringBuilder();

        try {
            InputStream is = new FileInputStream(file);

            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {

                String s = new String( buffer, 0, len, "UTF-8");
                sb.append( s );
            }
            fileContents = sb.toString();

            if( is != null)
                is.close();
        } catch (FileNotFoundException e) {
            LogUtil.logException( FileUtil.class, e);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileContents;
    }

    public static void writeFile(File file, String fileContents) {

        try {
            OutputStream out = null;

            FileUtils.forceMkdirParent( file);

            out = new BufferedOutputStream( new FileOutputStream( file));

            out.write(fileContents.getBytes());

            if( out != null)
                out.close();
        }
        catch (IOException e) {
            LogUtil.log( FileUtil.class, "File write failed: " + e.toString());
        }
    }
}
