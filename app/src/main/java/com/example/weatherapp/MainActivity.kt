package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.example.weatherapp.Models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener.Builder.withContext
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit.*

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog:Dialog ?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        if(!getLocation()){
            Toast.makeText(this@MainActivity,"Your location provider is turned off.Please Turn on",
                Toast.LENGTH_LONG).show()
            Log.e("off","turned off")
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{
            Dexter.withContext(this).withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()){
                        Log.e("On","turned On")
                        requestNewLocationData()
                        Toast.makeText(this@MainActivity,"Location Permission is granted",
                            Toast.LENGTH_LONG).show()
                    }
                    if(report.isAnyPermissionPermanentlyDenied){
                        Toast.makeText(this@MainActivity,"You have denied location Permission! Please enable it.",
                            Toast.LENGTH_LONG).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    showRationalDialogForPermission()
                }
            }).onSameThread().check()

        }
    }

    private fun getLocation():Boolean {
        //this provide access to the system location services.
        val locationManager: LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)  || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        val locationRequest = LocationRequest.create()?.apply {
            //interval = 10000
            //fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        }

        mFusedLocationClient.requestLocationUpdates(locationRequest,mLocationCallBack,
            Looper.myLooper())
    }


    private val mLocationCallBack =object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult?) {
            val mLastLocation: Location =locationResult!!.lastLocation
            val mLatitude = mLastLocation.latitude
            Log.i("currant location","$mLatitude")
          val mLongitude = mLastLocation.longitude
            Log.i("currant location","$mLongitude")
            getLocationWeatherDetails(mLatitude,mLongitude)

        }
    }


    private fun getLocationWeatherDetails(latitude:Double,longitude:Double){

        if(Constents.isNetworkAvailable(this)){
             val retrofit: Retrofit =Retrofit.Builder().baseUrl(Constents.BASE_URL).addConverterFactory(
                 GsonConverterFactory.create()).build()

              val service:WeatherService= retrofit.create(WeatherService::class.java)
            Log.e("BASE_URL>>","${Constents.BASE_URL}")
            val listCall:Call<WeatherResponse> = service.getWeather(
                latitude,longitude,Constents.METRIC_UNIT,Constents.APP_ID
            )
            showCustomProgressDialog()
             listCall.enqueue(object :Callback<WeatherResponse>{
                 override fun onResponse(
                     response: Response<WeatherResponse>?,
                     retrofit: Retrofit?
                 ) {
                     if(response!!.isSuccess){
                         hideProgressDialog()
                         val weatherList:WeatherResponse = response.body()
                         setupUI(weatherList)
                     }else{
                         val rc =response.code()
                         when(rc){
                             400->{
                                 Log.e("Error 400","bad request")
                                 hideProgressDialog()
                             }
                             404->{
                                 Log.e("Error 404","not found")
                             }
                             else ->{
                                 Log.e("Error","generic error")
                             }
                         }
                     }
                 }

                 override fun onFailure(t: Throwable?) {
                    Log.e("Errorrr","${t!!.message.toString()}")
                     hideProgressDialog()
                 }

             })


            Toast.makeText(this,"You have connected to internet! now you can make request",Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(this,"No Internet Connection",Toast.LENGTH_LONG).show()
        }
    }



    private fun showRationalDialogForPermission() {
        AlertDialog.Builder(this).setMessage("It looks like you have turned off permission required for this feature. It can be enabled under App settings").
        setPositiveButton("GO TO SETTINGS"){
                _,_ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package",packageName,null)
                intent.data = uri
                startActivity(intent)
            }catch (e: ActivityNotFoundException){
                e.printStackTrace()
            }
        }.setNegativeButton("Cancel"){
                dialog,_->
            dialog.dismiss()
        }.show()
    }

    private fun showCustomProgressDialog(){
        mProgressDialog= Dialog(this)

        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)

        mProgressDialog!!.show()
    }


    private fun hideProgressDialog(){

        if(mProgressDialog != null){
            mProgressDialog!!.dismiss()
        }

    }

    private fun setupUI(WeatherList:WeatherResponse){
         for(i in WeatherList.weather.indices){
              Log.i("wather name","${WeatherList.weather.toString()}")
             val tv_main =findViewById<TextView>(R.id.tv_main)
             val tv_main_description =findViewById<TextView>(R.id.tv_main_description)
             val tv_temp = findViewById<TextView>(R.id.tv_temp)
             tv_main.text =WeatherList.weather[i].main
             tv_main_description.text =WeatherList.weather[i].description
             tv_temp.text =WeatherList.main.temp.toString() + getUnit(application.resources.configuration.toString())
         }
    }

    private fun getUnit(value: String): String? {
         var value ="°C"
        if("US" ==value || "LR" ==value || "MM"==value){
            value ="°F"
        }
        return value
    }

}