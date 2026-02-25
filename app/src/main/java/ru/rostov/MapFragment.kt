package ru.rostov

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.PointF
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRouterType
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.directions.driving.VehicleType
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.PedestrianRouter
import com.yandex.mapkit.transport.masstransit.Route
import com.yandex.mapkit.transport.masstransit.RouteOptions
import com.yandex.mapkit.transport.masstransit.Session
import com.yandex.mapkit.transport.masstransit.TimeOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.collections.first
import kotlin.collections.isNotEmpty
import androidx.core.graphics.toColorInt
import com.yandex.mapkit.transport.masstransit.FitnessOptions
import com.yandex.runtime.image.ImageProvider

@Suppress("DEPRECATION")
class MapFragment : Fragment(), CameraListener, DrivingSession.DrivingRouteListener {
    enum class RouteMode {
        DRIVING,
        PEDESTRIAN,
        SIM
    }
    private val parser = Parsing()


    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private var mapView: MapView? = null
    private var topPanel: ConstraintLayout? = null
    private var cityName: TextView? = null
    private var imageOfCity: ImageView? = null

    private var currentCity: String? = null
    private lateinit var drivingRouter: DrivingRouter
    private var drivingSession: DrivingSession? = null

    private lateinit var pedestrianRouter: PedestrianRouter
    private var currentRouteMode: RouteMode = RouteMode.DRIVING

    private var ai_generate_button: ConstraintLayout? = null
    private var ai_regen: ConstraintLayout? = null
    private var ai_clear: ConstraintLayout? = null

