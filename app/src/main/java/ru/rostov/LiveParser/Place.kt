package ru.rostov.LiveParser

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class Place(
    val id: String,
    val name: String,
    val adress: String,
    val docs: String,
    val type: String,
    val invalidnost: String,
    val rating: Int = 0,
    @Transient
    val photos: List<String> = emptyList()
)

class PlacesViewModel : ViewModel() {
    val openAddress = MutableLiveData<String?>()


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