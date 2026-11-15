package ru.kot.sked.components

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kot.sked.*
import java.util.Calendar
import kotlin.math.sin
import kotlin.random.Random

object BirthdayHelper {

    var confettiShownThisSession: Boolean = false

    fun isTodayBirthday(context: Context): Boolean {
        if (context.mainPrefs.get<Boolean>("force_birthday") == true) return true

        val birthDateStr = context.authPrefs.get<String>("student_birth_date")
            ?: try { DataService.profile.children.firstOrNull()?.birthDate } catch (_: Exception) { null }
            ?: return false

        val parts = if (birthDateStr.contains("-")) {
            birthDateStr.split("-")
        } else if (birthDateStr.contains(".")) {
            val p = birthDateStr.split(".")
            if (p.size == 3) listOf(p[2], p[1], p[0]) else emptyList()
        } else emptyList()

        if (parts.size >= 3) {
            val bMonth = parts[1].toIntOrNull() ?: return false
            val bDay = parts[2].toIntOrNull() ?: return false

            val cal = Calendar.getInstance()
            val curMonth = cal.get(Calendar.MONTH) + 1
            val curDay = cal.get(Calendar.DAY_OF_MONTH)

            return curMonth == bMonth && curDay == bDay
        }
        return false
    }

    fun getStudentFirstName(context: Context): String {
        return context.authPrefs.get<String>("student_first_name")
            ?: try { DataService.profile.children.firstOrNull()?.firstName } catch (_: Exception) { null }
            ?: ""
    }
}

@Composable
fun BirthdayCard(context: Context, onConfetti: () -> Unit = {}) {
    val name = remember { BirthdayHelper.getStudentFirstName(context) }
    val greetingName = if (name.isNotBlank()) ", $name" else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onConfetti() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "🎂",
                        fontSize = 28.sp
                    )
                    Text(
                        text = "С днём рождения$greetingName!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onConfetti() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎉", fontSize = 18.sp)
                    }
                }
            }

            Text(
                text = "Желаем лёгких уроков, только отличных отметок и чтобы все контрольные сдавались на изи! 🚀",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )
        }
    }
}

private class ConfettiParticle(
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val vRotation: Float,
    val wobbleFreq: Float,
    val wobbleAmp: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val isCircle: Boolean
)

@Composable
fun ConfettiOverlay() {
    val triggerTime by triggerBirthdayConfettiLive.observeAsState(0L)
    var isVisible by remember { mutableStateOf(false) }
    val anim = remember { Animatable(0f) }

    LaunchedEffect(triggerTime) {
        if (triggerTime > 0L) {
            isVisible = true
            anim.snapTo(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3500, easing = LinearEasing)
            )
            isVisible = false
        }
    }

    if (!isVisible) return

    val colors = remember {
        listOf(
            Color(0xFFFF5252),
            Color(0xFFFF4081),
            Color(0xFF7C4DFF),
            Color(0xFF536DFE),
            Color(0xFF40C4FF),
            Color(0xFF00E676),
            Color(0xFFFFD740),
            Color(0xFFFF6E40)
        )
    }

    val particles = remember(triggerTime) {
        List(160) {
            val isStreamer = Random.nextFloat() < 0.25f
            val isCircle = !isStreamer && Random.nextBoolean()
            val w = if (isStreamer) Random.nextFloat() * 4f + 6f else Random.nextFloat() * 10f + 12f
            val h = if (isStreamer) Random.nextFloat() * 12f + 20f else Random.nextFloat() * 8f + 8f
            ConfettiParticle(
                startX = Random.nextFloat() * 1000f,
                startY = -Random.nextFloat() * 550f - 20f,
                vx = (Random.nextFloat() - 0.5f) * 4.5f,
                vy = Random.nextFloat() * 3f + 3.2f,
                rotation = Random.nextFloat() * 360f,
                vRotation = (Random.nextFloat() - 0.5f) * 16f,
                wobbleFreq = Random.nextFloat() * 0.08f + 0.04f,
                wobbleAmp = Random.nextFloat() * 18f + 8f,
                width = w,
                height = h,
                color = colors[Random.nextInt(colors.size)],
                isCircle = isCircle
            )
        }
    }

    val progress = anim.value
    val timeFrames = progress * (3500f / 16.6f)
    val alpha = if (progress > 0.75f) ((1f - progress) / 0.25f).coerceIn(0f, 1f) else 1f

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val scaleX = size.width / 1000f
        particles.forEach { p ->
            val drawX = (p.startX + p.vx * timeFrames + sin(timeFrames * p.wobbleFreq) * p.wobbleAmp) * scaleX
            val drawY = p.startY + p.vy * timeFrames + 0.5f * 0.16f * timeFrames * timeFrames
            val drawRot = p.rotation + p.vRotation * timeFrames

            if (drawY in -60f..size.height + 60f && drawX in -60f..size.width + 60f) {
                rotate(drawRot, pivot = Offset(drawX, drawY)) {
                    val pColor = p.color.copy(alpha = alpha)
                    if (p.isCircle) {
                        drawCircle(
                            color = pColor,
                            radius = p.width / 2f,
                            center = Offset(drawX, drawY)
                        )
                    } else {
                        drawRect(
                            color = pColor,
                            topLeft = Offset(drawX - p.width / 2f, drawY - p.height / 2f),
                            size = Size(p.width, p.height)
                        )
                    }
                }
            }
        }
    }
}
