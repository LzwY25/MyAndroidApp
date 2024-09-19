package com.lzwy.myreply

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lzwy.myreply.ui.ReplyApp
import com.lzwy.myreply.ui.ReplyViewModel
import com.lzwy.myreply.ui.theme.ContrastAwareReplyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ReplyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContrastAwareReplyTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                val conversationState by viewModel.conversationState.collectAsStateWithLifecycle()
                val llmLastReply by viewModel.llmLastReply.collectAsStateWithLifecycle()
                ReplyApp(
                    replyHomeUIState = uiState,
                    llmLastReply = llmLastReply,
                    conversationState = conversationState,
                    closeDetailScreen = {
                        viewModel.closeDetailScreen()
                    },
                    navigateToDetail = { messageId ->
                        viewModel.setOpenedEmail(messageId)
                    },
                    navigateToWrite = {
                        viewModel.setWriting()
                    },
                    toggleMessageSelection = { messageId ->
                        viewModel.toggleSelectedEmail(messageId)
                    },
                    setLlmModel = { model ->
                        viewModel.setLlmModel(model)
                    },
                    chatWithLLM = { question, model ->
                        viewModel.chatWithLLM(question, model)
                    }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ContrastAwareReplyTheme {
        Greeting("Android")
    }
}