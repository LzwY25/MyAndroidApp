package com.lzwy.myreply.ui.component.writemessage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.jvziyaoyao.scale.image.previewer.ImagePreviewer
import com.jvziyaoyao.scale.image.previewer.TransformImageView
import com.jvziyaoyao.scale.zoomable.previewer.PreviewerState
import com.jvziyaoyao.scale.zoomable.previewer.rememberPreviewerState
import com.lzwy.myreply.R
import com.lzwy.myreply.ui.ReplyHomeUIState
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyGridState
import org.burnoutcrew.reorderable.reorderable
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.MaterialTheme as OldMaterialTheme

private const val TAG = "WriteMessage"

enum class RecordStatus {
    IDLE,
    RECORDING,
    RECORD_PAUSED,
    FINISHED,
    PLAYING,
    PLAY_PAUSED
}

var recordingTimer: CountDownTimer? = null
var mediaRecorder: MediaRecorder? = null
var recordFileName: String? = null

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
            Log.d(TAG,"PERMISSION GRANTED")

        } else {
            Log.e(TAG,"PERMISSION DENIED")
        }
    }

    val localContext = LocalContext.current
    val imageUri = remember {
        mutableStateOf<List<Uri>>(emptyList())
    }

    val previewState = rememberPreviewerState(pageCount = { imageUri.value.size }) { imageUri.value[it] }

    val recordStatus = remember {
        mutableStateOf(RecordStatus.IDLE)
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
                    Log.d(TAG,"Code requires permission")
                }
                else -> {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
        isFirstTime = false
    })

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .padding(bottom = 72.dp)
                .scrollable(state = rememberScrollState(), orientation = Orientation.Vertical, enabled = false)// 留出底部空间以容纳 FloatingActionButton
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .padding(top = 16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("文章内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 300.dp)
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(text = "Record",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline)
                RecordComposable(recordStatus)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(text = "Photos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline)
                PhotoComposable(imageUri, previewState)
            }
        }

        ImagePreviewer(
            state = previewState,
            imageLoader = { page ->
                val painter = rememberAsyncImagePainter(model = imageUri.value[page])
                Pair(painter, painter.intrinsicSize)
            }
        )

        FloatingActionButton(
            onClick = {
                Log.i(TAG, "Floating button clicked!")
                finishWriting(title.text, content.text,
                    imageUri.value, recordFileName ?: "")
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

@Composable
private fun PhotoComposable(imageUri: MutableState<List<Uri>>,
                            previewState: PreviewerState) {
    val scope = rememberCoroutineScope()
    val state = rememberReorderableLazyGridState(onMove = { from, to ->
        imageUri.value.apply {
            imageUri.value = imageUri.value.toMutableList().apply {
                Log.i("LZWY", "switch: ${from.index} and ${to.index}")
                add(to.index, removeAt(from.index))
                toList()
            }
        }
    })

    var hasImage by remember {
        mutableStateOf(false)
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uri ->
        Log.i(TAG, "get image: $uri")
        hasImage = ((uri as List<*>).isNotEmpty())
        imageUri.value = run {
            val tmp: MutableList<Uri> = mutableListOf()
            tmp.addAll(imageUri.value)
            tmp.addAll(uri)
            tmp.toList()
        }
    }

    val lineCount = 3

    LazyVerticalGrid(columns = GridCells.Fixed(3),
        state = state.gridState,
        modifier = Modifier
            .height(120.dp)
            .reorderable(state)
            .detectReorderAfterLongPress(state),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            imageUri.value.forEachIndexed { index, item ->
                item(key = index) {
                    val needStart = index % lineCount != 0
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
                            )
                            .size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TransformImageView(
                            modifier = Modifier
                                .size(120.dp)
                                .clickable {
                                    scope.launch {
                                        previewState.enterTransform(index)
                                    }
                                },
                            imageLoader = {
                                val painter = rememberAsyncImagePainter(model = item)
                                Triple(item, painter, painter.intrinsicSize)
                            },
                            transformState = previewState,
                        )
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
                                            imageUri.value.toMutableList()
                                        tmp.removeAt(index)
                                        imageUri.value = tmp.toList()
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

            if(imageUri.value.size < 9) {
                item {
                    AsyncImage(model = R.drawable.tianjia,
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

@Composable
private fun RecordComposable(recordStatus: MutableState<RecordStatus>) {
    val context = LocalContext.current
    val recordingDurationMillis = remember { mutableIntStateOf(0) }
    Row {
        if (recordStatus.value == RecordStatus.IDLE) {
            Button(
                onClick = {
                    Log.i("MediaRecorder", "STATUS: IDLE, start to record")
                    record(context, recordingDurationMillis)
                    recordStatus.value = RecordStatus.RECORDING
                }
            ) {
                Image(painter = painterResource(R.drawable.start),
                    contentDescription = "Start Icon",
                    modifier = Modifier.size(24.dp))
            }
        }

        if (recordStatus.value == RecordStatus.RECORDING || recordStatus.value == RecordStatus.RECORD_PAUSED) {
            Button(
                onClick = {
                    if (recordStatus.value == RecordStatus.RECORDING) {
                        Log.i("MediaRecorder", "STATUS RECORDING, recording pause")
                        recordingTimer?.cancel()
                        mediaRecorder?.pause()
                        recordStatus.value = RecordStatus.RECORD_PAUSED
                    } else {
                        Log.i("MediaRecorder", "STATUS PAUSE, recording resume")
                        recordingTimer?.start()
                        mediaRecorder?.resume()
                        recordStatus.value = RecordStatus.RECORDING
                    }
                }
            ) {
                if (recordStatus.value == RecordStatus.RECORDING) {
                    Icon(painter = painterResource(R.drawable.pause),
                        contentDescription = "Pasue icon",
                        modifier = Modifier.size(24.dp))
                } else {
                    Icon(painter = painterResource(R.drawable.jixu),
                        contentDescription = "Continue icon",
                        modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(modifier = Modifier
            .height(1.dp)
            .width(4.dp))

        if(recordStatus.value == RecordStatus.RECORDING) {
            Button(onClick = {
                Log.i("MediaRecorder", "STATUS RECORDING, recording clear")
                recordingTimer?.onFinish()
                recordingTimer?.cancel()
                recordingTimer = null
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                recordStatus.value = RecordStatus.IDLE
            }) {
                Icon(painter = painterResource(R.drawable.stop),
                    contentDescription = "Stop icon",
                    modifier = Modifier.size(24.dp))
            }

            Button(onClick = {
                Log.i("MediaRecorder", "STATUS RECORDING, recording save, time: ${recordingDurationMillis.intValue}, fileName: $recordFileName")
                recordingTimer?.onFinish()
                recordingTimer?.cancel()
                recordingTimer = null
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                recordStatus.value = RecordStatus.FINISHED
            }) {
                Icon(painter = painterResource(R.drawable.baocun),
                    contentDescription = "Save icon",
                    modifier = Modifier.size(24.dp))
            }
        }


        if(recordingDurationMillis.intValue > 0) {
            Spacer(modifier = Modifier
                .height(1.dp)
                .width(4.dp))
            Text(text = "Duration: ${recordingDurationMillis.intValue} s")
        }
        if(recordStatus.value >= RecordStatus.FINISHED) {
            val mediaPlayer = MediaPlayer()
            Spacer(modifier = Modifier
                .height(1.dp)
                .width(12.dp))
            Button(onClick = {
                if(recordStatus.value != RecordStatus.PLAYING) {
                    if(recordStatus.value == RecordStatus.FINISHED) {
                        mediaPlayer.let {
                            if (it.isPlaying) {
                                it.stop()
                                it.reset()
                            }
                        }

                        try {
                            mediaPlayer.apply {
                                setDataSource(context, Uri.parse(recordFileName))
                                prepare()
                                start()

                                setOnCompletionListener {
                                    // 播放完成后的操作，例如停止播放或进行其他处理
                                    Log.d("MediaPlayer", "Playback completed")
                                    stop()
                                    reset()
                                }
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    else {
                        mediaPlayer.start()
                    }
                    recordStatus.value = RecordStatus.PLAYING
                }
                else {
                    // STATUS: PLAYING
                    mediaPlayer.apply {
                        if(this.isPlaying) {
                            recordStatus.value = RecordStatus.PLAY_PAUSED
                            pause()
                        }
                    }
                }
            }) {
                if(recordStatus.value != RecordStatus.PLAYING) {
                    Icon(painter = painterResource(R.drawable.play),
                        contentDescription = "Play icon",
                        modifier = Modifier.size(24.dp))
                }
                else {
                    Icon(painter = painterResource(R.drawable.pause),
                        contentDescription = "Pause icon",
                        modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier
                .height(1.dp)
                .width(4.dp))
            Button(onClick = {
                if(mediaPlayer.isPlaying) {
                    stopPlayback(mediaPlayer)
                }
                recordStatus.value = RecordStatus.IDLE
                recordFileName = ""
            }) {
                Icon(painter = painterResource(R.drawable.delete),
                    contentDescription = "Delete icon",
                    modifier = Modifier.size(24.dp))
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

private fun record(localContext: Context, recordingDurationMillis: MutableState<Int>) {
    Log.i("MediaRecorder", "recording start")
    recordingTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            recordingDurationMillis.value += 1
        }

        override fun onFinish() {
            recordingDurationMillis.value = 0
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
