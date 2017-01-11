package com.nuvolect.securesuite.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;


/**
 * Simple confirmation dialog.  Cancel is permitted and ignored.
 */
public class DialogConfirm extends DialogFragment {

    public static DialogConfirm newInstance(String title, String message, int msg_id) {

        DialogConfirm frag = new DialogConfirm();
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

        m_dialog = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mCallbacks.onConfirmOk( msg_id);
                        }
                    }
                )
                .setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mCallbacks.onConfirmCancel( msg_id);
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

        public void onConfirmOk( int msg_id);
        public void onConfirmCancel( int msg_id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onConfirmOk(int msg_id){}
        @Override
        public void onConfirmCancel(int msg_id){}
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