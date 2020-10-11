package com.example.sbmaproject.ui

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.activity_exercise_result.*
import java.util.*
import kotlin.collections.ArrayList

class ExerciseResultActivity : AppCompatActivity() {

    private val database = Firebase.firestore

    private var distance: String = "0.00"
    private var time: String = "00:00:00"
    private var steps: String = "0"
    private var highestSpeed: Double = 0.00
    private var averageSpeed: Double = 0.00
    private var route: String = ""
    private var altitudes: ArrayList<Double> = ArrayList()
    private var speeds: ArrayList<Double> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_result)

        val extras = intent.extras

        if (extras !== null) {
            route = extras.get("route") as String
            distance = extras.get("distance") as String
            time = extras.get("time") as String
            steps = extras.get("steps") as String
            highestSpeed = extras.getDouble("highestSpeed")
            averageSpeed = extras.getDouble("averageSpeed")
            speeds = extras.get("speeds") as ArrayList<Double>
            altitudes = extras.get("altitudes") as ArrayList<Double>
        }

        distanceResultLabel.text = distance
        timeResultLabel.text = time
        highestSpeedResultLabel.text = getString(R.string.speed_value, highestSpeed)
        stepsResultLabel.text = steps
        averageSpeedResultLabel.text = getString(R.string.speed_value, averageSpeed)

        postGoalButton.setOnClickListener {
            postResultsToFirebase()
        }

        cancelBtn.setOnClickListener {
            val mainActivity = Intent(this, MainActivity::class.java)
            startActivity(mainActivity)
        }
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