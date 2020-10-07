package com.example.sbmaproject.ui

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.sbmaproject.MainActivity
import com.example.sbmaproject.R
import com.example.sbmaproject.classes.Exercise
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.android.synthetic.main.activity_exercise_result.*
import java.util.*

class ExerciseResultActivity : AppCompatActivity() {

    private val database = Firebase.firestore
    private lateinit var map: MapboxMap
    private lateinit var geoJsonSource: GeoJsonSource

    private var distance: String = "0.00"
    private var time: String = "00:00:00"
    private var steps: String = "0"
    private var highestSpeed: Double = 0.00
    private var averageSpeed: Double = 0.00
    private var route: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        setContentView(R.layout.activity_exercise_result)

        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync {
            map = it
            map.setStyle(Style.MAPBOX_STREETS) {style ->
                drawRouteToMap(style)
            }
        }

        geoJsonSource = GeoJsonSource("line-source")

        val extras = intent.extras

        if (extras !== null) {
            route = extras.get("route") as String
            distance = extras.get("distance") as String
            time = extras.get("time") as String
            steps = extras.get("steps") as String
            highestSpeed = extras.getDouble("highestSpeed")
            averageSpeed = extras.getDouble("averageSpeed")
        }

        distanceResultLabel.text = distance
        timeResultLabel.text = time
        highestSpeedResultLabel.text = getString(R.string.speed_value, highestSpeed)
        stepsResultLabel.text = steps
        averageSpeedResultLabel.text = getString(R.string.speed_value, averageSpeed)

        postGoalButton.setOnClickListener {
            postResultsToFirebase()
        }

        geoJsonSource = GeoJsonSource("line-source")

        mapView.getMapAsync {
            map = it
            map.setStyle(Style.MAPBOX_STREETS) {style ->
                drawRouteToMap(style)
            }
        }
    }

    private fun drawRouteToMap(style: Style) {
        style.addSource(geoJsonSource)

        val routePoints = PolylineUtils.decode(route, 5)
        geoJsonSource.setGeoJson(
            Feature.fromGeometry(
                LineString.fromLngLats(routePoints)
            )
        )

        style.addLayer(LineLayer("lineLayer", "line-source")
            .withProperties(
                PropertyFactory.lineColor(Color.parseColor("#32a852")),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineWidth(8f)
            ))
    }

    private fun postResultsToFirebase() {

        val user = Firebase.auth.currentUser
        val date = Date()

        val exercise = Exercise(
            user?.displayName ?: "",
            user?.uid ?: "",
            route,
            distance,
            time,
            date,
            steps,
            highestSpeed,
            averageSpeed
        )

        database.collection("exercises")
            .add(exercise)
            .addOnSuccessListener {

                if (user?.uid != null) {
                    database.collection("users")
                        .document(user.uid)
                        .collection("exercises")
                        .add(exercise)
                }

                Toast.makeText(
                    this,
                    getString(R.string.success),
                    Toast.LENGTH_LONG)
                    .show()
                val mainIntent = Intent(this, MainActivity::class.java)
                startActivity(mainIntent)
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    getString(R.string.failure, it.localizedMessage),
                    Toast.LENGTH_LONG)
                    .show()
            }
    }
}