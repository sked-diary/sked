package ru.kot.sked.network.gosuslugi

import ru.kot.sked.AppLog
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import ru.kot.sked.DataService
import ru.kot.sked.Screen
import ru.kot.sked.authPrefs
import ru.kot.sked.cachePrefs
import ru.kot.sked.get
import ru.kot.sked.isDemo
import ru.kot.sked.save
import ru.kot.sked.screenLive
import ru.kot.sked.security.GosuslugiVault
import ru.kot.sked.models.events.Event
import ru.kot.sked.models.homeworks2.Homework
import ru.kot.sked.models.marklistdate.MarkListDate
import ru.kot.sked.models.marklistdate.Mark as DateMark
import ru.kot.sked.models.marklistsubject.MarkListSubjectItem
import ru.kot.sked.models.marklistsubject.Period
import ru.kot.sked.models.marklistsubject.Mark as SubjectMark
import ru.kot.sked.models.visits.VisitsResponse
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object GosuslugiDataBridge {

    var rawMarks: List<GMark> = emptyList()

    fun getMarkId(m: GMark): Long {
        return m.id?.hashCode()?.toLong() ?: "${m.subjectName}_${m.date}_${m.value}".hashCode().toLong()
    }

    fun getMarkById(id: Long): GMark? {
        return rawMarks.firstOrNull { getMarkId(it) == id }
    }

    private val inputDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S", Locale.getDefault())
    private val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
    private val loadedWeekIntervals = Collections.synchronizedSet(mutableSetOf<String>())

    fun getPrimaryIntervalForDate(date: Date): String {
        val cal = Calendar.getInstance().apply { time = date }
        return String.format(Locale.US, "%02d%d", cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
    }

    fun getIntervalsForDate(date: Date): List<String> {
        val intervals = LinkedHashSet<String>()
        val cal = Calendar.getInstance().apply { time = date }
        for (offset in -1..1) {
            val c = (cal.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, offset) }
            intervals.add(String.format(Locale.US, "%02d%d", c.get(Calendar.WEEK_OF_YEAR), c.get(Calendar.YEAR)))
        }
        val isoCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            time = date
        }
        for (offset in -1..1) {
            val c = (isoCal.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, offset) }
            intervals.add(String.format(Locale.US, "%02d%d", c.get(Calendar.WEEK_OF_YEAR), c.get(Calendar.YEAR)))
        }
        return intervals.toList()
    }

    fun isWeekLoaded(date: Date): Boolean = loadedWeekIntervals.contains(getPrimaryIntervalForDate(date))

    fun parseDateToIso(dateStr: String): String {
        return try {
            val d = inputDateFormatter.parse(dateStr)
            isoDateFormatter.format(d ?: Date())
        } catch (e: Exception) {
            try {
                val d2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)
                isoDateFormatter.format(d2 ?: Date())
            } catch (e2: Exception) {
                dateStr
            }
        }
    }

    fun applyProfile(context: Context) {
        if (context.isDemo) return
        // Neutral placeholders until real data arrives from Gosuslugi (never real personal data)
        val fn = context.authPrefs.get<String>("student_first_name") ?: "Ученик"
        val ln = context.authPrefs.get<String>("student_last_name") ?: ""
        val mn = context.authPrefs.get<String>("student_middle_name") ?: ""
        val cn = context.authPrefs.get<String>("student_class_name") ?: ""
        val sn = context.authPrefs.get<String>("student_school_name") ?: "Школа"
        val bd = context.authPrefs.get<String>("student_birth_date") ?: "2010-01-01"
        val snils = context.authPrefs.get<String>("student_snils") ?: ""
        val phone = context.authPrefs.get<String>("student_phone") ?: ""
        val email = context.authPrefs.get<String>("student_email") ?: ""

        try {
            if (!DataService.hasProfile) {
                DataService.run { context.loadDemoCache() }
            }
            val child = DataService.profile.children.firstOrNull()
            if (child != null) {
                val updatedChild = child.copy(
                    firstName = fn,
                    lastName = ln,
                    middleName = mn,
                    className = cn,
                    birthDate = bd,
                    snils = snils,
                    phone = phone,
                    email = email,
                    school = child.school.copy(shortName = sn, name = sn)
                )
                val updatedProfile = DataService.profile.profile.copy(
                    firstName = fn,
                    lastName = ln,
                    middleName = mn,
                    birthDate = bd,
                    snils = snils,
                    phone = phone,
                    email = email
                )
                DataService.profile = DataService.profile.copy(
                    children = listOf(updatedChild),
                    profile = updatedProfile
                )
                DataService.hasProfile = true
            }
        } catch (e: Exception) {
            AppLog.e("GosuslugiBridge", "Error updating profile", e)
        }
    }

    fun loadFromCache(context: Context): Boolean {
        try {
            if (!DataService.hasProfile) {
                DataService.run { context.loadDemoCache() }
            }
            applyProfile(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val gson = Gson()
        val lessonsJson = context.cachePrefs.get<String>("gosuslugi_lessons")
        val marksJson = context.cachePrefs.get<String>("gosuslugi_marks")
        val hwJson = context.cachePrefs.get<String>("gosuslugi_homeworks")

        if (lessonsJson.isNullOrEmpty()) return false

        return try {
            val lessonType = object : TypeToken<List<GLesson>>() {}.type
            val markType = object : TypeToken<List<GMark>>() {}.type
            val hwType = object : TypeToken<List<GHomework>>() {}.type

            val lessons: List<GLesson> = gson.fromJson(lessonsJson, lessonType) ?: emptyList()
            val marks: List<GMark> = if (!marksJson.isNullOrEmpty()) gson.fromJson(marksJson, markType) ?: emptyList() else emptyList()
            val hw: List<GHomework> = if (!hwJson.isNullOrEmpty()) gson.fromJson(hwJson, hwType) ?: emptyList() else emptyList()

            if (lessons.isNotEmpty()) {
                rawMarks = marks
                applyProfile(context)
                applyLessons(lessons, hw, marks)
                applyMarks(marks)
                applyHomeworks(context, hw)
                if (DataService.eventCalendar.isNotEmpty()) {
                    context.cachePrefs.save("eventCalendar" to gson.toJson(DataService.eventCalendar))
                }
                true
            } else false
        } catch (e: Exception) {
            AppLog.e("GosuslugiBridge", "Failed to load from cache", e)
            false
        }
    }

    fun saveToCache(context: Context, lessons: List<GLesson>, marks: List<GMark>, homeworks: List<GHomework>) {
        rawMarks = marks
        val gson = Gson()
        val calendarJson: String? = try {
            gson.toJson(DataService.eventCalendar)
        } catch (_: UninitializedPropertyAccessException) {
            null // cold start: calendar not built yet, keep the previously cached one
        }
        context.cachePrefs.save(
            "gosuslugi_lessons" to gson.toJson(lessons),
            "gosuslugi_marks" to gson.toJson(marks),
            "gosuslugi_homeworks" to gson.toJson(homeworks),
            "gosuslugi_cache_time" to System.currentTimeMillis()
        )
        if (calendarJson != null) {
            context.cachePrefs.save("eventCalendar" to calendarJson)
        }
        try {
            CoroutineScope(Dispatchers.IO).launch {
                ru.kot.sked.widget.StatusWidget().updateAll(context)
            }
        } catch (_: Exception) {}
    }

    private var isRefreshing = false

    fun extractCookieValue(cookieString: String, name: String): String? {
        val items = cookieString.split(";")
        for (item in items) {
            val trimmed = item.trim()
            if (trimmed.startsWith("$name=")) {
                return trimmed.substringAfter("$name=")
            }
        }
        return null
    }

    fun isJwtExpired(jwt: String?): Boolean {
        jwt ?: return true
        try {
            val parts = jwt.split(".")
            if (parts.size >= 2) {
                val decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                val payload = String(decodedBytes, Charsets.UTF_8)
                val jsonObj = Gson().fromJson(payload, JsonObject::class.java)
                if (jsonObj.has("exp")) {
                    val expSec = jsonObj.get("exp").asLong
                    val nowSec = System.currentTimeMillis() / 1000
                    return nowSec >= (expSec - 1800)
                }
            }
        } catch (e: Exception) {
            AppLog.e("GosuslugiBridge", "Error checking JWT expiration", e)
        }
        return true
    }

    fun refreshSessionSilently(context: Context, onResult: (Boolean) -> Unit) {
        if (isRefreshing) {
            AppLog.d("GosuslugiBridge", "Silent refresh already in progress")
            onResult(false)
            return
        }
        isRefreshing = true

        Handler(Looper.getMainLooper()).post {
            try {
                var finished = false
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)

                val webView = WebView(context)
                cookieManager.setAcceptThirdPartyCookies(webView, true)

                val settings = webView.settings
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                val oldCookie = context.authPrefs.get<String>("cookie") ?: ""
                val oldAccT = extractCookieValue(oldCookie, "acc_t")

                fun cleanup() {
                    if (!finished) {
                        finished = true
                        isRefreshing = false
                        try {
                            webView.stopLoading()
                            webView.destroy()
                        } catch (_: Exception) {}
                    }
                }

                val mainHandler = Handler(Looper.getMainLooper())
                var injectionAttempts = 0
                val timeoutRunnable = Runnable {
                    if (!finished) {
                        AppLog.w("GosuslugiBridge", "Silent refresh timed out after 40s")
                        cleanup()
                        onResult(false)
                    }
                }
                mainHandler.postDelayed(timeoutRunnable, 40000L)

                fun checkCookies(url: String?) {
                    if (finished) return

                    // Check if fresh auth token is already present
                    val currentCookies = cookieManager.getCookie("https://www.gosuslugi.ru") ?: ""
                    val currentAccT = extractCookieValue(currentCookies, "acc_t")

                    if (!currentAccT.isNullOrEmpty() && !isJwtExpired(currentAccT)) {
                        AppLog.d("GosuslugiBridge", "Silent refresh SUCCEEDED! Fresh acc_t obtained.")
                        mainHandler.removeCallbacks(timeoutRunnable)
                        context.authPrefs.save("cookie" to currentCookies)
                        cookieManager.flush()
                        cleanup()
                        onResult(true)
                        return
                    }

                    // If redirected to login page: check if we have saved credentials in GosuslugiVault
                    if (url?.contains("esia.gosuslugi.ru/login") == true || url?.contains("esia.gosuslugi.ru/aas") == true) {
                        if (GosuslugiVault.hasCredentials(context) && injectionAttempts < 90) {
                            injectionAttempts++
                            val creds = GosuslugiVault.getDecryptedCredentials(context)
                            if (creds != null) {
                                val loginEscaped = creds.first.replace("\\", "\\\\").replace("\"", "\\\"")
                                val passEscaped = creds.second.replace("\\", "\\\\").replace("\"", "\\\"")
                                val has2fa = GosuslugiVault.has2fa(context)
                                val totpCode = if (has2fa) {
                                    val secret = GosuslugiVault.getDecryptedTotpSecret(context)
                                    if (secret != null) GosuslugiVault.generateCurrentTotp(secret) ?: "" else ""
                                } else ""

                                val js = """
                                    (function() {
                                        function setVal(el, val) {
                                            if (!el) return false;
                                            el.focus();
                                            el.value = val;
                                            el.dispatchEvent(new Event('input', { bubbles: true }));
                                            el.dispatchEvent(new Event('change', { bubbles: true }));
                                            el.dispatchEvent(new Event('blur', { bubbles: true }));
                                            return true;
                                        }
                                        function vis(el) { return !!el && el.offsetParent !== null; }
                                        function clickSubmit() {
                                            setTimeout(function() {
                                                var btn = document.querySelector('button[type="submit"]') || document.querySelector('button.plain-button') || document.querySelector('button');
                                                if (btn && !btn.disabled) btn.click();
                                            }, 300);
                                        }
                                        // Do not re-submit the same step too often: ESIA is a SPA and needs time between steps
                                        var st = window.__octoStep || { name: '', ts: 0 };
                                        function step(name) {
                                            var now = Date.now();
                                            if (st.name === name && now - st.ts < 8000) return false;
                                            window.__octoStep = { name: name, ts: now };
                                            return true;
                                        }
                                        var errEl = document.querySelector('[class*="error"], [class*="Error"], [role="alert"]');
                                        var errTxt = errEl && errEl.innerText ? (' ERR=' + errEl.innerText.trim().substring(0, 80)) : '';

                                        var codeEl = document.getElementById('code') || document.querySelector('input[name="code"]') || document.querySelector('input[autocomplete="one-time-code"]') || document.querySelector('input[inputmode="numeric"]') || document.querySelector('input[name="otp"]');
                                        var loginEl = document.getElementById('login') || document.querySelector('input[name="login"]') || document.querySelector('input[type="text"]') || document.querySelector('input[type="tel"]');
                                        var passEl = document.getElementById('password') || document.querySelector('input[name="password"]') || document.querySelector('input[type="password"]');

                                        // 1. TOTP / 2FA code input (checked first: it appears after password)
                                        if (vis(codeEl) && !vis(passEl)) {
                                            if ("$totpCode".length === 0) return 'TOTP_NEEDED_BUT_NO_SECRET' + errTxt;
                                            if (!step('totp')) return 'WAIT_TOTP' + errTxt;
                                            setVal(codeEl, "$totpCode");
                                            clickSubmit();
                                            return 'TOTP_SUBMITTED' + errTxt;
                                        }

                                        // 2. Login + password on one page
                                        if (vis(loginEl) && vis(passEl)) {
                                            if (!step('login_pass')) return 'WAIT_LOGIN_PASS' + errTxt;
                                            setVal(loginEl, "$loginEscaped");
                                            setVal(passEl, "$passEscaped");
                                            clickSubmit();
                                            return 'LOGIN_SUBMITTED' + errTxt;
                                        }

                                        // 3. Single password input (multistep step 2)
                                        if (vis(passEl)) {
                                            if (!step('pass')) return 'WAIT_PASS' + errTxt;
                                            setVal(passEl, "$passEscaped");
                                            clickSubmit();
                                            return 'PASS_STEP2_SUBMITTED' + errTxt;
                                        }

                                        // 4. Single login input (multistep step 1)
                                        if (vis(loginEl)) {
                                            if (!step('login')) return 'WAIT_LOGIN' + errTxt;
                                            setVal(loginEl, "$loginEscaped");
                                            clickSubmit();
                                            return 'LOGIN_STEP1_SUBMITTED' + errTxt;
                                        }
                                        return 'WAITING url=' + location.pathname + ' inputs=' + document.querySelectorAll('input').length + errTxt;
                                    })();
                                """.trimIndent()

                                webView.evaluateJavascript(js) { res ->
                                    AppLog.d("GosuslugiBridge", "Auto-login injection: $res (attempt=$injectionAttempts)")
                                }
                                return
                            }
                        }

                        AppLog.w("GosuslugiBridge", "Silent refresh redirected to ESIA login: no auto-login available or attempts exhausted")
                        mainHandler.removeCallbacks(timeoutRunnable)
                        cleanup()
                        onResult(false)
                        return
                    }
                }

                val checkInterval = object : Runnable {
                    override fun run() {
                        if (!finished) {
                            checkCookies(webView.url)
                            if (!finished) {
                                mainHandler.postDelayed(this, 500L)
                            }
                        }
                    }
                }
                mainHandler.postDelayed(checkInterval, 500L)

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        AppLog.d("GosuslugiBridge", "Silent WebView onPageStarted: $url")
                        checkCookies(url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        AppLog.d("GosuslugiBridge", "Silent WebView onPageFinished: $url")
                        checkCookies(url)
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        AppLog.d("GosuslugiBridge", "Silent WebView history: $url")
                        checkCookies(url)
                    }
                }

                AppLog.d("GosuslugiBridge", "Triggering silent refresh via WebView on https://www.gosuslugi.ru/school/feed...")
                webView.onResume()
                webView.resumeTimers()
                webView.loadUrl("https://www.gosuslugi.ru/school/feed")
            } catch (e: Exception) {
                AppLog.e("GosuslugiBridge", "Exception in silent refresh setup", e)
                isRefreshing = false
                onResult(false)
            }
        }
    }

    private fun handleAuthFailure(context: Context, onComplete: (Boolean) -> Unit) {
        Handler(Looper.getMainLooper()).post {
            if (!DataService.hasEventCalendar || DataService.eventCalendar.isEmpty()) {
                loadFromCache(context)
            }
            applyProfile(context)
            DataService.loadedEverything.value = true
            // Silent offline fallback: do not spam the user with Toast notifications
            onComplete(false)
        }
    }

    /** Resolves the student id for the logged-in account from the Gosuslugi API (no hardcoded ids). */
    fun resolveStudentId(cookie: String): String? = try {
        GosuslugiApiService.create().getStudentInfo(cookie).execute().body()
            ?.firstOrNull()?.studentId?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        AppLog.e("GosuslugiBridge", "Failed to resolve student id", e)
        null
    }

    fun fetchAndApply(context: Context, onComplete: (Boolean) -> Unit) {
        val cookie = context.authPrefs.get<String>("cookie") ?: return onComplete(false)
        val studentId = context.authPrefs.get<String>("student_id")?.takeIf { it.isNotBlank() }
            ?: run {
                AppLog.w("GosuslugiBridge", "student_id is not known yet, cannot fetch data")
                return onComplete(false)
            }

        val currentAccT = extractCookieValue(cookie, "acc_t")
        if (isJwtExpired(currentAccT)) {
            AppLog.d("GosuslugiBridge", "Local acc_t is expired before queries. Triggering silent refresh immediately...")
            refreshSessionSilently(context) { success ->
                if (success) {
                    AppLog.d("GosuslugiBridge", "Silent refresh succeeded! Re-executing fetchAndApply...")
                    fetchAndApply(context, onComplete)
                } else {
                    handleAuthFailure(context, onComplete)
                }
            }
            return
        }

        // Ensure baseline demo cache and profile exist so lateinit fields don't throw
        try {
            if (!DataService.hasProfile) {
                DataService.run { context.loadDemoCache() }
                applyProfile(context)
            }
            if (!DataService.hasEventCalendar || DataService.eventCalendar.isEmpty()) {
                loadFromCache(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val now = System.currentTimeMillis()
        DataService.eventsRange = listOf(now - 28 * 86400000L, now + 14 * 86400000L)

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)

        val lessonWeeksStart = (currentWeek - 4).coerceAtLeast(1)
        val lessonWeeksEnd = (currentWeek + 2).coerceAtMost(52)
        val initialIntervals = (lessonWeeksStart..lessonWeeksEnd).map { w ->
            String.format(Locale.US, "%02d%d", w, year)
        }

        val queries = mutableListOf(
            DatamartQuery(studentId, "student_classes", year.toString())
        )
        initialIntervals.forEach { interval ->
            queries.add(DatamartQuery(studentId, "student_lessons", interval))
            queries.add(DatamartQuery(studentId, "student_homeworks_materials", interval))
        }
        // Fetch all marks for the school year (weeks 36 to currentWeek + 1)
        val startWeek = 36
        val endWeek = (currentWeek + 1).coerceAtMost(52)
        for (w in startWeek..endWeek) {
            val interval = String.format(Locale.US, "%02d%d", w, year)
            queries.add(DatamartQuery(studentId, "student_marks", interval))
        }

        Thread {
            val api = GosuslugiApiService.create()

            // 1. Fetch official student profile info
            try {
                val studentResp = api.getStudentInfo(cookie).execute()
                if (studentResp.code() == 401 || studentResp.code() == 403) {
                    AppLog.w("GosuslugiBridge", "StudentInfo 401/403. Triggering silent refresh...")
                    refreshSessionSilently(context) { success ->
                        if (success) {
                            fetchAndApply(context, onComplete)
                        } else {
                            handleAuthFailure(context, onComplete)
                        }
                    }
                    return@Thread
                }
                if (studentResp.isSuccessful) {
                    val sInfo = studentResp.body()?.firstOrNull()
                    if (sInfo != null) {
                        val fn = sInfo.firstName?.trim()
                        val ln = sInfo.lastName?.trim()
                        val mn = sInfo.middleName?.trim()
                        if (!fn.isNullOrEmpty()) context.authPrefs.save("student_first_name" to fn)
                        if (!ln.isNullOrEmpty()) context.authPrefs.save("student_last_name" to ln)
                        if (!mn.isNullOrEmpty()) context.authPrefs.save("student_middle_name" to mn)
                        sInfo.birthDate?.let { bd ->
                            val parts = bd.split(".")
                            if (parts.size == 3) {
                                val formatted = "${parts[2]}-${parts[1]}-${parts[0]}"
                                context.authPrefs.save("student_birth_date" to formatted)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLog.e("GosuslugiBridge", "Error fetching student info", e)
            }

            // 2. Fetch user personal data (SNILS, phone, email)
            try {
                val userResp = api.getUserData(cookie).execute()
                if (userResp.isSuccessful) {
                    val userObj = userResp.body()
                    if (userObj != null) {
                        val snils = if (userObj.has("personSNILS") && !userObj.get("personSNILS").isJsonNull) {
                            userObj.get("personSNILS").asString
                        } else if (userObj.has("snils") && !userObj.get("snils").isJsonNull) {
                            userObj.get("snils").asString
                        } else null

                        val phone = if (userObj.has("personMobilePhone") && !userObj.get("personMobilePhone").isJsonNull) {
                            userObj.get("personMobilePhone").asString
                        } else if (userObj.has("mobile") && !userObj.get("mobile").isJsonNull) {
                            userObj.get("mobile").asString
                        } else null

                        val email = if (userObj.has("personEMail") && !userObj.get("personEMail").isJsonNull) {
                            userObj.get("personEMail").asString
                        } else if (userObj.has("email") && !userObj.get("email").isJsonNull) {
                            userObj.get("email").asString
                        } else null

                        if (!snils.isNullOrEmpty()) context.authPrefs.save("student_snils" to snils)
                        if (!phone.isNullOrEmpty()) context.authPrefs.save("student_phone" to phone)
                        if (!email.isNullOrEmpty()) context.authPrefs.save("student_email" to email)
                    }
                }
            } catch (e: Exception) {
                AppLog.e("GosuslugiBridge", "Error fetching user data", e)
            }

            // 3. Fetch Datamart data (with retry for 202)
            try {
                var isAuthError = false
                var snapshots: List<DatamartSnapshot> = emptyList()
                for (attempt in 0..2) {
                    AppLog.d("GosuslugiBridge", "Requesting datamart (attempt $attempt)...")
                    val call = api.getDatamart(cookie, queries)
                    val resp = call.execute()
                    if (resp.code() == 401 || resp.code() == 403) {
                        isAuthError = true
                        AppLog.w("GosuslugiBridge", "Gosuslugi session expired (HTTP ${resp.code()}). Triggering silent refresh...")
                        refreshSessionSilently(context) { success ->
                            if (success) {
                                fetchAndApply(context, onComplete)
                            } else {
                                handleAuthFailure(context, onComplete)
                            }
                        }
                        return@Thread
                    }
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        if (!body.isNullOrEmpty()) {
                            snapshots = body
                            break
                        }
                    }
                    if (attempt < 2) Thread.sleep(1500)
                }

                val gson = Gson()
                val lessons = mutableListOf<GLesson>()
                val marks = mutableListOf<GMark>()
                val homeworks = mutableListOf<GHomework>()

                snapshots.forEach { snap ->
                    snap.data?.forEach { item ->
                        when (snap.objType) {
                            "student_lessons" -> lessons.add(gson.fromJson(item, GLesson::class.java))
                            "student_marks" -> marks.add(gson.fromJson(item, GMark::class.java))
                            "student_homeworks_materials" -> {
                                val hw = gson.fromJson(item, GHomework::class.java).apply {
                                    intervalId = snap.intervalId
                                }
                                homeworks.add(hw)
                            }
                            "student_classes" -> {
                                val shortName = item.get("short_name")?.asString
                                val classNum = item.get("class_num")?.asString
                                val classLetter = item.get("class_letter")?.asString
                                if (!shortName.isNullOrEmpty()) context.authPrefs.save("student_school_name" to shortName)
                                if (!classNum.isNullOrEmpty() && !classLetter.isNullOrEmpty()) {
                                    val cName = "$classNum-$classLetter"
                                    context.authPrefs.save("student_class_name" to cName)
                                }
                            }
                        }
                    }
                }

                if (lessons.isNotEmpty()) {
                    loadedWeekIntervals.addAll(initialIntervals)
                    AppLog.d("GosuslugiBridge", "Extracted ${lessons.size} lessons, ${marks.size} marks, ${homeworks.size} homeworks. Saving to cache.")
                    saveToCache(context, lessons, marks, homeworks)

                    var statusMap: Map<String, Boolean>? = null
                    try {
                        val csResp = api.getHomeworkStatus(cookie, studentId, "homework", initialIntervals).execute()
                        if (csResp.isSuccessful) {
                            val csHws = csResp.body()?.homeworks
                            if (csHws != null) {
                                statusMap = csHws.associate { (it.homeworkId ?: "") to (it.completed == true) }
                                AppLog.d("GosuslugiBridge", "Loaded ${statusMap.size} homework status entries from Gosuslugi customservices")
                            }
                        } else {
                            AppLog.w("GosuslugiBridge", "Failed to get customservices homework status: ${csResp.code()}")
                        }
                    } catch (e: Exception) {
                        AppLog.e("GosuslugiBridge", "Error fetching homework status", e)
                    }

                    Handler(Looper.getMainLooper()).post {
                        applyProfile(context)
                        applyLessons(lessons, homeworks, marks)
                        applyMarks(marks)
                        applyHomeworks(context, homeworks, statusMap)
                        DataService.loadedEverything.value = true
                        onComplete(true)
                    }
                } else {
                    AppLog.w("GosuslugiBridge", "Received 0 lessons from datamart (authError=$isAuthError). Keeping current data.")
                    Handler(Looper.getMainLooper()).post {
                        if (!DataService.hasEventCalendar || DataService.eventCalendar.isEmpty()) {
                            loadFromCache(context)
                        }
                        applyProfile(context)
                        DataService.loadedEverything.value = true
                        onComplete(false)
                    }
                }
            } catch (e: Exception) {
                AppLog.e("GosuslugiBridge", "Exception in Datamart processing (offline)", e)
                Handler(Looper.getMainLooper()).post {
                    if (!DataService.hasEventCalendar || DataService.eventCalendar.isEmpty()) {
                        loadFromCache(context)
                    }
                    applyProfile(context)
                    DataService.loadedEverything.value = true
                    onComplete(false)
                }
            }
        }.start()
    }

    fun fetchWeekData(context: Context, date: Date, onComplete: (Boolean) -> Unit = {}) {
        val cookie = context.authPrefs.get<String>("cookie") ?: return onComplete(false)
        val studentId = context.authPrefs.get<String>("student_id")?.takeIf { it.isNotBlank() }
            ?: return onComplete(false)

        val currentAccT = extractCookieValue(cookie, "acc_t")
        if (isJwtExpired(currentAccT)) {
            refreshSessionSilently(context) { success ->
                if (success) {
                    fetchWeekData(context, date, onComplete)
                } else {
                    onComplete(false)
                }
            }
            return
        }

        val intervals = getIntervalsForDate(date)
        val queries = mutableListOf<DatamartQuery>()
        intervals.forEach { interval ->
            queries.add(DatamartQuery(studentId, "student_lessons", interval))
            queries.add(DatamartQuery(studentId, "student_homeworks_materials", interval))
        }

        Thread {
            try {
                val api = GosuslugiApiService.create()
                var snapshots: List<DatamartSnapshot> = emptyList()
                for (attempt in 0..2) {
                    val resp = api.getDatamart(cookie, queries).execute()
                    if (resp.code() == 401 || resp.code() == 403) {
                        refreshSessionSilently(context) { success ->
                            if (success) {
                                fetchWeekData(context, date, onComplete)
                            } else {
                                onComplete(false)
                            }
                        }
                        return@Thread
                    }
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        if (!body.isNullOrEmpty()) {
                            snapshots = body
                            break
                        }
                    }
                    if (attempt < 2) Thread.sleep(1200)
                }

                val gson = Gson()
                val newLessons = mutableListOf<GLesson>()
                val newHomeworks = mutableListOf<GHomework>()

                snapshots.forEach { snap ->
                    snap.data?.forEach { item ->
                        when (snap.objType) {
                            "student_lessons" -> newLessons.add(gson.fromJson(item, GLesson::class.java))
                            "student_homeworks_materials" -> {
                                val hw = gson.fromJson(item, GHomework::class.java).apply {
                                    intervalId = snap.intervalId
                                }
                                newHomeworks.add(hw)
                            }
                        }
                    }
                }

                val lessonType = object : TypeToken<List<GLesson>>() {}.type
                val markType = object : TypeToken<List<GMark>>() {}.type
                val hwType = object : TypeToken<List<GHomework>>() {}.type

                val lessonsJson = context.cachePrefs.get<String>("gosuslugi_lessons")
                val marksJson = context.cachePrefs.get<String>("gosuslugi_marks")
                val hwJson = context.cachePrefs.get<String>("gosuslugi_homeworks")

                val cachedLessons: List<GLesson> =
                    if (!lessonsJson.isNullOrEmpty()) gson.fromJson(lessonsJson, lessonType) ?: emptyList() else emptyList()
                val cachedMarks: List<GMark> =
                    if (!marksJson.isNullOrEmpty()) gson.fromJson(marksJson, markType) ?: emptyList() else emptyList()
                val cachedHw: List<GHomework> =
                    if (!hwJson.isNullOrEmpty()) gson.fromJson(hwJson, hwType) ?: emptyList() else emptyList()

                val mergedLessons = (cachedLessons + newLessons).distinctBy {
                    it.id ?: "${it.startDatetime}_${it.subjectName}"
                }
                val mergedHomeworks = (cachedHw + newHomeworks).distinctBy {
                    it.id ?: "${it.subjectName}_${it.planReadyDate ?: it.issueDate}_${it.description?.trim()}"
                }

                loadedWeekIntervals.addAll(intervals)
                saveToCache(context, mergedLessons, cachedMarks, mergedHomeworks)

                var statusMap: Map<String, Boolean>? = null
                try {
                    val csResp = api.getHomeworkStatus(cookie, studentId, "homework", intervals).execute()
                    if (csResp.isSuccessful) {
                        val csHws = csResp.body()?.homeworks
                        if (csHws != null) {
                            statusMap = csHws.associate { (it.homeworkId ?: "") to (it.completed == true) }
                        }
                    }
                } catch (e: Exception) {
                    AppLog.e("GosuslugiBridge", "Error fetching week homework status", e)
                }

                Handler(Looper.getMainLooper()).post {
                    applyLessons(mergedLessons, mergedHomeworks, cachedMarks)
                    applyHomeworks(context, mergedHomeworks, statusMap)
                    onComplete(true)
                }
            } catch (e: Exception) {
                AppLog.e("GosuslugiBridge", "Exception in fetchWeekData", e)
                Handler(Looper.getMainLooper()).post {
                    onComplete(false)
                }
            }
        }.start()
    }

    fun applyLessons(
        lessons: List<GLesson>,
        homeworks: List<GHomework> = emptyList(),
        marks: List<GMark> = emptyList()
    ) {
        val gson = Gson()
        var idCounter = 1000L

        // Deduplicate and sort lessons chronologically by startDatetime so breaks and ordering are always correct
        val uniqueLessons = lessons.distinctBy {
            it.id ?: "${it.startDatetime}_${it.subjectName}"
        }
        val sortedLessons = uniqueLessons.sortedBy { it.startDatetime }

        val events = sortedLessons.map { lesson ->
            idCounter++
            val isoStart = parseDateToIso(lesson.startDatetime)
            val isoEnd = parseDateToIso(lesson.endDatetime)
            val lessonDate = lesson.startDatetime.substringBefore(" ")

            // Find homework assigned on this lesson or due for this lesson
            val matchingHw = homeworks.filter { hw ->
                hw.subjectName.equals(lesson.subjectName, ignoreCase = true) &&
                        (hw.issueDate == lessonDate || hw.planReadyDate == lessonDate) &&
                        !hw.description.isNullOrBlank() &&
                        !hw.description.startsWith("Без задания", ignoreCase = true)
            }
            val hwDescriptions = matchingHw.map { hw ->
                if (hw.issueDate == lessonDate && !hw.planReadyDate.isNullOrEmpty() && hw.planReadyDate != lessonDate) {
                    "${hw.description} (к ${hw.planReadyDate})"
                } else {
                    hw.description!!
                }
            }.distinct()

            // Find marks given for this lesson/date
            val matchingMarks = marks.filter { m ->
                m.subjectName.equals(lesson.subjectName, ignoreCase = true) &&
                        m.date == lessonDate &&
                        !m.value.isNullOrEmpty()
            }

            val json = JsonObject().apply {
                addProperty("id", idCounter)
                addProperty("title", lesson.subjectName)
                addProperty("subject_name", lesson.subjectName)
                addProperty("subject_id", lesson.subjectName.hashCode().toLong())
                addProperty("source", "PLAN")
                addProperty("start_at", isoStart)
                addProperty("finish_at", isoEnd)

                val room = lesson.room?.trim()?.takeIf { it.isNotEmpty() && it != "-" }
                if (room != null) {
                    addProperty("room_name", room)
                    addProperty("room_number", room)
                }

                if (!lesson.teacherFullName.isNullOrBlank() && lesson.teacherFullName != "Не указан") {
                    addProperty("author_name", lesson.teacherFullName)
                }

                if (!lesson.theme.isNullOrBlank() && lesson.theme != "-") {
                    addProperty("description", lesson.theme)
                    addProperty("lesson_theme", lesson.theme)
                }

                addProperty("cancelled", lesson.active == false)
                addProperty("is_all_day", false)
                addProperty("is_distant", lesson.isDistant ?: false)

                if (hwDescriptions.isNotEmpty()) {
                    val hwObj = JsonObject().apply {
                        val descArray = JsonArray()
                        hwDescriptions.forEach { descArray.add(it) }
                        add("descriptions", descArray)
                        addProperty("total_count", hwDescriptions.size)
                        addProperty("presence_status_id", 0)
                    }
                    add("homework", hwObj)
                }

                if (matchingMarks.isNotEmpty()) {
                    val marksArray = JsonArray()
                    matchingMarks.forEach { m ->
                        val mObj = JsonObject().apply {
                            addProperty("id", m.id?.hashCode()?.toLong() ?: idCounter)
                            addProperty("value", m.value)
                            addProperty("weight", m.weight?.toIntOrNull() ?: 1)
                            addProperty("control_form_name", m.workType ?: "Ответ на уроке")
                            addProperty("comment", m.comment)
                            addProperty("comment_exists", !m.comment.isNullOrEmpty())
                            addProperty("is_exam", false)
                            addProperty("is_point", false)
                            addProperty("original_grade_system_type", "5")
                        }
                        marksArray.add(mObj)
                    }
                    add("marks", marksArray)
                }
            }

            gson.fromJson(json, Event::class.java)
        }

        DataService.eventCalendar = events
        DataService.hasEventCalendar = true

        // Extract real school & teachers
        try {
            val teachers = sortedLessons
                .filter { !it.teacherLastName.isNullOrEmpty() }
                .groupBy { it.teacherFullName }
                .map { (_, lessonList) ->
                    val first = lessonList.first()
                    val subjects = lessonList.map { it.subjectName }.distinct().sorted()
                    ru.kot.sked.models.schoolinfo.Teacher(
                        firstName = first.teacherFirstName?.trim() ?: "",
                        lastName = first.teacherLastName?.trim() ?: "",
                        middleName = first.teacherPatronymic?.trim() ?: "",
                        subjectNames = subjects
                    )
                }.sortedBy { it.lastName }

            val profileSchool = try {
                DataService.profile.children.firstOrNull()?.school
            } catch (_: Exception) { null }

            val schoolName = profileSchool?.name?.takeIf { it.isNotBlank() }
                ?: profileSchool?.shortName?.takeIf { it.isNotBlank() }
                ?: "Школа"
            val schoolPhone = profileSchool?.phone?.takeIf { it.isNotBlank() }
            val schoolPrincipal = profileSchool?.principal?.takeIf { it.isNotBlank() }
            val schoolCounty = profileSchool?.county?.takeIf { it.isNotBlank() }

            DataService.schoolInfo = ru.kot.sked.models.schoolinfo.SchoolInfo(
                address = ru.kot.sked.models.schoolinfo.Address(
                    address = null,
                    county = schoolCounty,
                    district = null
                ),
                branches = emptyList(),
                classroomTeachers = emptyList(),
                email = null,
                id = profileSchool?.id ?: 0L,
                name = schoolName,
                phone = schoolPhone,
                principal = schoolPrincipal,
                teachers = teachers,
                type = "Общеобразовательная школа",
                websiteLink = null
            )
        } catch (e: Exception) {
            AppLog.e("GosuslugiBridge", "Error populating school info", e)
        }
    }

    fun applyHomeworks(context: Context, hwList: List<GHomework>, statusMap: Map<String, Boolean>? = null) {
        val hwPrefs = context.getSharedPreferences("homework_done", Context.MODE_PRIVATE)
        var hwId = 5000L
        val uniqueHw = hwList.distinctBy {
            it.id ?: "${it.subjectName}_${it.planReadyDate ?: it.issueDate}_${it.description?.trim()}"
        }
        val result = uniqueHw.mapNotNull { gHw ->
            val readyDate = gHw.planReadyDate?.trim()?.substringBefore(" ")?.substringBefore("T")?.takeIf { it.isNotEmpty() }
            val issuedDate = gHw.issueDate?.trim()?.substringBefore(" ")?.substringBefore("T")?.takeIf { it.isNotEmpty() }
            val date = readyDate ?: issuedDate ?: return@mapNotNull null
            val desc = gHw.description ?: "Без задания"
            hwId++
            val isoDate = "${date}T12:00:00+03:00"
            val defaultDone = desc.startsWith("Без задания", ignoreCase = true)
            val hwKey = "${gHw.subjectName}_${date}_${desc.hashCode()}"
            val isDone = if (!gHw.id.isNullOrEmpty() && statusMap != null && statusMap.containsKey(gHw.id)) {
                statusMap[gHw.id] == true
            } else if (!gHw.id.isNullOrEmpty() && hwPrefs.contains("gosuslugi_${gHw.id}")) {
                hwPrefs.getBoolean("gosuslugi_${gHw.id}", false)
            } else {
                hwPrefs.getBoolean(hwKey, defaultDone)
            }
            hwPrefs.edit().putBoolean(hwKey, isDone).apply()
            val effInterval = gHw.intervalId ?: run {
                try {
                    val c = Calendar.getInstance()
                    c.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: Date()
                    String.format(Locale.US, "%02d%d", c.get(Calendar.WEEK_OF_YEAR), c.get(Calendar.YEAR))
                } catch (_: Exception) { null }
            }
            if (!gHw.id.isNullOrEmpty()) {
                hwPrefs.edit().putBoolean("gosuslugi_${gHw.id}", isDone).apply()
                if (!effInterval.isNullOrEmpty()) {
                    hwPrefs.edit().putString("interval_${gHw.id}", effInterval).apply()
                }
            }
            Homework(
                attachments = emptyList(),
                comments = emptyList(),
                date = date,
                dateAssignedOn = gHw.issueDate ?: date,
                datePreparedFor = date,
                description = desc,
                groupId = 0L,
                hasTeacherAnswer = false,
                homework = desc,
                homeworkCreatedAt = gHw.createDatetime ?: isoDate,
                homeworkEntryId = hwId,
                homeworkEntryStudentId = hwId,
                homeworkId = hwId.toInt(),
                homeworkUpdatedAt = isoDate,
                isDone = isDone,
                lessonDateTime = isoDate,
                materials = emptyList(),
                subjectId = (gHw.subjectId?.hashCode() ?: gHw.subjectName.hashCode()).toLong(),
                subjectName = gHw.subjectName,
                type = "Домашнее задание",
                writtenAnswer = null,
                gosuslugiId = gHw.id,
                gosuslugiIntervalId = effInterval,
                gosuslugiMaterials = gHw.materials
            )
        }
        DataService.homeworks = result
        DataService.hasHomeworks = true
        ru.kot.sked.screens.navsections.homeworks.updatedHomeworksLive.postValue(System.currentTimeMillis())
    }

    fun toggleHomework(
        context: Context,
        homeworkId: String,
        intervalId: String,
        completed: Boolean,
        onDone: ((Boolean) -> Unit)? = null
    ) {
        Thread {
            try {
                val cookie = context.authPrefs.get<String>("cookie")
                val studentId = context.authPrefs.get<String>("student_id")
                if (cookie.isNullOrEmpty() || studentId.isNullOrBlank()) {
                    onDone?.invoke(false)
                    return@Thread
                }
                val api = GosuslugiApiService.create()
                val resp = if (completed) {
                    val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    val req = ToggleHomeworkRequest(
                        studentId = studentId,
                        homeworks = listOf(
                            ToggleHomeworkItem(
                                intervalId = intervalId,
                                homeworkId = homeworkId,
                                completeDatetime = nowStr
                            )
                        )
                    )
                    api.setHomeworkDone(cookie, req).execute()
                } else {
                    val req = ToggleHomeworkRequest(
                        studentId = studentId,
                        homeworks = listOf(
                            ToggleHomeworkItem(
                                intervalId = intervalId,
                                homeworkId = homeworkId
                            )
                        )
                    )
                    api.setHomeworkUndone(cookie, req).execute()
                }

                if (resp.code() == 401 || resp.code() == 403) {
                    AppLog.w("GosuslugiBridge", "toggleHomework got ${resp.code()}, refreshing session...")
                    refreshSessionSilently(context) { success ->
                        if (success) {
                            toggleHomework(context, homeworkId, intervalId, completed, onDone)
                        } else {
                            onDone?.invoke(false)
                        }
                    }
                    return@Thread
                }

                val success = resp.isSuccessful
                AppLog.d("GosuslugiBridge", "toggleHomework $homeworkId -> completed=$completed result=${resp.code()}")
                onDone?.invoke(success)
            } catch (e: Exception) {
                AppLog.e("GosuslugiBridge", "Error in toggleHomework", e)
                onDone?.invoke(false)
            }
        }.start()
    }

    fun applyMarks(marks: List<GMark>) {
        rawMarks = marks
        val quarterRanges = listOf(
            Triple("I четверть", "2026-09-01", "2026-10-24"),
            Triple("II четверть", "2026-11-04", "2026-12-30"),
            Triple("III четверть", "2027-01-11", "2027-03-26"),
            Triple("IV четверть", "2027-04-05", "2027-05-28")
        )

        val distinctMarks = marks.distinctBy { it.id ?: "${it.subjectName}_${it.date}_${it.value}" }
        val groupedBySubject = distinctMarks.groupBy { it.subjectName }
        val subjectItems = mutableListOf<MarkListSubjectItem>()

        groupedBySubject.forEach { (subjectName, allSubjectMarks) ->
            val periods = quarterRanges.map { (title, startDate, endDate) ->
                val qMarks = allSubjectMarks.filter {
                    val d = it.date
                    d in startDate..endDate
                }
                val numericValues = qMarks.mapNotNull { it.value?.toDoubleOrNull() }
                val avg = if (numericValues.isNotEmpty()) {
                    String.format(Locale.US, "%.2f", numericValues.average())
                } else "0.00"

                val subMarks = qMarks.map { m ->
                    SubjectMark(
                        comment = m.comment,
                        commentExists = m.comment != null,
                        controlFormName = m.workName ?: m.workType ?: "Обычная отметка",
                        createdAt = null,
                        criteria = null,
                        date = m.date,
                        id = getMarkId(m),
                        isExam = false,
                        isPoint = false,
                        originalGradeSystemType = "five",
                        pointDate = null,
                        updatedAt = null,
                        value = m.value ?: "5",
                        values = null,
                        weight = m.weight?.toIntOrNull() ?: 1
                    )
                }

                Period(
                    count = subMarks.size,
                    dynamic = "STABLE",
                    end = endDate,
                    endIso = endDate,
                    fixedValue = avg,
                    marks = subMarks,
                    start = startDate,
                    startIso = startDate,
                    target = null,
                    title = title,
                    value = avg
                )
            }

            val allNumeric = allSubjectMarks.mapNotNull { it.value?.toDoubleOrNull() }
            val totalAvg = if (allNumeric.isNotEmpty()) {
                String.format(Locale.US, "%.2f", allNumeric.average())
            } else "0.00"

            subjectItems.add(
                MarkListSubjectItem(
                    average = totalAvg,
                    averageByAll = totalAvg,
                    dynamic = "STABLE",
                    periods = periods,
                    subjectId = subjectName.hashCode().toLong(),
                    subjectName = subjectName,
                    yearMark = null
                )
            )
        }

        DataService.marksSubject = subjectItems
        DataService.hasMarksSubject = true

        val sortedMarks = distinctMarks.sortedByDescending { it.date }
        val dateMarks = sortedMarks.map { m ->
            val isoUpdated = "${m.date}T12:00:00"
            DateMark(
                comment = m.comment ?: "",
                commentExists = m.comment != null,
                controlFormName = m.workName ?: m.workType ?: "Обычная отметка",
                createdAt = isoUpdated,
                criteria = null,
                lessonDate = m.date,
                id = getMarkId(m),
                isExam = false,
                isPoint = false,
                originalGradeSystemType = "five",
                pointDate = null,
                subjectId = m.subjectName.hashCode().toLong(),
                subjectName = m.subjectName,
                updatedAt = isoUpdated,
                value = m.value ?: "5",
                values = null,
                weight = m.weight?.toIntOrNull() ?: 1
            )
        }
        DataService.marksDate = MarkListDate(dateMarks)
        DataService.hasMarksDate = true
    }
}