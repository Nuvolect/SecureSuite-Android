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

package com.nuvolect.securesuite.main;//

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.nuvolect.securesuite.data.MyGroups;
import com.nuvolect.securesuite.data.NameUtil;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.data.SqlSyncTest;
import com.nuvolect.securesuite.license.LicensePersist;
import com.nuvolect.securesuite.util.CustomDialog;
import com.nuvolect.securesuite.util.DialogUtil;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.WorkerCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage a list of developer commands.  This dialog is only displayed for users on the whitelist.
 */
public class DeveloperDialog {

    private static Activity m_act;
    /**
     * True when the developers menu is disabled, ie demos and videos
     */
    private static boolean m_developerIsEnabled = true;

    public static boolean isEnabled() {
        return m_developerIsEnabled;
    }

    /**
     * Developer menu: in menu order.  Replaces '_' with ' ' on menu.
     */
    private static enum DevMenu {
        Toggle_Verbose_LogCat,
        Clear_Data_Close_App,
        Temporary_Disable_Developer_Menu,
        Dump_Group_Title_Table,
        Dump_Account_Data_Table,
        DB_Groups_Validate,
        DB_Validate,
        DB_Repair,
        DB_Counts,
        Start_Incremental_Sync,
        Ping_Pong_Companion_Speed_Test,
        Decrement_App_Version,
        Set_Default_Display_names,
        Test_RateThisApp,
        Test_MakeDonation,
    };

    public static void start(Activity act) {

        m_act = act;

        final List<String> stringMenu = new ArrayList<String>();

        for( DevMenu menuItem : DevMenu.values()){

            String item = menuItem.toString().replace('_', ' ');
            stringMenu.add( item);
        }
        final CharSequence[] items = stringMenu.toArray(new CharSequence[stringMenu.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(m_act);
        builder.setTitle("Developer Menu")
                .setItems( items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        DevMenu menuItem = DevMenu.values()[which];

                        switch( menuItem){

                            case Start_Incremental_Sync:

                                WorkerCommand.queStartIncSync(m_act);
                                break;

                            case Ping_Pong_Companion_Speed_Test:
                                SqlSyncTest.getInstance().pingPongConfirmDiag(m_act);
                                break;
                            case Dump_Group_Title_Table:{

                                MyGroups.dumpGroupTitleTable();
                                break;
                            }
                            case Dump_Account_Data_Table:{

                                MyGroups.dumpAccountDataTable();
                                break;
                            }
                            case Clear_Data_Close_App:{

                                DialogUtil.confirmDialog(
                                        m_act,
                                        "Clear all data?",
                                        "Are you sure? Is your data backed up?",
                                        new DialogUtil.DialogUtilCallbacks() {
                                            @Override
                                            public void confirmed( boolean confirmed) {

                                                if( confirmed ) {
                                                    Persist.clearAll(m_act);
                                                    LicensePersist.clearAll(m_act);
                                                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(m_act);
                                                    pref.edit().clear().commit();
                                                    SqlCipher.deleteDatabases(m_act);
                                                    System.exit(0);
                                                }
                                            }
                                        }
                                );
                                break;
                            }
                            case DB_Groups_Validate:{

                                boolean fixErrors = false;
                                WorkerCommand.queValidateDbGroups(m_act, fixErrors);
                                break;
                            }
                            case DB_Validate:{

                                boolean fixErrors = false;
                                WorkerCommand.queValidateDbCounts(m_act, fixErrors);
                                break;
                            }
                            case DB_Repair:{

                                boolean fixErrors = true;
                                WorkerCommand.queValidateDbCounts(m_act, fixErrors);
                                break;
                            }
                            case DB_Counts:{
                                LogUtil.log(LogUtil.LogType.DEVELOPER_DIALOG, SqlCipher.dbCounts());
                                break;
                            }
                            case Temporary_Disable_Developer_Menu:
                                m_developerIsEnabled = false;
                                m_act.invalidateOptionsMenu();
                                break;
                            case Decrement_App_Version:{

                                int appVersion = LicensePersist.getAppVersion(m_act);
                                if( --appVersion < 1)
                                    appVersion = 1;
                                LicensePersist.setAppVersion(m_act, appVersion);
                                Toast.makeText(m_act, "App version: "+appVersion, Toast.LENGTH_SHORT).show();
                                break;
                            }
                            case Set_Default_Display_names:{
                                new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... voids) {

                                        NameUtil.setAllDefaultDisplayNames();
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Void aVoid) {
                                        super.onPostExecute(aVoid);
                                        Toast.makeText(m_act, "Done", Toast.LENGTH_SHORT).show();
                                    }
                                }.execute();
                                break;
                            }
                            case  Test_RateThisApp:{

                                CustomDialog.rateThisApp(m_act, true);
                                break;
                            }
                            case  Test_MakeDonation:{

                                CustomDialog.makeDonation(m_act, true);
                                break;
                            }
                            case Toggle_Verbose_LogCat:{
                                LogUtil.setVerbose( ! LogUtil.VERBOSE);
                                Toast.makeText(m_act, "Verbose LogCat: "+LogUtil.VERBOSE, Toast.LENGTH_SHORT).show();
                                break;
                            }
                            default:
                                break;
                        }
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

}
