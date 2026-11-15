package dev.diskettefox.madridtransport.models

import com.google.gson.annotations.SerializedName

data class StopModel(
    @SerializedName("code")
    val code: String = "",
    @SerializedName("data")
    val stopsData: ArrayList<Data> = ArrayList()
) {
    data class Data(
        @SerializedName("stops")
        val stops: List<Stop> = emptyList()
    )

    data class Stop(
        @SerializedName("stop")
        val stopId: String = "",
        @SerializedName("name")
        val name: String = "",
        @SerializedName("dataLine")
        val dataLine: List<Dataline> = emptyList(),
        var favorite: Boolean = false
    ) {
        fun isFavorite(): Boolean = favorite
    }

    data class Dataline(
        @SerializedName("line")
        val lineId: String = "",
        @SerializedName("label")
        val label: String = "",
        @SerializedName("direction")
        val direction: String = "",
        @SerializedName("headerA")
        val headerA: String = "",
        @SerializedName("headerB")
        val headerB: String = "",
        var timeArriving: String = "",
        var timeNext: String = ""
    )
}
