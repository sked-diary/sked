package ru.kot.sked.models.marklistdate


import com.google.gson.annotations.SerializedName

data class MarkListDate(
    @SerializedName("payload")
    val payload: List<Mark>
)