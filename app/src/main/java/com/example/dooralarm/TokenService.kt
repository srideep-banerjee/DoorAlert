package com.example.dooralarm

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService

class TokenService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        FirebaseDatabase.getInstance().reference.child("Token").setValue(token)
        super.onNewToken(token)
    }
}