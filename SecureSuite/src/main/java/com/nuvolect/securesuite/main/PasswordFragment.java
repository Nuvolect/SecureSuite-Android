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

package com.nuvolect.securesuite.main;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.util.AppTheme;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Passphrase;

import static com.nuvolect.securesuite.util.Passphrase.getPasswordGenHistory;

public class PasswordFragment extends DialogFragment {

    private static Activity m_act;
    private static PasswordFragment m_fragment;
    private String[] m_password_list;
    private boolean DEBUG = false;
    private boolean rogueFirstSelection;
    private static Callbacks m_listener;


    /**
     * Callback interface used when the generated password is applied to a field.
     */
    public interface Callbacks {

        public void passwordApply(String password);
    }

    public static PasswordFragment newInstance(Activity act) {

        m_act = act;
        m_listener = null;
        m_fragment = new PasswordFragment();

        // Supply no input as an argument, not required, left as example
        Bundle args = new Bundle();
        m_fragment.setArguments(args);

        return m_fragment;
    }

    public void start(){

        FragmentTransaction ft = m_act.getFragmentManager().beginTransaction();
        Fragment prev = m_act.getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        this.show(ft, "dialog");
    }

    public void start(Callbacks listener){

        m_listener = listener;
        start();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.password_fragment, container, false);

        // Add the theme background outline and fill color behind fragment
        AppTheme.applyDrawableShape(m_act, rootView);

        rootView.findViewById(R.id.copyPasswordTv).setOnClickListener(onClickCopyPassword);
        rootView.findViewById(R.id.refreshPasswordButton).setOnClickListener(onClickRefreshPassword);
        rootView.findViewById(R.id.checkBox1).setOnClickListener(onClickCheckBox);
        rootView.findViewById(R.id.checkBox2).setOnClickListener(onClickCheckBox);
        rootView.findViewById(R.id.checkBox3).setOnClickListener(onClickCheckBox);
        rootView.findViewById(R.id.checkBox4).setOnClickListener(onClickCheckBox);
        rootView.findViewById(R.id.checkBox5).setOnClickListener(onClickCheckBox);
        rootView.findViewById(R.id.pw_cancelFl).setOnClickListener(cancelButtonOnClick);
        rootView.findViewById(R.id.clearPasswordListTv).setOnClickListener(onClickClearPasswordList);
        rootView.findViewById(R.id.clearPasteBufferTv).setOnClickListener(onClickClearPasteBuffer);

        /**
         * When the listener is enabled, the fragment will return the password when
         * the Apply key is pressed.
         */
        if( m_listener != null){

            rootView.findViewById(R.id.pw_cancelButton).setVisibility(Button.VISIBLE);
            rootView.findViewById(R.id.pw_cancelButton).setOnClickListener(cancelButtonOnClick);
            rootView.findViewById(R.id.pw_applyButton).setVisibility(Button.VISIBLE);
            rootView.findViewById(R.id.pw_applyButton).setOnClickListener( applyButtonOnClick);
        }

        // Get the current persisted set of password generation parameters
        setGenPassMode(rootView, Passphrase.getPasswordGenMode(m_act));

        // Create the spinner that maintains a history of passwords
        createPasswordSpinner( rootView );

        // Create an ArrayAdapter for the password length spinner
        ArrayAdapter<CharSequence> pw_length_adapter = ArrayAdapter.createFromResource(m_act,
                R.array.password_length_array, android.R.layout.simple_spinner_item);

        // Apply the adapter to the spinner
        Spinner pw_length_spinner = (Spinner) rootView.findViewById(R.id.password_length_spinner);
        pw_length_spinner.setAdapter(pw_length_adapter);
        // No passwords shorter than 4
        pw_length_spinner.setSelection( Passphrase.getPasswordLength(m_act)-4, true);

