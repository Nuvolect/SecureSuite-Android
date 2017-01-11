package com.nuvolect.securesuite.license;

import android.content.Context;

import com.nuvolect.securesuite.util.DeviceInfo;

import java.util.HashSet;
import java.util.Set;

public class Whitelist {

	/**
	 * Check if a device is on the whitelist.  This is done using the a unique install ID
	 * in place of an email to avoid having to use the accounts permission.
	 * @param ctx
	 * @return
	 */
	public static String getWhiteListCredentials(Context ctx) {

		String uniqueDeviceId = DeviceInfo.getUniqueInstallId(ctx);

		if( developers.contains(uniqueDeviceId))
			return uniqueDeviceId;
		else
			return "";
	}

	/** Build the set of whitelist emails, all must be lower case */
	private static Set<String> developers = new HashSet<String>() {
		private static final long serialVersionUID = 1L;
	{
        add("bfdcdf9d4013bd90"); // Matt's nexus 9
		add("c1e933593df14675"); // Matt's nexus 5
		add("f87effdfee3d8a8a"); // Mom 4.2.2 in genymotion
	}};

}
