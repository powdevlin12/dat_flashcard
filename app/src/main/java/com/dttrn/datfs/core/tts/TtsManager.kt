package com.dttrn.datfs.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quản lý Android TextToSpeech engine.
 * - Hoàn toàn miễn phí, offline (dùng TTS engine có sẵn trên máy)
 * - Chỉ phát âm tiếng Anh (Locale.US)
 * - Dọn dẹp tài nguyên khi không còn dùng nữa
 */
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    enum class TtsStatus { INITIALIZING, READY, ERROR, SPEAKING }

    private val _status = MutableStateFlow(TtsStatus.INITIALIZING)
    val status: StateFlow<TtsStatus> = _status.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { initStatus ->
            if (initStatus == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                _status.value = if (
                    result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    TtsStatus.ERROR
                } else {
                    TtsStatus.READY
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _status.value = TtsStatus.SPEAKING
                    }
                    override fun onDone(utteranceId: String?) {
                        _status.value = TtsStatus.READY
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _status.value = TtsStatus.READY
                    }
                })
            } else {
                _status.value = TtsStatus.ERROR
            }
        }
    }

    /**
     * Phát âm từ tiếng Anh.
     * @param text Từ cần đọc (thường là frontText của flashcard)
     */
    fun speak(text: String) {
        if (_status.value == TtsStatus.ERROR || text.isBlank()) return
        tts?.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, null, "tts_utterance")
    }

    /**
     * Dừng phát âm ngay lập tức.
     */
    fun stop() {
        tts?.stop()
        if (_status.value == TtsStatus.SPEAKING) {
            _status.value = TtsStatus.READY
        }
    }

    /**
     * Giải phóng tài nguyên — gọi khi app bị destroy.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
