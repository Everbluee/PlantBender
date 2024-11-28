package com.example.plantbender

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("HttpTrigger1?code=GLB6SjJCG0G2-q2DCgRl5uBmP2rN0IjNR8ifvFinS67XAzFuea78Mg%3D%3D")
    fun getData(): Call<List<GroundHumidity>>

    @POST("HttpTrigger2?code=CXZODUDTebCvcJi5HVrj37k9XjZHueBq9RJ3lncGCMPPAzFua_M9sA%3D%3D")
    fun sendActivation(@Body value: String): Call<Void>
}