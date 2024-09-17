package com.lzwy.myreply.ui.component.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lzwy.myreply.data.llm.Model
import com.lzwy.myreply.ui.component.ChatAppBar

private val ChatBubbleShape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)

@Composable
fun Conversation(llmState: String,
                 chatWithLLM: (String, Model) -> Unit,
                 onBackPressed: () -> Unit,
                 onChannelChanged: (String) -> Unit
) {
    var isAsking by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = null) {
        // chatWithLLM("你好", "")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 72.dp)
    ) {
        ChatAppBar(channels = listOf("123", "456", "789"),
            onBackPressed = { onBackPressed() },
            onChannelChanged = { channel -> onChannelChanged(channel)})


//        LazyColumn {
//            items()
//        }

        Text(text = llmState)

        Button(onClick = {
            isAsking = true
            chatWithLLM("你好", Model.BearOne)
        }) { }
    }
}
