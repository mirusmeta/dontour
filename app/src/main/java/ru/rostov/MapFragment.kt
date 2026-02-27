package ru.rostov

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.rostov.LiveParser.Place
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var searchManager: SearchManager
    private val viewModel: PlacesViewModel by activityViewModels()

    private lateinit var locationBtn: View
    private lateinit var busBtn: View

    private var isDataLoaded = false
    private var stopsCollection: MapObjectCollection? = null
    private var stopsVisible = false

    /* =========================
       PERMISSION
       ========================= */

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) enableUserLocation()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        mapView = view.findViewById(R.id.mapview)
        locationBtn = view.findViewById(R.id.location_con)
        busBtn = view.findViewById(R.id.bus_con)

        mapView.map.mapType = MapType.VECTOR_MAP
        mapView.map.isNightModeEnabled = true

        mapView.map.move(
            CameraPosition(Point(47.2357, 39.7015), 12f, 0f, 0f)
        )

        searchManager = SearchFactory.getInstance()
            .createSearchManager(SearchManagerType.COMBINED)

        /* ===== ПАРСЕР МЕСТ ===== */

        viewModel.placesData.observe(viewLifecycleOwner) { places ->
            if (places != null && !isDataLoaded) {
                isDataLoaded = true
                lifecycleScope.launch {
                    addPlacesToMap(places)
                }
            }
        }

        /* ===== КНОПКИ ===== */

        locationBtn.setOnClickListener { checkLocationPermission() }

        busBtn.setOnClickListener { toggleStops() }

        return view
    }

    /* =========================
       LOCATION
       ========================= */

    private fun checkLocationPermission() {
        val ctx = requireContext()
        val permission = Manifest.permission.ACCESS_FINE_LOCATION

        if (ContextCompat.checkSelfPermission(ctx, permission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            enableUserLocation()
        } else {
            locationPermissionLauncher.launch(permission)
        }
    }

    private fun enableUserLocation() {
        val layer = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)
        layer.isVisible = true

        val pos = layer.cameraPosition()
        if (pos != null) {
            mapView.map.move(
                pos,
                Animation(Animation.Type.SMOOTH, 0.5f),
                null
            )
        }

        setSelected(locationBtn, true)
        locationBtn.postDelayed({ setSelected(locationBtn, false) }, 300)
    }

    /* =========================
       STOPS
       ========================= */

    private fun toggleStops() {
        if (stopsVisible) {
            stopsCollection?.clear()
            stopsVisible = false
            setSelected(busBtn, false)
        } else {
            loadStops()
            stopsVisible = true
            setSelected(busBtn, true)
        }
    }

    private fun loadStops() {
        val region = VisibleRegionUtils.toPolygon(mapView.map.visibleRegion)

        val options = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
        }

        searchManager.submit(
            "остановка транспорта",
            region,
            options,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    showStops(response)
                }

                override fun onSearchError(error: com.yandex.runtime.Error) {}
            }
        )
    }

    private fun showStops(response: Response) {
        if (stopsCollection == null) {
            stopsCollection = mapView.map.mapObjects.addCollection()
        }

        stopsCollection?.clear()

        for (child in response.collection.children) {
            val point = child.obj?.geometry?.firstOrNull()?.point ?: continue
            stopsCollection?.addPlacemark(point)
        }
    }

    /* =========================
       PLACES (PARSER)
       ========================= */

    private suspend fun addPlacesToMap(places: List<Place>) {
        for (place in places) {
            val point = geocode(place.adress)
            if (point != null) {
                addPlacemark(point, place)
            }
            delay(120)
        }
    }

    private suspend fun geocode(address: String): Point? = withContext(Dispatchers.Main) {
        suspendCoroutine { cont ->
            val options = SearchOptions().apply { searchTypes = SearchType.GEO.value }

            searchManager.submit(
                address,
                VisibleRegionUtils.toPolygon(mapView.map.visibleRegion),
                options,
                object : Session.SearchListener {
                    override fun onSearchResponse(response: Response) {
                        val point = response.collection.children
                            .firstOrNull()?.obj?.geometry?.firstOrNull()?.point
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
        placemark.userData = place
        placemark.addTapListener { _, _ -> true }
    }

    /* =========================
       UI
       ========================= */

    private fun setSelected(view: View, selected: Boolean) {
        view.setBackgroundResource(
            if (selected) R.drawable.bottom_nav_back_selected
            else R.drawable.bottom_nav_back
        )
    }

    /* =========================
       LIFECYCLE
       ========================= */

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