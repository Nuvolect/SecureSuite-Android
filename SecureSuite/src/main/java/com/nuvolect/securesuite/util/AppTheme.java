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

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.main.SettingsActivity;

/**
 * Utility for managing the current theme.  Note the theme is either an int
 * from '1' to 'n' or it is an R.style int. For web app use enums SS_THEMES.
 */
public class AppTheme {

    public enum SS_THEMES { nil, // 0
        grey_squares, // 1
        blue_wheat, // 2
        earth_hex,  // 3
        placeHolder4,
        placeHolder5,
        placeHolder6,
        placeHolder7,
        placeHolder8,
        placeHolder9,
        matrix,      // 10
    };

    /**
     * Activate and return the current theme.
     * @param act
     * @return
     */
    public static int activateTheme(Activity act) {

        act.setTheme(getThemeRStyle( act));
        return SettingsActivity.getThemeNumber(act);
    }

    /**
     * <pre>
     * Test a given theme against the current theme, and when different activate the
     * current theme.  Return the current theme.
     * Use:
     * Initiate from an activity onCreate
     *      m_theme = activateTheme( m_act);
     * in onResume:
     *      m_theme = activateWhenChanged( m_act, m_theme);
     * </pre>
     * @param m_act
     * @param theme
     * @return
     */
    public static int activateWhenChanged(Activity m_act, int theme) {

        int currentTheme = SettingsActivity.getThemeNumber( m_act);

        if( currentTheme != theme){

            Intent intent = m_act.getIntent();
            m_act.finish();
            m_act.startActivity(intent);

            return currentTheme;
        }else
            return theme;
    }

    public static void applyActionBarTheme(Activity act, ActionBar actionBar) {

        actionBar.setBackgroundDrawable( new ColorDrawable(getThemeColor( act, R.attr.action_bar_background)));
        actionBar.setStackedBackgroundDrawable(new ColorDrawable(getThemeColor(act, R.attr.action_bar_background)));
    }

    /**
     * Get the current theme R.style
     * @param ctx
     * @return int R.style of current theme
     */
    public static int getThemeRStyle(Context ctx){

        int theme = SettingsActivity.getThemeNumber(ctx);

        return getThemeRStyle( theme);
    }

    /**
     * Settings saves the current theme as a string '1' to 'n'.  It is converted
     * to an int then this method will return the actual theme R.style.
     * @param themeValue
     * @return int theme R.style
     */
    public static int getThemeRStyle( int themeValue){

        switch( themeValue ){
        case 1:
            return R.style.AppTheme1;
        case 2:
            return R.style.AppTheme2;
        case 3:
            return R.style.AppTheme3;
        case 4:
            return R.style.AppTheme4;
        case 5:
            return R.style.AppTheme5;
        case 6:
            return R.style.AppTheme6;
        case 7:
            return R.style.AppTheme7;
        case 8:
            return R.style.AppTheme8;
        case 9:
            return R.style.AppTheme9;
        case 10:
            return R.style.AppTheme10;
        default:
            return R.style.AppTheme1;
        }
    }

    /**
     * Return the background drawable for the current theme.  This will
     * be the image behind all of content.
     * @param m_act
     * @return drawable
     */
    public static Drawable backgroundDrawableImage(Activity m_act) {

        int currentTheme = SettingsActivity.getThemeNumber(m_act);

        switch( currentTheme ){
        case 1:
            return m_act.getResources().getDrawable(R.drawable.app_theme_1);
        case 2:
            return m_act.getResources().getDrawable(R.drawable.app_theme_2);
        case 3:
            return m_act.getResources().getDrawable(R.drawable.app_theme_3);
        case 4:
            return m_act.getResources().getDrawable(R.drawable.app_theme_4);
        case 5:
            return m_act.getResources().getDrawable(R.drawable.app_theme_5);
        case 6:
            return m_act.getResources().getDrawable(R.drawable.app_theme_6);
        case 7:
            return m_act.getResources().getDrawable(R.drawable.app_theme_7);
        case 8:
            return m_act.getResources().getDrawable(R.drawable.app_theme_8);
        case 9:
            return m_act.getResources().getDrawable(R.drawable.app_theme_9);
        case 10:
            return m_act.getResources().getDrawable(R.drawable.app_theme_10);
        default:
        }
        return m_act.getResources().getDrawable(R.drawable.app_theme_1);
    }

