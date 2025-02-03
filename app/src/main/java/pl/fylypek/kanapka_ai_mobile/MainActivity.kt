package pl.fylypek.kanapka_ai_mobile

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            csrfRequest()
                .then { return@then loginRequest() }
                .catch { error ->
                    val outputView = TextView(this)
                    outputView.text = error.message

                    setContentView(outputView)
                }
        } catch (e: Exception) {
            val outputView = TextView(this)
            outputView.text = e.message

            setContentView(outputView)
        }
    }

    fun csrfRequest(): Promise<Any?> {
        val url = "http://192.168.1.21:3000/api/auth/csrf"

        println("csrfRequest")

        return HttpClient.get(url)
            .then { text -> Gson().fromJson(text, Map::class.java) }
            .then { data ->
                val outputView = TextView(this)
                outputView.text = HttpClient.listCookies().toString()

                setContentView(outputView)

                outputView.textSize = 18f
            }
            .catch { error ->
                val outputView = TextView(this)
                outputView.text = error.message

                setContentView(outputView)
            }
    }

    fun loginRequest(): Promise<Any?> {
        val url = "http://192.168.1.21:3000/api/auth/callback/credentials"

        val csrfToken = HttpClient.listCookies().find { it.name == "next-auth.csrf-token" }?.value

        val body = mapOf(
            "emailOrUsername" to "fylyp@fylyp.fy",
            "password" to "fylyp@fylyp.fY",
            "callbackUrl" to "/dashboard",
            "csrfToken" to csrfToken,
            "json" to "true"
        )

        println("loginRequest")

        return HttpClient.post(
            url, body, PayloadType.FORM
        )
            .then {
                println("loginRequest success")
            }
            .catch { error ->
                val outputView = TextView(this)
                outputView.text = error.message

                setContentView(outputView)
            }
    }

    fun favoritesRequest(): Promise<Any?> {
        val url = "http://192.168.1.21:3000/api/recipse/favorite"

        println("favoritesRequest")

        return HttpClient.get(url)
            .then { text -> Gson().fromJson(text, Map::class.java) }
            .then { data ->
                val outputView = TextView(this)
                outputView.text = data.toString()

                setContentView(outputView)

                outputView.textSize = 18f
            }
            .catch { error ->
                val outputView = TextView(this)
                outputView.text = error.message

                setContentView(outputView)
            }
    }
}