package pl.fylypek.kanapka_ai_mobile

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class FetchOptions(
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
)

private fun setRequestMethod(connection: HttpURLConnection, method: String) {
    connection.requestMethod = method
}

private fun setRequestHeaders(connection: HttpURLConnection, headers: Map<String, String>) {
    headers.forEach { (key, value) ->
        connection.setRequestProperty(key, value)
    }
}

private fun setRequestBody(connection: HttpURLConnection, body: String?) {
    if (body == null) return
    connection.doOutput = true
    OutputStreamWriter(connection.outputStream).use { writer ->
        writer.write(body)
        writer.flush()
    }
}

fun fetch(url: String, options: FetchOptions = FetchOptions()): Promise<String> {
    return Promise { resolve, reject ->
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestMethod(this, options.method)
            setRequestHeaders(this, options.headers)
            setRequestBody(this, options.body)
        }

        val responseCode = connection.responseCode
        val inputStream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val response = inputStream?.bufferedReader()?.readText() ?: ""

        resolve(response)
    }
}