package com.lzwy.myreply.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lzwy.myreply.R
import com.lzwy.myreply.data.Message
import com.lzwy.myreply.ui.component.MessageDetailAppBar
import com.lzwy.myreply.ui.component.ReplyDockedSearchBar
import com.lzwy.myreply.ui.component.ReplyMessageItem
import com.lzwy.myreply.ui.component.ReplyMessageListItem
import com.lzwy.myreply.ui.utils.ReplyContentType

@Composable
fun ReplyInboxScreen(
    replyHomeUIState: ReplyHomeUIState,
    closeDetailScreen: () -> Unit,
    navigateToDetail: (Long, ReplyContentType) -> Unit,
    toggleMessageSelection: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val messageLazyListState = rememberLazyListState()

    // TODO: Show top app bar over full width of app when in multi-select mode

    Box(modifier = modifier.fillMaxSize()) {
        ReplySinglePaneContent(
            replyHomeUIState = replyHomeUIState,
            toggleMessageSelection = toggleMessageSelection,
            messageLazyListState = messageLazyListState,
            modifier = Modifier.fillMaxSize(),
            closeDetailScreen = closeDetailScreen,
            navigateToDetail = navigateToDetail
        )

        LargeFloatingActionButton(
            onClick = { /*TODO*/ },
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

@Composable
fun ReplySinglePaneContent(
    replyHomeUIState: ReplyHomeUIState,
    toggleMessageSelection: (Long) -> Unit,
    messageLazyListState: LazyListState,
    modifier: Modifier = Modifier,
    closeDetailScreen: () -> Unit,
    navigateToDetail: (Long, ReplyContentType) -> Unit
) {
    if (replyHomeUIState.openedMessage != null) {
        BackHandler {
            closeDetailScreen()
        }
        ReplyMessageDetail(message = replyHomeUIState.openedMessage) {
            closeDetailScreen()
        }
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
    navigateToDetail: (Long, ReplyContentType) -> Unit
) {
    Box(modifier = modifier.windowInsetsPadding(WindowInsets.statusBars)) {
        ReplyDockedSearchBar(
            messages = messages,
            onSearchItemSelected = { searchedMessage ->
                navigateToDetail(searchedMessage.id, ReplyContentType.SINGLE_PANE)
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
                        navigateToDetail(messageId, ReplyContentType.SINGLE_PANE)
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
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = true,
    onBackPressed: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colorScheme.inverseOnSurface)
    ) {
        item {
            MessageDetailAppBar(message, isFullScreen) {
                onBackPressed()
            }
        }
        item {
            ReplyMessageItem(message = message)
        }
        item {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}
