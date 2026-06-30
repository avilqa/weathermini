package com.example.weathermini.data.model

import com.google.gson.annotations.SerializedName

data class CitySearchResponse(
    val results: List<CityDto>? = emptyList()
)

data class CityDto(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("country") val country: String?,
    @SerializedName("admin1") val region: String?
)