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
package com.nuvolect.securesuite.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.nuvolect.securesuite.license.AppSpecific;
import com.nuvolect.securesuite.main.CConst;
import com.nuvolect.securesuite.main.ContactListActivity;

import java.util.ArrayList;

public class NotificationUtil {
	private static final int NOTIFICATION_ID = 1;

	private static ArrayList<String> smallTextHistory = new ArrayList<String>();
	private static int runningTotal = 0;

	/**
	 * Push a notification to the notification area.
	 * Up to 6 previous notifications will be shown as small text.
	 * @param ctx
	 * @param title
     */
	public static void pushNotification(Context ctx, String title) {

		Class<?> nextActivity = ContactListActivity.class;

		// Make sure duplicates are not added to the list
		if( smallTextHistory.size() > 0 && smallTextHistory.get(0).contentEquals( title))
			return;

		if( smallTextHistory.size() > 6){       // Only keep last six

			for(int i = smallTextHistory.size() -1; i > 5; --i)
				smallTextHistory.remove(i);
		}

		Resources res = ctx.getResources();
		Bitmap largeIcon = BitmapFactory.decodeResource( res, CConst.LARGE_ICON);

		// When using proper icon sizes this scaling code is not required
		//		int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
		//		int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
		//		largeIcon = Bitmap.createScaledBitmap(largeIcon, width, height, false);

		@SuppressWarnings("deprecation")
		NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)
				.setTicker( title)
				.setContentTitle( title)
				.setSmallIcon(CConst.SMALL_ICON)
				.setLargeIcon( largeIcon)
				.setAutoCancel( false)
				.setNumber( ++runningTotal)
				.setAutoCancel(true)
				.setContentIntent(
						TaskStackBuilder.from(ctx)
								.addParentStack( nextActivity)
								.addNextIntent(new Intent(ctx, nextActivity)
								).getPendingIntent(0, 0));

		// Use the previous notification as small text, if there is one
		if( smallTextHistory.size() > 0)
			builder.setContentText( smallTextHistory.get(0));

		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

		inboxStyle.setBigContentTitle( title );
		builder.setStyle(inboxStyle);

		for( String textItem : smallTextHistory)
			inboxStyle.addLine(textItem);

		NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify( NOTIFICATION_ID, inboxStyle.build());

		// Save the title as the top of the small text list for next notification
		smallTextHistory.add( 0, title);
	}

	/**
	 * Push a notification to the notification area.
	 * Independently show the title and small text.
	 * The small text also has a history of six items.
	 * @param ctx
	 * @param title
	 * @param smallText
     */
	public static void pushNotification(Context ctx, String title, String smallText) {

		Class<?> nextActivity = ContactListActivity.class;

		// Make sure duplicates are not added to the list
		if( smallTextHistory.size() > 0 && smallTextHistory.get(0).contentEquals( smallText))
		    return;

		smallTextHistory.add( 0, smallText);
		if( smallTextHistory.size() > 6){       // Only keep last six

		    for(int i = smallTextHistory.size() -1; i > 5; --i)
		        smallTextHistory.remove(i);
		}

		Resources res = ctx.getResources();
		Bitmap largeIcon = BitmapFactory.decodeResource( res, CConst.LARGE_ICON);

		// When using proper icon sizes this scaling code is not required
		//		int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
		//		int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
		//		largeIcon = Bitmap.createScaledBitmap(largeIcon, width, height, false);

		@SuppressWarnings("deprecation")
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)
		.setTicker( title)
		.setContentTitle( title)
		.setContentText( smallText)
		.setSmallIcon(CConst.SMALL_ICON)
		.setLargeIcon( largeIcon)
		.setAutoCancel( false)
		.setNumber( ++runningTotal)
		.setAutoCancel(true)
		.setContentIntent(
				TaskStackBuilder.from(ctx)
				.addParentStack( nextActivity)
				.addNextIntent(new Intent(ctx, nextActivity)
				).getPendingIntent(0, 0));

		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

		inboxStyle.setBigContentTitle(AppSpecific.APP_NAME+" Activity");
		builder.setStyle(inboxStyle);

		for( String textItem : smallTextHistory)
			inboxStyle.addLine(textItem);

		NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify( NOTIFICATION_ID, inboxStyle.build());
	}

	public static void cancelAll(Context context) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancelAll();
	}
}
