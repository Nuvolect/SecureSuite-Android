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

package com.nuvolect.securesuite.webserver;//

import android.content.Context;

import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Passphrase;
import com.nuvolect.securesuite.util.PassphraseManager;
import com.nuvolect.securesuite.util.Util;

import java.io.File;
import java.io.IOException;

import static com.nuvolect.securesuite.util.LogUtil.log;

public class PasswordModal {

    public static final String PASSWORD_MODAL_FILLED = "password_modal_filled.htm";
    public static final String PASSWORD_MODAL_APPLY_FILLED = "password_modal_apply_filled.htm";
    /**
     * <pre>
     * Update the password modal and save it to a file in the private file area.
     * This gets saved in the init method and again every time a password is generated.
     * This way it is always ready to go and can be loaded from javascript.
     *
     * Dependencies:
     * Passphrase.getPasswordGenMode
     * Passphrase.getPasswordLength
     * Passphrase.getPasswordGenHistory
     * </pre>
     * //TODO purge password model file when logging out or session expire
     * //TODO display message when no advanced options are selected
     * //TODO don't generate a password when no options are selected
     */
    public static void buildPasswordModal(Context ctx, boolean displayUsePasswordButton) {

        try {

            MiniTemplator t = new MiniTemplator(WebService.assetsDirPath+"/password_modal.htm");

            int mode = PassphraseManager.getPasswordGenMode(ctx);
            int len  = PassphraseManager.getPasswordLength(ctx);
            // Inflate list of current and past passwords
            String[] history = PassphraseManager.getPasswordGenHistory(ctx);
            /**
             * Check for first use.  If so generate a password with defaults, save it and use it
             */
            if( history.length == 0){

                String first = Passphrase.generateRandomString(len, mode);
                PassphraseManager.appendPasswordHistory(ctx, first);
                history = PassphraseManager.getPasswordGenHistory(ctx);
            }
            t.setVariable("newest_password", history[0]);

            for(String password : history){

                t.setVariable("pass_item", password);
                t.addBlock("password_history");
            }

            t.setVariable("pass_len", len);

            t.setVariable("password_option_state_caps","value="+((mode & Passphrase.ALPHA_UPPER)>0?"\"1\"":"\"0\""));
            t.setVariable("password_option_state_lc",  "value="+((mode & Passphrase.ALPHA_LOWER)>0?"\"1\"":"\"0\""));
            t.setVariable("password_option_state_09",  "value="+((mode & Passphrase.NUMERIC)    >0?"\"1\"":"\"0\""));
            t.setVariable("password_option_state_spec","value="+((mode & Passphrase.SPECIAL)    >0?"\"1\"":"\"0\""));
            t.setVariable("password_option_state_hex", "value="+((mode & Passphrase.HEX)        >0?"\"1\"":"\"0\""));

            t.setVariable("use_password_button", displayUsePasswordButton ?"display:inline":"display:none");

            String htm = t.generateOutput();
            String fileName = displayUsePasswordButton?
                    PASSWORD_MODAL_APPLY_FILLED :
                    PASSWORD_MODAL_FILLED;

            File file = new File( ctx.getFilesDir()+"/"+fileName);
            Util.writeFile(file, htm);

            log( PasswordModal.class, "Modal generated: " + file.getPath());

        } catch (IOException e) {
            LogUtil.logException(PasswordModal.class, e);
        }
    }
}
