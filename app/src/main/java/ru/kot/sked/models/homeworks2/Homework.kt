package ru.kot.sked.models.homeworks2


import com.google.gson.annotations.SerializedName

data class Homework(
    @SerializedName("attachments")
    val attachments: List<Any?>,
    @SerializedName("comments")
    val comments: List<Any?>,
    @SerializedName("date")
    val date: String,
    @SerializedName("date_assigned_on")
    val dateAssignedOn: String,
    @SerializedName("date_prepared_for")
    val datePreparedFor: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("group_id")
    val groupId: Long,
    @SerializedName("has_teacher_answer")
    val hasTeacherAnswer: Boolean,
    @SerializedName("homework")
    val homework: String,
    @SerializedName("homework_created_at")
    val homeworkCreatedAt: String,
    @SerializedName("homework_entry_id")
    val homeworkEntryId: Long,
    @SerializedName("homework_entry_student_id")
    val homeworkEntryStudentId: Long,
    @SerializedName("homework_id")
    val homeworkId: Int,
    @SerializedName("homework_updated_at")
    val homeworkUpdatedAt: String,
    @SerializedName("is_done")
    var isDone: Boolean,
    @SerializedName("lesson_date_time")
    val lessonDateTime: String,
    @SerializedName("materials")
    val materials: List<Material>,
    @SerializedName("subject_id")
    val subjectId: Long,
    @SerializedName("subject_name")
    val subjectName: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("written_answer")
    val writtenAnswer: Any?,
    @SerializedName("gosuslugi_id")
    var gosuslugiId: String? = null,
    @SerializedName("gosuslugi_interval_id")
    var gosuslugiIntervalId: String? = null,
    @SerializedName("gosuslugi_materials")
    var gosuslugiMaterials: List<ru.kot.sked.network.gosuslugi.GHomeworkMaterial>? = null
)