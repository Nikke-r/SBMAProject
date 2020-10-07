package com.example.sbmaproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.example.sbmaproject.ui.ExerciseActivity
import com.example.sbmaproject.ui.FeedFragment
import com.example.sbmaproject.ui.HomeFragment
import com.example.sbmaproject.ui.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    //Declare fragment variables
    private lateinit var homeFragment: HomeFragment
    private lateinit var feedFragment: FeedFragment
    private lateinit var profileFragment: ProfileFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var fragmentTransaction: FragmentTransaction

    //Declare variable that handles the bottom navigation menu fragment transactions
    private val onNavigationItemSelected =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when(item.itemId) {

                R.id.feed -> {
                    changeFragment(feedFragment)
                    true
                }

                R.id.home -> {
                    changeFragment(homeFragment)
                    true
                }

                R.id.profile -> {
                    changeFragment(profileFragment)
                    true
                }

                else -> false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var fbAuth = FirebaseAuth.getInstance()

        //Setup fragments
        homeFragment = HomeFragment()
        feedFragment = FeedFragment()
        profileFragment = ProfileFragment()

        if(Firebase.auth.currentUser == null){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {

            //First show the Home Fragment
            fragmentManager = supportFragmentManager
            fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.add(R.id.main_container, homeFragment)
            fragmentTransaction.commit()

            //Set the home item selected and add a listener for bottom navigation bar
            bottom_navigation_view.selectedItemId = R.id.home
            bottom_navigation_view.setOnNavigationItemSelectedListener(onNavigationItemSelected)
        }

        //Set the home item selected and add a listener for bottom navigation bar
        bottom_navigation_view.selectedItemId = R.id.home
        bottom_navigation_view.setOnNavigationItemSelectedListener(onNavigationItemSelected)

        newExerciseBtn.setOnClickListener {
            val exerciseIntent = Intent(this, ExerciseActivity::class.java)
            startActivity(exerciseIntent)
        }
    }

    //Private function that handles the fragment transaction
    private fun changeFragment(fragment: Fragment) {
        fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.main_container, fragment)
        fragmentTransaction.commit()
    }

}