    private var currentModeIndex = 0
    private val routeIcons = listOf(
        R.drawable.marshrut_pedestrain, // пешком
        R.drawable.marshrut_car,        // машина
        R.drawable.marshrut_scooter     // самокат
    )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        MapKitFactory.initialize(requireContext())
        TransportFactory.getInstance()
        drivingRouter =
            DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.ONLINE)
        // Инициализация пешеходного маршрутизатора
        val transport = TransportFactory.getInstance()
        pedestrianRouter = transport.createPedestrianRouter()

        fusedLocationClient =
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(
                requireContext()
            )

        val view = inflater.inflate(R.layout.fragment_map, container, false)

        ai_regen = view.findViewById(R.id.ai_regen)
        ai_clear = view.findViewById(R.id.ai_clear)
        ai_generate_button = view.findViewById(R.id.ai_generate_button)
        mapView = view.findViewById(R.id.mapview)
        topPanel = view.findViewById(R.id.topPanel)
        cityName = view.findViewById(R.id.cityname)
        imageOfCity = view.findViewById(R.id.imageOfCity)

        //Обработка маршрутов
        val typeMershrut = view.findViewById<ConstraintLayout>(R.id.typeMershrut)
        val routeTypeIcon = view.findViewById<ImageView>(R.id.routeTypeIcon)

        typeMershrut.setOnClickListener {
            // --- Анимация увеличения ---
            typeMershrut.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(120)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .withEndAction {
                    typeMershrut.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                }
                .start()

            // --- Меняем иконку ---
            currentModeIndex = (currentModeIndex + 1) % routeIcons.size
            routeTypeIcon.setImageResource(routeIcons[currentModeIndex])

            // --- При желании можно логировать ---
            when (currentModeIndex) {
                0 -> currentRouteMode = RouteMode.PEDESTRIAN
                1 -> currentRouteMode = RouteMode.DRIVING
                2 -> currentRouteMode = RouteMode.SIM
            }
        }

        // Стартовая позиция
        mapView?.map?.move(
            CameraPosition(Point(47.2357, 39.7015), 12.0f, 0.0f, 0.0f)
        )

        ai_generate_button!!.setOnClickListener {
            getCurrentLocation { startLat, startLon ->
                ai_generate_button!!.visibility = View.GONE
                typeMershrut!!.visibility = View.GONE

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val result = AiRouteBuilder.buildRoute(
                            context = requireContext(),
                            startLat = startLat,
                            startLon = startLon,
                            city = currentCity ?: "Ростов-на-Дону"
                        )

                        if (!result.isNullOrBlank()) {
                            Log.d("MAP_ROUTE", "Результат нейросети: $result")

                            val map = mapView?.map ?: return@launch
                            val mapObjects = map.mapObjects
                            mapObjects.clear()
                            animateCityChange(currentCity.toString())
                            val points = result.split("|").filter { it.isNotBlank() }
                            val routePoints =
                                mutableListOf<Pair<String, Point>>() // Название + координаты

                            // Стартовая точка
                            val startPoint = Point(startLat, startLon)
                            routePoints.add("Начало маршрута" to startPoint)
                            mapObjects.addPlacemark(startPoint).apply { setText("Начало маршрута") }

                            // Достопримечательности
                            points.forEach { item ->
                                val parts = item.split(":")
                                if (parts.size == 2) {
                                    val name = parts[0]
                                    val coords = parts[1].split(",")
                                    if (coords.size == 2) {
                                        val lat = coords[0].toDoubleOrNull()
                                        val lon = coords[1].toDoubleOrNull()
                                        if (lat != null && lon != null) {
                                            val point = Point(lat, lon)
                                            routePoints.add(name to point)
                                            mapObjects.addPlacemark(point).apply { setText(name) }
                                            Log.d("MAP_POINT", "$name — $lat, $lon")
                                        }
                                    }
                                }
                            }

                            // --- Оптимизация маршрута (жадный TSP) ---
                            val start = routePoints.first()
                            val remaining = routePoints.drop(1).toMutableList()
                            val optimized = mutableListOf<Pair<String, Point>>()
                            var current = start
                            optimized.add(current)
                            while (remaining.isNotEmpty()) {
                                val nextIndex = remaining.indices.minByOrNull {
                                    distanceBetween(current.second, remaining[it].second)
                                } ?: 0
                                current = remaining.removeAt(nextIndex)
                                optimized.add(current)
                            }

                            // --- Создаём RequestPoints ---
                            val requestPoints = optimized.map {
                                RequestPoint(it.second, RequestPointType.WAYPOINT, null, null, null)
                            }

                            // --- Вызов универсального метода маршрута ---
                            buildRoute(requestPoints)

                            showWithAnimation(ai_regen, delay = 0)
                            showWithAnimation(ai_clear, delay = 150)

                            //Перевязка кнопки маршрутов
                            // ✅ Перевязка кнопки маршрутов к ai_regen с анимацией
                            // ✅ Перевязка кнопки маршрутов к ai_regen с фиксированным размером (60dp)
                            val rootLayout = view.findViewById<ConstraintLayout>(R.id.rootLayout)

                            rootLayout.post {
                                val set = ConstraintSet().apply { clone(rootLayout) }

                                // Очистим старые связи
                                set.clear(R.id.typeMershrut, ConstraintSet.END)
                                set.clear(R.id.typeMershrut, ConstraintSet.TOP)
                                set.clear(R.id.typeMershrut, ConstraintSet.BOTTOM)

                                // Привязываем typeMershrut к ai_regen
                                set.connect(
                                    R.id.typeMershrut,
                                    ConstraintSet.END,
                                    R.id.ai_regen,
                                    ConstraintSet.START
                                )
                                set.connect(
                                    R.id.typeMershrut,
                                    ConstraintSet.TOP,
                                    R.id.ai_regen,
                                    ConstraintSet.TOP
                                )
                                set.connect(
                                    R.id.typeMershrut,
                                    ConstraintSet.BOTTOM,
                                    R.id.ai_regen,
                                    ConstraintSet.BOTTOM
                                )

                                // Устанавливаем фиксированные размеры 60dp
                                val scale = resources.displayMetrics.density
                                val sizePx = (60 * scale).toInt()
                                set.constrainWidth(R.id.typeMershrut, sizePx)
                                set.constrainHeight(R.id.typeMershrut, sizePx)

                                // Включаем анимацию изменения связей
                                val transition = androidx.transition.ChangeBounds().apply {
                                    duration = 400
                                    interpolator =
                                        android.view.animation.OvershootInterpolator(1.2f)
                                }

                                androidx.transition.TransitionManager.beginDelayedTransition(
                                    rootLayout,
                                    transition
                                )
                                set.applyTo(rootLayout)

                                // Отображаем кнопку
                                typeMershrut.visibility = View.VISIBLE
                            }
                            ai_clear?.setOnClickListener {
                                ai_clear?.animate()
                                    ?.scaleX(1.3f)
                                    ?.scaleY(1.3f)
                                    ?.setDuration(120)
                                    ?.setInterpolator(android.view.animation.OvershootInterpolator())
                                    ?.withEndAction {
                                        ai_clear?.animate()
                                            ?.scaleX(1f)
                                            ?.scaleY(1f)
                                            ?.setDuration(150)
                                            ?.setInterpolator(android.view.animation.DecelerateInterpolator())
                                            ?.start()
                                    }
                                    ?.start()

                                val mapObjects = mapView?.map?.mapObjects
                                mapObjects?.clear()
                                animateCityChange(currentCity.toString())

                            }
                            ai_regen?.setOnClickListener {
                                // 🔄 Анимация нажатия
                                ai_regen?.animate()
                                    ?.scaleX(1.2f)
                                    ?.scaleY(1.2f)
                                    ?.setDuration(120)
                                    ?.setInterpolator(android.view.animation.OvershootInterpolator())
                                    ?.withEndAction {
                                        ai_regen?.animate()
                                            ?.scaleX(1f)
                                            ?.scaleY(1f)
                                            ?.setDuration(150)
                                            ?.setInterpolator(android.view.animation.DecelerateInterpolator())
                                            ?.start()
                                    }?.start()

                                // ⚙️ Очищаем карту
                                val mapObjects = mapView?.map?.mapObjects
                                mapObjects?.clear()

                                animateCityChange(currentCity.toString())

                                // ⚙️ Запускаем новый запрос маршрута через нейросеть
                                viewLifecycleOwner.lifecycleScope.launch {
                                    try {
                                        val newResult = AiRouteBuilder.buildRoute(
                                            context = requireContext(),
                                            startLat = startLat,
                                            startLon = startLon,
                                            city = currentCity ?: "Ростов-на-Дону"
                                        )

                                        if (!newResult.isNullOrBlank()) {
                                            Log.d(
                                                "MAP_ROUTE",
                                                "♻️ Новый маршрут от нейросети: $newResult"
                                            )

                                            val map = mapView?.map ?: return@launch
                                            val mapObjects = map.mapObjects
                                            val points =
                                                newResult.split("|").filter { it.isNotBlank() }
                                            val routePoints = mutableListOf<Pair<String, Point>>()

                                            // Стартовая точка
                                            val startPoint = Point(startLat, startLon)
                                            routePoints.add("Начало маршрута" to startPoint)
                                            mapObjects.addPlacemark(startPoint)
                                                .apply { setText("Начало маршрута") }

                                            // Добавляем достопримечательности
                                            points.forEach { item ->
                                                val parts = item.split(":")
                                                if (parts.size == 2) {
                                                    val name = parts[0]
                                                    val coords = parts[1].split(",")
                                                    if (coords.size == 2) {
                                                        val lat = coords[0].toDoubleOrNull()
                                                        val lon = coords[1].toDoubleOrNull()
                                                        if (lat != null && lon != null) {
                                                            val point = Point(lat, lon)
                                                            routePoints.add(name to point)
                                                            mapObjects.addPlacemark(point)
                                                                .apply { setText(name) }
                                                        }
                                                    }
                                                }
                                            }

                                            // ⚙️ Оптимизация маршрута
                                            val start = routePoints.first()
                                            val remaining = routePoints.drop(1).toMutableList()
                                            val optimized = mutableListOf<Pair<String, Point>>()
                                            var current = start
                                            optimized.add(current)
                                            while (remaining.isNotEmpty()) {
                                                val nextIndex = remaining.indices.minByOrNull {
                                                    distanceBetween(
                                                        current.second,
                                                        remaining[it].second
                                                    )
                                                } ?: 0
                                                current = remaining.removeAt(nextIndex)
                                                optimized.add(current)
                                            }

                                            // ⚙️ Строим новый маршрут
                                            val requestPoints = optimized.map {
                                                RequestPoint(
                                                    it.second,
                                                    RequestPointType.WAYPOINT,
                                                    null,
                                                    null,
                                                    null
                                                )
                                            }
                                            buildRoute(requestPoints)
                                        } else {
                                            Log.e(
                                                "MAP_ROUTE",
                                                "Нейросеть не вернула результат при регенерации маршрута"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MAP_ROUTE", "Ошибка при регенерации маршрута", e)
                                    }
                                }
                            }


                        } else {
                            Log.e("MAP_ROUTE", "Нейросеть не вернула результат")
                        }

                    } catch (e: Exception) {
                        Log.e("MAP_ROUTE", "Ошибка при вызове нейросети", e)
                    }
                }
            }

        }

        imageOfCity?.setBackgroundResource(R.drawable.logo_rostov)
        mapView?.map?.addCameraListener(this)

        startEnterAnimations()
        return view
    }

    fun distanceBetween(p1: Point, p2: Point): Double {
        val latDiff = p1.latitude - p2.latitude
        val lonDiff = p1.longitude - p2.longitude
        return latDiff * latDiff + lonDiff * lonDiff
    }

    private fun buildRoute(requestPoints: List<RequestPoint>) {
        // Проверяем, есть ли хотя бы две точки (начало и конец)
        if (requestPoints.size < 2) {
            Log.e("MAP_ROUTE_DEBUG", "Недостаточно точек для построения маршрута")
            return
        }

        // Очищаем существующие объекты маршрутов на карте
        val mapObjects = mapView?.map?.mapObjects ?: return
        mapObjects.clear()

        Log.d("MAP_ROUTE_DEBUG", "Старт buildRoute с ${requestPoints.size} точками")
        requestPoints.forEachIndexed { index, rp ->
            Log.d("MAP_ROUTE_DEBUG", "Точка $index: ${rp.point.latitude}, ${rp.point.longitude}")
        }

        if (currentRouteMode == RouteMode.DRIVING) {
            val drivingOptions = DrivingOptions()
            val vehicleOptions = VehicleOptions().apply { vehicleType = VehicleType.DEFAULT }
            try {
                drivingSession = drivingRouter.requestRoutes(
                    requestPoints,
                    drivingOptions,
                    vehicleOptions,
                    this@MapFragment
                )
                Log.d("MAP_ROUTE_DEBUG", "🚗 Запрошен автомобильный маршрут")
            } catch (e: Exception) {
                Log.e("MAP_ROUTE_DEBUG", "Ошибка при построении авто-маршрута: ${e.message}", e)
            }
        } else if (currentRouteMode == RouteMode.PEDESTRIAN) {
            val pedRouter = TransportFactory.getInstance().createPedestrianRouter()
            val routeOptions = RouteOptions(FitnessOptions()) // Важно: конструктор с FitnessOptions

            fun requestNextSegment(i: Int) {
                if (i >= requestPoints.size - 1) return

                val segmentPoints = listOf(requestPoints[i], requestPoints[i + 1])
                pedRouter.requestRoutes(
                    segmentPoints,
                    TimeOptions(),
                    routeOptions,
                    object : Session.RouteListener {
                        override fun onMasstransitRoutes(routes: MutableList<Route>) {
                            if (routes.isNotEmpty()) {
                                val route = routes.first()
                                mapObjects.addPolyline(route.geometry)?.apply {
                                    setStrokeColor("#4285F4".toColorInt())
                                    strokeWidth = 5f
                                }
                                Log.d("MAP_ROUTE", "Сегмент $i построен")
                            } else {
                                Log.e("MAP_ROUTE", "Маршрут не найден для сегмента $i")
                            }
                            requestNextSegment(i + 1)
                        }

                        override fun onMasstransitRoutesError(error: com.yandex.runtime.Error) {
                            Log.e("MAP_ROUTE", "Ошибка построения сегмента $i: $error")
                            requestNextSegment(i + 1)
                        }
                    }
                )
            }

            requestNextSegment(0)
        } else if (currentRouteMode == RouteMode.SIM) {
            val scooterRouter = TransportFactory.getInstance().createScooterRouter()

            val routeOptions = RouteOptions(FitnessOptions())

            val timeOptions = TimeOptions().apply {
                departureTime = System.currentTimeMillis()
            }

            scooterRouter.requestRoutes(
                requestPoints,
                timeOptions,
                routeOptions,
                object : Session.RouteListener {
                    override fun onMasstransitRoutes(routes: MutableList<Route>) {
                        if (routes.isNotEmpty()) {
                            val route = routes.first()
                            mapObjects.addPolyline(route.geometry)?.apply {
                                setStrokeColor("#4285F4".toColorInt())
                                strokeWidth = 5f
                            }
                            Log.d("MAP_ROUTE", "Маршрут самоката построен")
                        } else {
                            Log.e("MAP_ROUTE", "Маршруты самоката не найдены")
                        }
                    }

                    override fun onMasstransitRoutesError(error: com.yandex.runtime.Error) {
                        Log.e("MAP_ROUTE", "Ошибка построения маршрута самоката: $error")
                    }
                }
            )
        }

    }


    // --- Слушатель результата построения маршрута ---
    override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
        val map = mapView?.map ?: return
        val mapObjects = map.mapObjects

        if (routes.isNotEmpty()) {
            val route = routes.first()
            mapObjects.addPolyline(route.geometry)
            Log.d("MAP_ROUTE", "Маршрут успешно построен")
        } else {
            Log.e("MAP_ROUTE", "Маршруты не найдены")
        }
    }

    override fun onDrivingRoutesError(error: com.yandex.runtime.Error) {
        Log.e("MAP_ROUTE_ERROR", "Ошибка при построении маршрута: $error")
    }

    private fun startEnterAnimations() {
        val allViews = listOf(topPanel, cityName, imageOfCity)
        allViews.forEach { it?.alpha = 0f }

        topPanel?.translationY = -100f
        topPanel?.animate()
            ?.alpha(1f)
            ?.translationY(0f)
            ?.setDuration(700)
            ?.setInterpolator(OvershootInterpolator(1.2f))
            ?.start()

        cityName?.postDelayed({
            cityName?.translationY = 40f
            cityName?.animate()
                ?.alpha(1f)
                ?.translationY(0f)
                ?.setDuration(600)
                ?.setInterpolator(OvershootInterpolator(1.2f))
                ?.start()
        }, 250)

        imageOfCity?.apply {
            scaleX = 0.6f
            scaleY = 0.6f
            translationY = 40f
            postDelayed({
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(700)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }, 500)
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        mapView?.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onCameraPositionChanged(
        map: com.yandex.mapkit.map.Map,
        cameraPosition: CameraPosition,
        cameraUpdateReason: com.yandex.mapkit.map.CameraUpdateReason,
        finished: Boolean
    ) {
        if (finished) {
            val center = cameraPosition.target
            val geocoder = Geocoder(requireContext(), Locale.getDefault())

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val addresses = geocoder.getFromLocation(center.latitude, center.longitude, 1)
                    val city = addresses?.firstOrNull()?.locality ?: "Не найден"

                    withContext(Dispatchers.Main) {
                        if (city == currentCity) return@withContext
                        currentCity = city
                        animateCityChange(city)
                    }
                } catch (e: Exception) {
                    Log.e("MapFragment", "Geocoding error: ${e.message}")
                    withContext(Dispatchers.Main) { cityName?.text = "Ошибка" }
                }
            }
        }
    }

    private fun animateCityChange(city: String) {
        cityName?.animate()
            ?.alpha(0f)
            ?.setDuration(150)
            ?.withEndAction {
                cityName?.text = city
                cityName?.animate()
                    ?.alpha(1f)
                    ?.setDuration(300)
                    ?.start()
            }?.start()

        imageOfCity?.animate()
            ?.alpha(0f)
            ?.scaleX(0.8f)
            ?.scaleY(0.8f)
            ?.setDuration(150)
            ?.withEndAction {
                when (city) {
                    "Ростов-на-Дону" -> setResourceBg(imageOfCity!!, R.drawable.logo_rostov)
                    "Таганрог" -> setResourceBg(imageOfCity!!, R.drawable.logo_taganrog)
                    "Азов" -> setResourceBg(imageOfCity!!, R.drawable.logo_azov)
                    "Новочеркасск" -> setResourceBg(imageOfCity!!, R.drawable.logo_novocherkask)
                    "Волгодонск" -> setResourceBg(imageOfCity!!, R.drawable.logo_volgodonsk)
                    "Цимлянск" -> setResourceBg(imageOfCity!!, R.drawable.logo_cimlansk)
                    "Семикаракорск" -> setResourceBg(imageOfCity!!, R.drawable.logo_semikarakorsk)
                    "Батайск" -> setResourceBg(imageOfCity!!, R.drawable.logo_bataysk)
                    "Новошахтинск" -> setResourceBg(imageOfCity!!, R.drawable.logo_novoshahtink)
                    "Старочеркасская" -> setResourceBg(imageOfCity!!, R.drawable.logo_starocherkask)
                    else -> imageOfCity?.setBackgroundResource(0)
                }
                loadCityAttractions(city)

                imageOfCity?.animate()
                    ?.alpha(1f)
                    ?.scaleX(1f)
                    ?.scaleY(1f)
                    ?.setDuration(300)
                    ?.setInterpolator(OvershootInterpolator(1.5f))
                    ?.start()
            }?.start()
    }

    private fun setResourceBg(view: View, image: Int) {
        view.setBackgroundResource(image)
    }

    private fun showWithAnimation(view: ConstraintLayout?, delay: Long = 0) {
        view?.visibility = View.VISIBLE
        view?.scaleX = 0f
        view?.scaleY = 0f
        view?.alpha = 0f
        view?.animate()
            ?.setStartDelay(delay)
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.alpha(1f)
            ?.setDuration(300)
            ?.setInterpolator(android.view.animation.OvershootInterpolator())
            ?.start()
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(onLocationReady: (latitude: Double, longitude: Double) -> Unit) {
        val context = requireContext()

        // Проверяем разрешение
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Запрашиваем разрешение у пользователя
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            // Пока нет разрешения — используем центр города
            Log.w("MAP_LOCATION", "📍 Нет разрешения, используем центр города временно")
            onLocationReady(47.222078, 39.720349)
            return
        }

        // Если разрешение уже есть — получаем текущую локацию
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(
                        "MAP_LOCATION",
                        "✅ Локация получена: ${location.latitude}, ${location.longitude}"
                    )
                    onLocationReady(location.latitude, location.longitude)
                } else {
                    Log.w("MAP_LOCATION", "⚠️ Локация null, fallback в центр города")
                    onLocationReady(47.222078, 39.720349)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MAP_LOCATION", "❌ Ошибка получения локации: ${e.message}")
                onLocationReady(47.222078, 39.720349)
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MAP_LOCATION", "✅ Разрешение получено, повторный вызов маршрута")

                // После получения разрешения — повторно получаем локацию и перегенерируем маршрут
                getCurrentLocation { lat, lon ->
                    ai_generate_button?.performClick() // повторное нажатие, чтобы запустить маршрут
                }
            } else {
                Log.e("MAP_LOCATION", "🚫 Разрешение отклонено, используем центр города")
            }
        }
    }

    private fun loadCityAttractions(city: String) {
        val context = requireContext()
        val mapObjects = mapView?.map?.mapObjects ?: return
        mapObjects.clear()

        // Преобразуем название города в имя файла (например: rostov.txt)
        val fileName = when (city) {
            "Ростов-на-Дону" -> "rostov.txt"
            "Таганрог" -> "tagonrog.txt"
            "Азов" -> "azov.txt"
            "Новочеркасск" -> "novocherkassk.txt"
            "Волгодонск" -> "volgodonsk.txt"
            "Цимлянск" -> "cimlansk.txt"
            "Семикаракорск" -> "semikarakorsk.txt"
            "Батайск" -> "bataysk.txt"
            "Новошахтинск" -> "novoshahtinsk.txt"
            "Старочеркасская" -> "starocherkassk.txt"
            "Шахты" -> "shahti.txt"
            else -> null
        }

        if (fileName == null) {
            Log.w("MAP_ATTR", "Для города '$city' нет файла в assets")
            return
        }

        try {
            val txt = parser.readPlacesFromAssets(context, fileName)
            val places = parser.parsePlaces(txt)

            if (places.isEmpty()) {
                Log.w("MAP_ATTR", "Нет достопримечательностей в файле $fileName")
                return
            }

            // Загружаем кастомную иконку
            val iconImage = ImageProvider.fromResource(context, R.drawable.landmark)

            places.forEach { attraction ->
                val coords = attraction.coordinates ?: return@forEach
                val point = com.yandex.mapkit.geometry.Point(coords.lat, coords.lng)

                // Добавляем placemark с кастомной иконкой
                val placemark = mapObjects.addPlacemark(point, iconImage)

                // Настраиваем стиль иконки
                placemark.setIconStyle(
                    com.yandex.mapkit.map.IconStyle().apply {
                        scale = 0.22f
                        anchor = PointF(0.5f, 1.0f)
                    }
                )


                Log.d("MAP_ATTR", "Добавлена точка: ${attraction.name} (${coords.lat}, ${coords.lng})")
            }

            Log.d("MAP_ATTR", "✅ Загружено ${places.size} достопримечательностей для $city")

        } catch (e: Exception) {
            Log.e("MAP_ATTR", "Ошибка загрузки достопримечательностей для $city", e)
        }
    }

}