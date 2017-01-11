/*******************************************************************************
 * Copyright (c) 2011 - 2014, Nuvolect LLC. All Rights Reserved.
 * All intellectual property rights, including without limitation to
 * copyright and trademark of this work and its derivative works are
 * the property of, or are licensed to, Nuvolect LLC.
 * Any unauthorized use is strictly prohibited.
 ******************************************************************************/
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
