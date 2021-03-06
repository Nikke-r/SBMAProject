package com.example.sbmaproject.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.sbmaproject.AddGoalActivity
import com.example.sbmaproject.R
import com.example.sbmaproject.classes.Exercise
import com.example.sbmaproject.classes.Goal
import com.example.sbmaproject.classes.Prize
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import java.lang.String
import kotlin.properties.Delegates


//This whole class looks currently horrible

class HomeFragment : Fragment() {
    var fbAuth = FirebaseAuth.getInstance()
    private val database = Firebase.firestore
    private val exerciseList: MutableList<Exercise>? = ArrayList()
    private var currentlyRunDistance: Double = 0.00
    private var currentGoalAsDouble: Double = 0.00
    val user = Firebase.auth.currentUser
    var totalvalue: Double = 0.00
    private var prizeCountUser = 0

    @ExperimentalStdlibApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        Log.i("DBG", "onCreateView")

        //fetch currently run distance and goal from fs
        fetchGoalData2()
        fetchCurrentExerciseDistance()
        compareResult()
        //updateChart()

        val setGoalButton = view.setGoal

        //open add goal activity
        setGoalButton.setOnClickListener {
            val intent = Intent(getActivity(), AddGoalActivity::class.java)
            getActivity()?.startActivity(intent)
        }
        return view
    }

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

                        //if user has goal set it to home graph
                        if (goal != null) {
                            Log.d("TAGYESS", goal.distance.toString())
                            currentGoalAsDouble = goal.distance.toDouble()
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
                    //WTF ?? Fetch the goal info again to compare to exrcs.. im such a bad coder
                    database.collection("users")
                        .document(currentUser.uid)
                        .collection("goals")
                        .get()
                        .addOnSuccessListener {
                            for (document in it.documents) {
                                val goal = document.toObject<Goal>()

                                //if user has goal set it to home
                                if (goal != null) {
                                    Log.d("TAGYESS", goal.distance)
                                    currentGoalAsDouble = goal.distance.toDouble()
                                }
                            }
                            Log.d("TAGRUN", currentlyRunDistance.toString())
                            totalvalue = currentlyRunDistance / currentGoalAsDouble

                            Log.d("TAGFIN", currentGoalAsDouble.toString())

                            //No goal set condition
                            if (currentGoalAsDouble.toString() == "0.0") {
                                pietotal.text = "No goal set"
                                cheerText.text = ("Set a new goal to achieve prizes! You have completed " + currentlyRunDistance + " km.")
                                val pieChart: ProgressBar = stats_progressbar
                                val d = 0
                                val progress = (d * 100).toInt()
                                pieChart.progress = progress

                                //Goal achieved condition stuff
                            } else if (totalvalue >= 1) {
                                cheerText.text = "You have achieved your goal of " + currentGoalAsDouble.toString() + " km! Set a new higher one now!"

                                // Calculate the slice size and update the pie chart:
                                val pieChart: ProgressBar = stats_progressbar
                                val d = 100
                                val progress = (d * 100).toInt()
                                pieChart.progress = progress
                                //ADD A NEW PRIZE TO COLLECTION. OWN FUNCTION?
                                //Get current prize count
                                if (currentUser != null) {
                                    database.collection("users")
                                        .document(currentUser.uid)
                                        .collection("prizes")
                                        .get()
                                        .addOnSuccessListener {
                                            for (document in it.documents) {
                                                val prizeNow = document.toObject<Prize>()
                                                if (prizeNow != null) {
                                                    prizeCountUser = prizeNow.prizeCount.toInt()
                                                } else {
                                                    prizeCountUser = 0
                                                }
                                                Log.d("TAGPRI", prizeCountUser.toString())

                                                prizeCountUser = (prizeCountUser + 1)
                                                Log.d("TAGNPR", prizeCountUser.toString())

                                                val prize = Prize(
                                                    user?.displayName ?: "",
                                                    user?.uid ?: "",
                                                    prizeCountUser.toString(),
                                                )

                                                //Delete old prize info
                                                if (user?.uid != null) {
                                                    database.collection("users")
                                                        .document(user.uid)
                                                        .collection("prizes")
                                                        .get()
                                                        .addOnSuccessListener {
                                                            for (document in it.documents) {
                                                                var ref = document.reference
                                                                ref.delete()
                                                            }

                                                            database.collection("users")
                                                                .document(user.uid)
                                                                .collection("prizes")

                                                            //Add new prize info
                                                            database.collection("users")
                                                                .document(user.uid)
                                                                .collection("prizes")
                                                                .add(prize)
                                                                .addOnSuccessListener {
                                                                    Log.i("DBG", "SUCCESS: $prize")
                                                                }

                                                            //delete the goal after its done. currently not doing it only updates
                                                            //when user sets a new goal

                                val goal = Goal(
                                    user?.displayName ?: "",
                                    user?.uid ?: "",
                                    "0.0",
                                )

                                if (user != null) {
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
                                        }
                                }
                                                        }
                                                }
                                            }
                                        }}
                                } else {
                                    Log.d("TAGPIE", totalvalue.toString())
                                    pietotal.text =
                                        (currentlyRunDistance.toString() + "/" + currentGoalAsDouble.toString() + "km")

                                    var diff = currentGoalAsDouble - currentlyRunDistance
                                    cheerText.text =
                                        (diff.toString() + " km to go for current goal!")

                                    // Calculate the slice size and update the pie chart:
                                    val pieChart: ProgressBar = stats_progressbar
                                    val d = currentlyRunDistance / currentGoalAsDouble
                                    val progress = (d * 100).toInt()
                                    pieChart.progress = progress
                                }
                            }

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
                        }}}

        private fun compareResult() {

        }

        private fun updateChart() {
            // Update the text in a center of the chart:
            var totalvalue = (currentlyRunDistance.toString() + "/" + currentGoalAsDouble)

        }
}