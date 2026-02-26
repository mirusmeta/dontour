package ru.rostov

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.rostov.citymodule.CityAdapter
import ru.rostov.citymodule.CityItem

    class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var cityAdapter: CityAdapter

    companion object City{
        var city = "rostov.txt"
    }

    private val typesByCity = mapOf(
        "Ростов-на-Дону" to 1,
        "Азов" to 2,
        "Батайск" to 3,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }
}
