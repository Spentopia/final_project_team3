package com.ict.spentopia.feature.auth.connector

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64

class PhantomDeepLinkConnector(
    private val context: Context
) {

    private val redirectLink = "spentopia://wallet-callback"
    private val phantomPackageName = "app.phantom.mobile"

    fun connect(): Boolean {
        val uri = Uri.parse("https://phantom.app/ul/v1/connect")
            .buildUpon()
            .appendQueryParameter("app_url", "https://spentopia.com")
            .appendQueryParameter("cluster", "devnet")
            .appendQueryParameter("redirect_link", redirectLink)
            .build()

        return openPhantom(uri)
    }

    fun signMessage(message: String): Boolean {
        val encodedMessage = Base64.encodeToString(
            message.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )

        val uri = Uri.parse("https://phantom.app/ul/v1/signMessage")
            .buildUpon()
            .appendQueryParameter("app_url", "https://spentopia.com")
            .appendQueryParameter("cluster", "devnet")
            .appendQueryParameter("redirect_link", redirectLink)
            .appendQueryParameter("message", encodedMessage)
            .build()

        return openPhantom(uri)
    }

    private fun openPhantom(uri: Uri): Boolean {
        val phantomIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(phantomPackageName)
        }
        val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)

        return try {
            context.startActivity(phantomIntent)
            true
        } catch (_: Exception) {
            try {
                context.startActivity(fallbackIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun isConnectCallback(uri: Uri): Boolean {
        return uri.getQueryParameter("public_key") != null
    }

    fun isSignCallback(uri: Uri): Boolean {
        return uri.getQueryParameter("signature") != null
    }

    fun parseConnectCallback(uri: Uri): String? {
        return uri.getQueryParameter("public_key")
    }

    fun parseSignCallback(uri: Uri): String? {
        return uri.getQueryParameter("signature")
    }
}
