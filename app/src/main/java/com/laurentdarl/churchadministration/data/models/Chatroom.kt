package com.laurentdarl.churchadministration.data.models

data class Chatroom(
    var chatroom_name: String? = null,
    var creator_id: String? = null,
    var security_level: String? = null,
    var chatroom_id: String? = null,
    var chatroom_messages: List<ChatMessage>? = null,
    var users: List<String>? = null
)
