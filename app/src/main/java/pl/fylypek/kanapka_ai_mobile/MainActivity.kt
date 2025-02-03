package pl.fylypek.kanapka_ai_mobile

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val url = "https://jsonplaceholder.typicode.com/todos/1"
        fetch(url)
            .then { data ->
                val outputView = TextView(this)
                outputView.text = data

                setContentView(outputView)

                outputView.textSize = 18f
                outputView.setPadding(1 * 3, 32 * 3, 0, 0)
            }
            .catch { error ->
                val outputView = TextView(this)
                outputView.text = error.message

                setContentView(outputView)
            }
    }
}