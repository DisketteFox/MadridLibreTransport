package dev.diskettefox.madridtransport.models

import com.google.gson.annotations.SerializedName

data class StopsModel(
    @SerializedName("code")
    val code: String = "",
    @SerializedName("data")
    val stopsData: ArrayList<Stops> = ArrayList()
) {
    data class Stops(
        @SerializedName("node")
        val stopId: String = "",
        @SerializedName("name")
        val stopName: String = "",
        @SerializedName("geometry")
        val geometry: Geometry? = null
    )

    data class Geometry(
        @SerializedName("coordinates")
        val coordinates: List<Double> = emptyList()
    )
}
