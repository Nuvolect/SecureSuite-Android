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

package com.nuvolect.securesuite.data;

import android.content.Context;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.nuvolect.securesuite.data.MyGroups.mGroupTitle;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;


/**
 * {@link MyGroups} utility class tests.
 * Also tests {@link SqlCipher} database delete and creation.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MyGroupsTest {

    private String testGroupTitle;
    private String testAccount;
    private String testAccountType;

    @Before
    public void setUp() throws Exception {


        this.testGroupTitle ="Test group title";
        this.testAccount ="test@account.com";
        this.testAccountType ="currently_unused";
        int testGroupId = -1;
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void initGroupMemory() throws Exception {

        /**
         * Create an empty database.
         * Initialize in memory group cache.
         * Create a group instance in database and cache.
         */
        Context ctx = getTargetContext();
        SqlCipher.deleteDatabases(ctx);
        SqlCipher.getInstance(ctx); // Force db creation

        MyGroups.initGroupMemory();

        assertThat(MyGroups.mGroupAccount.size(), is(0));
        assertThat(mGroupTitle.size(), is(0));
        assertThat(MyGroups.mGroupCount.size(), is(0));
        assertThat(MyGroups.mCloudRemapGroupId.size(), is(0));
        assertThat(MyGroups.mGroupIdByAccountPlusTitle.size(), is(0));

        int newId =
               MyGroups.addGroup( ctx, this.testGroupTitle, this.testAccount, this.testAccountType);
        assertThat( newId, not(0));
        assertThat( newId, not(-1));
        assertThat( newId, not(-2));

        assertThat(MyGroups.mGroupAccount.size(), is(1));
        assertThat(mGroupTitle.size(), is(1));
        assertThat(MyGroups.mGroupCount.size(), is(1));
        assertThat(MyGroups.mCloudRemapGroupId.size(), is(0));
        assertThat(MyGroups.mGroupIdByAccountPlusTitle.size(), is(0));

        int cachedId = MyGroups.getGroupId( this.testAccount, this.testGroupTitle);
        assertThat( newId, is( cachedId));

        MyGroups.deleteGroup( ctx, newId, false);
        assertThat(MyGroups.mGroupAccount.size(), is(0));
        assertThat(mGroupTitle.size(), is(0));
        assertThat(MyGroups.mGroupCount.size(), is(0));
        assertThat(MyGroups.mCloudRemapGroupId.size(), is(0));
        assertThat(MyGroups.mGroupIdByAccountPlusTitle.size(), is(0));
    }
}