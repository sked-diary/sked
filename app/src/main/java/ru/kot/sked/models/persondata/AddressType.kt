package ru.kot.sked.models.persondata


import com.google.gson.annotations.SerializedName

data class AddressType(
    @SerializedName("actual_from")
    val actualFrom: String,
    @SerializedName("actual_to")
    val actualTo: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)