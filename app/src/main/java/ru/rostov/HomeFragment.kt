package ru.rostov

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.rostov.citymodule.CityAdapter
import ru.rostov.citymodule.CityItem

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var cityAdapter: CityAdapter
    private lateinit var transportBlock: View
    private lateinit var problemsBlock: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val main = activity as? MainPage ?: return

        // --- баннеры ---
        val banner = view.findViewById<View>(R.id.main_banner)
        val places = view.findViewById<View>(R.id.b_places)
        val oteli = view.findViewById<View>(R.id.b_oteli)
        transportBlock = view.findViewById(R.id.b_transport)
        problemsBlock = view.findViewById(R.id.problems)

        // главный баннер
        banner.setOnClickListener {
            main.openMap(null)
        }

        // доступные места
        places.setOnClickListener {
            main.openMap("places")
        }

        // где остановиться
        oteli.setOnClickListener {
            main.openMap(null)
        }

        // транспорт
        transportBlock.setOnClickListener {
            main.openMap("transport")
        }

        // проблемные зоны
        problemsBlock.setOnClickListener {
            main.openMap("problems")
        }

        // --- список городов ---
        val cities = mutableListOf(
            CityItem("Ростов-на-Дону", R.drawable.logo_rostov, true),
            CityItem("Батайск", R.drawable.logo_bataysk, false),
            CityItem("Азов", R.drawable.logo_azov, false)
        )

        val recycler = view.findViewById<RecyclerView>(R.id.city_recycler)

        recycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        recycler.setHasFixedSize(true)
        recycler.isNestedScrollingEnabled = false

        recycler.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        cityAdapter = CityAdapter(cities) { city ->
            onCitySelected(city.title)
        }

        recycler.adapter = cityAdapter

        applyCityUi(main.currentCity)
    }

    private fun onCitySelected(title: String) {
        val main = activity as? MainPage ?: return

        // ⭐ сохраняем город
        main.currentCity = title

        // ⭐ уведомляем карту сразу
        main.setCity(title)

        // ⭐ обновляем UI
        applyCityUi(title)
    }

    private fun applyCityUi(city: String) {
        val isRostov = city == "Ростов-на-Дону"

        if (isRostov) {
            transportBlock.fadeVisible()
            problemsBlock.fadeVisible()
        } else {
            transportBlock.fadeGone()
            problemsBlock.fadeGone()
        }
    }
}

/* ---------- АНИМАЦИИ ---------- */

fun View.fadeGone(duration: Long = 200) {
    if (visibility == View.GONE) return
    animate()
        .alpha(0f)
        .setDuration(duration)
        .withEndAction {
            visibility = View.GONE
            alpha = 1f
        }
}

fun View.fadeVisible(duration: Long = 200) {
    if (visibility == View.VISIBLE) return
    alpha = 0f
    visibility = View.VISIBLE
    animate()
        .alpha(1f)
        .setDuration(duration)
}