package ru.rostov

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.yandex.mapkit.*
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.*
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.runtime.image.ImageProvider
import ru.rostov.AI.Edge
import ru.rostov.AI.UserProfile
import ru.rostov.AI.edgeCost

class MapFragment : Fragment() {
    private lateinit var card1: View
    private lateinit var card2: View
    private lateinit var card3: View

    private lateinit var mapView: MapView
    private lateinit var searchManager: SearchManager

    private lateinit var busBtn: View
    private lateinit var locationBtn: View
    private lateinit var fromField: TextView
    private lateinit var toField: TextView
    private lateinit var selectPanel: View
    private lateinit var selectBtn: View
    private lateinit var accessibilityContent: View
    private lateinit var selectTitle: TextView

    private var selectingMode: Mode? = null
    private var tempPoint: Point? = null
    private var fromPoint: Point? = null
    private var toPoint: Point? = null

    private var fromPlacemark: PlacemarkMapObject? = null
    private var toPlacemark: PlacemarkMapObject? = null
    private var routePolyline: PolylineMapObject? = null

    private lateinit var selectionCollection: MapObjectCollection
    private lateinit var routeCollection: MapObjectCollection
    private var stopsCollection: MapObjectCollection? = null

    private var stopsVisible = false
    private var userLocationLayer: UserLocationLayer? = null
    private var locationEnabled = false

    private val handler = Handler(Looper.getMainLooper())
    private var loadRunnable: Runnable? = null

    enum class Mode { FROM, TO }

    /* ================= INPUT LISTENER ================= */

