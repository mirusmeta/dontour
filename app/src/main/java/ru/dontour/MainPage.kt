package ru.dontour

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.fragment.app.Fragment
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import com.yandex.mapkit.map.MapObjectCollection
import ru.dontour.AI.ChatWithGPT

class MainPage : AppCompatActivity() {
    private var landmarksLayer: MapObjectCollection? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        val chosenGlass = findViewById<View>(R.id.chosen_glass)
        val homeIcon = findViewById<ImageView>(R.id.home_icon)
        val mapIcon = findViewById<ImageView>(R.id.map_icon)
        val mainBlock = findViewById<ConstraintLayout>(R.id.main_block)
        val searchBlock = findViewById<ConstraintLayout>(R.id.search_block)

        setupEdgeToEdge()

        searchBlock.setOnClickListener {
            val intent = Intent(this, ChatWithGPT::class.java)
            startActivity(intent)
            finish()
        }

        mainBlock.clipChildren = false
        mainBlock.clipToPadding = false
        chosenGlass.alpha = 0.95f

        replaceFragment(HomeFragment())

        mainBlock.post {
            val moveDistance = mapIcon.x - homeIcon.x
            var selectedIndex = 0

            fun animateTo(index: Int) {
                if (index == selectedIndex) return
                val targetX = if (index == 0) 0f else moveDistance

                chosenGlass.animate()
                    .translationX(targetX)
                    .setDuration(350)
                    .setInterpolator(OvershootInterpolator(1.1f))
                    .start()

                if (index == 0) {
                    homeIcon.animate().alpha(1f).setDuration(250).start()
                    mapIcon.animate().alpha(0.5f).setDuration(250).start()
                    replaceFragment(HomeFragment())
                } else {
                    mapIcon.animate().alpha(1f).setDuration(250).start()
                    homeIcon.animate().alpha(0.5f).setDuration(250).start()
                    replaceFragment(MapFragment())
                }

                selectedIndex = index
            }

            val springX = SpringAnimation(chosenGlass, SpringAnimation.SCALE_X, 1f)
            val springY = SpringAnimation(chosenGlass, SpringAnimation.SCALE_Y, 1f)
            springX.spring = SpringForce(1f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
            springY.spring = SpringForce(1f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }

            val holdListener = View.OnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        springX.start(); springY.start()
                    }
                }
                false
            }

            homeIcon.setOnTouchListener(holdListener)
            mapIcon.setOnTouchListener(holdListener)

            homeIcon.setOnClickListener { animateTo(0) }
            mapIcon.setOnClickListener { animateTo(1) }
        }

        // запрет на кнопку "назад"
        onBackPressedDispatcher.addCallback(this) { }
    }

    fun showLandmarkCard(attraction: Attraction) {
        val card = findViewById<ConstraintLayout>(R.id.landmarkInfoBlock)
        val closeIcon = findViewById<CardView>(R.id.closeIcon)
        val imageView = findViewById<ImageView>(R.id.imageOfLandMark)
        val nameText = findViewById<TextView>(R.id.nameOfLandmark)
        val buttonMore = findViewById<ConstraintLayout>(R.id.buttonModeInf)

        // Если карточка уже открыта → плавно обновляем контент
        if (card.visibility == View.VISIBLE) {
            card.animate()
                .scaleX(0.97f).scaleY(0.97f).alpha(0.9f)
                .setDuration(120)
                .withEndAction {
                    updateCardContent(attraction, imageView, nameText, buttonMore)
                    card.animate().scaleX(1f).scaleY(1f).alpha(1f)
                        .setDuration(220)
                        .setInterpolator(OvershootInterpolator(1.2f))
                        .start()
                }
                .start()
            return
        }

        // --- Если карточка ещё скрыта ---
        card.visibility = View.INVISIBLE
        closeIcon.visibility = View.INVISIBLE

        card.doOnLayout {
            updateCardContent(attraction, imageView, nameText, buttonMore)

            card.translationY = card.height.toFloat()
            card.alpha = 0f
            closeIcon.alpha = 0f

            card.visibility = View.VISIBLE
            closeIcon.visibility = View.VISIBLE

            card.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()

            closeIcon.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(200)
                .start()
        }

        closeIcon.setOnClickListener { hideLandmarkCard() }

        buttonMore.setOnClickListener {
            val intent = Intent(this, AboutAttraction::class.java).apply {
                putExtra("name", attraction.name)
                putExtra("desc", attraction.description)
                putExtra("wiki", attraction.wiki_link)
                putExtra("pic", attraction.pic)
            }
            startActivity(intent)
        }
    }



    private fun updateCardContent(
        attraction: Attraction,
        imageView: ImageView,
        nameText: TextView,
        buttonMore: View
    ) {
        nameText.alpha = 0f
        imageView.alpha = 0f
        buttonMore.alpha = 0f

        nameText.text = attraction.name

        Picasso.get()
            .load(attraction.pic)
            .placeholder(R.drawable.resource_bg_selected)
            .error(R.drawable.image_404)
            .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
            .into(imageView, object : com.squareup.picasso.Callback {
                override fun onSuccess() {
                    imageView.animate().alpha(1f).setDuration(250).start()
                }
                override fun onError(e: Exception?) {
                    imageView.animate().alpha(1f).setDuration(250).start()
                }
            })

        nameText.animate().alpha(1f).setDuration(250).setStartDelay(100).start()
        buttonMore.animate().alpha(1f).setDuration(250).setStartDelay(150).start()
    }


    fun hideLandmarkCard() {
        val card = findViewById<ConstraintLayout>(R.id.landmarkInfoBlock)
        val closeIcon = findViewById<CardView>(R.id.closeIcon)

        if (card.visibility != View.VISIBLE) return

        card.animate()
            .translationY(card.height.toFloat())
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    card.visibility = View.INVISIBLE
                    card.translationY = 0f
                    card.alpha = 1f
                }
            })
            .start()

        closeIcon.animate()
            .alpha(0f)
            .setDuration(250)
            .withEndAction { closeIcon.visibility = View.INVISIBLE }
            .start()
    }





    // 🔄 мягкая замена фрагмента через fade-анимацию
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in_fast,
                R.anim.fade_out_fast
            )
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val mainLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            val bottomPadding = when {
                ime.bottom > 0 -> ime.bottom // если клавиатура открыта
                systemBars.bottom > 100 -> systemBars.bottom // если панель навигации большая (например, на раскладушке)
                else -> 0 // иначе — ничего не добавляем
            }

            v.setPadding(
                0, // сверху ничего не добавляем — фуллскрин остаётся
                0,
                0,
                bottomPadding
            )

            // возвращаем insets, чтобы они не "съедались"
            insets
        }
    }

}
