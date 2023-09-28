package ru.netology.yandexmaps.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.*
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import ru.netology.yandexmaps.R
import ru.netology.yandexmaps.adapter.MyListAdapter
import ru.netology.yandexmaps.model.Model
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.search.*
import ru.netology.yandexmaps.BuildConfig.MAPS_API_KEY


class MainActivity : AppCompatActivity(), UserLocationObjectListener,Session.SearchListener,
    CameraListener, DrivingSession.DrivingRouteListener {

    lateinit var mapview: MapView
    lateinit var probkibut: Button
    lateinit var locationmapkit: UserLocationLayer
    lateinit var searchEdit: EditText
    lateinit var searchManager: SearchManager
    lateinit var searchSession: Session
    private var ROUTE_START_LOCATION = Point(47.229410, 39.718281)
    private var ROUTE_END_LOCATION = Point(47.214004, 39.794605)
    private var SCREEN_CENTER = Point(
        (ROUTE_START_LOCATION.latitude + ROUTE_END_LOCATION.latitude) / 2,
        (ROUTE_START_LOCATION.longitude + ROUTE_END_LOCATION.longitude) / 2
    )
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var mapObjects: MapObjectCollection? = null
    private var drivingRouter: DrivingRouter? = null
    private var drivingSession: DrivingSession? = null
    lateinit var btlv: Button
    lateinit var btral: Button

    lateinit var listview: ListView
    var list = mutableListOf<Model>()
    lateinit var adapter: MyListAdapter

    private fun sumbitQuery(query: String) {
        searchSession = searchManager.submit(
            query,
            VisibleRegionUtils.toPolygon(mapview.map.visibleRegion),
            SearchOptions(),
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey(MAPS_API_KEY,)
        MapKitFactory.initialize(this)
        setContentView(R.layout.activity_main)
        mapview = findViewById(R.id.mapview)
        var mapKit: MapKit = MapKitFactory.getInstance()
        requstLocationPermission()
        var probki = mapKit.createTrafficLayer(mapview.mapWindow)
        probki.isTrafficVisible = false
        probkibut = findViewById(R.id.probkibut)
        probkibut.setOnClickListener {
            if (probki.isTrafficVisible == false) {
                probki.isTrafficVisible = true
                probkibut.setBackgroundResource(R.drawable.simpleblue)
            } else {
                probki.isTrafficVisible = false
                probkibut.setBackgroundResource(R.drawable.blueoff)
            }
        }
        listview = findViewById(R.id.listview)
        btlv = findViewById(R.id.btlv)
        btral = findViewById(R.id.btral)
        btral.setOnClickListener { mapObjects!!.clear() }
        var opened = false
        btlv.setOnClickListener {
            if (opened) {
                listview.visibility = View.VISIBLE
                opened = false
            } else {
                listview.visibility = View.INVISIBLE
                opened = true
            }
        }
        locationmapkit = mapKit.createUserLocationLayer(mapview.mapWindow)

        locationmapkit.isVisible = true
        locationmapkit.setObjectListener(this)
        SearchFactory.initialize(this)

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        mapview.map.addCameraListener(this)
        searchEdit = findViewById(R.id.search_edit)
        searchEdit.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                sumbitQuery(searchEdit.text.toString())
            }
            false
        }
        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter()
        mapObjects = mapview.map.mapObjects.addCollection()
        reaacord()
    }

    @SuppressLint("MissingPermission")
    private fun reaacord() {
        val db = Firebase.firestore
        db.collection("cords")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    var name = document.get("name").toString()
                    var lat = document.get("lat").toString().toDouble()
                    var lon = document.get("lon").toString().toDouble()
                    mapObjects!!.addPlacemark(Point(lat, lon))
                    list.add(Model(name, lat.toString(), lon.toString()))
                    adapter = MyListAdapter(this, R.layout.row, list)
                    listview.adapter = adapter
                    fusedLocationProviderClient =
                        LocationServices.getFusedLocationProviderClient(this)
                    fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                        ROUTE_START_LOCATION = Point(
                            location?.latitude.toString().toDouble(),
                            location?.longitude.toString().toDouble()
                        )
                        true
                    }
                    listview.setOnItemClickListener { parent, view, position, id ->
                        when (position) {
                            0 -> {
                                ROUTE_END_LOCATION = Point(47.255556, 39.793932);
                                SCREEN_CENTER = Point(
                                    (ROUTE_START_LOCATION.latitude + ROUTE_END_LOCATION.latitude) / 2,
                                    (ROUTE_START_LOCATION.longitude + ROUTE_END_LOCATION.longitude) / 2
                                )
                                sumbitRequest()
                            }

                            1 -> {
                                ROUTE_END_LOCATION = Point(47.268415, 39.714822)
                                SCREEN_CENTER = Point(
                                    (ROUTE_START_LOCATION.latitude + ROUTE_END_LOCATION.latitude) / 2,
                                    (ROUTE_START_LOCATION.longitude + ROUTE_END_LOCATION.longitude) / 2
                                )
                                sumbitRequest()
                            }

                            2 -> {
                                ROUTE_END_LOCATION = Point(47.203156, 39.641854)
                                SCREEN_CENTER = Point(
                                    (ROUTE_START_LOCATION.latitude + ROUTE_END_LOCATION.latitude) / 2,
                                    (ROUTE_START_LOCATION.longitude + ROUTE_END_LOCATION.longitude) / 2
                                )
                                sumbitRequest()
                            }
                        }
                    }


                }
            }
            .addOnFailureListener { exception ->

            }
    }

    private fun requstLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                0
            )
            return
        }
    }

    override fun onStop() {
        mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        mapview.onStart()
        MapKitFactory.getInstance().onStart()
        super.onStart()
    }

    override fun onObjectAdded(userLocationView: UserLocationView) {
        locationmapkit.setAnchor(
            PointF((mapview.width() * 0.5).toFloat(), (mapview.height() * 0.5).toFloat()),
            PointF((mapview.width() * 0.5).toFloat(), (mapview.height() * 0.83).toFloat())
        )
        userLocationView.arrow.setIcon(ImageProvider.fromResource(this, R.drawable.user_arrow))
        val picIcon = userLocationView.pin.useCompositeIcon()
        picIcon.setIcon(
            "icon",
            ImageProvider.fromResource(this, R.drawable.nothing),
            IconStyle().setAnchor(PointF(0f, 0f))

        )

        picIcon.setIcon(
            "pin", ImageProvider.fromResource(this, R.drawable.nothing),
            IconStyle().setAnchor(PointF(0.5f, 05f)).setRotationType(RotationType.ROTATE)
                .setZIndex(1f).setScale(0.5f)
        )
        userLocationView.accuracyCircle.fillColor = Color.BLUE and -0x66000001
    }

    override fun onObjectRemoved(p0: UserLocationView) {
    }

    override fun onObjectUpdated(p0: UserLocationView, p1: ObjectEvent) {

    }

    override fun onSearchResponse(response: Response) {
        val mapObjects: MapObjectCollection = mapview.map.mapObjects
        for (searchResult in response.collection.children) {
            val resultLocation = searchResult.obj!!.geometry[0].point!!
            if (response != null) {
                mapObjects.addPlacemark(
                    resultLocation,
                    ImageProvider.fromResource(this, R.drawable.search_result)
                )
            }
        }
    }

    override fun onSearchError(p0: com.yandex.runtime.Error) {
        var errorMessage = "Неизвестная Ошибка!"
        if (p0 is RemoteError) {
            errorMessage = "Беспрводная ошибка!"
        } else if (p0 is NetworkError) {
            errorMessage = "Проблема с интрнетом!"
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }



    override fun onCameraPositionChanged(
        map: Map,
        cameraPosition: CameraPosition,
        cameraUpdateReason: CameraUpdateReason,
        finished: Boolean
    ) {
        if (finished) {
            sumbitQuery(searchEdit.text.toString())
        }
    }

    override fun onDrivingRoutes(p0: MutableList<DrivingRoute>) {
        for (route in p0) {
            mapObjects!!.addPolyline(route.geometry)
        }
    }

    override fun onDrivingRoutesError(p0: com.yandex.runtime.Error) {
        var errorMessage = "Неизвестная ошибка!"
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT)
    }

    //Важная -----------------------------------------------------------------------
    private fun sumbitRequest() {
        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()
        val requestPoints: ArrayList<RequestPoint> = ArrayList()
        requestPoints.add(RequestPoint(ROUTE_START_LOCATION, RequestPointType.WAYPOINT, null))
        requestPoints.add(RequestPoint(ROUTE_END_LOCATION, RequestPointType.WAYPOINT, null))
        drivingSession =
            drivingRouter!!.requestRoutes(requestPoints, drivingOptions, vehicleOptions, this)
    }
}