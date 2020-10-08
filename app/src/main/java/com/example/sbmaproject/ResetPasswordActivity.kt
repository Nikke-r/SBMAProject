package com.example.sbmaproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_reset_password.*

class ResetPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        resetPasswordBtn.setOnClickListener {
            sendResetPasswordEmail()
        }
    }

    private fun sendResetPasswordEmail() {
        val email = emailEditText.text.toString()

        if (email.isNotEmpty()) {
            Firebase.auth.sendPasswordResetEmail(email)
                .addOnCompleteListener {
                    showSnackBar(snackBar, "Email sent! Please follow the instructions there")
                }
                .addOnFailureListener {
                    showSnackBar(snackBar, "Failure: ${it.localizedMessage}")
                }
        } else {
            showSnackBar(snackBar, "Please enter email address")
        }
    }

    private fun showSnackBar(view: View, message: String) {
        Snackbar
            .make(view, message, Snackbar.LENGTH_LONG)
            .show()
    }
}