package com.laurentdarl.churchadministration.data.models

data class User(
    var name: String? = null,
    var phone: String? = null,
    var profile_image: String? = null,
    var user_id: String? = null,
    var security_level: String? = null,
    val messaging_token: String? = null,
    val department: String? = null
)
