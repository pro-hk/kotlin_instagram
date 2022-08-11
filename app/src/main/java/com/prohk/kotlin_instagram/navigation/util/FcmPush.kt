package com.prohk.kotlin_instagram.navigation.util

import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.prohk.kotlin_instagram.navigation.model.PushDTO
import com.squareup.okhttp.*
import java.io.IOException

class FcmPush {
    // push를 전달
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AAAAVSLfUJ8:APA91bEPLi6QMfp_DqidgoDlRkldvBRPC2YVulpvGDmZCqRtPXvAW7qxfkTWewxRQuUwLXB89RaPgDTPsKHKHb9ljIfv4DMNrnr-V-C1I_jkIY7CqCbVEA33Fnlgr2nX5keQMkREZGAA"

    var gson: Gson? = null
    var okHttpClient: OkHttpClient? = null

    companion object {
        var instance = FcmPush()
    }

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }

    fun sendMessage(destinationUid: String, title: String, message: String) {
        FirebaseFirestore.getInstance()
            .collection("pushTokens")
            .document(destinationUid)
            .get()
            .addOnCompleteListener { task ->
            if(task.isSuccessful) {
                var token = task?.result?.get("pushToken").toString()

                var pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification.title = title
                pushDTO.notification.body = message

                var body = RequestBody.create(JSON, gson?.toJson(pushDTO))
                var request = Request.Builder()
                    .addHeader("Content-Type","application/json")
                    .addHeader("Authorization", "key="+serverKey)
                    .url(url)
                    .post(body)
                    .build()

                okHttpClient?.newCall(request)?.enqueue(object: Callback{
                    override fun onFailure(request: Request?, e: IOException?) {

                    }

                    override fun onResponse(response: Response?) {
                        println(response?.body()?.string())
                    }
                })
            }
        }
    }
}