package com.example.rankup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RankUpMessagingService : FirebaseMessagingService() {

    /**
     * Called when a new FCM token is generated (first launch, token refresh, etc.).
     * Saves the fresh token to the signed-in user's Firestore document.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val email = FirebaseAuth.getInstance().currentUser?.email ?: run {
            Log.w("RankUpMessaging", "onNewToken: no signed-in user, token not persisted yet")
            return
        }
        Log.d("RankUpMessaging", "Saving refreshed FCM token for $email")
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(email)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("RankUpMessaging", "Failed to save FCM token: ${e.message}")
            }
    }

    /**
     * Called when a push message arrives while the app is in the foreground,
     * or for data-only messages in any state.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body  = message.notification?.body  ?: message.data["body"]  ?: return
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = CHANNEL_ID
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the channel once (no-op if already exists)
        val channel = NotificationChannel(
            channelId,
            "Match Invitations",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Notifications for new match invitations" }
        manager.createNotificationChannel(channel)

        // Tap opens the app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "rankup_invitations"
    }
}

