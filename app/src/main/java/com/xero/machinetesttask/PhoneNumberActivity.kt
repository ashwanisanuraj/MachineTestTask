package com.xero.machinetesttask

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class PhoneNumberActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var editTextPhoneNumber: EditText
    private lateinit var buttonRequestOtp: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var hideButton: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_number)

        auth = FirebaseAuth.getInstance()
        hideButton = findViewById(R.id.hidethis)
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber)
        buttonRequestOtp = findViewById(R.id.buttonRequestOTP)
        progressBar = findViewById(R.id.progressBar)

        hideButton.isEnabled = false

        editTextPhoneNumber.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(10))

        val textColorHex = "#FF3F3C3C"
        val textColor = Color.parseColor(textColorHex)

        hideButton.setTextColor(textColor)

        editTextPhoneNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s?.length ?: 0 > 10) {
                    s?.delete(10, s.length)
                }
            }
        })

        buttonRequestOtp.setOnClickListener {
            val phoneNumber = editTextPhoneNumber.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phoneNumber.length != 10) {
                Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fullPhoneNumber = "+91$phoneNumber" // Assuming Indian numbers

            buttonRequestOtp.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE

            requestOTP(fullPhoneNumber)
        }
    }

    fun requestOTP(fullPhoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // No action needed here, as we're moving to OtpVerificationActivity
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    val errorMessage = when (e) {
                        is FirebaseAuthInvalidCredentialsException -> "Invalid request: ${e.message}"
                        is FirebaseTooManyRequestsException -> "SMS quota exceeded: ${e.message}"
                        is FirebaseAuthMissingActivityForRecaptchaException -> "reCAPTCHA verification attempt failed: ${e.message}"
                        else -> "Verification failed: ${e.message}"
                    }
                    Toast.makeText(this@PhoneNumberActivity, errorMessage, Toast.LENGTH_SHORT).show()

                    progressBar.visibility = View.INVISIBLE
                    buttonRequestOtp.visibility = View.VISIBLE
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    super.onCodeSent(verificationId, token)
                    val intent = Intent(this@PhoneNumberActivity, OtpVerificationActivity::class.java)
                    intent.putExtra("verificationId", verificationId)
                    intent.putExtra("phoneNumber", fullPhoneNumber) // Pass fullPhoneNumber to OtpVerificationActivity
                    startActivity(intent)
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}
