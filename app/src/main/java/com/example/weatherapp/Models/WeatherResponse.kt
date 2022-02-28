package com.example.weatherapp.Models

import java.io.Serializable

data class WeatherResponse (
    val coord:Coord,
      val weather: List<Weather>,
      val main:Main,
      val visibility :Int,
      val wind: Wind,
      val cloud: Cloud,
       val dt: Int,
      val sys:Sys,
       val name:String,
      val cod:Int
    ): Serializable

//https://api.openweathermap.org/data/2.5/weather?q=London,uk&APPID=1120feb191caa933432854b82d3f503f

//https://api.openweathermap.org/data/2.5/weather?lat=34.30703&lon=19.99428&APPID=1120feb191caa933432854b82d3f503f