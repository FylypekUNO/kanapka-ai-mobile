package pl.fylypek.kanapka_ai_mobile

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(cookieManager)

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        request()
            .then { response -> response.text() }
            .then { response ->
                println("Response: $response")
                "fetched"
            }
            .catch { error ->
                println("Error: $error")
                "error while fetching"
            }
            .then { result ->
                val outputView = TextView(this)
                outputView.text = when (result) {
                    is Either.Resolved -> "Success: ${result.value}"
                    is Either.Rejected -> "Error: ${result.value}"
                }

                runOnUiThread {
                    setContentView(outputView)
                }
            }

    }

    private fun request(): Promise<HttpResponse> {
        return fetch("...")
    }
}
