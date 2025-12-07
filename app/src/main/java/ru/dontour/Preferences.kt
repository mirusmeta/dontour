package ru.dontour

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.vk.id.VKID

class Preferences : AppCompatActivity() {
    private var selectedViewId: Int? = null
    private var myVKID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        myVKID = VKID.instance.accessToken?.idToken.toString()
        animateViews()
        setupClickAnimations()
        setupEdgeToEdge()
    }

    private fun animateViews() {
        val question = findViewById<TextView>(R.id.questinon)
        val parksContainer = findViewById<View>(R.id.parks_container)
        val memorialContainer = findViewById<View>(R.id.memorial_container)
        val eventsContainer = findViewById<View>(R.id.events_container)
        val allContainer = findViewById<View>(R.id.all_container)
        val buttonNext = findViewById<View>(R.id.button_next)

        val allViews = listOf(
            question,
            parksContainer,
            memorialContainer,
            eventsContainer,
            allContainer,
            buttonNext
        )

        allViews.forEach {
            it.alpha = 0f
            it.translationY = 80f
        }

        allViews.forEachIndexed { index, view ->
            view.postDelayed({
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setInterpolator(
                        if (index == allViews.lastIndex)
                            OvershootInterpolator(1.3f)
                        else
                            DecelerateInterpolator()
                    )
                    .start()
            }, (index * 150 + 200).toLong())
        }

        buttonNext.postDelayed({
            buttonNext.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .withEndAction {
                    buttonNext.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }, 1300)
    }

    private fun setupClickAnimations() {
        val parksContainer = findViewById<View>(R.id.parks_container)
        val memorialContainer = findViewById<View>(R.id.memorial_container)
        val eventsContainer = findViewById<View>(R.id.events_container)
        val allContainer = findViewById<View>(R.id.all_container)
        val buttonNext = findViewById<View>(R.id.button_next)

        val clickableViews =
            listOf(parksContainer, memorialContainer, eventsContainer, allContainer)

        clickableViews.forEach { view ->
            view.setOnClickListener {
                animateClick(view)
                selectView(view, clickableViews)
            }
        }

        buttonNext.setOnClickListener {
            animateClick(buttonNext)
            var hasAIpro = false
            val db = Firebase.firestore
            if (selectedViewId != null) {
                val selectedType = when (selectedViewId) {
                    R.id.parks_container -> "Парки"
                    R.id.memorial_container -> "Мемориалы"
                    R.id.events_container -> "События"
                    R.id.all_container -> "Все сразу"
                    else -> "Неизвестно"
                }
                // Проверяем наличие пользователя в БД

                db.collection("users").document(myVKID).get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        hasAIpro = doc.getBoolean("aipro")!!
                        // Если есть — обновляем
                        db.collection("users").document(myVKID)
                            .update("preferences", selectedType)
                            .addOnSuccessListener {

                            }
                            .addOnFailureListener {

                            }
                    } else {
                        // Если нет — создаем новый документ
                        val userData = hashMapOf(
                            "aipro" to false,
                            "preferences" to selectedType,
                            "createdAt" to System.currentTimeMillis()
                        )

                        db.collection("users").document(myVKID)
                            .set(userData)
                            .addOnSuccessListener {

                            }
                            .addOnFailureListener {

                            }
                    }
                }.addOnFailureListener {

                }

                val sPref = getSharedPreferences("data", MODE_PRIVATE)
                sPref.edit {
                    putBoolean("first_meet", true)
                    putString("typePreference", selectedType)
                    putBoolean("hasaipro", hasAIpro)
                }

                // Переход на главную
                val intent = Intent(this, MainPage::class.java)
                intent.putExtra("selected_type", selectedType)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()

            } else {
                // Если ничего не выбрано — трясем кнопку
                buttonNext.animate()
                    .translationX(10f).setDuration(50)
                    .withEndAction {
                        buttonNext.animate().translationX(0f).setDuration(50).start()
                    }.start()
            }
        }

    }

    // Анимация при клике
    private fun animateClick(view: View) {
        view.animate()
            .scaleX(1.07f)
            .scaleY(1.07f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .setInterpolator(OvershootInterpolator(1.3f))
                    .start()
            }
            .start()
    }

    // Логика выбора блока
    private fun selectView(selectedView: View, allViews: List<View>) {
        selectedViewId = selectedView.id

        allViews.forEach { view ->
            // Подсветка выбранного
            if (view.id == selectedViewId) {
                view.setBackgroundResource(R.drawable.chosen_block) // выбранный фон
            } else {
                when (view.id) { // возвращаем исходные фоны
                    R.id.parks_container -> view.setBackgroundResource(R.drawable.green_block)
                    R.id.memorial_container -> view.setBackgroundResource(R.drawable.orange_block)
                    R.id.events_container -> view.setBackgroundResource(R.drawable.red_block)
                    R.id.all_container -> view.setBackgroundResource(R.drawable.button_next)
                }
            }
        }
    }

    private fun setupEdgeToEdge() {
        val mainLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            val bottomPadding = ime.bottom.coerceAtLeast(systemBars.bottom)

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                bottomPadding
            )
            insets
        }
    }

}