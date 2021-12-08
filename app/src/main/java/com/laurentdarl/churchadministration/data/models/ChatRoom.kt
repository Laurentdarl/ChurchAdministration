package com.laurentdarl.churchadministration.data.models

data class ChatRoom(
    val chatroom_name: String? = null,
    val creator_id: String? = null,
    val security_level: String? = null,
    val chatroom_id: String? = null,
    val chatroom_messages: List<ChatMessage>? = null,
    val users: List<String>? = null
)
