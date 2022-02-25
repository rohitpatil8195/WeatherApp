package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
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
import android.widget.Toast
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener.Builder.withContext
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
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
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)  || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        val locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
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
            getLocationWeatherDetails()

        }
    }


    private fun getLocationWeatherDetails(){
        if(Constents.isNetworkAvailable(this)){
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

}