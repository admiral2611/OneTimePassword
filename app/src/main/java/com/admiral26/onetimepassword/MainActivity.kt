package com.admiral26.onetimepassword

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Credentials
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    private var etOtp: TextInputEditText? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etOtp = findViewById(R.id.input_text)
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                SmsRetriever.getClient(this).startSmsUserConsent(null)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
                    registerReceiver(
                        smsVerificationReceiver,
                        intentFilter,
                        SmsRetriever.SEND_PERMISSION,
                        null
                    )
                } else {
                    val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
                    registerReceiver(
                        smsVerificationReceiver,
                        intentFilter,
                        SmsRetriever.SEND_PERMISSION,
                        null,
                        RECEIVER_NOT_EXPORTED
                    )
                }
            }
        } catch (e: Exception) {
        }

    }

    private val smsVerificationReceiver = object : BroadcastReceiver() {
        @SuppressLint("UnsafeIntentLaunch")
        override fun onReceive(context: Context, intent: Intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
                val smsRetrieverStatus = intent.extras ?: return
                when (smsRetrieverStatus.getInt(SmsRetriever.EXTRA_STATUS)) {
                    CommonStatusCodes.SUCCESS -> {
                        val consentIntent =
                            intent.getParcelableSafe<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                        try {
                            launcher.launch(consentIntent)
                        } catch (e: ActivityNotFoundException) {
                            logcat("SMS CODE MESSAGE ON:: exception:: ${e.printStackTrace()}")
                        }
                    }

                    CommonStatusCodes.TIMEOUT -> {
                        logcat("SMS CODE MESSAGE ON::  TIMEOUT")
                    }
                }
            }
        }
    }
    val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                data?.let {
                    val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                    message?.let {
                        val pattern = Regex("\\d{5}")
                        val matchResult = pattern.find(message)
                        val fiveDigitNumber = matchResult?.value
                        fiveDigitNumber?.let { code ->
                            //logcat("onActivityResult: $code")
                            etOtp?.setText(code)
                        }
                    }
                }
            }
        }

    @Suppress("DEPRECATION")
    inline fun <reified T : Parcelable> Intent.getParcelableSafe(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("UNCHECKED_CAST")
            getParcelableExtra(key) as? T
        }
    }

}


