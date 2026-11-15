package dev.diskettefox.madridtransport.api

import com.google.gson.annotations.SerializedName

data class TimeRequest(
    @SerializedName("cultureInfo")
    val cultureInfo: String = "ES",

    @SerializedName("Text_StopRequired_YN")
    val textStopRequiredYN: String = "Y",

    @SerializedName("Text_EstimationsRequired_YN")
    val textEstimationsRequiredYN: String = "Y",

    @SerializedName("Text_IncidencesRequired_YN")
    val textIncidencesRequiredYN: String = "N",

    @SerializedName("DateTime_Referenced_Incidencies_YYYYMMDD")
    val dateTimeReferencedIncidenciesYYYYMMDD: String = "20260219"
) {
    companion object {
        fun get(): TimeRequest = TimeRequest()
    }
}
