package com.laurentdarl.churchadministration.domain.fcm

import com.laurentdarl.churchadministration.data.models.fcm.FirebaseCloudMessage
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import java.util.HashMap

interface FCM {
    @POST("send")
    fun send(
        @HeaderMap headers: HashMap<String, String>,
        @Body message: FirebaseCloudMessage?
    ): Call<ResponseBody?>?
}