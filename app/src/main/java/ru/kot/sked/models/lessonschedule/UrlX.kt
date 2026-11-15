package ru.kot.sked.models.lessonschedule


import com.google.gson.annotations.SerializedName

data class UrlX(
    @SerializedName("type")
    val type: String,
    @SerializedName("url")
    val url: String
)