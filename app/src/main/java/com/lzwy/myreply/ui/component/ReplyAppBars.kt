package com.lzwy.myreply.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lzwy.myreply.R
import com.lzwy.myreply.data.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyDockedSearchBar(
    messages: List<Message>,
    onSearchItemSelected: (Message) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val searchResults = remember { mutableStateListOf<Message>() }
    val onExpandedChange: (Boolean) -> Unit = {
        expanded = it
    }

    LaunchedEffect(query) {
        searchResults.clear()
        if (query.isNotEmpty()) {
            searchResults.addAll(
                messages.filter {
                    it.title.startsWith(
                        prefix = query,
                        ignoreCase = true
                    ) || it.author.displayName.startsWith(
                        prefix =
                        query,
                        ignoreCase = true
                    )
                }
            )
        }
    }

    DockedSearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = {
                    query = it
                },
                onSearch = { expanded = false },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(id = R.string.search_emails)) },
                leadingIcon = {
                    if (expanded) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_button),
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .clickable {
                                    expanded = false
                                    query = ""
                                },
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(id = R.string.search),
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                },
                trailingIcon = {
                    ReplyProfileImage(
                        // TODO: current account's avatar?
                        drawableResource = R.drawable.avatar_6.toString(),
                        description = stringResource(id = R.string.profile),
                        modifier = Modifier
                            .padding(12.dp)
                            .size(32.dp)
                    )
                },
            )
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        content = {
            if (searchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = searchResults, key = { it.id }) { message ->
                        ListItem(
                            headlineContent = { Text(message.title) },
                            supportingContent = { Text(message.author.displayName) },
                            leadingContent = {
                                ReplyProfileImage(
                                    drawableResource = message.author.avatar,
                                    description = stringResource(id = R.string.profile),
                                    modifier = Modifier
                                        .size(32.dp)
                                )
                            },
                            modifier = Modifier.clickable {
                                onSearchItemSelected.invoke(message)
                                query = ""
                                expanded = false
                            }
                        )
                    }
                }
            } else if (query.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.no_item_found),
                    modifier = Modifier.padding(16.dp)
                )
            } else
                Text(
                    text = stringResource(id = R.string.no_search_history),
                    modifier = Modifier.padding(16.dp)
                )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailAppBar(
    message: Message,
    isFullScreen: Boolean,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.inverseOnSurface
        ),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (isFullScreen) Alignment.CenterHorizontally
                else Alignment.Start
            ) {
                Text(
                    text = message.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = message.author.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        navigationIcon = {
            if (isFullScreen) {
                FilledIconButton(
                    onClick = onBackPressed,
                    modifier = Modifier.padding(8.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back_button),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = { /*TODO*/ },
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.more_options_button),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAppBar(
    channels: List<String>,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    onChannelChanged: (String) -> Unit
) {
    var selectedChannel by remember { mutableStateOf(channels.first()) }
    var dropdownExpanded by remember {
        mutableStateOf(false)
    }

    TopAppBar(
        modifier = modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.inverseOnSurface
        ),
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.clickable { dropdownExpanded = true },
                    text = selectedChannel,
                    textAlign = TextAlign.Center
                )

                DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                    for (channel in channels) {
                        DropdownMenuItem(onClick = {
                            selectedChannel = channel
                            dropdownExpanded = false
                            onChannelChanged(channel)
                        }) {
                            Text(text = channel)
                        }
                    }
                }
            }
        },
        navigationIcon = {
            FilledIconButton(
                onClick = onBackPressed,
                modifier = Modifier.padding(8.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back_button),
                    modifier = Modifier.size(14.dp)
                )
            }
        },
        actions = {
            IconButton(
                onClick = { /*TODO*/ },
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.more_options_button),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}


