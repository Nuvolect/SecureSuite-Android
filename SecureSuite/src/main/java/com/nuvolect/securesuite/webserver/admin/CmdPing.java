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


import com.google.gson.JsonObject;
import com.nuvolect.securesuite.webserver.connector.base.ConnectorJsonCommand;

import java.io.InputStream;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * ping
 *
 * Respond with { timestamp: long}
 */
public class CmdPing extends ConnectorJsonCommand {

    @Override
    public InputStream go(@NonNull Map<String, String> params) {
        JsonObject object = new JsonObject();
        object.addProperty("timestamp", System.currentTimeMillis());

        return getInputStream(object);
    }
}
