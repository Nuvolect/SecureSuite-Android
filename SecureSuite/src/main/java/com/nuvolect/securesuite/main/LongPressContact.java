package com.nuvolect.securesuite.main;

import java.util.ArrayList;
import java.util.List;

import net.sqlcipher.Cursor;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.nuvolect.securesuite.R;

public class LongPressContact {

    private static Activity m_act;
    static LongPressContactCallbacks m_callbacks;
    private static Cursor m_cursor;

    public interface LongPressContactCallbacks {

        void postCommand(SharedMenu.POST_CMD post_cmd);

    }

    public static void longPress(Activity act, Cursor cursor,
            LongPressContactCallbacks callbacks) {

        m_act = act;
        m_cursor = cursor;
        m_callbacks = callbacks;
        longPressContactDialog();
    }

    /**
     * Developer menu: in menu order.  Replaces '_' with ' ' on menu.
     */
    public static enum LongPressContactMenu {
        Edit,
        Delete,
        Share,
    };

    private static void longPressContactDialog() {

        final List<String> stringMenu = new ArrayList<String>();

        for( LongPressContactMenu menuItem : LongPressContactMenu.values()){

            String item = menuItem.toString().replace('_', ' ');
            stringMenu.add( item);
        }
        final CharSequence[] items = stringMenu.toArray(new CharSequence[stringMenu.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(m_act);
        builder//.setTitle("Developer Menu")
        .setItems( items, new DialogInterface.OnClickListener() {

            private SharedMenu.POST_CMD post_cmd;

            public void onClick(DialogInterface dialog, int which) {

                LongPressContactMenu menuItem = LongPressContactMenu.values()[which];

                switch( menuItem){

                case Delete:{

                    post_cmd =
                       SharedMenu.processCmd(m_act, m_cursor, R.id.menu_delete_contact);
                    break;
                }
                case Edit:{

                    post_cmd =
                       SharedMenu.processCmd(m_act, m_cursor, R.id.menu_edit_contact);
                    break;
                }
                case Share:{

                    post_cmd =
                       SharedMenu.processCmd(m_act, m_cursor, R.id.menu_share_contact);
                    break;
                }
                default:
                    break;
                }
                m_callbacks.postCommand( post_cmd );
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

}
