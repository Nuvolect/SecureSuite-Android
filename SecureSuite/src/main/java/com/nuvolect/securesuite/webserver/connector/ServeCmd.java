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
import android.support.annotation.NonNull;

import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.webserver.admin.CmdPing;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorCommand;

import java.io.InputStream;
import java.util.Map;

/**
 * Dispatch to serve RESTful services.
 * This class will be expanded to serve the full set of elFinder commands.
 */
public class ServeCmd {

    private static final String CMD_PARAM = "cmd";

    private Context context;
    private Map<String, String> params;

    private enum CMD {
        // elFinder commands in documentation order
        open,      // open directory and initializes data when no directory is defined (first iteration)
        file,      // output file contents to the browser (download/preview)
        tree,      // return child directories
        parents,   // return parent directories and its subdirectory children
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
    }

    public ServeCmd(@NonNull Context context, @NonNull Map<String, String> params) {
        this.context = context;
        this.params = params;
    }

    public InputStream process() {
        String commandStr = params.get(CMD_PARAM);
        CMD cmd;

        try {
            cmd = CMD.valueOf(commandStr);
        } catch (IllegalArgumentException e) {
            LogUtil.log(LogUtil.LogType.CONNECTOR_SERVE_CMD, "Illegal command: " + commandStr);
            return null;
        }

        ConnectorCommand command = getConnectorCommand(cmd);
        /**
         * If command wasn't implemented
         */
        if (command == null) {
            return null;
        }

        return command.go(params);
    }

    private ConnectorCommand getConnectorCommand(CMD cmd) {
        switch (cmd) {
            case archive:
                return new CmdArchive(context);
            case duplicate:
                return new CmdDuplicate();
            case extract:
                return new CmdExtract();
            case file:
                return new CmdFile();
            case get:
                return new CmdGet();
            case info:
                return new CmdInfo();
            case ls:
                return new CmdLs();
            case mkdir:
                return new CmdMkdir();
            case mkfile:
                return new CmdMkfile();
            case open:
                return new CmdOpen();
            case parents:
                return new CmdParents();
            case paste:
                return new CmdPaste();
            case ping:
                return new CmdPing();
            case put:
                return new CmdPut();
            case size:
                return new CmdSize();
            case tree:
                return new CmdTree();
            case rename:
                return new CmdRename();
            case resize:
                return new CmdResize();
            case rm:
                return new CmdRm(context);
            case search:
                return new CmdSearch();
            case upload:
                return CmdUpload.getInstance(context);
            case zipdl:
                return new CmdZipdl(context);
            default:
                return null;
        }
    }
}
