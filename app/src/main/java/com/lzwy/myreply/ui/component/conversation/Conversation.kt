package com.lzwy.myreply.ui.component.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lzwy.myreply.ui.ReplyHomeUIState
import com.lzwy.myreply.ui.component.ChatAppBar
import kotlinx.coroutines.flow.Flow

private val ChatBubbleShape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)

@Composable
fun Conversation(uiState: ReplyHomeUIState,
                 llmState: String,
                 modifier: Modifier = Modifier,
                 accessLLM: () -> Unit,
                 onBackPressed: () -> Unit,
                 onChannelChanged: (String) -> Unit
) {
    LaunchedEffect(key1 = null) {
        accessLLM()
    }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .padding(bottom = 72.dp)
    ) {
        ChatAppBar(channels = listOf("123", "456", "789"),
            onBackPressed = { onBackPressed() },
            onChannelChanged = { channel -> onChannelChanged(channel)})

        Text(
            modifier = Modifier
                .padding(24.dp),
            text = llmState,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
