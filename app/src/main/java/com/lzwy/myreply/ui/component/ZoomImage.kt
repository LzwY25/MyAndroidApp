package com.lzwy.myreply.ui.component

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import com.lzwy.myreply.R
import kotlin.math.roundToInt

@Composable
fun FullScreenImage(
    photo: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Scrim(onDismiss, Modifier.fillMaxSize())
        ImageWithZoom(photo, modifier = Modifier, onDismiss)
    }
}

@Composable
fun ImageWithZoom(photo: String, modifier: Modifier, onClose: () -> Unit) {
    var zoomed by remember { mutableStateOf(false) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }

    val transformableStale = rememberTransformableState {
            zoomChange: Float, panChange: Offset, _: Float ->
        scale *= zoomChange
        Log.i("LZWY", "zoomOffset: $zoomOffset, panChange: $panChange, afterMultiple: ${panChange * scale}")
        zoomOffset += panChange
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(model = photo,
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onClose()
                        },
                        onDoubleTap = {
                            zoomOffset = Offset.Zero
                            scale = 1f
                        }
                    )
                }
                .scale(scale)
                .offset {
                    IntOffset(zoomOffset.x.roundToInt(), zoomOffset.y.roundToInt())
                }
                .transformable(state = transformableStale,
                    lockRotationOnZoomPan = true),
            filterQuality = FilterQuality.High,
            contentDescription = "ImageWithZoom")
    }
}

@Composable
fun ImageWithZoom(photo: Uri, modifier: Modifier, onClose: () -> Unit) {
    var zoomed by remember { mutableStateOf(false) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }

    val transformableStale = rememberTransformableState {
            zoomChange: Float, panChange: Offset, _: Float ->
        scale *= zoomChange
        Log.i("LZWY", "zoomOffset: $zoomOffset, panChange: $panChange, afterMultiple: ${panChange * scale}")
        zoomOffset += panChange
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(model = photo,
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onClose()
                        },
                        onDoubleTap = {
                            zoomOffset = Offset.Zero
                            scale = 1f
                        }
                    )
                }
                .scale(scale)
                .offset {
                    IntOffset(zoomOffset.x.roundToInt(), zoomOffset.y.roundToInt())
                }
                .transformable(state = transformableStale,
                    lockRotationOnZoomPan = true),
            filterQuality = FilterQuality.High,
            contentDescription = "ImageWithZoom")
    }
}

@Composable
fun Scrim(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val strClose = stringResource(R.string.close)
    Box(
        modifier
            // handle pointer input
            // [START android_compose_touchinput_pointerinput_scrim_highlight]
            .pointerInput(onClose) { detectTapGestures { onClose() } }
            // [END android_compose_touchinput_pointerinput_scrim_highlight]
            // handle accessibility services
            .semantics(mergeDescendants = true) {
                contentDescription = strClose
                onClick {
                    onClose()
                    true
                }
            }
            // handle physical keyboard input
            .onKeyEvent {
                if (it.key == Key.Escape) {
                    onClose()
                    true
                } else {
                    false
                }
            }
            // draw scrim
            .background(Color.DarkGray.copy(alpha = 0.75f))
    )
}