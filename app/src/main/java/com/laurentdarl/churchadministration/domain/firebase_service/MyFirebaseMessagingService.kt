package com.laurentdarl.churchadministration.domain.firebase_service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.laurentdarl.churchadministration.presentation.main.MainActivity
import com.laurentdarl.churchadministration.R
import com.laurentdarl.churchadministration.data.models.Chatroom
import com.laurentdarl.churchadministration.data.utility.UniversalImageLoader
import com.nostra13.universalimageloader.core.ImageLoader
import java.util.HashMap

class MyFirebaseMessagingService: FirebaseMessagingService() {
    private val TAG = "MyFirebaseMsgService"
    private val BROADCAST_NOTIFICATION_ID = 1

    private var mNumPendingMessages = 0

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        //init image loader since this will be the first code that executes if they click a notification
        initImageLoader()

        val identifyDataType = remoteMessage.data[getString(R.string.data_type)]
        //SITUATION: Application is in foreground then only send priority notificaitons such as an admin notification
        if (isApplicationInForeground()) {
            if (identifyDataType == getString(R.string.data_type_admin_broadcast)) {
                //build admin broadcast notification
                val title = remoteMessage.data[getString(R.string.data_title)]
                val message = remoteMessage.data[getString(R.string.data_message)]
                sendBroadcastNotification(title, message)
            }
        } else if (!isApplicationInForeground()) {
            if (identifyDataType == getString(R.string.data_type_admin_broadcast)) {
                //build admin broadcast notification
                val title = remoteMessage.data[getString(R.string.data_title)]
                val message = remoteMessage.data[getString(R.string.data_message)]
                sendBroadcastNotification(title, message)
            } else if (identifyDataType == getString(R.string.data_type_chat_message)) {
                //build chat message notification
                val title = remoteMessage.data[getString(R.string.data_title)]
                val message = remoteMessage.data[getString(R.string.data_message)]
                val chatroomId = remoteMessage.data[getString(R.string.data_chatroom_id)]
                Log.d(TAG, "onMessageReceived: chatroom id: $chatroomId")
                val query =
                    FirebaseDatabase.getInstance().reference.child(getString(R.string.dbnode_chatrooms))
                        .orderByKey()
                        .equalTo(chatroomId)
                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.children.iterator().hasNext()) {
                            val snapshot = dataSnapshot.children.iterator().next()
                            val chatroom = Chatroom()
                            val objectMap: Map<String, Any>? =
                                snapshot.value as HashMap<String, Any>?
                            chatroom.chatroom_id = objectMap!![getString(R.string.field_chatroom_id)].toString()
                            chatroom.chatroom_name = objectMap[getString(R.string.field_chatroom_name)].toString()
                            chatroom.security_level = objectMap[getString(R.string.field_security_level)].toString()
                            chatroom.creator_id = objectMap[getString(R.string.field_creator_id)].toString()

                            Log.d(TAG, "onDataChange: chatroom: $chatroom")
                            val numMessagesSeen = snapshot
                                .child(getString(R.string.field_users))
                                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                                .child(getString(R.string.field_last_message_seen))
                                .value.toString().toInt()
                            val numMessages = snapshot
                                .child(getString(R.string.field_chatroom_messages)).childrenCount
                                .toInt()
                            mNumPendingMessages = numMessages - numMessagesSeen
                            Log.d(
                                TAG,
                                "onDataChange: num pending messages: $mNumPendingMessages"
                            )
                            sendChatmessageNotification(title, message, chatroom)
                        }
                    }
                    override fun onCancelled(databaseError: DatabaseError) {}
                })
            }
        }
    }

    /**
     * Build a push notification for a chat message
     * @param title
     * @param message
     */
    private fun sendChatmessageNotification(title: String?, message: String?, chatroom: Chatroom) {
        Log.d(TAG, "sendChatmessageNotification: building a chatmessage notification")

        //get the notification id
        val notificationId = buildNotificationId(chatroom.chatroom_id.toString())

        // Instantiate a Builder object.
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(
            this,
            getString(R.string.default_notification_channel_name)
        )
        // Creates an Intent for the Activity
//        val pendingIntent = Intent(this, SignedInActivity::class.java)
        val pendingIntent = Intent(this, MainActivity::class.java)
        // Sets the Activity to start in a new, empty task
        pendingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        pendingIntent.putExtra(getString(R.string.intent_chatroom), chatroom)
        // Creates the PendingIntent
        val notifyPendingIntent = PendingIntent.getActivity(
            this,
            0,
            pendingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        //add properties to the builder
        builder.setSmallIcon(R.drawable.ic_chat)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    applicationContext.resources,
                    R.drawable.ic_chat
                )
            )
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentTitle(title)
            .setContentText("New messages in " + chatroom.chatroom_name)
            .setColor(getColor(R.color.blue4))
            .setAutoCancel(true)
            .setSubText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("New messages in " + chatroom.chatroom_name)
                    .setSummaryText(message)
            )
            .setNumber(mNumPendingMessages)
            .setOnlyAlertOnce(true)
        builder.setContentIntent(notifyPendingIntent)
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(notificationId, builder.build())
    }

    /**
     * Build a push notification for an Admin Broadcast
     * @param title
     * @param message
     */
    private fun sendBroadcastNotification(title: String?, message: String?) {
        Log.d(TAG, "sendBroadcastNotification: building a admin broadcast notification")


        // Instantiate a Builder object.
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(
            this,
            getString(R.string.default_notification_channel_name)
        )
        // Creates an Intent for the Activity
//        val notifyIntent = Intent(this, SignedInActivity::class.java)
        val notifyIntent = Intent(this, MainActivity::class.java)
        // Sets the Activity to start in a new, empty task
        notifyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // Creates the PendingIntent
        val notifyPendingIntent = PendingIntent.getActivity(
            this,
            0,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        //add properties to the builder
        builder.setSmallIcon(R.drawable.ic_chat)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    applicationContext.resources,
                    R.drawable.ic_chat
                )
            )
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentTitle(title)
            .setContentText(message)
            .setColor(getColor(R.color.blue4))
            .setAutoCancel(true)
        builder.setContentIntent(notifyPendingIntent)
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(BROADCAST_NOTIFICATION_ID, builder.build())
    }

    private fun buildNotificationId(id: String): Int {
        Log.d(TAG, "buildNotificationId: building a notification id.")
        var notificationId = 0
        for (i in 0..8) {
            notificationId += id[0].toInt()
        }
        Log.d(TAG, "buildNotificationId: id: $id")
        Log.d(TAG, "buildNotificationId: notification id:$notificationId")
        return notificationId
    }

    private fun isApplicationInForeground(): Boolean {
        //check all the activities to see if any of them are running
//        val isActivityRunning = (SignedInActivity.isActivityRunning
//                || ChatActivity.isActivityRunning || AdminActivity.isActivityRunning
//                || ChatroomActivity.isActivityRunning || LoginActivity.isActivityRunning
//                || RegisterActivity.isActivityRunning || SettingsActivity.isActivityRunning)
//        if (isActivityRunning) {
//            Log.d(TAG, "isApplicationInForeground: application is in foreground.")
//            return true
//        }
        Log.d(TAG, "isApplicationInForeground: application is in background or closed.")
        return false
    }

    /**
     * init universal image loader
     */
    private fun initImageLoader() {
        val imageLoader = UniversalImageLoader()
        ImageLoader.getInstance().init(imageLoader.getConfig())
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        // Get updated InstanceID token.
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        Log.e("Refreshed token:",p0)

    }



}