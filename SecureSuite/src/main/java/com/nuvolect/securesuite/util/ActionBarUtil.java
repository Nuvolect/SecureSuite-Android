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
