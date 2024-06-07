package com.xero.machinetesttask

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var inputCode: EditText
    private lateinit var verificationId: String
    private lateinit var textViewResendOtp: TextView
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var buttonVerifyOtp: Button
    private lateinit var progressBar: ProgressBar

    private var resendCountdown: Long = 60000
    private val countdownInterval: Long = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        auth = FirebaseAuth.getInstance()
        inputCode = findViewById(R.id.inputCode)
        textViewResendOtp = findViewById(R.id.textViewResendOtp)
        buttonVerifyOtp = findViewById(R.id.buttonVerifyOtp)
        progressBar = findViewById(R.id.progressVerifyOtp)

        verificationId = intent.getStringExtra("verificationId") ?: ""

        inputCode.requestFocus()

        startCountdown()

        textViewResendOtp.setOnClickListener {
            resendOtp()
        }

        buttonVerifyOtp.setOnClickListener {
            buttonVerifyOtp.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
            verifyOtp()
        }

        inputCode.filters = arrayOf(InputFilter.LengthFilter(6))

        inputCode.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData: ClipData? = clipboard.primaryClip
            clipData?.let {
                val otp = clipData.getItemAt(0).text.toString().trim()
                inputCode.setText(otp)
            }
            true
        }
    }
    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(resendCountdown, countdownInterval) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                val secondsUntilFinished = millisUntilFinished / 1000
                if (secondsUntilFinished == 0L) {
                    textViewResendOtp.visibility = View.VISIBLE
                    textViewResendOtp.text = "Resend OTP"
                    textViewResendOtp.isEnabled = true
                } else {
                    textViewResendOtp.text = "Please Wait $secondsUntilFinished Seconds."
                    textViewResendOtp.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    textViewResendOtp.isEnabled = false
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                textViewResendOtp.visibility = View.VISIBLE
                textViewResendOtp.text = "Resend OTP"
                textViewResendOtp.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                textViewResendOtp.isEnabled = true
            }
        }

        countDownTimer.start()
    }
    private fun resendOtp() {
        countDownTimer.cancel()
        val phoneNumber = intent.getStringExtra("phoneNumber")
        phoneNumber?.let {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(it)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
            startCountdown()
            Toast.makeText(this, "Resending OTP...", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Phone number not found.", Toast.LENGTH_SHORT).show()
        }
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Automatically verify and sign in the user
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(this@OtpVerificationActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            // Save verification ID and resending token
            this@OtpVerificationActivity.verificationId = verificationId
            Toast.makeText(this@OtpVerificationActivity, "OTP sent successfully.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun verifyOtp() {
        val otp = inputCode.text.toString().trim()

        if (otp.length == 6) {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            signInWithPhoneAuthCredential(credential)
        } else {
            Toast.makeText(this, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
        }
    }
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid
                    val phoneNumber = user?.phoneNumber
                    if (userId != null) {
                        saveUserLogin(userId, phoneNumber)
                    }
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun saveUserLogin(userId: String, phoneNumber: String?) {
        val userLoginRef = FirebaseDatabase.getInstance().getReference("user_logins")
            .child(userId)
        val uniqueKey = userLoginRef.push().key
        val timestamp = System.currentTimeMillis()
        val month = getMonth(timestamp)

        val userLoginMap = hashMapOf(
            "month" to month,
            "userId" to userId,
            "timestamp" to timestamp
        )
        userLoginRef.child("phoneNumber").setValue(phoneNumber)
        val uniqueKeyRef = userLoginRef.child(uniqueKey!!)
        uniqueKeyRef.setValue(userLoginMap)
            .addOnCompleteListener { loginTask ->
                if (loginTask.isSuccessful) {
                    Log.d(TAG, "User login event stored successfully")
                } else {
                    Log.w(TAG, "Failed to store user login event", loginTask.exception)
                }
            }
    }
    private fun getMonth(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.MONTH) + 1 // Months are 0-based, add 1 for readability
    }
    companion object {
        private const val TAG = "OtpVerificationActivity"
    }
    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Do you want to go back?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, _ ->
                System.exit(0)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }
}
