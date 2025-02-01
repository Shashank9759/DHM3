package com.example.dhm20

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object Helpers {

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

    }
}
