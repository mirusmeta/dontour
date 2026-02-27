package ru.rostov

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.runtime.image.ImageProvider

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var searchManager: SearchManager

    private lateinit var busBtn: View
    private lateinit var locationBtn: View

    private var stopsCollection: MapObjectCollection? = null
    private var stopsVisible = false

    /* ✅ фикс локации */
    private var userLocationLayer: UserLocationLayer? = null
    private var locationEnabled = false

    private val handler = Handler(Looper.getMainLooper())
    private var loadRunnable: Runnable? = null

    /* CAMERA LISTENER — грузим остановки только при нужном зуме */
    private val cameraListener = CameraListener { _, position, _, _ ->
        if (!stopsVisible) return@CameraListener

        if (position.zoom < 13f) {
            stopsCollection?.clear()
            return@CameraListener
        }

        debounceLoadStops()
    }

    /* LOCATION PERMISSION */
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
        busBtn = view.findViewById(R.id.bus_con)
        locationBtn = view.findViewById(R.id.location_con)

        mapView.map.mapType = MapType.VECTOR_MAP
        mapView.map.isNightModeEnabled = true
        mapView.map.move(CameraPosition(Point(47.2357, 39.7015), 12f, 0f, 0f))

        mapView.map.addCameraListener(cameraListener)

        searchManager = SearchFactory.getInstance()
            .createSearchManager(SearchManagerType.COMBINED)

        busBtn.setOnClickListener { toggleStopsMode() }
        locationBtn.setOnClickListener { checkLocationPermission() }

        return view
    }

    /* ---------------- BUS MODE ---------------- */

    private fun toggleStopsMode() {
        stopsVisible = !stopsVisible

        setBusSelected(stopsVisible)

        if (stopsVisible) {
            loadStopsInView()
        } else {
            stopsCollection?.clear()
        }
    }

    private fun setBusSelected(selected: Boolean) {
        busBtn.setBackgroundResource(
            if (selected) R.drawable.bus_selected
            else R.drawable.bottom_nav_back
        )
    }

    /* ---------------- LOCATION ---------------- */

    private fun checkLocationPermission() {
        val perm = Manifest.permission.ACCESS_FINE_LOCATION

        if (ContextCompat.checkSelfPermission(requireContext(), perm)
            == PackageManager.PERMISSION_GRANTED
        ) {
            enableUserLocation()
        } else {
            locationPermissionLauncher.launch(perm)
        }
    }

    private fun enableUserLocation() {

        /* создаём слой только 1 раз */
        if (userLocationLayer == null) {
            userLocationLayer = MapKitFactory.getInstance()
                .createUserLocationLayer(mapView.mapWindow)
        }

        /* переключаем режим */
        locationEnabled = !locationEnabled
        userLocationLayer?.isVisible = locationEnabled

        /* центрируем только при включении */
        if (locationEnabled) {
            userLocationLayer?.cameraPosition()?.let {
                mapView.map.move(
                    it,
                    Animation(Animation.Type.SMOOTH, 0.5f),
                    null
                )
            }
        }

        setLocationSelected(locationEnabled)
    }

    private fun setLocationSelected(selected: Boolean) {
        locationBtn.setBackgroundResource(
            if (selected) R.drawable.bus_selected
            else R.drawable.bottom_nav_back
        )
    }

    /* ---------------- STOPS ---------------- */

    private fun debounceLoadStops() {
        loadRunnable?.let { handler.removeCallbacks(it) }
        loadRunnable = Runnable { loadStopsInView() }
        handler.postDelayed(loadRunnable!!, 250)
    }

    private fun loadStopsInView() {
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

        val bmp = BitmapFactory.decodeResource(resources, R.drawable.landmark)
        val provider = ImageProvider.fromBitmap(bmp)

        for (child in response.collection.children) {
            val point = child.obj?.geometry?.firstOrNull()?.point ?: continue

            val mark = stopsCollection!!.addPlacemark(point)
            mark.setIcon(provider)
            mark.setIconStyle(
                IconStyle().apply {
                    scale = 0.45f
                    anchor = PointF(0.5f, 1f)
                }
            )
        }
    }

    /* ---------------- LIFECYCLE ---------------- */

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.map.removeCameraListener(cameraListener)
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}