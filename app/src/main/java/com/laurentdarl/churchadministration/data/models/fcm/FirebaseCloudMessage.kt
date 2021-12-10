package com.laurentdarl.churchadministration.data.models.fcm

data class FirebaseCloudMessage(
    var to: String? = null,
    var data: Data? = null
)
