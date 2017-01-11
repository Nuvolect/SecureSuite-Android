package com.nuvolect.securesuite.webserver;//

import android.content.Context;

import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Passphrase;
import com.nuvolect.securesuite.util.Util;

import java.io.File;
import java.io.IOException;

import static com.nuvolect.securesuite.util.LogUtil.log;

public class PasswordModal {

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
     * //WEBAPP purge password model file when logging out or session expire
     * //WEBAPP display message when no advanced options are selected
     * //WEBAPP don't generate a password when no options are selected
     */
    public static void buildPasswordModal(Context ctx, boolean displayUsePasswordButton) {

        try {

            MiniTemplator t = new MiniTemplator(WebService.assetsDirPath+"/password_modal.htm");

            int mode = Passphrase.getPasswordGenMode(ctx);
            int len  = Passphrase.getPasswordLength(ctx);
            // Inflate list of current and past passwords
            String[] history = Passphrase.getPasswordGenHistory(ctx);
            /**
             * Check for first use.  If so generate a password with defaults, save it and use it
             */
            if( history.length == 0){

                String first = Passphrase.generateRandomString(len, mode);
                Passphrase.appendPasswordHistory(ctx, first);
                history = Passphrase.getPasswordGenHistory(ctx);
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
                    CrypServer.password_modal_apply_filename :
                    CrypServer.password_modal_filename;

            File file = new File( ctx.getFilesDir()+"/"+fileName);
            Util.writeFile(file, htm);

        } catch (IOException e) {
            e.printStackTrace();
            log(LogUtil.LogType.LIST_HTM, "updatePasswordModal IOException");
        }


    }
}
