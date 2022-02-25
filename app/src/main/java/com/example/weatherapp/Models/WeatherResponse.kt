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


