package dev.diskettefox.madridtransport.models

import com.google.gson.annotations.SerializedName

data class HelloModel(
    @SerializedName("code")
    val code: String = ""
)
