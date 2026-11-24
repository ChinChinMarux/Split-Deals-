package com.example.splitthedeals.data.remote

import com.example.splitthedeals.domain.BillInput
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("bills")
    suspend fun getBills(): List<BillInput>

    @GET("bills/{id}")
    suspend fun getBill(@Path("id") id: String): BillInput

    @POST("bills")
    suspend fun saveBill(@Body bill: BillInput): BillInput

    @PUT("bills/{id}")
    suspend fun updateBill(@Path("id") id: String, @Body bill: BillInput): BillInput

    @PATCH("bills/{id}/status")
    suspend fun updateStatus(@Path("id") id: String, @Body body: Map<String, String>): BillInput

    @PATCH("bills/{id}/members/{memberId}/paid")
    suspend fun updatePaid(
        @Path("id") id: String,
        @Path("memberId") memberId: String,
        @Body body: Map<String, Boolean>,
    ): BillInput

    @DELETE("bills/{id}")
    suspend fun deleteBill(@Path("id") id: String)
}
