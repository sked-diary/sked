package ru.kot.sked.models.marklistsubject


import com.google.gson.annotations.SerializedName

data class MarkListSubject(
    @SerializedName("payload")
    val payload: List<MarkListSubjectItem>
)