package com.example.sbmaproject

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity: AppCompatActivity() {

    var fbAuth = FirebaseAuth.getInstance()
    lateinit var usernameF: EditText
    lateinit var passwordF: EditText

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_login)
            title = "KotlinApp"

            //usernameF = findViewById(R.id.userNameField)
            //passwordF = findViewById(R.id.passwordField)
            var usernameFromInput = userNameField.text
            var passwordFromInput = passwordField.text
            var email = usernameFromInput
            var password = passwordFromInput

            var btnLogin = findViewById<Button>(R.id.btnLogin)
            btnLogin.setOnClickListener { view ->
                Log.d("logintag", email.toString())
                Log.d("logintag", password.toString())
                if (passwordFromInput != null) {
                    signIn(view, usernameFromInput.toString(), passwordFromInput)
                }

            }

            toRegisterBtn.setOnClickListener {
                val registerIntent = Intent(this, RegisterActivity::class.java)
                startActivity(registerIntent)
            }

            toResetPasswordBtn.setOnClickListener {
                val resetPwIntent = Intent(this, ResetPasswordActivity::class.java)
                startActivity(resetPwIntent)
            }
        }

    fun signIn(view: View, email: String, password: Editable){
        showMessage(view,getString(R.string.authenticating))

        fbAuth.signInWithEmailAndPassword(email, password.toString()).addOnCompleteListener(this, OnCompleteListener<AuthResult> { task ->
            if(task.isSuccessful){
                var intent = Intent(this, MainActivity::class.java)
                intent.putExtra("id", fbAuth.currentUser?.email)
                startActivity(intent)
                finish()
            }else{
                showMessage(view,"Error: ${task.exception?.message}")
            }
        })

    }

    fun showMessage(view:View, message: String){
        Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).setAction("Action", null).show()
    }
}