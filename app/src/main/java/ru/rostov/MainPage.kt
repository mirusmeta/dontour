package ru.rostov

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
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
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.map.MapObjectCollection

class MainPage : AppCompatActivity() {

    private lateinit var homeIcon: ImageView
    private lateinit var mapIcon: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var profileIcon: ImageView

    private lateinit var container: View

    private val fragments = listOf(
        HomeFragment(),
        MapFragment(),
        MenuFragment(),
        ProfileFragment()
    )

    private var selectedIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        setupEdgeToEdge()
        MapKitFactory.initialize(this)

        container = findViewById(R.id.fragment_container)

        homeIcon = findViewById(R.id.home_icon)
        mapIcon = findViewById(R.id.map_icon)
        menuIcon = findViewById(R.id.menu_icon)
        profileIcon = findViewById(R.id.profile_icon)

        val icons = listOf(homeIcon, mapIcon, menuIcon, profileIcon)

        // создаём фрагменты 1 раз
        if (savedInstanceState == null) {
            initFragments()
        }

        // стартовые иконки
        icons.forEachIndexed { i, icon ->
            icon.alpha = if (i == selectedIndex) 1f else 0.4f
        }

        // анимация нажатия
        val holdListener = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(120).start()

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f)
                        .setDuration(180)
                        .setInterpolator(OvershootInterpolator())
                        .start()
            }
            false
        }

        icons.forEachIndexed { index, icon ->
            icon.setOnTouchListener(holdListener)
            icon.setOnClickListener { selectTab(index, icons) }
        }

        onBackPressedDispatcher.addCallback(this) { }
    }

    // создаём и прячем все кроме первого
    private fun initFragments() {
        val fm = supportFragmentManager

        fm.beginTransaction()
            .add(R.id.fragment_container, fragments[0], "0")
            .add(R.id.fragment_container, fragments[1], "1").hide(fragments[1])
            .add(R.id.fragment_container, fragments[2], "2").hide(fragments[2])
            .add(R.id.fragment_container, fragments[3], "3").hide(fragments[3])
            .commit()

        selectedIndex = 0
    }

    private fun selectTab(index: Int, icons: List<ImageView>) {
        if (index == selectedIndex) return

        icons.forEachIndexed { i, icon ->
            icon.animate()
                .alpha(if (i == index) 1f else 0.4f)
                .setDuration(180)
                .start()
        }

        switchTab(index)
    }

    // ПЛАВНОЕ ПЕРЕКЛЮЧЕНИЕ КАК В TELEGRAM
    private fun switchTab(index: Int) {

        if (index == selectedIndex) return

        val fm = supportFragmentManager
        val current = fragments[selectedIndex]
        val next = fragments[index]

        // показываем следующий под текущим
        fm.beginTransaction()
            .show(next)
            .commitNow()

        // начальное состояние
        next.view?.alpha = 0f

        // плавный fade нового
        next.view?.animate()
            ?.alpha(1f)
            ?.setDuration(180)
            ?.start()

        // лёгкий fade текущего
        current.view?.animate()
            ?.alpha(0f)
            ?.setDuration(180)
            ?.withEndAction {

                fm.beginTransaction()
                    .hide(current)
                    .commitNow()

                current.view?.alpha = 1f
            }
            ?.start()

        selectedIndex = index
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val mainLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            val bottomPadding = when {
                ime.bottom > 0 -> ime.bottom
                systemBars.bottom > 100 -> systemBars.bottom
                else -> 0
            }

            v.setPadding(0, 0, 0, bottomPadding)
            insets
        }
    }
    fun openMapTab() {
        val icons = listOf(homeIcon, mapIcon, menuIcon, profileIcon)
        selectTab(1, icons)   // 1 = MapFragment
    }
}