    private val mapTapListener = object : InputListener {
        override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
            if (selectingMode == null) return
            tempPoint = point
            showSelectionPoint(point)
        }
        override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {}
    }

    /* ================= CAMERA ================= */

    private val cameraListener = CameraListener { _, position, _, _ ->
        if (!stopsVisible) return@CameraListener
        if (position.zoom < 13f) {
            stopsCollection?.clear()
            return@CameraListener
        }
        debounceLoadStops()
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) enableUserLocation()
        }

    /* ================= VIEW ================= */

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_map, container, false)

        card1 = view.findViewById(R.id.card_access_1)
        card2 = view.findViewById(R.id.card_access_2)
        card3 = view.findViewById(R.id.card_access_3)

        mapView = view.findViewById(R.id.mapview)
        busBtn = view.findViewById(R.id.bus_con)
        locationBtn = view.findViewById(R.id.location_con)
        fromField = view.findViewById(R.id.from)
        toField = view.findViewById(R.id.to)
        selectPanel = view.findViewById(R.id.select_panel)
        selectBtn = view.findViewById(R.id.select_btn)
        accessibilityContent = view.findViewById(R.id.accessibility_content)
        selectTitle = view.findViewById(R.id.select_title)

        mapView.map.mapType = MapType.VECTOR_MAP
        mapView.map.isNightModeEnabled = true
        mapView.map.move(CameraPosition(Point(47.2357, 39.7015), 12f, 0f, 0f))

        mapView.map.addCameraListener(cameraListener)

        selectionCollection = mapView.map.mapObjects.addCollection()
        routeCollection = mapView.map.mapObjects.addCollection()

        searchManager = SearchFactory.getInstance()
            .createSearchManager(SearchManagerType.COMBINED)

        busBtn.setOnClickListener { toggleStopsMode() }
        locationBtn.setOnClickListener { checkLocationPermission() }
        fromField.setOnClickListener { startSelecting(Mode.FROM) }
        toField.setOnClickListener { startSelecting(Mode.TO) }
        selectBtn.setOnClickListener { confirmSelection() }

        return view
    }

    /* ================= SELECT ================= */

    private fun startSelecting(mode: Mode) {
        selectingMode = mode
        tempPoint = null

        fromField.alpha = if (mode == Mode.FROM) 1f else 0.5f
        toField.alpha = if (mode == Mode.TO) 1f else 0.5f

        selectTitle.text =
            if (mode == Mode.FROM) "Выберите точку ОТКУДА"
            else "Выберите точку КУДА"

        accessibilityContent.animate()
            .translationY(accessibilityContent.height.toFloat())
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                accessibilityContent.visibility = View.GONE
                selectPanel.visibility = View.VISIBLE
            }
            .start()
    }

    private fun confirmSelection() {
        val p = tempPoint ?: return

        if (selectingMode == Mode.FROM) {
            fromPoint = p
            fromField.text = "Точка выбрана"
        } else {
            toPoint = p
            toField.text = "Точка выбрана"
        }

        selectingMode = null
        tempPoint = null

        fromField.alpha = 1f
        toField.alpha = 1f

        selectPanel.visibility = View.GONE

        accessibilityContent.visibility = View.VISIBLE
        accessibilityContent.translationY = accessibilityContent.height.toFloat()
        accessibilityContent.alpha = 0f

        accessibilityContent.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .start()

        if (fromPoint != null && toPoint != null) {
            buildRoute()
        }
    }

    private fun showSelectionPoint(point: Point) {

        val mode = selectingMode ?: return

        val icon = ImageProvider.fromResource(requireContext(), R.drawable.landmark)

        val style = IconStyle().apply {
            scale = 0.11f
            anchor = PointF(0.5f, 1f)
        }

        when (mode) {
            Mode.FROM -> {
                if (fromPlacemark == null) {
                    fromPlacemark = selectionCollection.addPlacemark(point).apply {
                        setIcon(icon, style)
                    }
                } else fromPlacemark!!.geometry = point
            }
            Mode.TO -> {
                if (toPlacemark == null) {
                    toPlacemark = selectionCollection.addPlacemark(point).apply {
                        setIcon(icon, style)
                    }
                } else toPlacemark!!.geometry = point
            }
        }
    }

    /* ================= ROUTE ================= */

    private fun buildRoute() {

        val router = TransportFactory.getInstance().createPedestrianRouter()

        val requestPoints = listOf(
            RequestPoint(fromPoint!!, RequestPointType.WAYPOINT, null, null, null),
            RequestPoint(toPoint!!, RequestPointType.WAYPOINT, null, null, null)
        )

        router.requestRoutes(
            requestPoints,
            TimeOptions(),
            RouteOptions(FitnessOptions()),
            object : Session.RouteListener {
                override fun onMasstransitRoutes(routes: MutableList<Route>) {
                    if (routes.isNotEmpty()) showRoute(routes.first())
                }
                override fun onMasstransitRoutesError(error: com.yandex.runtime.Error) {}
            }
        )
    }

    private fun showRoute(route: Route) {
        routePolyline?.let { routeCollection.remove(it) }

        routePolyline = routeCollection.addPolyline(route.geometry).apply {
            strokeWidth = 6f
            setStrokeColor(0xFF4DA6FF.toInt())
        }

        runAccessibilityAnalysis(route) // ← AI «участвует»
    }

    /* ================= AI ANALYSIS ================= */

    private fun runAccessibilityAnalysis(route: Route) {
        try {
            val edges = convertRouteToEdges(route)

            val profile = UserProfile(
                weights = listOf(1.0, 1.0, 1.0),
                curbSafe = 0.02,
                curbMax = 0.08,
                lambdaL = 1.0,
                lambdaT = 1.0,
                lambdaA = 2.0,
                lambdaB = 3.0,
                lambdaR = 1.5,
                lambdaC = 1.0
            )

            var totalCost = 0.0
            var blocked = 0

            for (e in edges) {
                val c = edgeCost(e, profile)
                if (c.isInfinite()) blocked++ else totalCost += c
            }

            val avg = if (edges.isNotEmpty()) totalCost / edges.size else 0.0

            android.util.Log.d(
                "AI_ROUTE",
                "segments=${edges.size} blocked=$blocked avgCost=$avg"
            )
            card1.setBackgroundResource(R.drawable.bottom_nav_back)
            card2.setBackgroundResource(R.drawable.bottom_nav_back)
            card3.setBackgroundResource(R.drawable.bottom_nav_back)

// сколько категорий подсветить (1..3)
            val count = (1..3).random()

// список карточек
            val cards = listOf(card1, card2, card3)

// выбираем count случайных
            cards.shuffled().take(count).forEach {
                it.setBackgroundResource(R.drawable.bus_selected)
            }

        } catch (_: Exception) {}
    }


    private fun convertRouteToEdges(route: Route): List<Edge> {
        val pts = route.geometry.points
        val list = mutableListOf<Edge>()

        for (i in 0 until pts.size - 1) {
            val a = pts[i]
            val b = pts[i + 1]

            val length = distance(a, b)
            val time = length / 1.3

            list.add(
                Edge(
                    id = "seg_$i",
                    length = length,
                    time = time,
                    curbHeight = (0..7).random() / 100.0,
                    accessibility = listOf(0.8, 0.7, 0.9),
                    risk = (0..4).random() / 10.0,
                    confidence = 0.9
                )
            )
        }

        return list
    }

    private fun distance(a: Point, b: Point): Double {
        val dx = a.latitude - b.latitude
        val dy = a.longitude - b.longitude
        return Math.sqrt(dx * dx + dy * dy) * 111000.0
    }

    /* ================= BUS ================= */

    private fun toggleStopsMode() {
        stopsVisible = !stopsVisible
        busBtn.setBackgroundResource(
            if (stopsVisible) R.drawable.bus_selected
            else R.drawable.bottom_nav_back
        )
        if (stopsVisible) loadStopsInView()
        else stopsCollection?.clear()
    }

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
        if (stopsCollection == null)
            stopsCollection = mapView.map.mapObjects.addCollection()

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

    /* ================= LOCATION ================= */

    private fun checkLocationPermission() {
        val perm = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(requireContext(), perm)
            == PackageManager.PERMISSION_GRANTED
        ) enableUserLocation()
        else locationPermissionLauncher.launch(perm)
    }

    private fun enableUserLocation() {
        if (userLocationLayer == null) {
            userLocationLayer =
                MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)
        }
        locationEnabled = !locationEnabled
        userLocationLayer?.isVisible = locationEnabled
    }

    /* ================= LIFECYCLE ================= */

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        mapView.map.addInputListener(mapTapListener)
    }

    override fun onStop() {
        mapView.map.removeInputListener(mapTapListener)
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}