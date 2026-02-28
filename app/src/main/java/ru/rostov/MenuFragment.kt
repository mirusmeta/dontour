package ru.rostov

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.rostov.LiveParser.Place
import ru.rostov.LiveParser.PlacesViewModel
import ru.rostov.database.SupaBaseConfig

class MenuFragment : Fragment() {

    private val viewModel: PlacesViewModel by activityViewModels()
    private lateinit var adapter: AttractionsAdapter

    private val supabase = createSupabaseClient(
        supabaseUrl = SupaBaseConfig.supabaseUrl.supabaseUrl,
        supabaseKey = SupaBaseConfig.supabaseKey.supabaseKey
    ) {
        install(Postgrest)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.attractions_recycler)
        val filterBtn = view.findViewById<View>(R.id.filter_button)
        val addBtn = view.findViewById<View>(R.id.b_back)

        adapter = AttractionsAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        viewModel.placesData.observe(viewLifecycleOwner) { places ->
            places?.let {
                adapter.updateData(it)
                adapter.removeFirstItem()
            }
        }
        filterBtn.setOnClickListener { showFilterDialog() }
        addBtn.setOnClickListener { showAddPlaceDialog() }

        return view
    }

    private fun showAddPlaceDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val nameInput = EditText(context).apply { hint = "Название" }
        val addressInput = EditText(context).apply { hint = "Адрес" }
        layout.addView(nameInput)
        layout.addView(addressInput)

        val allPlaces = viewModel.placesData.value ?: emptyList()
        val options = allPlaces.map { it.invalidnost }.distinct().filter { it != "Нет" }.toTypedArray()
        var selectedInvalidnost = if (options.isNotEmpty()) options[0] else "Нет"

        AlertDialog.Builder(context)
            .setTitle("Добавить место")
            .setView(layout)
            .setNeutralButton("Доступность: $selectedInvalidnost") { _, _ ->
                AlertDialog.Builder(context)
                    .setItems(options) { _, which ->
                        selectedInvalidnost = options[which]
                        showAddPlaceDialog() // Переоткрываем для обновления текста (упрощенно)
                    }.show()
            }
            .setPositiveButton("Добавить") { _, _ ->
                val newPlace = Place(
                    id = (allPlaces.size + 1).toString(),
                    name = nameInput.text.toString(),
                    adress = addressInput.text.toString(),
                    docs = "",
                    type = "",
                    invalidnost = selectedInvalidnost,
                    photos = emptyList()
                )
                sendToSupabase(newPlace)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun sendToSupabase(place: Place) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("places").insert(place)
                }
                Toast.makeText(requireContext(), "Успешно добавлено", Toast.LENGTH_SHORT).show()
                viewModel.load(requireContext()) // Обновляем список
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showFilterDialog() {
        val allPlaces = viewModel.placesData.value ?: return
        val options = allPlaces.map { it.invalidnost }.distinct().toMutableList().apply { add(0, "Нет") }

        AlertDialog.Builder(requireContext())
            .setTitle("Фильтр")
            .setItems(options.toTypedArray()) { _, which ->
                val selected = options[which]
                adapter.updateData(if (selected == "Нет") allPlaces else allPlaces.filter { it.invalidnost == selected })
            }
            .show()
    }
}