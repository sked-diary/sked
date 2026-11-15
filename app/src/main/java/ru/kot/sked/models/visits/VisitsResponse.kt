package ru.kot.sked.models.visits


import com.google.gson.annotations.SerializedName

data class VisitsResponse(
    @SerializedName("payload")
    val payload: List<Payload>
)