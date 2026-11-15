package dev.diskettefox.madridtransport.models

import com.google.gson.annotations.SerializedName

data class LineModel(
    @SerializedName("data")
    val data: ArrayList<Data> = ArrayList()
) {
    data class Data(
        @SerializedName("line")
        var lineId: Int = 0,
        @SerializedName("nameA")
        var nameA: String = "",
        @SerializedName("nameB")
        var nameB: String = "",
        @SerializedName("label")
        var label: String = "",
        @SerializedName("color")
        var color: String = ""
    )
}
