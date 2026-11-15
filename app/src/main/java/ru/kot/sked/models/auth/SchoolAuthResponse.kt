package ru.kot.sked.models.auth


import com.google.gson.annotations.SerializedName

data class SchoolAuthResponse(
    @SerializedName("user_authentication_for_mobile_response")
    val userAuthenticationForMobileResponse: UserAuthenticationForMobileResponse
)