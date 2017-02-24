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

package com.nuvolect.securesuite.main;//

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.nuvolect.securesuite.R;
import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.util.Cryp;
import com.nuvolect.securesuite.util.LogUtil;

import net.sqlcipher.Cursor;

/**
 * Present a cancelable dialog to the user to search contacts.
 * A listener is used to send results back to the caller.
 */
public class SearchDialog {

    private static AlertDialog m_dialog;
    private static String m_search;
    private static SearchViewAdapter m_searchAdapter;
    private static Activity m_act;
    private static ListView m_listView;
    private static Cursor m_cursor;
    private static EditText m_searchEt;

    public interface SearchCallbacks {

        void onContactSelected(long contact_id);
    }

    public static void manageSearch(final Activity act,
                                    final SearchCallbacks listener) {

        m_act = act;
        m_search = Cryp.get(CConst.SEARCH, "");
        AlertDialog.Builder builder = new AlertDialog.Builder(act);

        View view = act.getLayoutInflater().inflate(R.layout.search_list_content, null);

        TextView backArrowTv = (TextView) view.findViewById(R.id.backArrowTv);
        backArrowTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleanUp();
            }
        });
        TextView clearSearchTv = (TextView) view.findViewById(R.id.clearSearchTv);
        clearSearchTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * If the search is already empty.  Treat user intent as an exit
                 */
                if( m_search.isEmpty()){
                    cleanUp();
                }else{
                    m_search = "";
                    LogUtil.log("SearchDialog.clearSearchTv: " + m_search);
                    m_searchEt.setText(m_search);
                    Cryp.put(CConst.SEARCH, m_search);
                    updateAdaptor();
                }
            }
        });
        m_listView = (ListView) view.findViewById(R.id.searchResultLv);
        updateAdaptor();

        m_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long _id) {

                long contact_id = SqlCipher.getDTabContactId(m_cursor, position);
                cleanUp();

                listener.onContactSelected(contact_id);
            }
        });

        m_searchEt = (EditText) view.findViewById(R.id.searchEt);
        /**
         * Persist the search if there is one, otherwise let the hint show
         */
        if( ! m_search.isEmpty())
            m_searchEt.setText( m_search);
        m_searchEt.requestFocus();

        m_searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                m_search = m_searchEt.getText().toString();
                Cryp.put(CConst.SEARCH, m_search);
                LogUtil.log("SearchDialog.onTextChanged: " + m_search);

                updateAdaptor();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        builder.setView(view);
        m_dialog = builder.create();
        m_dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {

                LogUtil.log("SearchDialog.onCancel: " + m_search);
                cleanUp();
            }
        });
        m_dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                LogUtil.log("onKey: " + keyCode);

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    cleanUp();
                }
                return false;
            }
        });
        m_dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        m_dialog.show();
    }
    private static void updateAdaptor() {

        //TODO mirror search from Web app.  App uses DTab, web app uses ATab
        Cursor new_cursor;
        if( m_search.length() < 1)
            new_cursor = SqlCipher.getSearchDTabCursor("oSoX0gxX_unmatchable_string");
        else
            new_cursor = SqlCipher.getSearchDTabCursor(m_search);

        if( m_searchAdapter == null){

            LogUtil.log("SearchDialog.updateAdapter new: " + m_search);
            m_searchAdapter = new SearchViewAdapter(m_act, new_cursor, 0,
                    R.layout.search_list_item, m_search);

            m_listView.setAdapter(m_searchAdapter);

        } else{

            LogUtil.log("SearchDialog.updateAdapter update: " + m_search);
            m_searchAdapter.setSearch( m_search);
            m_searchAdapter.changeCursor(new_cursor);
            m_searchAdapter.notifyDataSetChanged();
        }
        m_cursor = new_cursor; // Save the cursor to be later closed
    }

    private static void cleanUp(){

        if (m_dialog!= null && m_dialog.isShowing())
            m_dialog.dismiss();

        if( m_cursor != null && ! m_cursor.isClosed())
            m_cursor.close();

        m_searchAdapter = null;
    }
}
