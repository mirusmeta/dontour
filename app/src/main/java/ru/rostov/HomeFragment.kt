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

    private val typesByCity = mapOf(
        "Ростов-на-Дону" to 1,
        "Батайск" to 2,
        "Азов" to 3,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val banner = view.findViewById<View>(R.id.main_banner)
        transportBlock = view.findViewById(R.id.b_transport)
        problemsBlock = view.findViewById(R.id.problems)

        banner.setOnClickListener {
            (activity as? MainPage)?.openMapTab()
        }

        // список городов
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

        cityAdapter = CityAdapter(cities) { city ->
            val type = typesByCity[city.title] ?: return@CityAdapter
            onCitySelected(type, city.title)
        }

        recycler.adapter = cityAdapter

        // стартовое состояние (если вдруг первый не Ростов)
        val selectedCity = cities.firstOrNull { it.isSelected }?.title
        if (selectedCity != "Ростов-на-Дону") {
            transportBlock.visibility = View.GONE
            problemsBlock.visibility = View.GONE
        }
    }

    private fun onCitySelected(type: Int, title: String) {

        val isRostov = title == "Ростов-на-Дону"

        if (isRostov) {
            transportBlock.fadeVisible()
            problemsBlock.fadeVisible()
        } else {
            transportBlock.fadeGone()
            problemsBlock.fadeGone()
        }

        // твоя логика города
        // viewModel.setCity(type)
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