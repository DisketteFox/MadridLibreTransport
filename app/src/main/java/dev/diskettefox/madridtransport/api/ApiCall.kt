package dev.diskettefox.madridtransport.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiCall {
    var BASE_URL: String = "https://openapi.emtmadrid.es"
    var token: String = ""
        private set
    private var retrofit: Retrofit? = null

    fun callApi(): Retrofit {
        if (retrofit == null) {
            val interceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor { chain ->
                    val request: Request = chain.request().newBuilder().build()
                    chain.proceed(request)
                }
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    fun setToken(newToken: String) {
        token = newToken
    }
}
