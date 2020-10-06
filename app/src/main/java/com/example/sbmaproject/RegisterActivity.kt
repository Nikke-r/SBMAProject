package com.example.sbmaproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.sbmaproject.classes.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val database = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        registerBtn.setOnClickListener {
            registerUser(
                emailEditText.text.toString(),
                displayNameEditText.text.toString(),
                passwordEditText.text.toString()
            )
        }
    }

    //Check that the necessary fields are not empty
    private fun validateUsername() : Boolean {
        val username = displayNameEditText.text.toString()

        if (username.isEmpty()) {
            displayNameEditText.error = getString(R.string.cant_be_empty)
            return false
        } else {
            displayNameEditText.error = null
        }

        return true
    }

    private fun validateEmail() : Boolean {
        val email = emailEditText.text.toString()

        if (email.isEmpty()) {
            emailEditText.error = getString(R.string.cant_be_empty)
            return false
        } else {
            emailEditText.error = null
        }

        return true
    }

    private fun validatePassword() : Boolean {
        val password = passwordEditText.text.toString()
        val passwordAgain = passwordAgainEditText.text.toString()

        if (password.isEmpty()) {
            passwordEditText.error = getString(R.string.cant_be_empty)
            return false
        } else {
            passwordEditText.error = null
        }

        if (passwordAgain.isEmpty()) {
            passwordAgainEditText.error = getString(R.string.cant_be_empty)
            return false
        } else {
            passwordAgainEditText.error = null
        }

        if (password != passwordAgain) {
            passwordAgainEditText.error = getString(R.string.password_mismatch)
            return false
        } else {
            passwordAgainEditText.error = null
        }

        return true
    }

    //Function that handles the registering progress to firebase
    private fun registerUser(email: String, username: String, password: String) {

        if (!validateUsername() || !validateEmail() || !validatePassword()) {
            return
        }

        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    showSnackBar(getString(R.string.register_successful))
                    logUserIn(email, password, username)
                }
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
            }
    }

    //Log the user in automatically and update the userdata to firebase (username)
    private fun logUserIn(email: String, password: String, username: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {

                val user = auth.currentUser
                val date = Date()

                if (user != null) {

                    val newUser = User(user.uid, username, date)

                    user.updateProfile(userProfileChangeRequest {
                        displayName = username
                    })

                    database.collection("users")
                        .document(user.uid)
                        .set(newUser)
                        .addOnCompleteListener {
                            val mainActivity = Intent(this, MainActivity::class.java)
                            startActivity(mainActivity)
                            finish()
                        }
                }
            }
    }

    private fun showSnackBar(message: String) {
        Snackbar.make(snackBar, message, Snackbar.LENGTH_LONG).show()
    }

    //Hide other elements and show a progress bar when the registering is in progress
    private fun showLoading(visible: Boolean) {
        if (visible) {
            showSnackBar(getString(R.string.register_in_progress))

            registerTitle.visibility = View.GONE
            usernameLayout.visibility = View.GONE
            emailLayout.visibility = View.GONE
            passwordLayout.visibility = View.GONE
            passwordAgainLayout.visibility = View.GONE
            registerBtn.visibility = View.GONE
            backBtn.visibility = View.GONE

            progressBar.visibility = View.VISIBLE
        } else {
            registerTitle.visibility = View.VISIBLE
            usernameLayout.visibility = View.VISIBLE
            emailLayout.visibility = View.VISIBLE
            passwordLayout.visibility = View.VISIBLE
            passwordAgainLayout.visibility = View.VISIBLE
            registerBtn.visibility = View.VISIBLE
            backBtn.visibility = View.VISIBLE

            progressBar.visibility = View.GONE
        }
    }
}