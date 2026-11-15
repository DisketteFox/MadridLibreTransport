package dev.diskettefox.madridtransport.api

import dev.diskettefox.madridtransport.models.HelloModel
import dev.diskettefox.madridtransport.models.LineModel
import dev.diskettefox.madridtransport.models.StopModel
import dev.diskettefox.madridtransport.models.StopsModel
import dev.diskettefox.madridtransport.models.TimeModel
import dev.diskettefox.madridtransport.models.TokenModel
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiInterface {

    // Interface for ping
    @GET("/v1/hello")
    fun getHello(): Call<HelloModel>

    // Interface for retrieving the token
    @GET("/v3/mobilitylabs/user/login/")
    fun getToken(
        @Header("X-ClientId") clientId: String,
        @Header("passKey") passKey: String
    ): Call<TokenModel>

    @GET("/v3/mobilitylabs/user/login/")
    fun getTokenByUser(
        @Header("email") clientId: String,
        @Header("password") passKey: String
    ): Call<TokenModel>

    // Interfaces for stops
    @POST("/v1/transport/busemtmad/stops/list/")
    fun getStopsList(
        @Header("accessToken") accessToken: String
    ): Call<StopsModel>

    @GET("/v1/transport/busemtmad/stops/{stopId}/detail")
    fun getStop(
        @Path("stopId") stopId: Int,
        @Header("accessToken") accessToken: String
    ): Call<StopModel>

    @POST("/v2/transport/busemtmad/stops/{stopId}/arrives/{lineArrive}/")
    fun getTime(
        @Path("stopId") stopId: Int,
        @Path("lineArrive") lineId: Int,
        @Header("accessToken") accessToken: String,
        @Body body: TimeRequest
    ): Call<TimeModel>

    // Interfaces for lines
    @GET("/v2/transport/busemtmad/lines/info/{dateref}/")
    fun getLines(
        @Header("accessToken") accessToken: String
    ): Call<LineModel>

    @GET("/v1/transport/busemtmad/lines/{lineId}/info/20260218/")
    fun getLineDetail(
        @Path("lineId") lineId: Int,
        @Header("accessToken") accessToken: String
    ): Call<LineModel>
}
