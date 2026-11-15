package ru.kot.sked.screens.navsections.profile

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import ru.kot.sked.DataService
import ru.kot.sked.Diary
import ru.kot.sked.R
import ru.kot.sked.authPrefs
import ru.kot.sked.baseEnqueueOrNull
import ru.kot.sked.get
import ru.kot.sked.launchPickerLive
import ru.kot.sked.launchUrlLive
import ru.kot.sked.modalBottomSheetContentLive
import ru.kot.sked.modalBottomSheetStateLive
import ru.kot.sked.modalDialogContentLive
import ru.kot.sked.modalDialogStateLive
import ru.kot.sked.models.profile.Children
import ru.kot.sked.network.NetworkService
import ru.kot.sked.save
import ru.kot.sked.screens.navsections.profile.meal.MealDialog
import java.io.File

val avatarTriggerLive = MutableLiveData(false)

@Composable
fun ProfileScreen2() {
    if (!DataService.hasProfile || DataService.profile.children.isEmpty()) return
    val child = DataService.profile.children[DataService.currentProfile.coerceIn(0, DataService.profile.children.lastIndex)]
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ShortProfileInfo(child)
        Cards()
    }
}

fun showAvatarDialog(context: Context) {
    modalDialogContentLive.value = {
        var loading by remember { mutableStateOf(false) }
        val avatarFile = remember { File(context.filesDir, "custom_avatar.png") }
        val hasLocalAvatar = context.authPrefs.get<Boolean>("has_custom_avatar") == true && avatarFile.exists()

        Column(Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.avatar),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AnimatedVisibility(loading) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            AnimatedVisibility(!loading) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedCard(
                        onClick = {
                            loading = true
                            launchPickerLive.value?.invoke()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.choose_from_gallery),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    if (hasLocalAvatar || DataService.avatars.isNotEmpty()) {
                        OutlinedCard(
                            onClick = {
                                loading = true
                                if (hasLocalAvatar) {
                                    try {
                                        avatarFile.delete()
                                    } catch (_: Exception) {}
                                    context.authPrefs.save("has_custom_avatar" to false)
                                    modalDialogStateLive.postValue(false)
                                    avatarTriggerLive.value = avatarTriggerLive.value?.not() ?: true
                                } else {
                                    DataService.run {
                                        secondaryApi.deleteAvatar(
                                            "Bearer $token",
                                            profile.children[currentProfile].contingentGuid,
                                            avatars.first().id.toString()
                                        ).baseEnqueueOrNull {
                                            updateAvatars {
                                                modalDialogStateLive.postValue(false)
                                                avatarTriggerLive.value =
                                                    avatarTriggerLive.value?.not()
                                                        ?: true
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.delete),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { modalDialogStateLive.postValue(false) }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
    modalDialogStateLive.postValue(true)
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ShortProfileInfo(child: Children) {
    val context = LocalContext.current
    val trigger = avatarTriggerLive.observeAsState()
    val avatarFile = remember { File(context.filesDir, "custom_avatar.png") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padding(end = 16.dp)) {
                val onAvatarClick = {
                    showAvatarDialog(context)
                }
                AnimatedContent(trigger.value, label = "avatar_anim") {
                    val localBitmap = remember(it) {
                        if (avatarFile.exists()) {
                            try {
                                BitmapFactory.decodeFile(avatarFile.absolutePath)
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                    }

                    if (localBitmap != null) {
                        Image(
                            bitmap = localBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.avatar),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onAvatarClick() }
                                .size(64.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else if (DataService.avatars.isNotEmpty()) {
                        GlideImage(
                            DataService.avatars.first().url.let { string -> Uri.parse(string) },
                            stringResource(R.string.avatar),
                            Modifier
                                .clip(CircleShape)
                                .clickable { onAvatarClick() }
                                .size(64.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { onAvatarClick() }
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = child.firstName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            Column {
                Text(
                    child.run { "$lastName $firstName $middleName" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    child.school.shortName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.class_t, child.className),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Cards() {
    Column(
        Modifier
            .padding(16.dp)
            .clip(MaterialTheme.shapes.large),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ProfileCard(R.string.personal_data, Icons.Rounded.Person) { PersonalData() }
        ProfileCard(R.string.school_and_teachers, Icons.Rounded.School) { School() }
    }
}

private fun mealOnClick() {
    if (DataService.subsystem == Diary.MES) {
        modalDialogContentLive.value = { MealDialog() }
        modalDialogStateLive.postValue(true)
    } else {
        launchUrlLive.postValue(Uri.parse(NetworkService.MySchoolAPIConfig.FOOD_URI))
    }
}

@Composable
private fun ProfileCard(
    @StringRes textRes: Int,
    icon: ImageVector,
    bottomSheetContent: @Composable () -> Unit,
) {
    ProfileCard(stringResource(textRes), icon, bottomSheetContent)
}

@Composable
private fun ProfileCard(
    @StringRes textRes: Int,
    icon: ImageVector,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
) {
    ProfileCard(stringResource(textRes), icon, onLongClick, onClick)
}

@Composable
private fun ProfileCard(
    text: String,
    icon: ImageVector,
    bottomSheetContent: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraSmall
            )
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable { openBottomSheet { bottomSheetContent() } }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.padding(end = 8.dp), MaterialTheme.colorScheme.onSurface)
        Text(text)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileCard(
    text: String,
    icon: ImageVector,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraSmall
            )
            .clip(MaterialTheme.shapes.extraSmall)
            .combinedClickable(onLongClick = onLongClick) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.padding(end = 8.dp), MaterialTheme.colorScheme.onSurface)
        Text(text)
    }
}

private fun isExamsNotEmpty(): Boolean = DataService.govExams.data.isNotEmpty()

private fun openBottomSheet(content: @Composable () -> Unit) {
    modalBottomSheetStateLive.postValue(true)
    modalBottomSheetContentLive.postValue(content)
}