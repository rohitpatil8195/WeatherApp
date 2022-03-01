package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constents{


    const val APP_ID:String = "1120feb191caa933432854b82d3f503f"
    const val BASE_URL:String = "http://api.openweathermap.org/data/"
     const val METRIC_UNIT :String ="metric"
    const val PREFERENCE_NAME ="WeatherAppPreference"
    val WEATHER_RESPONSE_DATA ="weather_response_data"

    fun isNetworkAvailable(context: Context):Boolean{
           val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val network =connectivityManager.activeNetwork?:return false
              val activeNetwork= connectivityManager.getNetworkCapabilities(network)?: return false
            return when{
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->true
                else ->false
            }
        }else{
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }

    }
}