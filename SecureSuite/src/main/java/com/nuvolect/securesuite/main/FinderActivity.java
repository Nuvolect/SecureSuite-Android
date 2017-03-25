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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.util.LogUtil;
import com.nuvolect.securesuite.webserver.CrypServer;
import com.nuvolect.securesuite.webserver.WebUtil;


/**
 * Activity to run a full screen webview.
 */
public class FinderActivity extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView( R.layout.finder_activity);

        if(CrypServer.isServerEnabled()){

            startFileManagerFragment();
        }else{
            Toast.makeText(this, "Enable server to user finder",Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        LogUtil.log("FinderActivity.onBackPressed");
        finish();
    }

    private void startFileManagerFragment() {

        String url = WebUtil.getServerUrl(getApplicationContext())+CConst.ELFINDER_PAGE;
        WebFragment webFragment = new WebFragment().newInstance(url);
        String fragmentTag = "webFragmentTag";

        android.support.v4.app.FragmentTransaction ft
                = getSupportFragmentManager().beginTransaction();

        ft.replace(R.id.finder_webview, webFragment, fragmentTag);
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();
    }

}
