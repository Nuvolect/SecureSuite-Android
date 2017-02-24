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
package com.nuvolect.securesuite.main;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.util.WorkerCommand;
import com.nuvolect.securesuite.webserver.CrypServer;
import com.nuvolect.securesuite.webserver.WebService;
import com.nuvolect.securesuite.webserver.WebUtil;

public class BootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context ctx, Intent intent) {

		// Initialize SQL Cipher database
		SqlCipher.getInstance(ctx);

		if(CrypServer.isServerEnabled()){

        	// Start LAN web server
        	Intent serverIntent = new Intent( ctx, WebService.class);
        	ctx.startService(serverIntent);
		}

		/**
		 * If app is on the wifi LAN, sync any pending changes.
		 */
		if( WebUtil.wifiEnabled(ctx))
			WorkerCommand.queStartIncSync(ctx);
	}
}
