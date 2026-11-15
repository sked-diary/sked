package ru.kot.sked.models.events.encoded


import com.google.gson.annotations.SerializedName
import ru.kot.sked.models.events.encoded.MaterialObj.Type

data class MaterialObj(
    @SerializedName("goal")
    val goal: String,
    @SerializedName("name")
    val name: String,
    /**
     * Basically is [Type], just have no ability to know every possible type
     **/
    @SerializedName("type")
    val type: String,
    @SerializedName("uuid")
    val uuid: String,
) {
    enum class Type {
        FizikonModule,
        AtomicObject,
        GameApp,
        LessonTemplate,
        TestSpecBinding
    }
}