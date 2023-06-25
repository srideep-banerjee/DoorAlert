package com.example.dooralarm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.io.File

class ModifiedFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        if (remoteMessage.data.isNotEmpty()) {

            val file = File(filesDir.path + "/logs.txt")
            if(!file.exists())file.createNewFile()
            val dataMap=remoteMessage.data
            file.appendText("${dataMap["deviceID"]},${dataMap["date"]},${dataMap["time"]}\n")

        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            println("Message Notification Body: ${it.body}")
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

}