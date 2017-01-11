package com.nuvolect.securesuite.util;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.nuvolect.securesuite.data.MyAccounts;


/**
 * Select one of the user accounts.  Cancel is permitted and ignored.
 */
public class DialogAccount extends DialogFragment {

    public static DialogAccount newInstance(String title, int msg_id) {

        DialogAccount frag = new DialogAccount();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putInt("msg_id", msg_id);
        frag.setArguments(args);
        return frag;
    }

    private AlertDialog m_dialog;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String title = getArguments().getString("title");
        final int msg_id = getArguments().getInt("msg_id");

        List<String> list = Arrays.asList( MyAccounts.getAccounts());
        final CharSequence[] cs = list.toArray( new CharSequence[ list.size()]);;

        m_dialog = new AlertDialog.Builder(getActivity())
                .setTitle(title)
//                .setMessage(message) // do not set message, it will override setItems
                .setItems( cs,
                    new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface arg0, int position) {

                            // Inform calling activity
                            if( position >=0 )
                                mCallbacks.onAccountSelectOk( cs[ position].toString(), msg_id);
                    }})
                .setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
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

        public void onAccountSelectOk(String account, int msg_id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onAccountSelectOk(String account, int msg_id){}
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