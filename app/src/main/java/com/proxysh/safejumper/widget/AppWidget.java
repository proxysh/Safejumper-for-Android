/*
 * Copyleft (/c) MMXV, Proxy.sh
 * Distributed under the GNU GPL v2
 */
package com.proxysh.safejumper.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.proxysh.safejumper.AppActivity;
import com.proxy.sh.safejumper.R;

public class AppWidget extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds ) {

		// initializing widget layout
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
				R.layout.widget_main);

		// Create an Intent to launch ExampleActivity
		//for connect
		Intent intent1 = new Intent(context, AppActivity.class);
		intent1.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent1.setAction(AppActivity.SHORTCUT_CONNECT);
		PendingIntent pendingIntent1 = PendingIntent.getActivity(context, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
		//for disconnect
		Intent intent2 = new Intent(context, AppActivity.class);
		intent2.setAction(AppActivity.SHORTCUT_DISCONNECT);
		PendingIntent pendingIntent2 = PendingIntent.getActivity(context, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
		
		// register for button event
		remoteViews.setOnClickPendingIntent(R.id.btnWidgetConnct, pendingIntent1);
		remoteViews.setOnClickPendingIntent(R.id.btnWidgetDisconnect, pendingIntent2);

		// request for widget update
		ComponentName myWidget = new ComponentName(context,
				AppWidget.class);
		appWidgetManager.updateAppWidget(myWidget, remoteViews);
	}
}
