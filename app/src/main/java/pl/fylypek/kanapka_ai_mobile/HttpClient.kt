package pl.fylypek.kanapka_ai_mobile

import com.google.gson.Gson
import okhttp3.Cookie
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

enum class PayloadType {
    JSON,
    FORM
}

object HttpClient {
    private val cookieJar = SimpleCookieJar()
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .build()

    fun get(url: String): Promise<String> {
        val request = Request.Builder()
            .url(url)
            .build()

        return Promise { resolve, reject ->
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    reject(Exception("Unexpected code $response"))
                }

                resolve(response.body!!.string())
            }
        }
    }

    private fun processBody(body: Any, type: PayloadType): String {
        return when (type) {
            PayloadType.JSON -> Gson().toJson(body)
            PayloadType.FORM -> {
                val map = body as Map<*, *>
                map.map { (key, value) -> "$key=$value" }.joinToString("&")
            }
        }
    }

    fun post(url: String, body: Any, type: PayloadType = PayloadType.JSON): Promise<String> {
        val textBody = processBody(body, type)
//        val requestBody = textBody.toRequestBody()

//        val request = Request.Builder()
//            .url(url)
//            .post(requestBody)
//            .build()

//        return Promise { resolve, reject ->
//            client.newCall(request).execute().use { response ->
//                if (!response.isSuccessful) {
//                    reject(Exception("Unexpected code $response"))
//                }
//
//                resolve(response.body!!.string())
//            }
//        }

        val fetchOptions = FetchOptions(
            method = "POST",
            body = textBody,
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded"
            )
        )

        return fetch(url, fetchOptions)
    }

    fun listCookies(): List<Cookie> {
        return cookieJar.listCookies()
    }
}