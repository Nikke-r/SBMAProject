package com.example.sbmaproject.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.example.sbmaproject.LoginActivity
import com.example.sbmaproject.MainActivity
import com.example.sbmaproject.R
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*

class HomeFragment : Fragment() {
    var fbAuth = FirebaseAuth.getInstance()

    @ExperimentalStdlibApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val logOutButton1 = view.btnLogout

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

    fun signOut(){
        fbAuth.signOut()
    }

    fun showMessage(view: View, message: String){
        Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).setAction("Action", null).show()
    }
}