package ru.kot.sked.models.classranking


import com.google.gson.annotations.SerializedName

data class PreviousRank(
    @SerializedName("averageMarkFive")
    val averageMarkFive: Double,
    @SerializedName("rankPlace")
    val rankPlace: Int
)