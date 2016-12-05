package com.li.noteclock;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.util.Log;

/**
 * Implementation of App Widget functionality.
 */
public class ClockWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        Log.e("appwidget", "--update--");
        // 创建RemoteViews对象
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.clock_widget);
       // views.setImageViewResource(R.id.double_dot,R.drawable.blue_modern_middle);
        // 将刷新UI的service的必要的数据设置好（此处没有使用Bundle传递数据）
        ClockService.appWidgetManager = appWidgetManager;
        ClockService.context = context;
        ClockService.remoteViews = views;
        // 启动刷新UI的Service
        Intent intent = new Intent(context, ClockService.class);
        context.startService(intent);

        // Instruct the widget manager to update the widget
       //appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.e("appwidget", "--deleted--");
        Intent intent = new Intent(context, ClockService.class);
        context.stopService(intent);
    }
}

