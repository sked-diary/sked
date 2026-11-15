package ru.kot.sked.models.marklistsubjectshort


import com.google.gson.annotations.SerializedName

data class Criteria(
    @SerializedName("name")
    val name: Any?,
    @SerializedName("value")
    val value: String
)