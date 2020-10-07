package com.example.sbmaproject.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.sbmaproject.AddGoalActivity
import com.example.sbmaproject.R
import com.example.sbmaproject.classes.Exercise
import com.example.sbmaproject.classes.Goal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.exercise_result_item.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlin.math.log
import kotlin.properties.Delegates


//This whole class looks currently horrible

class HomeFragment : Fragment() {
    var fbAuth = FirebaseAuth.getInstance()
    private val database = Firebase.firestore
    private val exerciseList: MutableList<Exercise>? = ArrayList()
    private var currentlyRunDistance: Double = 0.00
    private var currentGoalAsDouble: Double = 0.00
    val user = Firebase.auth.currentUser

    @ExperimentalStdlibApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        //fetch currently run distance and goal from fs
        fetchGoalData2()
        fetchCurrentExerciseDistance()
        compareResult()

        val setGoalButton = view.setGoal

        //open add goal activity
        setGoalButton.setOnClickListener {
            val intent = Intent(getActivity(), AddGoalActivity::class.java)
            getActivity()?.startActivity(intent)
        }

        return view

    }

    /* NOT USED FETCHES THE DATA DIFFERENTLY
    private fun fetchGoalData() {
        val currentUser = fbAuth.currentUser?.uid
        if (currentUser != null) {
            Log.d("TAGLOL", currentUser)
        }

        if (currentUser != null) {
            database.collection("goals")
                .whereEqualTo("uid", currentUser)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val goal = document.toObject<Goal>()
                        Log.d("TAGYOO", goal.distance)
                        if (goal != null) {
                            currentGoal.text = goal.distance
                        } else {
                            currentGoal.text = "Not goal set"
                        }
                    }
                }
        }
    } */

    private fun fetchGoalData2() {
        val currentUser = Firebase.auth.currentUser

        if (currentUser != null) {
            database.collection("users")
                .document(currentUser.uid)
                .collection("goals")
                .get()
                .addOnSuccessListener {

                    for (document in it.documents) {
                        val goal = document.toObject<Goal>()

                        //if user has goal set it to home
                        if (goal != null) {
                            Log.d("TAGYESS", goal.distance.toString())
                            currentGoalAsDouble = goal.distance.toDouble()
                            currentGoal.text = currentGoalAsDouble.toString()
                        }
                    }
            }
            .addOnFailureListener {
                Toast
                    .makeText(
                        context,
                        "Error loading exercises: ${it.localizedMessage}",
                        Toast.LENGTH_LONG
                    )
                    .show()
            }
        }
    }

    private fun fetchCurrentExerciseDistance() {
            val currentUser = Firebase.auth.currentUser

            if (currentUser != null) {
                database.collection("users")
                    .document(currentUser.uid)
                    .collection("exercises")
                    .get()
                    .addOnSuccessListener {

                    exerciseList?.clear()
                    currentlyRunDistance = 0.00

                    for (document in it.documents) {
                        val exercise = document.toObject<Exercise>()
                        exerciseList?.add(exercise!!)
                        //format the distances so that u r able to count as double
                        var thisDistance = exercise?.distance?.replace(" km", "")
                        var thisDistance2 = thisDistance?.replace(",", ".")
                        var thisDistanceDouble = thisDistance2?.toDouble()
                        if (thisDistanceDouble != null) {
                            currentlyRunDistance += thisDistanceDouble
                        }
                    }

                    currentlyRun.text = currentlyRunDistance.toString()

                        /* fuck this code here
                    if (currentlyRunDistance >= currentGoalAsDouble) {
                        //set text goal ready!
                        winText.text = "You achieved your goal!"
                        //delete old goal, the run distance is still shown after this..
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
                                }
                        }
                        currentlyRunDistance = 0.00
                        currentGoalAsDouble = 0.00
                    } */
                }
                .addOnFailureListener {
                    Toast
                        .makeText(
                            context,
                            "Error loading exercises: ${it.localizedMessage}",
                            Toast.LENGTH_LONG
                        )
                        .show()
                }
        }
    }

    private fun compareResult() {

    }
}