package ru.kot.sked.models.homeworks


import com.google.gson.annotations.SerializedName

data class HomeworksResponse(
    @SerializedName("payload")
    val payload: List<Homework>
)