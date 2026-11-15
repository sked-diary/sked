package ru.kot.sked.models.events


import com.google.gson.annotations.SerializedName

data class Criteria(
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val value: String
)