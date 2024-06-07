package com.xero.machinetesttask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import java.util.regex.Pattern

class OtpBroadcastReceiver : BroadcastReceiver() {

    private var otpReceiverListener: OTPReceiveListener? = null

    fun initOTPListener(listener: OTPReceiveListener) {
        this.otpReceiverListener = listener
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            val extras: Bundle? = intent.extras
            val status: Status? = extras?.get(SmsRetriever.EXTRA_STATUS) as Status?

            when (status?.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    val message: String? = extras?.get(SmsRetriever.EXTRA_SMS_MESSAGE) as String?
                    otpReceiverListener?.onOTPReceived(message)
                }
                CommonStatusCodes.TIMEOUT -> {
                    otpReceiverListener?.onOTPTimeOut()
                }
            }
        }
    }

    interface OTPReceiveListener {
        fun onOTPReceived(otp: String?)
        fun onOTPTimeOut()
    }
}