        pw_length_spinner.setOnItemSelectedListener( new OnItemSelectedListener( ) {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {

                // Capture the password length from the spinner
                // No passwords shorter than 4
                Passphrase.setPasswordLength(m_act, position+4);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // Request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    void createPasswordSpinner(View rootView) {

        m_password_list = getPasswordGenHistory(m_act);

        // Create an ArrayAdapter for the history of passwords
        ArrayAdapter<String> history_adapter = new ArrayAdapter<String>(
                m_act, android.R.layout.simple_spinner_item, m_password_list);

        // Specify the layout to use when the list of choices appears
        history_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        Spinner password_spinner = (Spinner) rootView.findViewById(R.id.password_spinner);
        password_spinner.setAdapter(history_adapter);
        rogueFirstSelection = true;

        password_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {

                if (rogueFirstSelection) {
                    rogueFirstSelection = false;
                    return;
                }
                // Capture the password from the spinner
                Passphrase.setGenPassword(m_act, m_password_list[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    Button.OnClickListener onClickRefreshPassword = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            String randomPassword = "";
            try {
                int mode = getGenPassMode(v.getRootView());
                if (mode == 0) {
                    Toast.makeText(m_act, "Select one of the Advanced options",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                randomPassword = Passphrase.generateRandomString(
                        Passphrase.getPasswordLength(m_act), mode);
            } catch (Exception e) {
                LogUtil.logException(m_act, LogUtil.LogType.SECURE, e);
                Toast.makeText(m_act, "Internal error", Toast.LENGTH_SHORT).show();
            }
            Passphrase.appendPasswordHistory(m_act, randomPassword);
            Passphrase.setGenPassword(m_act, randomPassword);
            createPasswordSpinner(v.getRootView());
        }
    };

    CheckBox.OnClickListener onClickCheckBox = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Passphrase.setPasswordGenMode(m_act, getGenPassMode(v.getRootView()));
        }
    };

    public static int getGenPassMode(View rootView) {

        int mode = 0;

        CheckBox rb1 = (CheckBox) rootView.findViewById(R.id.checkBox1);
        if( rb1.isChecked())
            mode |= Passphrase.ALPHA_UPPER;
        CheckBox rb2 = (CheckBox) rootView.findViewById(R.id.checkBox2);
        if( rb2.isChecked())
            mode |= Passphrase.ALPHA_LOWER;
        CheckBox rb3 = (CheckBox) rootView.findViewById(R.id.checkBox3);
        if( rb3.isChecked())
            mode |= Passphrase.NUMERIC;
        CheckBox rb4 = (CheckBox) rootView.findViewById(R.id.checkBox4);
        if( rb4.isChecked())
            mode |= Passphrase.SPECIAL;
        CheckBox rb5 = (CheckBox) rootView.findViewById(R.id.checkBox5);
        if( rb5.isChecked())
            mode |= Passphrase.HEX;

        return mode;
    }

    public static int setGenPassMode(View rootView, int mode) {

        CheckBox rb1 = (CheckBox) rootView.findViewById(R.id.checkBox1);
        rb1.setChecked( (mode & Passphrase.ALPHA_UPPER) != 0);

        CheckBox rb2 = (CheckBox) rootView.findViewById(R.id.checkBox2);
        rb2.setChecked( (mode & Passphrase.ALPHA_LOWER) != 0);

        CheckBox rb3 = (CheckBox) rootView.findViewById(R.id.checkBox3);
        rb3.setChecked( (mode & Passphrase.NUMERIC) != 0);

        CheckBox rb4 = (CheckBox) rootView.findViewById(R.id.checkBox4);
        rb4.setChecked( (mode & Passphrase.SPECIAL) != 0);

        CheckBox rb5 = (CheckBox) rootView.findViewById(R.id.checkBox5);
        rb5.setChecked( (mode & Passphrase.HEX) != 0);

        return mode;
    }

    TextView.OnClickListener onClickCopyPassword = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            String password = Passphrase.getGenPassword(m_act);
            if (password.isEmpty()) {

                Toast.makeText(m_act, "Password is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    m_act.getSystemService(Context.CLIPBOARD_SERVICE);

            // Creates a new text clip to put on the clipboard
            ClipData clip = ClipData.newPlainText("SecureSuite", password);

            // Set the clipboard's primary clip.
            clipboard.setPrimaryClip(clip);

            Toast.makeText(m_act, "Copied", Toast.LENGTH_SHORT).show();
        }
    };

    TextView.OnClickListener onClickClearPasswordList = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Passphrase.clearPasswordGenHistory(m_act);

            // Clear in-memory list
            createPasswordSpinner(v.getRootView());
        }
    };

    TextView.OnClickListener onClickClearPasteBuffer = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    m_act.getSystemService(Context.CLIPBOARD_SERVICE);

            // Creates a new text clip to put on the clipboard
            ClipData clip = ClipData.newPlainText("SecureSuite", "");

            // Set the clipboard's primary clip.
            clipboard.setPrimaryClip(clip);

            Toast.makeText(m_act, "Paste buffer cleared", Toast.LENGTH_SHORT).show();
        }
    };

    Button.OnClickListener applyButtonOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            m_listener.passwordApply(Passphrase.getGenPassword(m_act));
        }
    };

    Button.OnClickListener cancelButtonOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            m_fragment.dismiss();
        }
    };
}
