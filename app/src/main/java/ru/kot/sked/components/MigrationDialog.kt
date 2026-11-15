package ru.kot.sked.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kot.sked.R

@Composable
fun MigrationDialog(close: () -> Unit) {
    AlertDialog(onDismissRequest = close, confirmButton = {
        TextButton(onClick = close) {
            Text(stringResource(R.string.log_out))
        }
    }, title = {
        Text(stringResource(id = R.string.migration))
    }, text = {
        Text(stringResource(id = R.string.migration_description))
    })
}