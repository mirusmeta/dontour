package ru.rostov

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class AIPro : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_aipro)
        val cross: ImageView = findViewById(R.id.cross)
        cross.setOnClickListener {
            finish()
        }
    }
}