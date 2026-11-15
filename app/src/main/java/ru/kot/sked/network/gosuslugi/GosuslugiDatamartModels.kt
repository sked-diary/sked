package ru.kot.sked.network.gosuslugi

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class DatamartQuery(
    @SerializedName("student_id") val studentId: String,
    @SerializedName("obj_type") val objType: String,
    @SerializedName("interval_id") val intervalId: String
)

data class DatamartSnapshot(
    @SerializedName("hash") val hash: String?,
    @SerializedName("student_id") val studentId: String,
    @SerializedName("interval_id") val intervalId: String,
    @SerializedName("obj_type") val objType: String,
    @SerializedName("data_status") val dataStatus: String?,
    @SerializedName("data") val data: List<JsonObject>?
)

data class GStudentTestMark(
    @SerializedName("student_id") val studentId: String? = null,
    @SerializedName("mark_value1") val value: String? = null,
    @SerializedName("mark_value2") val value2: String? = null,
    @SerializedName("weight") val weight: String? = null,
    @SerializedName("work_type_description") val workType: String? = null
)

data class GMark(
    @SerializedName("id") val id: String?,
    @SerializedName("subject_name") val subjectName: String,
    @SerializedName("subject_id") val subjectId: String?,
    @SerializedName("mark_value1") val value: String?,
    @SerializedName("mark_value2") val value2: String?,
    @SerializedName("mark_date") val date: String,
    @SerializedName("weight") val weight: String?,
    @SerializedName("work_type_description") val workType: String?,
    @SerializedName("work_kind") val workKind: String?,
    @SerializedName("comment") val comment: String?,
    @SerializedName("start_datetime") val startDatetime: String?,
    @SerializedName("lesson_id") val lessonId: String?,
    @SerializedName("work_name") val workName: String? = null,
    @SerializedName("work_type_code") val workTypeCode: String? = null,
    @SerializedName("students_test") val studentsTest: List<GStudentTestMark>? = null
)

data class GLesson(
    @SerializedName("id") val id: String?,
    @SerializedName("subject_name") val subjectName: String,
    @SerializedName("subject_id") val subjectId: String?,
    @SerializedName("start_datetime") val startDatetime: String,
    @SerializedName("end_datetime") val endDatetime: String,
    @SerializedName("firstname") val teacherFirstName: String?,
    @SerializedName("lastname") val teacherLastName: String?,
    @SerializedName("patronymic") val teacherPatronymic: String?,
    @SerializedName("room") val room: String?,
    @SerializedName("theme") val theme: String?,
    @SerializedName("type_description") val skipReason: String?,
    @SerializedName("skipping_description") val skippingDescription: String?,
    @SerializedName("is_distant") val isDistant: Boolean?,
    @SerializedName("active") val active: Boolean?
) {
    val teacherFullName: String
        get() {
            val parts = listOfNotNull(teacherLastName?.trim(), teacherFirstName?.trim(), teacherPatronymic?.trim())
            return if (parts.isNotEmpty()) parts.joinToString(" ") else "Не указан"
        }
}

data class GStudentInfo(
    @SerializedName("student_id") val studentId: String?,
    @SerializedName("student_first_name") val firstName: String?,
    @SerializedName("student_last_name") val lastName: String?,
    @SerializedName("student_middle_name") val middleName: String?,
    @SerializedName("student_gender") val gender: String?,
    @SerializedName("student_birth_date") val birthDate: String?,
    @SerializedName("student_region") val region: String?
)

data class GHomeworkMaterial(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("link") val link: String?,
    @SerializedName("size") val size: Long?
)

data class GHomework(
    @SerializedName("id") val id: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("subject_name") val subjectName: String,
    @SerializedName("subject_id") val subjectId: String?,
    @SerializedName("issue_date") val issueDate: String?,
    @SerializedName("plan_ready_date") val planReadyDate: String?,
    @SerializedName("create_datetime") val createDatetime: String?,
    @SerializedName("materials") val materials: List<GHomeworkMaterial>?,
    var intervalId: String? = null
)

data class CustomServicesHomeworkResponse(
    @SerializedName("homeworks") val homeworks: List<CustomServiceHomeworkItem>? = null
)

data class CustomServiceHomeworkItem(
    @SerializedName("interval_id") val intervalId: String? = null,
    @SerializedName("homework_id") val homeworkId: String? = null,
    @SerializedName("completed") val completed: Boolean? = null,
    @SerializedName("complete_datetime") val completeDatetime: String? = null
)

data class ToggleHomeworkRequest(
    @SerializedName("student_id") val studentId: String,
    @SerializedName("homeworks") val homeworks: List<ToggleHomeworkItem>
)

data class ToggleHomeworkItem(
    @SerializedName("interval_id") val intervalId: String,
    @SerializedName("homework_id") val homeworkId: String,
    @SerializedName("complete_datetime") val completeDatetime: String? = null
)
