package com.nuvolect.securesuite.util;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtil {

	private static SimpleDateFormat formatMDYM = new SimpleDateFormat("MMM d, yyyy  K:mm a", Locale.US);
	private static SimpleDateFormat formatHrMinSec = new SimpleDateFormat("K:mm:ss a", Locale.US);
	/**
	 * Return time as a string in a user friendly format
	 * @param t
	 * @return string
	 */
	static public String friendlyTimeMDYM(long t){

		formatMDYM.setTimeZone(TimeZone.getDefault());
		return formatMDYM.format( t );
	}
	/**
	 * Return time as a string in a user friendly format
	 * @param t
	 * @return string
	 */
	static public String friendlyTimeHrMinSec(long t){

		formatHrMinSec.setTimeZone(TimeZone.getDefault());
		return formatHrMinSec.format( t );
	}

}
