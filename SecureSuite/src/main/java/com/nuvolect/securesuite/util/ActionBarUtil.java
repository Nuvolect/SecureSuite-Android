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

package com.nuvolect.securesuite.util;//

import android.app.ActionBar;
import android.app.Activity;
import android.widget.ArrayAdapter;

/**
 * Methods specific to using the ActionBar.
 */
public class ActionBarUtil {
    /**
     * Show the Up button in the action bar.
     */
    public static boolean showActionBarUpButton(Activity act){

        return setDisplayHomeAsUpEnabled(act, true);
    }
    public static boolean setDisplayHomeAsUpEnabled(Activity act, boolean state){

        ActionBar actionBar = act.getActionBar();
        if( actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(state);
            return true;
        } else{
            return false;
        }
    }
    public static boolean setDisplayHomeAsUpEnabled(ActionBar actionBar, boolean state){

        if( actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(state);
            return true;
        } else{
            return false;
        }
    }
    public static boolean setDisplayShowTitleEnabled(Activity act, boolean b) {

        ActionBar actionBar = act.getActionBar();
        if( actionBar != null){
            actionBar.setDisplayShowTitleEnabled(b);
            return true;
        } else{
            return false;
        }
    }
    public static boolean setDisplayShowTitleEnabled(ActionBar actionBar, boolean b) {

        if( actionBar != null){
            actionBar.setDisplayShowTitleEnabled(b);
            return true;
        } else{
            return false;
        }
    }

    public static boolean setNavigationMode(ActionBar actionBar, int mode) {

        if( actionBar != null){
            actionBar.setNavigationMode(mode);
            return true;
        } else{
            return false;
        }
    }

    public static boolean setListNavigationCallbacks(
            ActionBar actionBar,
            ArrayAdapter<CharSequence> adapter,
            ActionBar.OnNavigationListener navigationListener) {

        if( actionBar != null){
            actionBar.setListNavigationCallbacks(adapter, navigationListener);
            return true;
        } else{
            return false;
        }
    }
}
