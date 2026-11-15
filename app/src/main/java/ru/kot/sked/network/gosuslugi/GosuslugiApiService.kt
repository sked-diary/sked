package ru.kot.sked.network.gosuslugi

import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GosuslugiApiService {

    @POST("api/myschool/v1/datamart")
    @Headers(
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Accept: application/json, text/plain, */*",
        "Content-Type: application/json",
        "Referer: https://www.gosuslugi.ru/school/feed"
    )
    fun getDatamart(
        @Header("Cookie") cookie: String,
        @Body query: List<DatamartQuery>
    ): Call<List<DatamartSnapshot>>

    @GET("api/myschool/v2/auth/student")
    @Headers(
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Accept: application/json, text/plain, */*",
        "Referer: https://www.gosuslugi.ru/school/feed"
    )
    fun getStudentInfo(
        @Header("Cookie") cookie: String,
        @Query("role") role: String = "student"
    ): Call<List<GStudentInfo>>

    @GET("api/lk/v1/users/data")
    @Headers(
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Accept: application/json, text/plain, */*",
        "Referer: https://www.gosuslugi.ru/"
    )
    fun getUserData(
        @Header("Cookie") cookie: String
    ): Call<JsonObject>

    @GET("api/myschool/v2/customservices/{student_id}")
    @Headers(
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Accept: application/json, text/plain, */*",
        "Referer: https://www.gosuslugi.ru/school/feed"
    )
    fun getHomeworkStatus(
        @Header("Cookie") cookie: String,
        @Path("student_id") studentId: String,
        @Query("object") obj: String = "homework",
        @Query("intervalId") intervalIds: List<String>
    ): Call<CustomServicesHomeworkResponse>

    @POST("api/myschool/v1/homework")
    @Headers(
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Accept: application/json, text/plain, */*",
        "Content-Type: application/json",
        "Referer: https://www.gosuslugi.ru/school/feed"
    )
    fun setHomeworkDone(
        @Header("Cookie") cookie: String,
        @Body request: ToggleHomeworkRequest
    ): Call<ResponseBody>

    @PUT("api/myschool/v1/homework")
    @Headers(
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Accept: application/json, text/plain, */*",
        "Content-Type: application/json",
        "Referer: https://www.gosuslugi.ru/school/feed"
    )
    fun setHomeworkUndone(
        @Header("Cookie") cookie: String,
        @Body request: ToggleHomeworkRequest
    ): Call<ResponseBody>

    companion object {
        private const val BASE_URL = "https://www.gosuslugi.ru/"

        fun create(): GosuslugiApiService {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GosuslugiApiService::class.java)
        }
    }
}
