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

package com.nuvolect.securesuite.util;//

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

/**
 * A fragment that displays a WebView.
 * <p>
 * The WebView is automatically paused or resumed when the Fragment is paused or resumed.
 *
 * http://stackoverflow.com/questions/9544734/cant-add-webviewfragment-derived-class-to-fragmenttransaction
 *
 * You cannot mix API Level 11 native fragments (android.app.Fragment) and Android Support package
 * fragments (android.support.v4.app.Fragment). You cannot create an android.webkit.WebViewFragment
 * and use it with an android.support.v4.app.FragmentActivity, because android.webkit.WebViewFragment
 * extends android.app.Fragment, not android.support.v4.app.Fragment.
 *
 * Either do not use ActionBarSherlock and the Android Support package, by creating an API
 * Level 11+ app, or do not use WebViewFragment (or copy it from the source code and refactor
 * it into your project).
 *
 * @JustLearningAgain: WebViewFragment is about 100 lines of code.
 * Copy it into your project, refactoring it into your own package,
 * and alter it to inherit from the support.v4 edition of Fragment.
 * I did this on one project (not yet released, or I'd point you to it),
 * and so far it has been working fine. – CommonsWare Mar 3 '12 at 19:47
 */
public class MyWebViewFragment extends Fragment {
    private WebView mWebView;
    private boolean mIsWebViewAvailable;

    public MyWebViewFragment() {
    }

    /**
     * Called to instantiate the view. Creates and returns the WebView.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mWebView != null) {
            mWebView.destroy();
        }
        mWebView = new WebView(getActivity());

        mIsWebViewAvailable = true;
        return mWebView;
    }

    /**
     * Called when the fragment is visible to the user and actively running. Resumes the WebView.
     */
    @Override
    public void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    /**
     * Called when the fragment is no longer resumed. Pauses the WebView.
     */
    @Override
    public void onResume() {
        mWebView.onResume();
        super.onResume();
    }

    /**
     * Called when the WebView has been detached from the fragment.
     * The WebView is no longer available after this time.
     */
    @Override
    public void onDestroyView() {
        mIsWebViewAvailable = false;
        super.onDestroyView();
    }

    /**
     * Called when the fragment is no longer in use. Destroys the internal state of the WebView.
     */
    @Override
    public void onDestroy() {
        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }

    /**
     * Gets the WebView.
     */
    public WebView getWebView() {
        return mIsWebViewAvailable ? mWebView : null;
    }
}
