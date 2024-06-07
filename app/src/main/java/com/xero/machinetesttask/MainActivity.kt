package com.xero.machinetesttask

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if the user is already logged in
        if (auth.currentUser != null) {
            // User is already logged in, start HomeActivity
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // User is not logged in, start PhoneNumberActivity
            startActivity(Intent(this, PhoneNumberActivity::class.java))
        }

        // Finish MainActivity to prevent going back to it
        finish()
    }
}
