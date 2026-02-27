package ru.rostov

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.yandex.mapkit.MapKitFactory
import ru.rostov.LiveParser.PlacesViewModel

class MainPage : AppCompatActivity() {

    private lateinit var homeIcon: ImageView
    private lateinit var mapIcon: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var profileIcon: ImageView

    private var selectedIndex = 0

    val viewModel: PlacesViewModel by viewModels()

    // ⭐ состояние карты
    var currentCity: String = "Ростов-на-Дону"
    var currentCategory: String? = "places"   // ← FIX: категория по умолчанию

    private val fragments = listOf(
        HomeFragment(),
        MapFragment(),
        MenuFragment(),
        ProfileFragment()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        setupEdgeToEdge()
        MapKitFactory.initialize(this)

        // загрузка данных парсера
        viewModel.load(applicationContext)

        homeIcon = findViewById(R.id.home_icon)
        mapIcon = findViewById(R.id.map_icon)
        menuIcon = findViewById(R.id.menu_icon)
        profileIcon = findViewById(R.id.profile_icon)

        val icons = listOf(homeIcon, mapIcon, menuIcon, profileIcon)

        if (savedInstanceState == null) {
            initFragments()
        }

        icons.forEachIndexed { i, icon ->
            icon.alpha = if (i == selectedIndex) 1f else 0.4f
            icon.setOnClickListener { selectTab(i, icons) }
        }

        onBackPressedDispatcher.addCallback(this) { }
    }

    private fun initFragments() {
        val fm = supportFragmentManager
        val tr = fm.beginTransaction()

        fragments.forEachIndexed { i, fragment ->
            tr.add(R.id.fragment_container, fragment, i.toString())
            if (i != 0) tr.hide(fragment)
        }

        tr.commit()
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

    private fun switchTab(index: Int) {
        val fm = supportFragmentManager
        val current = fragments[selectedIndex]
        val next = fragments[index]

        fm.beginTransaction().show(next).commitNow()

        next.view?.alpha = 0f
        next.view?.animate()?.alpha(1f)?.setDuration(180)?.start()

        current.view?.animate()?.alpha(0f)?.setDuration(180)?.withEndAction {
            fm.beginTransaction().hide(current).commitNow()
            current.view?.alpha = 1f
        }?.start()

        selectedIndex = index
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val mainLayout = findViewById<android.view.View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, bars.bottom)
            insets
        }
    }

    /* ================= MAP CONTROL ================= */

    private fun getMap(): MapFragment {
        return fragments[1] as MapFragment
    }

    fun openMapTab() {
        val icons = listOf(homeIcon, mapIcon, menuIcon, profileIcon)
        selectTab(1, icons)

        val map = getMap()

        // применяем состояние
        map.setCity(currentCity)

        // ⭐ всегда есть категория
        map.setCategory(currentCategory ?: "places")
    }

    fun openMap(category: String? = null) {
        currentCategory = category ?: "places"
        openMapTab()
    }

    fun openMapWithCategory(category: String) {
        currentCategory = category
        openMapTab()
    }

    fun setCity(city: String) {
        currentCity = city
        getMap().setCity(city)
    }
}