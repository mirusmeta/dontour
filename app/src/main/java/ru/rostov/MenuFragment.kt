package ru.rostov

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.rostov.LiveParser.PlacesViewModel

class MenuFragment : Fragment() {

    // Используем activityViewModels, чтобы получить ту же ViewModel, что и в MainPage
    private val viewModel: PlacesViewModel by activityViewModels()
    private lateinit var adapter: AttractionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.attractions_recycler)

        // Настройка списка
        adapter = AttractionsAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Наблюдаем за данными
        viewModel.placesData.observe(viewLifecycleOwner) { places ->
            if (places != null) {
                adapter.updateData(places)
            }
        }

        return view
    }
}