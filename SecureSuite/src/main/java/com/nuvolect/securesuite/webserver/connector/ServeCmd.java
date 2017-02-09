/*
 * Copyright (c) 2017. Nuvolect LLC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.nuvolect.securesuite.webserver.connector;//

import android.content.Context;

import com.nuvolect.securesuite.util.LogUtil;

import java.io.InputStream;
import java.util.Map;

//TODO create class description
//
public class ServeCmd {

    private static boolean DEBUG = LogUtil.DEBUG;

    enum CMD {
        // elFinder commands in documentation order
        open,      // open directory and initializes data when no directory is defined (first iteration)
        file,      // output file contents to the browser (download/preview)
        tree,      // return child directories
        parents,   // return parent directories and its subdirectory childs
        ls,        // list files in directory
        tmb,       // create thumbnails for selected files
        size,      // return size for selected files or total folder(s) size
        dim,       // return image dimensions
        mkdir,     // create directory
        mkfile,    // create text file
        rm,        // delete files/directories
        rename,    // rename file
        duplicate, // create copy of file
        paste,     // copy or move files
        upload,    // upload file
        get,       // output plain/text file contents (preview)
        put,       // save text file content
        archive,   // create archive
        extract,   // extract archive
        search,    // search for files
        info,      // return info for files. (used by client "places" ui)
        resize,    // modify image file (resize/crop/rotate)
        url,       // return file url
        netmount,  // mount network volume during user session. Only ftp now supported.
        ping,      // simple ping, returns the time
        zipdl,     // zip and download files

        // Image-swipe commands
        image_query,//TODO remove

        // App commands
        debug,     // debugging commands
        login,
        logout,
        pentest,   // penetration testing
        test,      // run a test
    }

    public static InputStream process(Context ctx, Map<String, String> params) {

        String error = "";

        CMD cmd = null;
        try {
            cmd = CMD.valueOf(params.get("cmd"));
        } catch (IllegalArgumentException e) {
            error = "Error, invalid command: "+params.get("cmd");
        }
        InputStream inputStream = null;

        switch ( cmd){

            case debug:
                inputStream = CmdDebug.go(ctx, params);
                break;
            default:
                LogUtil.log(LogUtil.LogType.SERVE, "Invalid connector command: "+error);
        }

        return inputStream;
    }
}
