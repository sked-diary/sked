package ru.kot.sked.models.profilesid


import com.google.gson.annotations.SerializedName

data class ProfileId(
    @SerializedName("id")
    val id: Long,
    @SerializedName("type")
    val type: String
)