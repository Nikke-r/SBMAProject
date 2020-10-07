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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
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
            usernamefield.text = ("Hello, " + fbAuth.currentUser?.email ?: "No username")


        fetchExerciseData()

        val logOutButton1 = view.logOutBtn

        logOutButton1.setOnClickListener {
            showMessage(view, "Logging Out...")
            signOut()
            val intent = Intent (getActivity(), LoginActivity::class.java)
            getActivity()?.startActivity(intent)
        }

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
        val currentUser = Firebase.auth.currentUser

        if (currentUser != null) {
            database.collection("users")
                .document(currentUser.uid)
                .collection("exercises")
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

    fun signOut(){
        fbAuth.signOut()
    }

    fun showMessage(view: View, message: String){
        Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).setAction("Action", null).show()
    }
}