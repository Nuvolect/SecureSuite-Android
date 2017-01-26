package com.nuvolect.securesuite.webserver;//

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.util.Persist;
import com.nuvolect.securesuite.util.TimeUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Generate and manage the login page.
 */
public class LoginHtm {

    private static String templateFile = "login.htm";

    public static String render(Context ctx, String uniqueId, Map<String, String> params) {

        /**
         * Generate the page html and return it as the session response
         */
        return generateHtml( ctx, uniqueId, params);
    }

    private static String generateHtml( Context ctx, String uniqueId, Map<String, String> params) {

        String generatedHtml = "";

        try {
            MiniTemplator t = new MiniTemplator(WebService.assetsDirPath+"/"+templateFile);

            /**
             * Parse parameters and process any updates
             */
            parse( ctx, uniqueId, params);

            // Show contact photo if they have one
            long contact_id = Persist.getProfileId(ctx);
            String encodedImage = "";
            if( contact_id > 0)
                encodedImage = SqlCipher.get(contact_id, SqlCipher.DTab.photo);
            if( encodedImage.isEmpty())
                t.setVariable("contact_photo", "/img/contact_picture_large.png");
            else{
                String contact_photo = "contact_photo.png";
                FileOutputStream out = null;
                try {
                    byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                    Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    out = ctx.openFileOutput(contact_photo, Context.MODE_PRIVATE);
                    decodedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                t.setVariable("contact_photo", ctx.getFilesDir()+"/"+contact_photo);
            }
            t.setVariable("notify_js", CrypServer.getNotify(uniqueId));

            generatedHtml = t.generateOutput();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return generatedHtml;
    }

    private static void parse( Context ctx, String uniqueId, Map<String, String> params) {

        if( params.containsKey(CConst.PASSWORD)){

            String password = params.get(CConst.PASSWORD);

            if( password.contentEquals(Cryp.getLockCode(ctx))) {

                CrypServer.put(uniqueId, "currentPage", CrypServer.URI_ENUM.list_htm.toString());
                LogUtil.log(LogUtil.LogType.LOGIN_HTM, "login approved");
                String status = "Login successful " + TimeUtil.friendlyTimeMDYM(System.currentTimeMillis());
                Cryp.put(CConst.LAST_LOGIN_STATUS, status);
            }
            else{
                CrypServer.notify(uniqueId, "Account or password invalid","warn");
                LogUtil.log(LogUtil.LogType.LOGIN_HTM, "login rejected: " + password);
                String status = "Login failed " + TimeUtil.friendlyTimeMDYM(System.currentTimeMillis());
                Cryp.put(CConst.LAST_LOGIN_STATUS, status);
            }
        }
    }
}
