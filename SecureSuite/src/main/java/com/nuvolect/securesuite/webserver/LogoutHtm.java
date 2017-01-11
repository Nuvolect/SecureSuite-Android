package com.nuvolect.securesuite.webserver;//

import android.content.Context;

import java.io.IOException;
import java.util.Map;

/** Generate content and manage user interactions of the Logout HTML page. */
public class LogoutHtm {
    public static String render(Context ctx, Map<String, String> params) {
        String templateFile = "logout.htm";
        String generatedHtml = "";

        try {
            MiniTemplator t = new MiniTemplator(WebService.assetsDirPath+"/"+templateFile);

            generatedHtml = t.generateOutput();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return generatedHtml;
    }
}
