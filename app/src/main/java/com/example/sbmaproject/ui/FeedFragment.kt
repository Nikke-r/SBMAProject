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
import com.example.sbmaproject.LoginActivity
import com.example.sbmaproject.R
import com.example.sbmaproject.classes.Exercise
import com.example.sbmaproject.classes.ExerciseRecyclerViewAdapter
import com.example.sbmaproject.classes.Prize
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.view.*

class FeedFragment : Fragment() {
    var fbAuth = FirebaseAuth.getInstance()
    private val database = Firebase.firestore
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var exerciseViewAdapter: ExerciseRecyclerViewAdapter
    private val exerciseList: MutableList<Exercise>? = ArrayList()
    val currentUser = Firebase.auth.currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_feed, container, false)

        fetchExerciseData()

        fbAuth.addAuthStateListener {
            if (fbAuth.currentUser == null) {
                //ohjaa loginii
            }
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        layoutManager = LinearLayoutManager(context)
        exerciseViewAdapter = ExerciseRecyclerViewAdapter(exerciseList)

        exerciseRecyclerView.layoutManager = layoutManager
        exerciseRecyclerView.adapter = exerciseViewAdapter
    }

    private fun fetchExerciseData() {

            database.collection("exercises").orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener {

                    exerciseList?.clear()

                    for (document in it.documents) {
                        val exercise = document.toObject<Exercise>()
                        exerciseList?.add(exercise!!)
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