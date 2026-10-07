package com.example.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "VoiceMessageManager"

data class VoiceRecordingState(
    val isRecording: Boolean = false,
    val durationSeconds: Int = 0,
    val recordedFilePath: String? = null
)

data class VoicePlaybackState(
    val isPlaying: Boolean = false,
    val activeMessageId: String? = null,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0
)

class VoiceMessageManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFile: File? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var playbackJob: Job? = null

    private val _recordingState = MutableStateFlow(VoiceRecordingState())
    val recordingState: StateFlow<VoiceRecordingState> = _recordingState.asStateFlow()

    private val _playbackState = MutableStateFlow(VoicePlaybackState())
    val playbackState: StateFlow<VoicePlaybackState> = _playbackState.asStateFlow()

    fun startRecording(): Boolean {
        stopRecording(save = false)
        stopPlayback()

        return try {
            val cacheDir = context.cacheDir
            val audioFile = File(cacheDir, "voice_msg_${System.currentTimeMillis()}.m4a")
            currentOutputFile = audioFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _recordingState.value = VoiceRecordingState(
                isRecording = true,
                durationSeconds = 0,
                recordedFilePath = audioFile.absolutePath
            )

            startRecordingTimer()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio recording: ${e.message}", e)
            mediaRecorder?.release()
            mediaRecorder = null
            _recordingState.value = VoiceRecordingState()
            false
        }
    }

    private fun startRecordingTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var seconds = 0
            while (isActive && _recordingState.value.isRecording) {
                delay(1000)
                seconds++
                _recordingState.value = _recordingState.value.copy(durationSeconds = seconds)
            }
        }
    }

    fun stopRecording(save: Boolean): String? {
        timerJob?.cancel()
        var filePath: String? = null

        try {
            mediaRecorder?.let { recorder ->
                try {
                    recorder.stop()
                } catch (ignored: Exception) { }
                recorder.reset()
                recorder.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping recorder: ${e.message}")
        } finally {
            mediaRecorder = null
        }

        if (save && currentOutputFile != null && currentOutputFile!!.exists() && currentOutputFile!!.length() > 0) {
            filePath = currentOutputFile!!.absolutePath
            _recordingState.value = VoiceRecordingState(
                isRecording = false,
                durationSeconds = _recordingState.value.durationSeconds,
                recordedFilePath = filePath
            )
        } else {
            currentOutputFile?.delete()
            currentOutputFile = null
            _recordingState.value = VoiceRecordingState()
        }

        return filePath
    }

    fun playVoiceMessage(messageId: String, audioPathOrUrl: String) {
        if (_playbackState.value.isPlaying && _playbackState.value.activeMessageId == messageId) {
            pausePlayback()
            return
        }

        stopPlayback()

        try {
            val player = MediaPlayer()
            if (audioPathOrUrl.startsWith("http://") || audioPathOrUrl.startsWith("https://") || audioPathOrUrl.startsWith("content://")) {
                player.setDataSource(context, android.net.Uri.parse(audioPathOrUrl))
            } else {
                player.setDataSource(audioPathOrUrl)
            }

            player.setOnPreparedListener { mp ->
                mp.start()
                _playbackState.value = VoicePlaybackState(
                    isPlaying = true,
                    activeMessageId = messageId,
                    durationMs = mp.duration.coerceAtLeast(1000)
                )
                startPlaybackTracker()
            }

            player.setOnCompletionListener {
                stopPlayback()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "Voice playback error: what=$what extra=$extra")
                stopPlayback()
                true
            }

            mediaPlayer = player
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play voice message: ${e.message}", e)
            stopPlayback()
        }
    }

    private fun startPlaybackTracker() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                val curPos = try {
                    if (mediaPlayer?.isPlaying == true) mediaPlayer?.currentPosition ?: 0 else 0
                } catch (e: Exception) { 0 }
                val dur = try {
                    mediaPlayer?.duration ?: _playbackState.value.durationMs
                } catch (e: Exception) { _playbackState.value.durationMs }

                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = curPos,
                    durationMs = dur
                )
                delay(200)
            }
        }
    }

    fun pausePlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
        } catch (e: Exception) {
            Log.w(TAG, "Pause error: ${e.message}")
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        try {
            mediaPlayer?.let {
                it.setOnCompletionListener(null)
                it.setOnErrorListener(null)
                it.setOnPreparedListener(null)
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Stop playback error: ${e.message}")
        } finally {
            mediaPlayer = null
            _playbackState.value = VoicePlaybackState()
        }
    }
}
