package ru.kot.sked.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kot.sked.R
import ru.kot.sked.network.NetworkService

@Composable
fun ErrorMessage(modifier: Modifier = Modifier, errorText: String) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Warning,
            stringResource(R.string.error_occurred),
            Modifier.size(64.dp),
            MaterialTheme.colorScheme.secondary
        )
        Text(
            stringResource(R.string.error_occurred),
            style = MaterialTheme.typography.titleLarge
        )
        val clipboardManager = LocalClipboardManager.current
        Text(errorText,
            Modifier
                .padding(horizontal = 16.dp)
                .clickable {
                    clipboardManager.setText(
                        AnnotatedString(errorText)
                    )
                }, textAlign = TextAlign.Center)
        val context = androidx.compose.ui.platform.LocalContext.current
        OutlinedButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(errorText))
                android.widget.Toast.makeText(context, "Текст ошибки скопирован", android.widget.Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.padding(16.dp),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                Icons.Rounded.ContentCopy,
                "Скопировать ошибку",
                Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Скопировать ошибку")
        }
    }
}