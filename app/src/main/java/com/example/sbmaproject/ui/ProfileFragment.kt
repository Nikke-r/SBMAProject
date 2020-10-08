package com.example.sbmaproject.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sbmaproject.LoginActivity
import com.example.sbmaproject.R
import com.example.sbmaproject.classes.Exercise
import com.example.sbmaproject.classes.ExerciseRecyclerViewAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.view.*

class ProfileFragment : Fragment() {

    var fbAuth = FirebaseAuth.getInstance()
    private val database = Firebase.firestore
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var exerciseViewAdapter: ExerciseRecyclerViewAdapter
    private val exerciseList: MutableList<Exercise>? = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_profile, container, false)

            val usernamefield = view.usernameTextfield
            usernamefield.text = fbAuth.currentUser?.email ?: "No username"

        fetchExerciseData()

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        layoutManager = LinearLayoutManager(context)
        exerciseViewAdapter = ExerciseRecyclerViewAdapter(exerciseList)

        exerciseRecyclerView.layoutManager = layoutManager
        exerciseRecyclerView.adapter = exerciseViewAdapter
        exerciseRecyclerView.addOnScrollListener(RecyclerViewScrollListener())
    }

    private fun fetchExerciseData() {
        val currentUser = Firebase.auth.currentUser

        if (currentUser != null) {
            database.collection("users")
                .document(currentUser.uid)
                .collection("exercises")
                .get()
                .addOnSuccessListener {
                    for (document in it.documents) {
                        val exercise = document.toObject<Exercise>()
                        exerciseList?.add(exercise!!)

                        val decodedRoute = PolylineUtils.decode(exercise!!.route, 5)
                        Log.i("DBG", "Decoded route: $decodedRoute")
                    }

                    exerciseViewAdapter.notifyDataSetChanged()
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

    private inner class RecyclerViewScrollListener: RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            Log.i("DBG", "Y: $dy")
        }
    }
}