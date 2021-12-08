package com.laurentdarl.churchadministration.data.models

data class User(
    val name: String? = null,
    val phone: String? = null,
    val profile_image: String? = null,
    val user_id: String? = null,
    val security_level: String? = null,
    val messaging_token: String? = null,
    val department: String? = null
)
