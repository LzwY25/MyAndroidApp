package com.lzwy.myreply.ui

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.lzwy.myreply.R
import com.lzwy.myreply.data.Message
import com.lzwy.myreply.ui.component.FullScreenImage
import com.lzwy.myreply.ui.component.MessageDetailAppBar
import com.lzwy.myreply.ui.component.ReplyDockedSearchBar
import com.lzwy.myreply.ui.component.ReplyMessageListItem
import com.lzwy.myreply.ui.component.writemessage.WriteMessage
import java.io.IOException

@Composable
fun ReplyInboxScreen(
    navController: NavHostController,
    replyHomeUIState: ReplyHomeUIState,
    closeDetailScreen: () -> Unit,
    navigateToDetail: (Long) -> Unit,
    navigateToWrite: (Boolean) -> Unit,
    finishWriting: (String, String, List<Uri>?, String?) -> Unit,
    toggleMessageSelection: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val messageLazyListState = rememberLazyListState()

    // TODO: Show top app bar over full width of app when in multi-select mode

    Box(modifier = modifier.fillMaxSize()) {
        ReplySinglePaneContent(
            navController = navController,
            replyHomeUIState = replyHomeUIState,
            toggleMessageSelection = toggleMessageSelection,
            messageLazyListState = messageLazyListState,
            modifier = Modifier.fillMaxSize(),
            closeDetailScreen = closeDetailScreen,
            navigateToWrite = navigateToWrite,
            finishWriting = finishWriting,
            navigateToDetail = navigateToDetail
        )

        if (!replyHomeUIState.isWriting) {
            FloatingActionButton(
                onClick = { navigateToWrite(true) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.compose),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ReplySinglePaneContent(
    navController: NavHostController,
    replyHomeUIState: ReplyHomeUIState,
    toggleMessageSelection: (Long) -> Unit,
    messageLazyListState: LazyListState,
    modifier: Modifier = Modifier,
    closeDetailScreen: () -> Unit,
    navigateToWrite: (Boolean) -> Unit,
    finishWriting: (String, String, List<Uri>?, String?) -> Unit,
    navigateToDetail: (Long) -> Unit
) {
    if (replyHomeUIState.openedMessage != null) {
        BackHandler {
            closeDetailScreen()
        }
        ReplyMessageDetail(message = replyHomeUIState.openedMessage) {
            closeDetailScreen()
        }
    } else if (replyHomeUIState.isWriting) {
        BackHandler {
            navigateToWrite(false)
        }
        WriteMessage(
            navController = navController,
            replyHomeUIState = replyHomeUIState,
            finishWriting = finishWriting)
    } else {
        ReplyMessageList(
            messages = replyHomeUIState.messages,
            openedMessage = replyHomeUIState.openedMessage,
            selectedMessageIds = replyHomeUIState.selectedMessages,
            toggleMessageSelection = toggleMessageSelection,
            messageLazyListState = messageLazyListState,
            modifier = modifier,
            navigateToDetail = navigateToDetail
        )
    }
}

@Composable
fun ReplyMessageList(
    messages: List<Message>,
    openedMessage: Message?,
    selectedMessageIds: Set<Long>,
    toggleMessageSelection: (Long) -> Unit,
    messageLazyListState: LazyListState,
    modifier: Modifier = Modifier,
    navigateToDetail: (Long) -> Unit
) {
    Box(modifier = modifier.windowInsetsPadding(WindowInsets.statusBars)) {
        ReplyDockedSearchBar(
            messages = messages,
            onSearchItemSelected = { searchedMessage ->
                navigateToDetail(searchedMessage.id)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        )

        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 80.dp),
            state = messageLazyListState
        ) {
            items(items = messages, key = { it.id }) { message ->
                ReplyMessageListItem(
                    message = message,
                    navigateToDetail = { messageId ->
                        navigateToDetail(messageId)
                    },
                    toggleSelection = toggleMessageSelection,
                    isOpened = openedMessage?.id == message.id,
                    isSelected = selectedMessageIds.contains(message.id)
                )
            }
            // Add extra spacing at the bottom if
            item {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
            }
        }
    }
}

@Composable
fun ReplyMessageDetail(
    message: Message,
    isFullScreen: Boolean = true,
    modifier: Modifier = Modifier.fillMaxSize(),
    onBackPressed: () -> Unit = {}
) {
    var activePhoto by remember {
        mutableStateOf<String?>(null)
    }

    val localContext = LocalContext.current

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var isPlayingPause by remember {
        mutableStateOf(false)
    }
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.inverseOnSurface)
            .padding(top = 16.dp)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .padding(bottom = 72.dp), // 留出底部空间以容纳 FloatingActionButton
        ) {
            MessageDetailAppBar(message, isFullScreen) {
                onBackPressed()
            }

            Text(
                modifier = Modifier
                    .padding(24.dp)
                    .defaultMinSize(minHeight = 200.dp),
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if(message.record != null) {
                Text(text = "Record",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline)

                Row {
                    val mediaPlayer = MediaPlayer()
                    Spacer(modifier = Modifier
                        .height(1.dp)
                        .width(12.dp))
                    Button(onClick = {
                        if(!isPlaying) {
                            isPlaying = true
                            if(!isPlayingPause) {
                                mediaPlayer.let {
                                    if (it.isPlaying) {
                                        it.stop()
                                        it.reset()
                                    }
                                }

                                try {
                                    mediaPlayer.apply {
                                        setDataSource(localContext, Uri.parse(message.record))
                                        prepare()
                                        start()

                                        setOnCompletionListener {
                                            // 播放完成后的操作，例如停止播放或进行其他处理
                                            Log.d("MediaPlayer", "Playback completed")
                                            stop()
                                            reset()
                                            isPlaying = false
                                            isPlayingPause = false
                                        }
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }
                            else {
                                mediaPlayer.start()
                            }
                        }
                        else {
                            isPlaying = false
                            isPlayingPause = true
                            mediaPlayer.apply {
                                if(this.isPlaying) {
                                    pause()
                                }
                            }
                        }
                    }) {
                        if(!isPlaying) {
                            Text(text = "Play")
                        }
                        else {
                            Text(text = "Pause")
                        }
                    }
                }
            }

            if(message.images != null) {
                Text(text = "Photos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline)

                val imageUri = message.images.split(",")
                Log.i("LZWY", "imageUri: ${imageUri.size}")
                LazyVerticalGrid(columns = GridCells.Fixed(3),
                    modifier = Modifier.height(222.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = imageUri) { item ->
                        Log.i("LZWY", "image: $item")
                        AsyncImage(
                            model = item,
                            modifier = Modifier
                                .width(100.dp)
                                .height(100.dp)
                                .clickable {
                                    activePhoto = item
                                },
                            contentDescription = "content image",
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        if (activePhoto != null) {
            FullScreenImage(photo = activePhoto!!, onDismiss = { activePhoto = null})
        }

    }
}