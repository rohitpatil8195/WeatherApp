package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.weatherapp.Models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener.Builder.withContext
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog:Dialog ?=null
    private lateinit var mSharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

         mSharedPreferences =getSharedPreferences(Constents.PREFERENCE_NAME,Context.MODE_PRIVATE)

        setupUI()

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
        val locationRequest = LocationRequest()
          //  interval = 10000
            //fastestInterval = 5000
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY


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
                        val weatherResponseJsonString =Gson().toJson(weatherList)
                         val editor =mSharedPreferences.edit()
                         editor.putString(Constents.WEATHER_RESPONSE_DATA,weatherResponseJsonString)
                         editor.apply()

                         setupUI()
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


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
      return when(item.itemId) {
          R.id.action_refresh ->{
              requestNewLocationData()
              true
          }
          else -> {
              super.onOptionsItemSelected(item)
          }
      }
    }



    private fun setupUI(){
      val weatherResponseJsonString =mSharedPreferences.getString(Constents.WEATHER_RESPONSE_DATA,"")

        if(!weatherResponseJsonString.isNullOrEmpty()){

            val WeatherList =Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)

            for(i in WeatherList.weather.indices){
                Log.i("wather name","${WeatherList.weather.toString()}")
                val tv_main =findViewById<TextView>(R.id.tv_main)
                val tv_main_description =findViewById<TextView>(R.id.tv_main_description)
                val tv_temp = findViewById<TextView>(R.id.tv_temp)
                tv_main.text =WeatherList.weather[i].main
                tv_main_description.text =WeatherList.weather[i].description
                tv_temp.text =WeatherList.main.temp.toString() + getUnit(application.resources.configuration.toString())

                val tv_sunrise_time = findViewById<TextView>(R.id.tv_sunrise_time)
                tv_sunrise_time.text = unixTime(WeatherList.sys.sunrise)

                val tv_sunset_time = findViewById<TextView>(R.id.tv_sunset_time)
                tv_sunset_time.text = unixTime(WeatherList.sys.sunset)

                val tv_humidity = findViewById<TextView>(R.id.tv_humidity)
                tv_humidity.text = WeatherList.main.humidity.toString()+ " per cent"

                val tv_min = findViewById<TextView>(R.id.tv_min)
                tv_min.text = WeatherList.main.temp_min.toString()+ " min"

                val tv_max = findViewById<TextView>(R.id.tv_max)
                tv_max.text = WeatherList.main.temp_max.toString() + " max"

                val tv_speed = findViewById<TextView>(R.id.tv_speed)
                tv_speed.text = WeatherList.wind.speed.toString()

                val tv_name = findViewById<TextView>(R.id.tv_name)
                tv_name.text = WeatherList.name

                val tv_country = findViewById<TextView>(R.id.tv_country)
                tv_country.text =WeatherList.sys.country


                val iv_main = findViewById<ImageView>(R.id.iv_main)
                when(WeatherList.weather[i].icon){
                    "01d" ->iv_main.setImageResource(R.drawable.sunny)
                    "02d" ->iv_main.setImageResource(R.drawable.cloud)
                    "03d" ->iv_main.setImageResource(R.drawable.cloud)
                    "04d" ->iv_main.setImageResource(R.drawable.cloud)

                    "04n" ->iv_main.setImageResource(R.drawable.cloud)
                    "10d" ->iv_main.setImageResource(R.drawable.rain)
                    "11d" ->iv_main.setImageResource(R.drawable.storm)
                    "13d" ->iv_main.setImageResource(R.drawable.snowflake)

                    "01n" ->iv_main.setImageResource(R.drawable.cloud)
                    "02n" ->iv_main.setImageResource(R.drawable.cloud)
                    "03n" ->iv_main.setImageResource(R.drawable.cloud)
                    "10n" ->iv_main.setImageResource(R.drawable.cloud)
                    "11n" ->iv_main.setImageResource(R.drawable.rain)
                    "13n" ->iv_main.setImageResource(R.drawable.snowflake)

                }

            }
        }

    }

    private fun getUnit(value: String): String? {
         var value ="°C"
        if("US" ==value || "LR" ==value || "MM"==value){
            value ="°F"
        }
        return value
    }

    private fun unixTime(timex:Long) : String ?{
      val date = Date(timex)
        val sdf = SimpleDateFormat("HH:mm",Locale.US)
        sdf.timeZone= TimeZone.getDefault()
        return sdf.format(date)
    }

}