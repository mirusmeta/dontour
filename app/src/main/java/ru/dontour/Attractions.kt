package ru.dontour

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Attractions : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterAttractions
    private lateinit var allPlaces: List<Attraction>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attractions)
        setupEdgeToEdge()

        findViewById<ConstraintLayout>(R.id.b_back).setOnClickListener {
            startActivity(Intent(this@Attractions, MainPage::class.java))
        }

        val parsing = Parsing()
        val city = intent.getStringExtra("city") ?: "rostov.txt"
        Log.d("Attractions1", city)

        val txt = parsing.readPlacesFromAssets(this, city)

        if (txt.isNotEmpty()) {
            allPlaces = parsing.parsePlaces(txt)
            Log.d("Attractions", "Loaded ${allPlaces.size} places")

            recyclerView = findViewById(R.id.attractions_recycler)
            recyclerView.layoutManager = LinearLayoutManager(this)

            adapter = AdapterAttractions(allPlaces.sortedByDescending { it.level ?: 0 })
            recyclerView.adapter = adapter

            setupSorting()
        } else {
            Log.e("Attractions", "Failed to load attractions data")
        }
    }

    private fun setupSorting() {
        val sortByImportance = findViewById<ConstraintLayout>(R.id.sort)
        val sortParks = findViewById<ConstraintLayout>(R.id.sort_parks)

        val defaultBg = R.drawable.resource_bg
        val selectedBg = R.drawable.resource_bg_selected

        fun selectButton(selected: ConstraintLayout, others: List<ConstraintLayout>) {
            selected.setBackgroundResource(selectedBg)
            others.forEach { it.setBackgroundResource(defaultBg) }
        }

        sortByImportance.setOnClickListener {
            adapter.items = allPlaces.sortedByDescending { it.level ?: 0 }
            adapter.notifyDataSetChanged()
            selectButton(sortByImportance, listOf(sortParks))
        }

        sortParks.setOnClickListener {
            val keywords = listOf("парк", "сад", "сквер")
            adapter.items = allPlaces.filter { place ->
                val nameWords = place.name.lowercase().split("""\W+""".toRegex())
                keywords.any { kw -> nameWords.contains(kw) }
            }
            adapter.notifyDataSetChanged()
            selectButton(sortParks, listOf(sortByImportance))
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
