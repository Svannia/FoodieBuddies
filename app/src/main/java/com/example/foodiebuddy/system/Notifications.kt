package com.example.foodiebuddy.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.foodiebuddy.MainActivity
import com.example.foodiebuddy.R
import com.example.foodiebuddy.viewModels.PreferencesViewModel

val CHANNELS = listOf("USER_JOINED_CHANNEL_ID", "RECIPE_ADDED_CHANNEL_ID", "NEW_FAVOURITE_CHANNEL_ID")
fun createNotifChannels(context: Context) {
    createNotificationChannel(
        context,
        CHANNELS[0],
        context.getString(R.string.notifChannel_newUser),
        context.getString(R.string.notifDesc_newUser)
    )
    createNotificationChannel(
        context,
        CHANNELS[2],
        context.getString(R.string.notifChannel_newRecipe),
        context.getString(R.string.notifDesc_newRecipe)
    )
    createNotificationChannel(
        context,
        CHANNELS[2],
        context.getString(R.string.notifChannel_newFavourite),
        context.getString(R.string.notifDesc_newFavourite)

    )
}

fun sendNotifications(context: Context, channelID: Int, title: String, content: String, intentData: String, preferencesViewModel: PreferencesViewModel) {
    val permission = preferencesViewModel.getNotificationState(CHANNELS[channelID])
    if (permission) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        /*val deepLinkIntent = Intent(Intent.ACTION_VIEW, "foodiebuddy://profile".toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, deepLinkIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)*/

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("notification_intent", true)
            putExtra("notification_channel", channelID)
            putExtra("notification_data", intentData)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        Log.d("Debug", "intent sent $intent with extras ${intent.extras}")
        val uniqueID = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(context, uniqueID, intent, PendingIntent.FLAG_IMMUTABLE)
        Log.d("Debug", "pending intent sent $pendingIntent")

        val notification = NotificationCompat.Builder(context, CHANNELS[channelID])
            .setSmallIcon(R.drawable.notification_logo)
            .setColor(ContextCompat.getColor(context, R.color.logo_purple))
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(channelID, notification)
    }
}
private fun createNotificationChannel(context: Context, id: String, name: String, notificationDescription: String) {
    val notificationChannel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT)
        .apply { description = notificationDescription }
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    notificationManager?.createNotificationChannel(notificationChannel)
}