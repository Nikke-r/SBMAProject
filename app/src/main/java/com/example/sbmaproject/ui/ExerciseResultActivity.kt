package com.example.sbmaproject.ui

import android.content.Intent
import android.graphics.Bitmap
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
import kotlinx.android.synthetic.main.activity_exercise_result.*
import java.util.*
import kotlin.math.roundToLong

class ExerciseResultActivity : AppCompatActivity() {

    private val database = Firebase.firestore

    private var distance: String = "0.00"
    private var time: String = "00:00:00"
    private var steps: String = "0"
    private var highestSpeed: Double = 0.00
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_result)

        val extras = intent.extras

        bitmap = extras?.get("mapBitmap") as Bitmap
        distance = extras.get("distance") as String
        time = extras.get("time") as String
        steps = extras.get("steps") as String
        highestSpeed = extras.get("highestSpeed") as Double

        runnedRoute.setImageBitmap(bitmap)

        distanceResultLabel.text = distance
        timeResultLabel.text = time
        highestSpeedResultLabel.text = String.format("%.2f", highestSpeed)
        stepsResultLabel.text = steps

        postToFbBtn.setOnClickListener {
            postResultsToFirebase()
        }
    }

    private fun postResultsToFirebase() {

        val uid = Firebase.auth.uid
        val date = Date()

        val exercise = Exercise(
            uid ?: "",
            distance,
            time,
            date,
            steps,
            highestSpeed
        )

        database.collection("exercises")
            .add(exercise)
            .addOnSuccessListener {
                Toast.makeText(this, "Posted!", Toast.LENGTH_LONG).show()
                val mainIntent = Intent(this, MainActivity::class.java)
                startActivity(mainIntent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}