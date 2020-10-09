package com.example.sbmaproject.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sbmaproject.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.*
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.activity_exercise.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class ExerciseActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {
    
    //Declare variables related to MapBox
    private lateinit var mapBoxMap: MapboxMap
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationComponent: LocationComponent
    private lateinit var locationEngine: LocationEngine
    private val locationCallback = LocationChangeListenerCallback()

    //Declare step counter variables
    private var sensorManager: SensorManager? = null
    private var steps: Int = 0

    //Declare Arrays/Lists
    private val routePoints: MutableList<Point> = ArrayList()
    private val speeds: ArrayList<Double> = arrayListOf()
    private val altitudes: ArrayList<Double> = arrayListOf()

    //Declare GeoJsonSources
    private lateinit var lineSource: GeoJsonSource

    //Declare Bottom sheet variable
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    //Declare exercise variables
    private var running: Boolean = false
    private var totalSpeeds: Double = 0.0
    private var highestSpeed: Double = 0.0
    private var averageSpeed: Double = 0.0

    //Declare variables for stopwatch
    private val handler = Handler(Looper.getMainLooper())
    private var seconds: Int = 0

    companion object {
        const val INTERVAL: Long = 1000
        const val MAX_WAIT_TIME: Long = 5000
        const val REQUEST_CODE_STEPS = 1000
        const val REQUEST_CODE_BGL = 2000
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_exercise)

        distanceValueLabel.text = getString(R.string.distance_value, 0.00)
        speedValueLabel.text = getString(R.string.speed_value, 0.00)
        stepsValueLabel.text = getString(R.string.steps_value, "0")
        timeValueLabel.text = getString(R.string.time_value, "0:00:00")
        highestSpeedValueLabel.text = getString(R.string.speed_value, 0.0)
        averageSpeedValueLabel.text = getString(R.string.speed_value, 0.0)
        currentAltitudeLabel.text = getString(R.string.altitude_value, 0.0)

        lineSource = GeoJsonSource("line-source")

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.setPeekHeight(200, true)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        updateFabButton()
    }

    /**
     * Override functions
     */

    //When the map has loaded this callback will be called
    override fun onMapReady(mapboxMap: MapboxMap) {
        mapBoxMap = mapboxMap
        mapBoxMap.setStyle(Style.MAPBOX_STREETS) {
            enableLocationComponent(it)
            addLineStringToMap(it)
        }
    }

    //If the user denies location permissions for the first time this function will be called
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Snackbar.make(snackBar, getString(R.string.location_needed), Snackbar.LENGTH_LONG).show()
    }

    //Pass the permission result handling to MapBox's permission manager
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //If the user grants or denies permissions
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapBoxMap.getStyle {
                enableLocationComponent(it)
            }
        } else {
            Snackbar.make(snackBar, "Location permissions not granted", Snackbar.LENGTH_LONG).show()
        }
    }

    /**
     * Private functions
     */

    //After the map has loaded activate the location component of the MapBox
    //We are using MapBox's permission manager to check the location permissions
    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(style: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationComponent = mapBoxMap.locationComponent

            val locationComponentOptions = LocationComponentActivationOptions
                .builder(this, style)
                .useDefaultLocationEngine(false)
                .build()

            locationComponent.activateLocationComponent(locationComponentOptions)
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS

            initializeLocationEngine()
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    //After we have enabled the location component we initialize the location engine and request
    //location updates
    @SuppressLint("MissingPermission")
    private fun initializeLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)

        val locationRequest = LocationEngineRequest.Builder(INTERVAL)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(MAX_WAIT_TIME)
            .build()

        locationEngine.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    //Add the linestring to loaded map to show the users route
    private fun addLineStringToMap(style: Style) {
        style.addSource(lineSource)
        style.addLayer(LineLayer("lineLayer", "line-source")
            .withProperties(
                lineColor(Color.parseColor("#00EA71")),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
                lineWidth(8f)
            ))
    }

    //Change state of the start/stop exercise button
    private fun updateFabButton() {
        if (!running) {
            exerciseFab.setOnClickListener {
                exerciseFab.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.ic_exercise_stop)
                )
                running = true
                handler.post(StopWatch())
                startStepCounter()
                updateFabButton()
            }
        } else {
            exerciseFab.setOnClickListener {
                confirmExit()
            }
        }
    }

    //Stat the step counter
    private fun startStepCounter() {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                REQUEST_CODE_STEPS
            )
        } else {
            val stepCounter = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

            if (stepCounter == null) {
                Snackbar.make(snackBar, "No step detector", Snackbar.LENGTH_LONG).show()
            } else {
                sensorManager?.registerListener(
                    StepDetectorCallback(),
                    stepCounter,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
        }
    }

    //When user press the exit exercise button this will be prompted
    private fun confirmExit() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.exit_exercise))
            .setMessage(getString(R.string.verification_text))
            .setPositiveButton(getString(R.string.yes), AlertDialogListener())
            .setNegativeButton(getString(R.string.no), AlertDialogListener())
            .show()
    }

    //Start the Summary Intent
    private fun finishExercise() {
        val resultIntent = Intent(this, ExerciseResultActivity::class.java)
        val encodedRoute = PolylineUtils.encode(routePoints, 5)

        resultIntent.putExtra("route", encodedRoute)
        resultIntent.putExtra("distance", distanceValueLabel.text)
        resultIntent.putExtra("time", timeValueLabel.text)
        resultIntent.putExtra("steps", stepsValueLabel.text)
        resultIntent.putExtra("highestSpeed", highestSpeed)
        resultIntent.putExtra("averageSpeed", averageSpeed)
        resultIntent.putExtra("speeds", speeds)
        resultIntent.putExtra("altitudes", altitudes)

        startActivity(resultIntent)
        finish()
    }

    /**
     * Private classes
     */

    //Listener for location updates
    private inner class LocationChangeListenerCallback: LocationEngineCallback<LocationEngineResult> {

        override fun onSuccess(result: LocationEngineResult?) {
            val location = result?.lastLocation ?: return

            if (running) {
                val point = Point.fromLngLat(location.latitude, location.longitude)
                Log.i("DBG", "Point: $point")
                routePoints.add(point)
                speeds.add(location.speed.toDouble())
                altitudes.add(location.altitude)

                val length = TurfMeasurement.length(routePoints, "meters")
                Log.i("DBG", "Length: $length")
                totalSpeeds += location.speed.toDouble()
                averageSpeed = totalSpeeds / speeds.size

                speedValueLabel.text = getString(R.string.speed_value, location.speed)
                averageSpeedValueLabel.text = getString(R.string.speed_value, averageSpeed)
                distanceValueLabel.text = getString(R.string.distance_value, length / 1000)
                currentAltitudeLabel.text = getString(R.string.altitude_value, location.altitude)

                if (location.speed > highestSpeed) {
                    highestSpeed = location.speed.toDouble()
                    highestSpeedValueLabel.text = getString(R.string.speed_value, highestSpeed)
                }

                lineSource.setGeoJson(
                    Feature.fromGeometry(
                        LineString.fromLngLats(routePoints)
                    )
                )

                Log.i("DBG", "LineSource: $lineSource")
            }

            mapBoxMap.locationComponent.forceLocationUpdate(location)
        }

        override fun onFailure(exception: Exception) {
            Snackbar.make(snackBar, exception.localizedMessage as CharSequence, Snackbar.LENGTH_LONG).show()
        }

    }

    //Stop watch for the timer
    private inner class StopWatch: Runnable {
        override fun run() {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60

            val time = String.format(
                Locale.getDefault(),
                "%d:%02d:%02d",
                hours,
                minutes,
                seconds % 60
            )

            timeValueLabel.text = time

            if (running) {
                seconds++
            }

            handler.postDelayed(this, 1000)
        }

    }

    //Listener for steps
    private inner class StepDetectorCallback: SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            Log.i("DBG", "onSensorChanged")
            if (running) {
                steps += event!!.values[0].toInt()
                Log.i("DBG", "Steps: $steps")
                stepsValueLabel.text = steps.toString()
            }
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            Log.i("DBG", "Accuracy changed: $p1")
        }
    }

    //Listener for the alert dialog
    private inner class AlertDialogListener: DialogInterface.OnClickListener {
        override fun onClick(p0: DialogInterface?, p1: Int) {
            if (p1 == -1) {
                finishExercise()
            }
        }

    }

    /**
     * Lifecycle methods
     */

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        locationEngine.removeLocationUpdates(locationCallback)
        locationComponent.isLocationComponentEnabled = false
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}