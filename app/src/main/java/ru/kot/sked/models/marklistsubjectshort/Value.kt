package ru.kot.sked.models.marklistsubjectshort


import com.google.gson.annotations.SerializedName
import ru.kot.sked.models.events.Grade

data class Value(
    @SerializedName("grade")
    val grade: Grade,
    @SerializedName("grade_system_id")
    val gradeSystemId: Any?,
    @SerializedName("grade_system_type")
    val gradeSystemType: String,
    @SerializedName("name")
    val name: Any?
)