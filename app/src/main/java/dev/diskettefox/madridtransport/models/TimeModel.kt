package dev.diskettefox.madridtransport.models

import com.google.gson.annotations.SerializedName

data class TimeModel(
    @SerializedName("data")
    val data: ArrayList<Data> = ArrayList()
) {
    data class Data(
        @SerializedName("Arrive")
        val lines: List<Line> = emptyList()
    )

    data class Line(
        @SerializedName("estimateArrive")
        val time: String = "",
        @SerializedName("DistanceBus")
        val distance: String = ""
    )
}
