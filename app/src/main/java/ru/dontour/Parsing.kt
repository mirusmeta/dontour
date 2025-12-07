package ru.dontour

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Coordinates(
    val lat: Double,
    val lng: Double
)

class Parsing {

    fun readPlacesFromAssets(context: Context, fileName: String): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("Parsing", "Error reading file: $fileName", e)
            ""
        }
    }

    fun parsePlaces(txt: String): List<Attraction> {
        if (txt.isEmpty()) return emptyList()

        return try {
            val gson = Gson()
            val listType = object : TypeToken<List<Attraction>>() {}.type
            Log.d("Parsing", txt)
            gson.fromJson<List<Attraction>>(txt, listType)
        } catch (e: Exception) {
            Log.e("Parsing", "Error parsing JSON data", e)
            emptyList()
        }
    }

}
