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

package com.nuvolect.securesuite.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.nuvolect.securesuite.R;


/**
 * Present a dialog to the user for input. A dialog title and message is presented to the user
 * and the user can cancel or select OK.  Upon OK, a callback is used to pass the user
 * input and a message ID.  This allows the same activity to call the dialog multiple times
 * and know what response is being provided.
 */
public class DialogInput extends DialogFragment {

    public static DialogInput newInstance(String title, String message, int msg_id) {

        DialogInput frag = new DialogInput();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        args.putInt("msg_id", msg_id);
        frag.setArguments(args);
        return frag;
    }

    private AlertDialog m_dialog;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String title = getArguments().getString("title");
        String message = getArguments().getString("message");
        final int msg_id = getArguments().getInt("msg_id");

        LayoutInflater i = getActivity().getLayoutInflater();
        View v = i.inflate(R.layout.dialog_input,null);
        final EditText m_et = (EditText) v.findViewById(R.id.txt_input);

        m_dialog = new AlertDialog.Builder(getActivity())
//                .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(title)
                .setMessage(message)
                .setView(v)
                .setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            String text_input = Safe.safeString( m_et.getText().toString().trim());
                            // Inform calling activity
                            mCallbacks.onInputEntered(text_input, msg_id);
                        }
                    }
                )
                .setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            LogUtil.log("cancel");

                            // Inform calling activity
                            mCallbacks.onInputCancel();
                        }
                    }
                )
                .create();

        return m_dialog;
    }

    /**
     * The fragment's current callback object, which is notified of user interaction.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * A callback interface that all activities containing this fragment must implement.
     */
    public interface Callbacks {

        public void onInputEntered(String input_text, int msg_id);
        public void onInputCancel();
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onInputEntered(String input_text, int msg_id){}
        @Override
        public void onInputCancel(){}
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

}