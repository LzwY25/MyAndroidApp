package com.lzwy.myreply

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.jvziyaoyao.scale.image.previewer.ImagePreviewer
import com.jvziyaoyao.scale.image.previewer.TransformImageView
import com.jvziyaoyao.scale.zoomable.previewer.TransformLayerScope
import com.jvziyaoyao.scale.zoomable.previewer.rememberPreviewerState
import com.lzwy.myreply.ui.ReplyApp
import com.lzwy.myreply.ui.ReplyViewModel
import com.lzwy.myreply.ui.theme.ContrastAwareReplyTheme
import kotlinx.coroutines.launch

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
                    navigateToWrite = { isOpened ->
                        viewModel.setWriting(isOpened)
                    },
                    finishWriting = { title, content, images, record ->
                        viewModel.finishWriting(this, title, content, images, record)
                    },
                    toggleMessageSelection = { messageId ->
                        viewModel.toggleSelectedEmail(messageId)
                    },
                    setLlmModel = { model ->
                        viewModel.setLlmModel(model)
                    },
                    chatWithLLM = { question ->
                        viewModel.chatWithLLM(question)
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

@Composable
fun test() {
    val scope = rememberCoroutineScope()
    val images = remember {
        mutableStateListOf(
            "https://t7.baidu.com/it/u=1595072465,3644073269&fm=193&f=GIF",
            "https://t7.baidu.com/it/u=4198287529,2774471735&fm=193&f=GIF",
        )
    }
    val state = rememberPreviewerState(pageCount = { images.size }) { images[it] }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            images.forEachIndexed { index, url ->
                TransformImageView(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable {
                            scope.launch {
                                state.enterTransform(index)
                            }
                        },
                    imageLoader = {
                        val painter = rememberAsyncImagePainter(model = url)
                        Triple(url, painter, painter.intrinsicSize)
                    },
                    transformState = state,
                )
            }
        }
    }

    ImagePreviewer(
        state = state,
        imageLoader = { page ->
            val painter = rememberAsyncImagePainter(model = images[page])
            Pair(painter, painter.intrinsicSize)
        }
    )
}