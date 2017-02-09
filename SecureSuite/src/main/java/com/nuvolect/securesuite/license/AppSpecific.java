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

package com.nuvolect.securesuite.license;

import android.net.Uri;

/**
 * Details of the license specific to this app. Keep separate from other license classes
 * to enable plug-in-play ease of maintenance.
 */
public class AppSpecific {

    public static final String APP_NAME                   = "SecureSuite";

    public static final String APP_CRYP_SEED_HEX          = "34694f5f7969cac1750e084dace6b6c3";

    public final static String GOOGLE_PLAY_HREF_URL       = "<a href='https://play.google.com/store/apps/details?id=com.nuvolect.securesuite'>SecureSuite at Google Play</a>";
    public final static String TOC_HREF_URL               = "<a href='http://nuvolect.com/securesuite_terms'>Terms and Conditions</a>";
    public final static String PP_HREF_URL                = "<a href='http://nuvolect.com/privacy'>Privacy Policy</a>";

    public final static String APP_GOOGLE_PLAY_URL        = "https://play.google.com/store/apps/details?id=com.nuvolect.securesuite";
    public static final String APP_TERMS_URL              = "https://nuvolect.com/securesuite_terms/";
    public static final String APP_INFO_URL               = "https://nuvolect.com/securesuite/";
    public static final String APP_HELP_URL               = "https://nuvolect.com/securesuite_help/";
    public final static Uri APP_GOOGLE_PLAY_URI           = Uri.parse("market://details?id=com.nuvolect.securesuite");

}