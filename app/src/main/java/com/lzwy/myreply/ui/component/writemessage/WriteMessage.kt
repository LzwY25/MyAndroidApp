package com.lzwy.myreply.ui.component.writemessage

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.MaterialTheme as OldMaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.lzwy.myreply.R
import com.lzwy.myreply.ui.ReplyHomeUIState
import com.lzwy.myreply.ui.imageViewer.previewer.ImagePreviewer
import com.lzwy.myreply.ui.imageViewer.previewer.TransformImageView
import com.lzwy.myreply.ui.imageViewer.previewer.VerticalDragType
import com.lzwy.myreply.ui.imageViewer.previewer.rememberPreviewerState
import com.lzwy.myreply.ui.imageViewer.previewer.rememberTransformItemState
import com.lzwy.myreply.ui.imageViewer.util.DetectScaleGridGesture
import com.lzwy.myreply.ui.imageViewer.util.ScaleGrid
import com.lzwy.myreply.ui.imageViewer.util.rememberCoilImagePainter
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyGridState
import org.burnoutcrew.reorderable.reorderable
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "WriteMessage"

@Composable
fun WriteMessage(
    navController: NavHostController,
    replyHomeUIState: ReplyHomeUIState,
    finishWriting: (String, String, List<Uri>, String) -> Unit
) {
    var title by remember { mutableStateOf(TextFieldValue()) }
    var content by remember { mutableStateOf(TextFieldValue()) }
    var isFirstTime by remember { mutableStateOf(true) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission Accepted: Do something
            Log.d(TAG,"PERMISSION GRANTED")

        } else {
            // Permission Denied: Do something
            Log.d(TAG,"PERMISSION DENIED")
        }
    }
    var recordingDurationMillis by remember { mutableLongStateOf(0L) }
    var recordingTimer: CountDownTimer? = null

    val localContext = LocalContext.current
    var mediaRecorder: MediaRecorder? = null
    var recordFileName: String = ""

    var imageUri by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }
    var hasImage by remember {
        mutableStateOf(false)
    }

    var recordStatus by remember {
        mutableStateOf(RECORDSTATUS.IDLE)
    }

    var isRecording by remember {
        mutableStateOf(false)
    }
    var isRecordingPause by remember {
        mutableStateOf(false)
    }
    var isRecorded by remember {
        mutableStateOf(false)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }
    var isPlayingPause by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()
    val settingState = rememberSettingState()
    val previewerState = rememberPreviewerState(
        animationSpec = tween(settingState.animationDuration),
        verticalDragType = settingState.verticalDrag,
        pageCount = { imageUri.size }
    ) {
        it
    }

    if (previewerState.canClose || previewerState.animating) BackHandler {
        if (previewerState.canClose) scope.launch {
            if (settingState.transformExit) {
                previewerState.closeTransform()
            } else {
                previewerState.close()
            }
        }
    }

    LaunchedEffect(key1 = replyHomeUIState.isWriting, block = {
        if(!isFirstTime && !replyHomeUIState.isWriting) {
            Log.i("LZWY", "not writing, pop back!")
            navController.popBackStack()
        }
        if(isFirstTime) {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(
                    localContext,
                    Manifest.permission.RECORD_AUDIO
                ) -> {
                    // Some works that require permission
                    Log.d(TAG,"Code requires permission")
                }
                else -> {
                    // Asking for permission
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
        isFirstTime = false
    })

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uri ->
        Log.i(TAG, "get image: $uri")
        hasImage = ((uri as List<*>).isNotEmpty())
        imageUri = run {
            val tmp: MutableList<Uri> = mutableListOf()
            tmp.addAll(imageUri)
            tmp.addAll(uri)
            tmp.toList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .padding(bottom = 72.dp), // 留出底部空间以容纳 FloatingActionButton
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("文章内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 300.dp)
                        .padding(8.dp)
                )

            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(text = "Record",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline)
            }

            item {
                Row {
                    if(!isRecorded) {
                        Button(
                            onClick = {
                                isRecording = !isRecording
                                if(isRecording) {
                                    if(!isRecordingPause) {
                                        Log.i("MediaRecorder", "recording start")
                                        recordingTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
                                            override fun onTick(millisUntilFinished: Long) {
                                                recordingDurationMillis += 1
                                            }

                                            override fun onFinish() {
                                                recordingDurationMillis = 0
                                            }
                                        }
                                        recordingTimer?.start()
                                        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
                                            Date()
                                        )
                                        val recordingDir = localContext.getExternalFilesDir(
                                            Environment.DIRECTORY_MUSIC + "/MyRecordings")

                                        if (recordingDir != null && !recordingDir.exists()) {
                                            recordingDir.mkdirs()
                                        }
                                        val fileName = "${recordingDir?.absolutePath}/recording_$timeStamp.3gp"
                                        Log.i("MediarRecorder", "save record at: $fileName")
                                        recordFileName = fileName
                                        mediaRecorder = MediaRecorder(localContext)
                                        mediaRecorder?.apply {
                                            setAudioSource(MediaRecorder.AudioSource.MIC)
                                            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                                            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                            setOutputFile(fileName)
                                        }
                                        try {
                                            mediaRecorder?.prepare()
                                            mediaRecorder?.start()
                                        } catch (e: IOException) {
                                            Log.e(TAG, e.toString())
                                        }
                                    }
                                    else {
                                        Log.i("MediaRecorder", "recording resume")
                                        recordingTimer?.start()
                                        mediaRecorder?.resume()
                                        isRecordingPause = false
                                    }
                                }
                                else {
                                    Log.i("MediaRecorder", "recording pause")
                                    recordingTimer?.cancel()
                                    mediaRecorder?.pause()
                                    isRecordingPause = true
                                }
                            }
                        ) {
                            if(!isRecording) {
                                Text(text = "Record")
                            }
                            else {
                                Text(text = "Pause")
                            }
                        }
                    }

                    Spacer(modifier = Modifier
                        .height(1.dp)
                        .width(4.dp))

                    if(isRecording) {
                        Button(onClick = {
                            Log.i("MediaRecorder", "recording clear")
                            recordingTimer?.onFinish()
                            recordingTimer?.cancel()
                            recordingTimer = null
                            mediaRecorder?.stop()
                            mediaRecorder?.release()
                            mediaRecorder = null
                            isRecording = false
                        }) {
                            Text(text = "Clear")
                        }

                        Button(onClick = {
                            Log.i("MediaRecorder", "recording save")
                            recordingTimer?.onFinish()
                            recordingTimer?.cancel()
                            recordingTimer = null
                            mediaRecorder?.stop()
                            mediaRecorder?.release()
                            mediaRecorder = null
                            isRecorded = true
                            isRecording = false
                        }) {
                            Text(text = "Save")
                        }
                    }


                    if(!isRecorded && recordingDurationMillis > 0) {
                        Spacer(modifier = Modifier
                            .height(1.dp)
                            .width(4.dp))
                        Text(text = "Duration: $recordingDurationMillis s")
                    }
                    if(isRecorded) {
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
                                            setDataSource(localContext, Uri.parse(recordFileName))
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
                        Spacer(modifier = Modifier
                            .height(1.dp)
                            .width(4.dp))
                        Button(onClick = {
                            if(mediaPlayer.isPlaying) {
                                stopPlayback(mediaPlayer)
                            }
                            isRecorded = false
                            isPlaying = false
                            isPlayingPause = false
                            recordFileName = ""
                        }) {
                            Text(text = "Delete")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(text = "Photos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline)
            }


            item {
                val state = rememberReorderableLazyGridState(onMove = { from, to ->
                    imageUri.apply {
                        imageUri = imageUri.toMutableList().apply {
                            Log.i("LZWY", "switch: ${from.index} and ${to.index}")
                            add(to.index, removeAt(from.index))
                            toList()
                        }
                    }
                })
                val lineCount = 3
                LazyVerticalGrid(columns = GridCells.Fixed(3),
                    state = state.gridState,
                    modifier = Modifier
                        .height(222.dp)
                        .reorderable(state)
                        .detectReorderAfterLongPress(state),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = {

                        imageUri.forEachIndexed { index, item ->
                            item(key = index) {
                                val needStart = index % lineCount != 0
                                val painter = rememberAsyncImagePainter(model = item)
                                val itemState = rememberTransformItemState()
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1F)
                                Box(
                                    modifier = Modifier
                                        .animateItem(
                                            fadeInSpec = null,
                                            fadeOutSpec = null
                                        )
                                        .padding(
                                            start = if (needStart) 2.dp else 0.dp,
                                            bottom = 2.dp
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ScaleGrid(
                                        detectGesture = DetectScaleGridGesture(
                                            onPress = {
                                                scope.launch {
                                                    Log.i("LZWY", "onPress: $index: $item")
                                                    if (settingState.transformEnter) {
                                                        previewerState.openTransform(
                                                            index = index,
                                                            itemState = itemState,
                                                        )
                                                    } else {
                                                        previewerState.open(index)
                                                    }

                                                }
                                            }
                                        )
                                    ) {
                                        TransformImageView(
                                            painter = painter,
                                            key = index,
                                            itemState = itemState,
                                            previewerState = previewerState,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    OldMaterialTheme.colors.onBackground.copy(
                                                        0.4F
                                                    )
                                                )
                                                .padding(vertical = 4.dp)
                                                .clickable {
                                                    val tmp: MutableList<Uri> =
                                                        imageUri.toMutableList()
                                                    tmp.removeAt(index)
                                                    imageUri = tmp.toList()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                modifier = Modifier.size(16.dp),
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = null,
                                                tint = OldMaterialTheme.colors.surface.copy(0.6F)
                                            )
                                        }
                                    }
                                }
                            }

                        }

                        if(imageUri.size < 9) {
                            item {
                                AsyncImage(model = R.drawable.add_more,
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(100.dp)
                                        .padding(8.dp)
                                        .clickable(onClick = {
                                            imagePicker.launch("image/*")
                                        }),
                                    contentDescription = "add button")
                            }
                        }
                    })
            }
        }

        ImagePreviewer(
            modifier = Modifier.fillMaxSize(),
            state = previewerState,
            imageLoader = { index ->
                if (settingState.loaderError && (index % 2 == 0)) {
                    null
                } else {
                    val image = imageUri[index]
                    rememberCoilImagePainter(image = image)
                }
            },
            previewerLayer = {
                foreground = { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp, end = 16.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    androidx.compose.material.MaterialTheme.colors.surface.copy(
                                        0.2F
                                    )
                                )
                                .clickable {
                                    val tmp: MutableList<Uri> = imageUri.toMutableList()
                                    tmp.removeAt(index)
                                    imageUri = tmp.toList()
                                }
                                .padding(16.dp)
                        ) {
                            androidx.compose.material.Icon(
                                modifier = Modifier.size(22.dp),
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = androidx.compose.material.MaterialTheme.colors.surface
                            )
                        }
                    }
                }
            }
        )

        if(!previewerState.visible) {
            FloatingActionButton(
                onClick = {
                    Log.i(TAG, "Floating button clicked!")
                    if(!previewerState.visible) {
                        // double check
                        finishWriting(title.text, content.text,
                            imageUri, recordFileName)
                    }
                },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.edit),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

fun stopPlayback(mediaPlayer: MediaPlayer) {
    mediaPlayer.let {
        if (it.isPlaying) {
            it.stop()
            it.reset()
        }
    }
}

@Composable
fun rememberSettingState(): TransformSettingState {
    return rememberSaveable(saver = TransformSettingState.Saver) {
        TransformSettingState()
    }
}

val VERTICAL_DRAG_ENABLE = VerticalDragType.UpAndDown
val VERTICAL_DRAG_DISABLE = VerticalDragType.None
val DEFAULT_VERTICAL_DRAG = VERTICAL_DRAG_ENABLE
const val DEFAULT_LOADER_ERROR = false
const val DEFAULT_TRANSFORM_ENTER = true
const val DEFAULT_TRANSFORM_EXIT = true
const val DEFAULT_ANIMATION_DURATION = 400
const val DEFAULT_DATA_REPEAT = 1
class TransformSettingState {

    var loaderError by mutableStateOf(DEFAULT_LOADER_ERROR)

    var verticalDrag by mutableStateOf(DEFAULT_VERTICAL_DRAG)

    var transformEnter by mutableStateOf(DEFAULT_TRANSFORM_ENTER)

    var transformExit by mutableStateOf(DEFAULT_TRANSFORM_EXIT)

    var animationDuration by mutableStateOf(DEFAULT_ANIMATION_DURATION)

    var dataRepeat by mutableStateOf(DEFAULT_DATA_REPEAT)

    fun reset() {
        loaderError = DEFAULT_LOADER_ERROR
        verticalDrag = DEFAULT_VERTICAL_DRAG
        transformEnter = DEFAULT_TRANSFORM_ENTER
        transformExit = DEFAULT_TRANSFORM_EXIT
        animationDuration = DEFAULT_ANIMATION_DURATION
        dataRepeat = DEFAULT_DATA_REPEAT
    }

    companion object {
        val Saver: Saver<TransformSettingState, *> = mapSaver(
            save = {
                mapOf<String, Any>(
                    it::loaderError.name to it.loaderError,
                    it::verticalDrag.name to it.verticalDrag,
                    it::transformEnter.name to it.transformEnter,
                    it::transformExit.name to it.transformExit,
                    it::animationDuration.name to it.animationDuration,
                    it::dataRepeat.name to it.dataRepeat,
                )
            },
            restore = {
                val state = TransformSettingState()
                state.loaderError = it[state::loaderError.name] as Boolean
                state.verticalDrag = it[state::verticalDrag.name] as VerticalDragType
                state.transformEnter = it[state::transformEnter.name] as Boolean
                state.transformExit = it[state::transformExit.name] as Boolean
                state.animationDuration = it[state::animationDuration.name] as Int
                state.dataRepeat = it[state::dataRepeat.name] as Int
                state
            }
        )
    }
}