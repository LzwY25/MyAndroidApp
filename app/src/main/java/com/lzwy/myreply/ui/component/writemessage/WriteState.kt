package com.lzwy.myreply.ui.component.writemessage

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue

const val RECORD_EMPTY = -1
const val RECORD_RESUME = 0
const val RECORD_PAUSE = 1
const val RECORD_FINISH = 2

const val PLAY_EMPTY = -1
const val PLAY_RESUME = 0
const val PLAY_PAUSE = 1

data class WriteState(
    var title: TextFieldValue = TextFieldValue(),
    var content: TextFieldValue = TextFieldValue(),

    var isFirstTime: Boolean = true,   // for check Permission

    var imageUri: List<Uri> = emptyList(),
    var hasImage:Boolean = false,

    var isRecording: Boolean = false,
    var isRecordingPause: Boolean = false,
    var isRecorded: Boolean = false,
    var recordState: Int = -1,
    var recordFileName: String = "",
    var recordDurationMillis: Long = 0L,

    var isPlaying: Boolean = false,
    var isPlayingPause: Boolean = false,
    var playState: Int = -1,
)