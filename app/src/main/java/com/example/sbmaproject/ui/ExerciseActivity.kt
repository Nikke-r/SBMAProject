package com.example.sbmaproject.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.sbmaproject.classes.Exercise
import com.example.sbmaproject.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.snapshotter.MapSnapshot
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.activity_exercise.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round
import kotlin.system.measureTimeMillis

class ExerciseActivity :
    AppCompatActivity(),
    PermissionsListener,
    LocationEngineCallback<LocationEngineResult>,
    SensorEventListener,
    DialogInterface.OnClickListener {

    //Declare needed variables
    private lateinit var map: MapboxMap
    private lateinit var permissionManager: PermissionsManager
    private lateinit var locationEngine: LocationEngine
    private lateinit var geoJsonSource: GeoJsonSource
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private var sensorManager: SensorManager? = null
    private val locationListenerCallback = this
    private val routePoints: MutableList<Point> = ArrayList()
    private var running: Boolean = false
    private var highestSpeed: Double = 0.00
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())


    //Set interval and maximum wait times for location updates in milliseconds
    companion object {
        private const val INTERVAL: Long = 1000
        private const val MAX_WAIT_TIME: Long = 5000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        setContentView(R.layout.activity_exercise)

        mapView.onCreate(savedInstanceState)

        geoJsonSource = GeoJsonSource("line-source")

        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap
            map.setMinZoomPreference(12.0)
            map.setStyle(Style.MAPBOX_STREETS) {
                initializeLocationComponent(it)
                initializeLinePath(it)
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        distanceValueLabel.text = getString(R.string.distance_value, 0.00)
        speedValueLabel.text = getString(R.string.speed_value, 0.00)
        stepsValueLabel.text = getString(R.string.steps_value, "0")
        timeValueLabel.text = getString(R.string.time_value, "0:00:00")

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.setPeekHeight(150, true)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        handleFabChange()
    }

    //Initialize Mapbox location component. We suppress MissingPermission lint because we are
    //asking the permissions with Mapbox permission manager
    @SuppressLint("MissingPermission")
    private fun initializeLocationComponent(loadedMapStyle: Style) {

        //Use the Mapbox permissions manager to check if the location permissions are granted
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            //Create options for location component, in this case just add the pulse animation
            val locationComponentOptions = LocationComponentOptions.builder(this)
                .pulseEnabled(true)
                .build()

            //Get the location component
            val locationComponent = map.locationComponent

            //Activate the location component with options
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .locationComponentOptions(locationComponentOptions)
                    .build()
            )

            //Enable the location component and set the camera and render modes
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING_GPS
            locationComponent.renderMode = RenderMode.NORMAL
        } else {

            //If the location permissions are not granted, request them.
            //After this onPermissionResult will be called
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
        }
    }

    //If the user denys the access for the location first time, this Toast will be shown
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, getString(R.string.location_needed), Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //If the user allows the location initialize the location component
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            map.getStyle() {
                initializeLocationComponent(it)
            }
        }
    }

    //Initialize the location engine to update the users location
    //We also Suppress MissingPermissions lint since this wont be called unless we have permissions
    @SuppressLint("MissingPermission")
    private fun initializeLocationEngine() {

        //Init the locationEngine
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)

        //Init the request
        val request = LocationEngineRequest.Builder(INTERVAL)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(MAX_WAIT_TIME)
            .build()

        //Request location updates
        locationEngine.requestLocationUpdates(
            request,
            locationListenerCallback,
            Looper.getMainLooper()
        )
    }

    //Initialize the geoJsonSource to show the path user has ran/walked
    private fun initializeLinePath(style: Style) {
        style.addSource(geoJsonSource)
        style.addLayer(LineLayer("lineLayer", "line-source")
            .withProperties(
                PropertyFactory.lineColor(Color.parseColor("#32a852")),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineWidth(8f)
            ))
    }

    //Initialize step counter
    private fun initializeStepCounter() {
        val stepCounter = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepCounter == null) {
            Toast.makeText(this, "Step Counter Sensor not available", Toast.LENGTH_LONG)
                .show()
        } else {
            sensorManager?.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_UI)
        }
    }

    //If the location update is successfully received, pass the point to our point array
    //Then add the array to our to geoJsonSource to draw the route on the map.
    //After that calculate the route length using Turf and update the textView and
    //Also calculate the estimated speed
    override fun onSuccess(result: LocationEngineResult?) {
        for (location in result!!.locations) {
            val point = Point.fromLngLat(location.longitude, location.latitude)

            routePoints.add(point)

            geoJsonSource.setGeoJson(
                Feature.fromGeometry(
                    LineString.fromLngLats(routePoints)
                )
            )

            val length = TurfMeasurement.length(routePoints, TurfConstants.UNIT_METERS)
            distanceValueLabel.text = getString(R.string.distance_value, (length / 1000))

            val timeInMillis = seconds * 1000

            calculateSpeed(length, timeInMillis)
        }
    }

    //If the location update fails inform the user
    override fun onFailure(exception: Exception) {
        Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_LONG).show()
    }

    //Take a snapshot of the map in the end of exercise to show a picture of ran route
    private fun snapShotMapView() {
        val snapshotOptions = MapSnapshotter.Options(500, 500)

        snapshotOptions.withRegion(map.projection.visibleRegion.latLngBounds)
        snapshotOptions.withStyle(map.style!!.uri)

        val mapSnapshotter = MapSnapshotter(this, snapshotOptions)

        mapSnapshotter.start {
            val resultIntent = Intent(this, ExerciseResultActivity::class.java)

            resultIntent.putExtra("mapBitmap", it.bitmap)
            resultIntent.putExtra("distance", distanceValueLabel.text)
            resultIntent.putExtra("time", timeValueLabel.text)
            resultIntent.putExtra("steps", stepsValueLabel.text)
            resultIntent.putExtra("highestSpeed", highestSpeed)

            startActivity(resultIntent)
        }
    }

    private fun calculateSpeed(meters: Double, milliseconds: Int) {
        val seconds = milliseconds / 1000
        val meterPerSecond = meters / seconds
        val kilometersPerHour = meterPerSecond * 3.6

        speedValueLabel.text = getString(R.string.speed_value, kilometersPerHour)

        if (kilometersPerHour > highestSpeed) {
            highestSpeed = kilometersPerHour
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (running) {
            val steps = event!!.values[0]
            stepsValueLabel.text = steps.toString()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {
        Log.i("DBG", "Accuracy changed on sensor: $sensor, new value: $p1")
    }

    private fun handleFabChange() {
        if (!running) {
            exerciseFab.setOnClickListener {
                running = true
                exerciseFab.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.ic_exercise_stop)
                )
                handler.post(StopWatch())
                initializeLocationEngine()
                initializeStepCounter()
                handleFabChange()
            }
        } else {
            exerciseFab.setOnClickListener {
                confirmExit()
            }
        }
    }

    private fun confirmExit() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_exercise))
            .setMessage(getString(R.string.verification_text))
            .setPositiveButton(getString(R.string.yes), this)
            .setNegativeButton(getString(R.string.no), this)
            .show()
    }

    override fun onClick(p0: DialogInterface?, p1: Int) {
        if (p1 == -1) {
            running = false
            locationEngine.removeLocationUpdates(locationListenerCallback)
            handleFabChange()
            snapShotMapView()
        }
    }

    private inner class StopWatch: Runnable {
        override fun run() {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60

            val time = String.format(
                Locale.getDefault(),
                "%d:%02d:%02d",
                hours,
                minutes,
                secs
            )

            timeValueLabel.text = getString(R.string.time_value, time)

            if (running) {
                seconds++
            }

            handler.postDelayed(this, 1000)
        }

    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapView.onSaveInstanceState(outState)
    }
}