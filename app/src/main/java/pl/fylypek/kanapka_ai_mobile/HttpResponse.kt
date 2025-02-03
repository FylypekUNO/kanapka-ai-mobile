package pl.fylypek.kanapka_ai_mobile

import com.google.gson.Gson

data class HttpResponse(
    val status: Int,
    val headers: Map<String, List<String>>,
    val body: String
)

inline fun <reified T> HttpResponse.json(): T {
    return Gson().fromJson(this.body, T::class.java)
}