import com.ryomen.bikee.R

import com.ryomen.bikee.StartBikeReceiver
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class StartBikeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            val intent = Intent(context, StartBikeReceiver::class.java).apply {
                action = "com.ryomen.bikee.START_BIKE"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val views = RemoteViews(context.packageName, R.layout.widget_start_bike)
            views.setOnClickPendingIntent(R.id.widgetBtnStart, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}