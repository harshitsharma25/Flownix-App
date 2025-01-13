package com.example.flownix.screens

import android.content.Context
import android.util.Log
import com.example.flownix.R
import com.google.auth.oauth2.GoogleCredentials
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

object AccessToken {

    private const val firebaseMessagingScope = "https://www.googleapis.com/auth/cloud-platform"

    fun getAccessToken(context : Context): String? {
        return try {
            val inputStream: InputStream = context.resources.openRawResource(R.raw.service_account)
            val googleCredential = GoogleCredentials.fromStream(inputStream)
                .createScoped(arrayListOf(firebaseMessagingScope))

            // Refresh the token if expired
            googleCredential.refresh()

            googleCredential.accessToken.tokenValue
        } catch (e: IOException) {
            Log.e("FCM", "Error loading service account or refreshing token: ${e.message}")
            null
        }
    }

}