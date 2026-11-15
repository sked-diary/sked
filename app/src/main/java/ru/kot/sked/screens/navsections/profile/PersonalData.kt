package ru.kot.sked.screens.navsections.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import ru.kot.sked.DataService
import ru.kot.sked.R
import ru.kot.sked.formatToHumanDate
import ru.kot.sked.parseFromDay
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PersonalData() {
    Column(
        Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        val clipboardManager = LocalClipboardManager.current
        with(DataService.profile.children[DataService.currentProfile]) {
            Text("$lastName $firstName $middleName", style = MaterialTheme.typography.titleMedium)
            val parsedDate = if (birthDate.contains("-")) {
                try { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(birthDate) } catch (e: Exception) { null }
            } else if (birthDate.contains(".")) {
                try { SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).parse(birthDate) } catch (e: Exception) { null }
            } else null
            val formattedBirth = parsedDate?.let {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(it)
            } ?: birthDate
            Text(
                stringResource(R.string.birth_date_t, formattedBirth)
            )
            Row {
                Text(stringResource(R.string.snils_t))
                var snilsShown by remember { mutableStateOf(false) }
                var snilsCopied by remember { mutableStateOf(false) }
                AnimatedVisibility(!snilsShown) {
                    Text(stringResource(R.string.show), Modifier.clickable {
                        if (!snilsCopied) {
                            clipboardManager.setText(AnnotatedString(snils))
                            snilsCopied = true
                        }
                        snilsShown = true
                    }, color = MaterialTheme.colorScheme.secondary)
                }
                AnimatedVisibility(snilsShown) {
                    Text(snils, Modifier.clickable {
                        snilsShown = false
                    })
                }
            }
        }
    }
}