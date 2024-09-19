package com.lzwy.myreply.ui.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp


@Composable
fun FunctionalityNotAvailablePopup(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(
                text = "Functionality not available \uD83D\uDE48",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "CLOSE")
            }
        }
    )
}

// for show ime & textField correctly when navigation bottom bar exists
fun Modifier.positionAwareImePadding() = composed {
    var consumePadding by remember { mutableStateOf(0) }
    onGloballyPositioned { coordinates ->
        val rootCoordinate = coordinates.findRootCoordinates()
        val bottom = coordinates.positionInWindow().y + coordinates.size.height

        consumePadding = (rootCoordinate.size.height - bottom).toInt()
    }
        .consumeWindowInsets(PaddingValues(bottom = (consumePadding / LocalDensity.current.density).dp))
        .imePadding()
}
