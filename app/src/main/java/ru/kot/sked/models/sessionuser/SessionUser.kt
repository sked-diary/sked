package ru.kot.sked.models.sessionuser


import com.google.gson.annotations.SerializedName

data class SessionUser(val a: String) {
    data class Body(
        @SerializedName("auth_token")
        val accessToken: String,
    )
}