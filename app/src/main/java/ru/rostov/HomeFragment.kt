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

    private val typesByCity = mapOf(
        "Ростов-на-Дону" to 1,
        "Батайск" to 2,
        "Азов" to 3,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val banner = view.findViewById<View>(R.id.main_banner)

        banner.setOnClickListener {
            (activity as? MainPage)?.openMapTab()
        }

        // ✅ список городов (первый выбран)
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
    }

    private fun onCitySelected(type: Int, title: String) {
        // 👉 тут твоя логика при выборе города
        // например загрузка данных города
        // viewModel.setCity(type)
    }
}