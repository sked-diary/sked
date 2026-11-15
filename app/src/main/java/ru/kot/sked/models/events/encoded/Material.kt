package ru.kot.sked.models.events.encoded


import com.google.gson.annotations.SerializedName

data class Material(
    @SerializedName("material_ids")
    val materialIds: List<String>,
    @SerializedName("materialObj")
    val materialObj: List<MaterialObj>
)