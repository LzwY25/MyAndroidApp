package com.lzwy.myreply.ui.component.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lzwy.myreply.R
import com.lzwy.myreply.data.llm.Model
import com.lzwy.myreply.ui.ConversationState
import com.lzwy.myreply.ui.component.ChatAppBar
import com.lzwy.myreply.ui.utils.positionAwareImePadding

private val ChatBubbleShape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 20.dp)

@Composable
fun Conversation(conversationState: ConversationState,
                 llmLastReply: String,
                 setLlmModel: (String) -> Unit,
                 chatWithLLM: (String, Model) -> Unit,
                 onBackPressed: () -> Unit
) {
    var question by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = llmLastReply) {
        if (llmLastReply.isNotEmpty()) {
            listState.animateScrollToItem(conversationState.historyList.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ChatAppBar(channels = Model.entries.map { it.displayName },
            onBackPressed = { onBackPressed() },
            onChannelChanged = { channel -> setLlmModel(channel)})

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (conversationState.historyList.isNotEmpty()) {
                items(conversationState.historyList) { item ->
                    ChatItemBubble(
                        message = item.content,
                        isUserMe = item.isUser)
                }
            }

            if (conversationState.isAsking) {
                item {
                    ChatItemBubble(message = llmLastReply, isUserMe = false)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .fillMaxWidth()
                .positionAwareImePadding()
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = question,
                onValueChange = { question = it },
                label = { Text(text = "Ask me!")}
            )

            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                painter = painterResource(id = R.drawable.add_more),
                contentDescription = "Send Button",
                modifier = Modifier.clickable {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    chatWithLLM(question, Model.BearOne)
                    question = ""
                })
        }
    }
}

@Composable
fun ChatItemBubble(
    message: String,
    isUserMe: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isUserMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = if (isUserMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = backgroundColor,
            shape = ChatBubbleShape
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium.copy(color = LocalContentColor.current)
            )
        }
    }
}