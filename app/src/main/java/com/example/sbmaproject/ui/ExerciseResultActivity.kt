package com.example.sbmaproject.ui

import android.content.Intent
import android.util.Log


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
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.mapbox.geojson.utils.PolylineUtils
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
    private var routePoints: MutableList<com.mapbox.geojson.Point> = ArrayList()
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

        initializeGraphViews()

        postGoalButton.setOnClickListener {
            postResultsToFirebase()
        }

        cancelBtn.setOnClickListener {
            val mainActivity = Intent(this, MainActivity::class.java)
            startActivity(mainActivity)
        }
    }

    private fun initializeGraphViews() {
        val altitudeLineGraph = LineGraphSeries<DataPoint>()
        val speedLineGraph = LineGraphSeries<DataPoint>()
        routePoints = PolylineUtils.decode(route, 5)

        Log.i("DBG", "routePoints: ${routePoints.size}, altitudes: ${altitudes.size}, speeds: ${speeds.size}")
        if (routePoints.size > 0) {
            for ((altitudeIndex, altitude) in altitudes.withIndex()) {
                //val length = TurfMeasurement.length(routePoints.take(altitudeIndex), "kilometers")
                val length = 0.0
                val dataPoint = DataPoint(length, altitude)
                altitudeLineGraph.appendData(dataPoint, false, 100000)
            }

            for ((speedIndex, speed) in speeds.withIndex()) {
                //val length = TurfMeasurement.length(routePoints.take(speedIndex), "kilometers")
                val length = 0.0
                val dataPoint = DataPoint(length, speed)
                speedLineGraph.appendData(dataPoint, false, 100000)
            }
        }

        altitudesGraphView.title = "Altitudes"
        altitudesGraphView.addSeries(altitudeLineGraph)

        speedsGraphView.title = "Speed"
        speedsGraphView.addSeries(speedLineGraph)
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