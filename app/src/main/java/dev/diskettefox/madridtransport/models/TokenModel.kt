package dev.diskettefox.madridtransport.models

import com.google.gson.annotations.SerializedName

data class TokenModel(
    @SerializedName("data")
    val data: ArrayList<Data> = ArrayList(),
    @SerializedName("code")
    val code: Int = 0
) {
    data class Data(
        @SerializedName("accessToken")
        val accessToken: String = ""
    )
}
