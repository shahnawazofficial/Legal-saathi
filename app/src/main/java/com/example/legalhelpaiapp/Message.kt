package com.example.legalhelpaiapp

import java.util.Date

data class Message(
    val text: String,
    val isUser: Boolean,
    val timestamp: Date = Date(),
    val id: String = java.util.UUID.randomUUID().toString(),
    var isRead: Boolean = false,
    var isSent: Boolean = true,
    var isDelivered: Boolean = false
)