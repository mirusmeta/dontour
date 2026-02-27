package ru.rostov.LiveParser

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class Place(
    val id: String,
    val name: String,
    val adress: String,
    val docs: String,
    val type: String,
    val invalidnost: String,
    val photos: List<String>
)

class PlacesViewModel : ViewModel() {

    val selectedCity = MutableLiveData<String>()

    fun selectCity(city: String) {
        selectedCity.value = city
    }

    val placesData = MutableLiveData<List<Place>>()

    fun load(context: Context) {
        if (placesData.value != null) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = PlacesParser(context).parsePlaces()
                placesData.postValue(result)
            } catch (_: Exception) {}
        }
    }
}