package com.example.flownix.api

import com.example.flownix.models.NotificationPayload
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface NotificationInterface {
    @POST("v1/projects/flownix-3a85a/messages:send")

    fun sendNotification(
        @Header("Authorization") authHeader: String,
        @Body notification: NotificationPayload
    ): Call<Void>
}