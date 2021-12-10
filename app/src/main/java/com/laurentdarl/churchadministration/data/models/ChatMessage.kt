package com.laurentdarl.churchadministration.data.models

data class ChatMessage(
    var name: String? = null,
    var message: String? = null,
    var user_id: String? = null,
    var timestamp: String? = null,
    var profile_image: String? = null,
)
