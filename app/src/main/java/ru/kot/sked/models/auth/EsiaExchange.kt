package ru.kot.sked.models.auth


import com.google.gson.annotations.SerializedName

data class EsiaExchange(
    @SerializedName("token")
    val token: String
)