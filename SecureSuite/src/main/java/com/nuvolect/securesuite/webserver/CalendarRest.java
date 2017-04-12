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

package com.nuvolect.securesuite.webserver;

import android.content.Context;

import com.nuvolect.securesuite.data.SqlCipher;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.util.LogUtil;

import org.joda.time.DateTime;
import org.json.JSONArray;

import java.util.Map;

/**
 * Calendar REST services
 *
 *
 * Request:
 {
   start=2017-04-30,
   _=1491578379806,
   unique_id=1490473603271,
   url=https: //10.0.1.3: 8002,
   end=2017-06-11,
   queryParameterStrings=start=2017-04-30&end=2017-06-11&_=1491578379806,
   uri=/calendar
 }
 * Response format:
 * [
 {
 "id": 26265,
 "title": "asdf",
 "description": "dsafdsafsdaf",
 "start": "2017-07-01T02:00:00Z",
 "end": "2016-12-01T00:15:00Z",
 "allDay": true,
 "recurring": false
 },
 {
 "id": 41046,
 "title": "test",
 "description": "rrrr",
 "start": "2017-04-15T09:00:00Z",
 "end": "2017-04-15T09:10:00Z",
 "allDay": false,
 "recurring": true
 },
 {
 "id": 43059,
 "title": "Day Shift",
 "description": "AA",
 "start": "2017-04-11T08:00:00Z",
 "end": "2017-04-11T16:15:00Z",
 "allDay": true,
 "recurring": true
 },
 "id": 51347,
 "title": "dsdsd",
 "description": "dsdsdsdsdsdsd",
 "start": "2017-04-11T17:57:00Z",
 "end": "2017-04-11T18:12:00Z",
 "allDay": false,
 "recurring": true
 }
 ]
 */
public class CalendarRest {

    private enum CMD {
        NIL,
        getevents,
        events,
        save,
        delete,
    }
    public static String process(Context ctx, Map<String, String> params) {

        int id = 0;
        long start=0;
        long end=0;

        CMD cmd = CMD.NIL;
        try {
            String[] parts = params.get(CConst.URI).split("/");
            cmd = CMD.valueOf(parts[2]);
        } catch (Exception e) {
            LogUtil.log(LogUtil.LogType.CAL_REST, "Error invalid command: " + params);
            LogUtil.logException(ctx, LogUtil.LogType.CAL_REST, e);
        }
        LogUtil.log(LogUtil.LogType.CAL_REST, "cmd=" + cmd +" params: "+params);

        if( params.containsKey("id")){
            id = Integer.valueOf(params.get("id"));
        }
        if( params.containsKey("start")){
            start = new DateTime( params.get("start")).getMillis();
        }
        if( params.containsKey("end")){
            end = new DateTime( params.get("end")).getMillis();
        }

        LogUtil.log( "cmd: "+cmd);
        LogUtil.log( "id: "+id);
        LogUtil.log( "start: "+start);
        LogUtil.log( "  end: "+end);

        try {

            switch ( cmd ){

                case NIL:
                    break;
                case events:
                case getevents:
                    JSONArray jsonArray = SqlCipher.getEvents( start, end);
                    return jsonArray.toString();
                case save:{

                    return SqlCipher.putEvent( id, start, end, params.get("data")).toString();
                }
                case delete:
                    return SqlCipher.deleteEvent( id ).toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new JSONArray().toString();
    }
}
