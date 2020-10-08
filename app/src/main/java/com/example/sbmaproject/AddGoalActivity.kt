package com.example.sbmaproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.sbmaproject.classes.Goal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_add_goal.*
import java.util.*

//THIS CLASS LOOKS LIKE SHIT TOO
class AddGoalActivity : AppCompatActivity() {

    var fbAuth = FirebaseAuth.getInstance()
    private val database = Firebase.firestore
    var distanceDouble: Double = 0.00
    lateinit var distance: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_goal)

        postGoalButton.setOnClickListener {
            postGoalToFirebase()
        }
    }

    private fun postGoalToFirebase() {

        val currentUser = fbAuth.currentUser?.uid
        val user = Firebase.auth.currentUser
        val goalCollection = database.collection("goals")

        var distanceFromKm = distancekmFromInput.text
        var distanceFromM = distanceMFromInput.text
        var distanceKm = distanceFromKm.toString()
        var DistanceKMInt = distanceKm.toDouble()
        var distanceM = distanceFromM.toString()
        var DistanceMInt = distanceM.toDouble() * 0.001
        var finalDistance = DistanceKMInt + DistanceMInt

        distance = finalDistance.toString()
        val date = Date()

        val goal = Goal(
            user?.displayName ?: "",
            user?.uid ?: "",
            distance,
        )

        database.collection("goals")
            .add(goal)
            .addOnSuccessListener {

                if (user?.uid != null) {
                    database.collection("users")
                        .document(user.uid)
                        .collection("goals")
                        .get()
                        .addOnSuccessListener {

                            for (document in it.documents) {
                                var ref = document.reference
                                ref.delete()
                            }

                            database.collection("users")
                                .document(user.uid)
                                .collection("goals")


                            database.collection("users")
                                .document(user.uid)
                                .collection("goals")
                                .add(goal)
                            Toast.makeText(
                                this,
                                getString(R.string.success),
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                getString(R.string.failure, it.localizedMessage),
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    val mainIntent = Intent(this, MainActivity::class.java)
                    startActivity(mainIntent)
                }
            }
    }
}
