package ru.kot.sked

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** In-app log buffer (Gosuslugi connection / data loading), shown in About after 5 taps on "Sked". */
object AppLog {
    private const val MAX = 500
    val lines = mutableStateListOf<String>()
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private fun add(level: String, tag: String, msg: String, t: Throwable?) {
        val text = "${fmt.format(Date())} $level/$tag: $msg" + (t?.let { " | ${it.javaClass.simpleName}: ${it.message}" } ?: "")
        synchronized(this) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                lines.add(text)
                while (lines.size > MAX) lines.removeAt(0)
            }
        }
    }

    fun d(tag: String, msg: String) { Log.d(tag, msg); add("D", tag, msg, null) }
    fun i(tag: String, msg: String) { Log.i(tag, msg); add("I", tag, msg, null) }
    fun w(tag: String, msg: String) { Log.w(tag, msg); add("W", tag, msg, null) }
    fun e(tag: String, msg: String, t: Throwable? = null) { Log.e(tag, msg, t); add("E", tag, msg, t) }
}
