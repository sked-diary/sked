package ru.kot.sked.network.interfaces

import okhttp3.ResponseBody
import ru.kot.sked.Diary
import ru.kot.sked.models.auth.RegionalCredentialsResponse
import ru.kot.sked.network.NetworkService.BaseUrl
import ru.kot.sked.network.RegionalOnly
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Regional credentials authorization API.
 *
 * baseUrl - [BaseUrl.MOSREG_SECONDARY]
 */
interface RegionalAuthAPI {
    companion object : BaseUrls() {
        override fun getBaseUrl(diary: Diary): String {
            return when (diary) {
                Diary.MES -> throw NoSuchFieldError()
                Diary.MySchool -> BaseUrl.MOSREG_SECONDARY
            }
        }
    }

    @RegionalOnly
    @POST("lms/api/sessions")
    fun enterCredentials(
        @Body body: RegionalCredentialsResponse.Body
    ): Call<RegionalCredentialsResponse>

    @RegionalOnly
    @GET("v3/auth/kauth/callback")
    fun exchangeToken(
        @Query("code") code: String
    ): Call<ResponseBody>
}