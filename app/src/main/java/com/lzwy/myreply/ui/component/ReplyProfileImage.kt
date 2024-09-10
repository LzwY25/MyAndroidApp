package com.lzwy.myreply.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ReplyProfileImage(
    drawableResource: String,
    description: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = drawableResource,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape),
        contentDescription = description,
    )
}