    /**
     * Return the background drawable for the current theme.  This will
     * be the shape background and border behind and containing a layout.
     * @param m_act
     * @return drawable
     */
    public static Drawable backgroundDrawableShape(Activity m_act) {

        int currentTheme = SettingsActivity.getThemeNumber( m_act);

        switch( currentTheme ){
        case 1:
            return m_act.getResources().getDrawable(R.drawable.app_theme_1_shape);
        case 2:
            return m_act.getResources().getDrawable(R.drawable.app_theme_2_shape);
        case 3:
            return m_act.getResources().getDrawable(R.drawable.app_theme_3_shape);
        case 4:
            return m_act.getResources().getDrawable(R.drawable.app_theme_4_shape);
        case 5:
            return m_act.getResources().getDrawable(R.drawable.app_theme_5_shape);
        case 6:
            return m_act.getResources().getDrawable(R.drawable.app_theme_6_shape);
        case 7:
            return m_act.getResources().getDrawable(R.drawable.app_theme_7_shape);
        case 8:
            return m_act.getResources().getDrawable(R.drawable.app_theme_8_shape);
        case 9:
            return m_act.getResources().getDrawable(R.drawable.app_theme_9_shape);
        case 10:
            return m_act.getResources().getDrawable(R.drawable.app_theme_10_shape);
        default:
        }
        return m_act.getResources().getDrawable(R.drawable.app_theme_1_shape);
    }

    /**
     * Given a theme attribute defined in themes.xml and attrs.xml, return the color
     * @param ctx
     * @param attr
     * @return
     */
    public static int getThemeColor(Context ctx, int attr){

        TypedValue typedValue = new TypedValue();
        Theme theme = ctx.getTheme();
        theme.resolveAttribute( attr, typedValue, true);
        int color = typedValue.data;

        return( color);
    }
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void applyDrawableShape(Activity m_act, View view) {

        if( Build.VERSION.SDK_INT >= 16)
            view.setBackground( AppTheme.backgroundDrawableShape( m_act));
        else
            view.setBackgroundDrawable( AppTheme.backgroundDrawableShape( m_act));
    }

    /**
     * <pre>
     * Make a color version of a drawable
     * Example:
     *   Drawable d = AppTheme.colorBackgroundDrawable( m_act, R.drawable.ic_menu_star_filled, h1_color);
     * </pre>
     * @param drawable - source drawable
     * @param color - color to apply
     */
    public static Drawable colorDrawable(Activity act, int drawable, int color) {

        Drawable d = act.getResources().getDrawable( drawable );
        d.mutate().setColorFilter( color, Mode.MULTIPLY);
        return d;
    }

    /**
     * <pre>
     * Make a color version of a drawable and apply it to an image view
     * Example:
     *   AppTheme.colorBackgroundDrawable( m_act, imageView, R.drawable.ic_menu_star_filled, h1_color);
     * </pre>
     * @param act
     * @param iv imageview
     * @param color
     */
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void colorBackgroundDrawable(Activity act, ImageView iv, int drawable, int color) {

        Drawable d2 = act.getResources().getDrawable( drawable );
        d2.mutate().setColorFilter( color, Mode.MULTIPLY);
        if( Build.VERSION.SDK_INT >= 16)
            iv.setBackground( d2 );
        else
            iv.setBackgroundDrawable( d2 );
    }

    /**
     * Given a theme key, return the ordinal theme number.
     * @param themeString
     * @return
     */
    public static int getThemeNumber(String themeString){

        SS_THEMES theme = SS_THEMES.valueOf(themeString);

        return theme.ordinal();
    }

    /**
     * Return the ordinal number of the current theme.
     * @param ctx
     * @return
     */
    public static String getThemeName(Context ctx){

        int intTheme = SettingsActivity.getThemeNumber(ctx);

        SS_THEMES theme = SS_THEMES.values()[intTheme];

        return theme.toString();
    }

    public static String getThemeName(Context ctx, int intTheme) {

        SS_THEMES theme = SS_THEMES.values()[intTheme];

        return theme.toString();
    }
}
