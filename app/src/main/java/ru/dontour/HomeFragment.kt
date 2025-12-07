package ru.dontour

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.dontour.citymodule.CityAdapter
import ru.dontour.citymodule.CityItem

    class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var cityAdapter: CityAdapter

    companion object City{
        var city = "rostov.txt"
    }

    private val typesByCity = mapOf(
        "Ростов-на-Дону" to 1,
        "Таганрог" to 2,
        "Азов" to 3,
        "Новочеркасск" to 4,
        "Волгодонск" to 5,
        "Цимлянск" to 6,
        "Семикаракорск" to 7,
        "Батайск" to 8,
        "Новошахтинск" to 9,
        "Старочеркасск" to 10
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val label = view.findViewById<TextView>(R.id.label)
        val subtitle = view.findViewById<TextView>(R.id.subtitile)
        val banner = view.findViewById<View>(R.id.main_banner)
        val cityTitle = view.findViewById<TextView>(R.id.citytitle)
        val cityRecycler = view.findViewById<RecyclerView>(R.id.city_recycler)

        val b_places = view.findViewById<CardView>(R.id.b_places)
        val b_oteli = view.findViewById<CardView>(R.id.b_oteli)
        val b_eda = view.findViewById<CardView>(R.id.b_eda)
        val b_afisha = view.findViewById<CardView>(R.id.b_afisha)
        val wifi = view.findViewById<ConstraintLayout>(R.id.wifi_b)

        wifi.setOnClickListener {
            val intent = Intent(requireContext(), WebViewActivity::class.java)
            intent.putExtra("url", "https://yandex.ru/maps/39/rostov-na-donu/category/wi_fi_hotspot")
            startActivity(intent)
        }
        b_places.setOnClickListener {
            val intent = Intent(requireContext(), Attractions::class.java)
            intent.putExtra("city", City.city)
            startActivity(intent)
        }
        b_oteli.setOnClickListener {
            val intent = Intent(requireContext(), WebViewActivity::class.java)
            intent.putExtra("url", "https://travel.yandex.ru/")
            startActivity(intent)
        }
        b_eda.setOnClickListener {
            val intent = Intent(requireContext(), WebViewActivity::class.java)
            intent.putExtra("url", "https://eda.yandex.ru/")
            startActivity(intent)
        }
        b_afisha.setOnClickListener {
            val intent = Intent(requireContext(), WebViewActivity::class.java)
            intent.putExtra("url", "https://afisha.yandex.ru/rostov-na-donu/events")
            startActivity(intent)
        }

//        val typeRecycler = view.findViewById<RecyclerView>(R.id.type_recycler)

//        val allViews = listOf(label, subtitle, banner, cityTitle, cityRecycler, typeRecycler)
//        allViews.forEach {
//            it.alpha = 0f
//            it.translationY = 100f
//        }
//
//        allViews.forEachIndexed { index, v ->
//            v.animate()
//                .alpha(1f)
//                .translationY(0f)
//                .setDuration(600)
//                .setStartDelay(index * 150L)
//                .setInterpolator(OvershootInterpolator(1.2f))
//                .start()
//        }

        val cities = listOf(
            CityItem(getString(R.string.rostov), R.drawable.logo_rostov, isSelected = true),
            CityItem(getString(R.string.taganrog), R.drawable.logo_taganrog),
            CityItem(getString(R.string.azov), R.drawable.logo_azov),
            CityItem(getString(R.string.novocherkassk), R.drawable.logo_novocherkask),
            CityItem(getString(R.string.volgodonsk), R.drawable.logo_volgodonsk),
            CityItem(getString(R.string.tsimlyansk), R.drawable.logo_cimlansk),
            CityItem(getString(R.string.semikarakorsk), R.drawable.logo_semikarakorsk),
            CityItem(getString(R.string.bataysk), R.drawable.logo_bataysk),
            CityItem(getString(R.string.novocherkassk), R.drawable.logo_novoshahtink),
            CityItem(getString(R.string.starocherkasskaya), R.drawable.logo_starocherkask)
        )

        cityAdapter = CityAdapter(cities) { clickedCity ->
            cities.forEach { it.isSelected = it.title == clickedCity.title }
            cityAdapter.notifyDataSetChanged()
            City.city = when(clickedCity.title){
                getString(R.string.rostov) -> "rostov.txt"
                getString(R.string.taganrog) -> "tagonrog.txt"
                getString(R.string.azov) -> "azov.txt"
                getString(R.string.novocherkassk) -> "novocherkassk.txt"
                getString(R.string.volgodonsk) -> "volgodonsk.txt"
                getString(R.string.tsimlyansk) -> "cimlansk.txt"
                getString(R.string.semikarakorsk) -> "semikarakorsk.txt"
                getString(R.string.bataysk) -> "bataysk.txt"
                getString(R.string.novocherkassk) -> "novoshahtinsk.txt"
                getString(R.string.starocherkasskaya) -> "starocherkassk.txt"
                else -> ""
            }.toString()
        }

        cityRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        cityRecycler.adapter = cityAdapter

//        typeRecycler.layoutManager = LinearLayoutManager(requireContext())
//        typeRecycler.adapter = typeAdapter
//
//        cityRecycler.isNestedScrollingEnabled = false
//        typeRecycler.isNestedScrollingEnabled = false

    }
}
