package ru.dontour

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.squareup.picasso.Picasso

class AboutAttraction : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about_attraction)

        val bBack: ConstraintLayout = findViewById(R.id.b_back)
        val titleTv: TextView = findViewById(R.id.textView6)
        val descTv: TextView = findViewById(R.id.descriptionText)
        val imageView: ImageView = findViewById(R.id.imageView4)
        val wikiButton: ConstraintLayout? = findViewById(R.id.wiki_button)

        // Извлекаем данные из Intent
        val name = intent.getStringExtra("name")
        val description = intent.getStringExtra("desc")
        val wikiLink = intent.getStringExtra("wiki")
        val pic = intent.getStringExtra("pic")

        // Отображаем данные
        titleTv.text = name ?: "Без названия"
        descTv.text = description ?: "Описание отсутствует"

        // Загружаем фото
        if (!pic.isNullOrEmpty()) {
            Picasso.get()
                .load(pic)
                .placeholder(R.mipmap.ic_launcher_foreground)
                .error(R.mipmap.ic_launcher_foreground)
                .into(imageView)
        } else {
            imageView.setImageResource(R.mipmap.ic_launcher_foreground)
        }
        // Кнопка "Назад"
        bBack.setOnClickListener {
            val intent = Intent(this@AboutAttraction, MainPage::class.java)
            startActivity(intent)
            finish()
        }

        // Кнопка перехода на Википедию (если есть)
        wikiButton?.setOnClickListener {
            wikiLink?.let {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                startActivity(browserIntent)
            }
        }

        setupEdgeToEdge()
        animateAboutScreen()
    }

    private fun setupEdgeToEdge() {
        val mainLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = ime.bottom.coerceAtLeast(systemBars.bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            insets
        }
    }

    private fun animateAboutScreen() {
        val titleTv = findViewById<TextView>(R.id.textView6)
        val descTv = findViewById<TextView>(R.id.descriptionText)
        val imageView = findViewById<ImageView>(R.id.imageView4)
        val wikiButton = findViewById<View>(R.id.wiki_button)
        val bBack = findViewById<View>(R.id.b_back)

        val allViews = listOf(titleTv, descTv, imageView, wikiButton, bBack)

        allViews.forEach {
            it.alpha = 0f
            it.translationY = 100f
        }

        imageView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()

        titleTv.postDelayed({
            titleTv.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }, 200)

        descTv.postDelayed({
            descTv.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }, 400)

        // Анимация кнопки Википедии
        wikiButton?.postDelayed({
            wikiButton.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.3f))
                .start()
        }, 800)

        bBack.postDelayed({
            bBack.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.3f))
                .start()
        }, 1000)
    }
}