package com.spamshield

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val input = EditText(this).apply {
            hint = "Backend URL (e.g. http://10.0.2.2:8080)"
            setText(Api.baseUrl)
        }

        val btn = Button(this).apply {
            text = "Save"
            setOnClickListener {
                Api.baseUrl = input.text.toString().trim()
                Toast.makeText(this@MainActivity, "Saved: ${Api.baseUrl}", Toast.LENGTH_SHORT).show()
            }
        }

        val layout = androidx.core.widget.NestedScrollView(this).apply {
            addView(android.widget.LinearLayout(this@MainActivity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                val pad = (16 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
                addView(input)
                addView(btn)
            })
        }

        setContentView(layout)
    }
}
