package com.zahri.lighttodo.feature.noteeditor

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.data.NoteDao
import com.zahri.lighttodo.data.NoteEntity
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class NoteMediaSessionState(
    val recording: Boolean = false,
    val playingAudioRef: String? = null
)

data class RecordedNoteAudio(
    val file: File,
    val durationMillis: Long
)

class NoteEditViewModel(
    private val app: App = App.instance,
    private val noteDao: NoteDao = app.db.noteDao()
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _createdAt = MutableStateFlow(0L)
    val createdAt: StateFlow<Long> = _createdAt.asStateFlow()

    private val _updatedAt = MutableStateFlow(0L)
    val updatedAt: StateFlow<Long> = _updatedAt.asStateFlow()

    private val _mediaState = MutableStateFlow(NoteMediaSessionState())
    val mediaState: StateFlow<NoteMediaSessionState> = _mediaState.asStateFlow()

    private var noteId: Long? = null
    private var loaded = false
    private var saveJob: Job? = null
    private var saveAgainAfterCurrentJob = false
    private var lastSavedTitle = ""
    private var lastSavedContent = ""
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingStartedAt: Long = 0L
    private var player: MediaPlayer? = null

    fun load(id: Long?, launchSeed: NoteEditLaunchSeed? = null) {
        if (loaded) return
        loaded = true
        if (id == null) {
            val now = System.currentTimeMillis()
            _createdAt.value = now
            _updatedAt.value = now
            return
        }
        noteId = id
        if (launchSeed?.id == id) {
            applyLaunchSeed(launchSeed)
            return
        }
        viewModelScope.launch {
            val note = withContext(Dispatchers.IO) {
                noteDao.findById(id)
            } ?: return@launch
            applyNote(note)
        }
    }

    fun updateTitle(value: String) {
        if (_title.value == value) return
        _title.value = value
    }

    fun updateContent(value: String) {
        if (_content.value == value) return
        _content.value = value
    }

    /** 自动保存：有内容时写库 */
    fun save() {
        if (saveJob?.isActive == true) {
            saveAgainAfterCurrentJob = true
            return
        }
        saveJob = viewModelScope.launch {
            do {
                saveAgainAfterCurrentJob = false
                // 非取消块：防止按返回时 viewModelScope 被取消导致
                // 内容（含录音/图片引用）未写入数据库，造成文件被错误清理而丢失
                withContext(NonCancellable + Dispatchers.IO) {
                    val t = _title.value.trim()
                    val c = _content.value
                    if (t.isEmpty() && c.isBlank()) return@withContext
                    if (noteId != null && t == lastSavedTitle && c == lastSavedContent) return@withContext

                    val now = System.currentTimeMillis()
                    val entity = NoteEntity(
                        id = noteId ?: 0L,
                        title = t.ifEmpty { null },
                        content = c,
                        createdAtMillis = _createdAt.value,
                        updatedAtMillis = now
                    )
                    val newId = noteDao.upsert(entity)
                    if (noteId == null) noteId = newId
                    _updatedAt.value = now
                    NoteAttachmentStore.deleteRemovedRefs(app, lastSavedContent, c)
                    lastSavedTitle = t
                    lastSavedContent = c
                }
            } while (saveAgainAfterCurrentJob)
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = noteId ?: run { onDone(); return }
        viewModelScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                val content = noteDao.findById(id)?.content ?: _content.value
                NoteAttachmentStore.deleteRefs(app, NoteAttachmentMarkdown.refsIn(content))
                noteDao.delete(id)
                cleanupUnreferencedAttachments()
            }
            onDone()
        }
    }

    fun startRecording(context: Context): Boolean {
        stopAudioPlayback()

        val appContext = context.applicationContext
        val file = NoteAttachmentStore.createAudioFile(appContext)
        val nextRecorder = createNoteMediaRecorder(appContext, file)
        return runCatching {
            nextRecorder.prepare()
            nextRecorder.start()
        }.onSuccess {
            recordingFile = file
            recordingStartedAt = System.currentTimeMillis()
            recorder = nextRecorder
            _mediaState.value = _mediaState.value.copy(recording = true)
        }.onFailure {
            nextRecorder.release()
            file.delete()
        }.isSuccess
    }

    fun stopRecording(): RecordedNoteAudio? {
        val activeRecorder = recorder ?: return null
        val file = recordingFile
        val startedAt = recordingStartedAt
        recorder = null
        recordingFile = null
        recordingStartedAt = 0L
        _mediaState.value = _mediaState.value.copy(recording = false)

        val stopped = runCatching { activeRecorder.stop() }.isSuccess
        activeRecorder.release()
        val duration = System.currentTimeMillis() - startedAt
        if (stopped && file != null && file.exists() && file.length() > 0L) {
            return RecordedNoteAudio(file = file, durationMillis = duration)
        }
        file?.delete()
        return null
    }

    fun toggleAudioPlayback(context: Context, ref: String): Boolean {
        if (_mediaState.value.playingAudioRef == ref) {
            stopAudioPlayback()
            return true
        }

        val file = NoteAttachmentStore.resolve(context.applicationContext, ref) ?: return false
        return runCatching {
            stopAudioPlayback()
            val nextPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { completed ->
                    completed.release()
                    if (player === completed) {
                        player = null
                        _mediaState.value = _mediaState.value.copy(playingAudioRef = null)
                    }
                }
                prepare()
                start()
            }
            player = nextPlayer
            _mediaState.value = _mediaState.value.copy(playingAudioRef = ref)
        }.onFailure {
            stopAudioPlayback()
        }.isSuccess
    }

    fun stopAudioPlayback(ref: String? = null) {
        if (ref != null && _mediaState.value.playingAudioRef != ref) return
        val activePlayer = player
        player = null
        if (activePlayer != null) {
            activePlayer.runCatching { stop() }
            activePlayer.release()
        }
        _mediaState.value = _mediaState.value.copy(playingAudioRef = null)
    }

    override fun onCleared() {
        recorder?.runCatching { stop() }
        recorder?.release()
        recordingFile?.delete()
        recorder = null
        recordingFile = null
        stopAudioPlayback()
        super.onCleared()
    }

    private fun applyLaunchSeed(seed: NoteEditLaunchSeed) {
        _title.value = seed.title.orEmpty()
        _content.value = seed.content
        _createdAt.value = seed.createdAtMillis
        _updatedAt.value = seed.updatedAtMillis
        lastSavedTitle = _title.value.trim()
        lastSavedContent = _content.value
    }

    private fun applyNote(note: NoteEntity) {
        _title.value = note.title.orEmpty()
        _content.value = note.content
        _createdAt.value = note.createdAtMillis
        _updatedAt.value = note.updatedAtMillis
        lastSavedTitle = _title.value.trim()
        lastSavedContent = _content.value
    }

    private suspend fun cleanupUnreferencedAttachments() {
        val refs = noteDao.listAll()
            .flatMap { NoteAttachmentMarkdown.refsIn(it.content) }
            .toSet()
        NoteAttachmentStore.deleteUnreferenced(app, refs)
    }
}

private fun createNoteMediaRecorder(context: Context, file: File): MediaRecorder {
    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }
    return recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(96_000)
        setAudioSamplingRate(44_100)
        setOutputFile(file.absolutePath)
    }
}
