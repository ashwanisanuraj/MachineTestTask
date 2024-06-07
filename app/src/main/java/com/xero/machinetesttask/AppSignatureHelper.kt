package com.xero.machinetesttask

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object AppSignatureHelper {
    fun getAppSignatures(context: Context): String? {
        try {
            val packageName = context.packageName
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            val signatures = packageInfo.signatures

            for (signature in signatures) {
                val messageDigest = MessageDigest.getInstance("SHA")
                messageDigest.update(signature.toByteArray())
                val hash = Base64.encodeToString(messageDigest.digest(), Base64.NO_WRAP)
                return hash.substring(0, 11)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return null
    }
}
