package ru.rostov

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.MapType
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.rostov.LiveParser.Place
import ru.rostov.LiveParser.PlacesParser
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var searchManager: SearchManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        mapView = view.findViewById(R.id.mapview)

        // тёмная карта
        mapView.map.mapType = MapType.VECTOR_MAP
        mapView.map.isNightModeEnabled = true

        // стартовая позиция Ростов
        mapView.map.move(
            com.yandex.mapkit.map.CameraPosition(
                Point(47.2357, 39.7015),
                12f,
                0f,
                0f
            )
        )

        searchManager = SearchFactory.getInstance()
            .createSearchManager(SearchManagerType.COMBINED)

        loadPlaces()

        return view
    }

    private fun loadPlaces() {
        lifecycleScope.launch {
            val places = withContext(Dispatchers.IO) {
                PlacesParser(requireContext()).parsePlaces()
            }

            addPlacesToMap(places)
        }
    }

    private suspend fun addPlacesToMap(places: List<Place>) {
        for (place in places) {
            val point = geocode(place.adress)
            if (point != null) {
                addPlacemark(point, place)
            }
            delay(120) // чтобы не ловить лимиты геокода
        }
    }

    private suspend fun geocode(address: String): Point? =
        withContext(Dispatchers.Main) {
            suspendCoroutine { cont ->

                val options = SearchOptions().apply {
                    searchTypes = SearchType.GEO.value
                }

                searchManager.submit(
                    address,
                    VisibleRegionUtils.toPolygon(mapView.map.visibleRegion),
                    options,
                    object : Session.SearchListener {

                        override fun onSearchResponse(response: Response) {
                            val obj = response.collection.children.firstOrNull()
                            val point = obj?.obj?.geometry?.firstOrNull()?.point
                            cont.resume(point)
                        }

                        override fun onSearchError(error: com.yandex.runtime.Error) {
                            cont.resume(null)
                        }
                    }
                )
            }
        }

    private fun addPlacemark(point: Point, place: Place) {
        val placemark = mapView.map.mapObjects.addPlacemark(point)

        // дефолтная метка MapKit (без setIcon)

        placemark.userData = place

        placemark.addTapListener { _, _ ->
            // TODO открыть карточку объекта
            true
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

}