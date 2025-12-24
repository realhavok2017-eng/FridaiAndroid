package com.fridai.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridai.app.audio.AudioPlayer
import com.fridai.app.audio.AudioRecorder
import com.fridai.app.repository.FridaiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentMood: String = "chill",
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isThinking: Boolean = false,
    val lastTranscription: String = "",
    val lastResponse: String = "",
    val error: String? = null
)

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val repository: FridaiRepository,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init {
        // Observe audio states
        viewModelScope.launch {
            audioRecorder.isRecording.collect { isRecording ->
                _uiState.update { it.copy(isListening = isRecording) }
            }
        }

        viewModelScope.launch {
            audioPlayer.isPlaying.collect { isPlaying ->
                _uiState.update { it.copy(isSpeaking = isPlaying) }
            }
        }

        // Fetch initial mood
        viewModelScope.launch {
            repository.getEmotionState().onSuccess { emotion ->
                _uiState.update { it.copy(currentMood = emotion.current_emotion) }
            }
        }
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(error = null, currentMood = "listening") }

                // Record audio
                val audioData = try {
                    audioRecorder.recordWithVAD()
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Mic error: ${e.message}", currentMood = "chill") }
                    return@launch
                }

                if (audioData == null) {
                    _uiState.update { it.copy(error = "No audio recorded", currentMood = "chill") }
                    return@launch
                }

                // Transcribe
                _uiState.update { it.copy(currentMood = "thinking", isThinking = true) }
                val transcribeResult = repository.transcribe(audioData)
                transcribeResult.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = "Transcription failed: ${e.message}",
                            currentMood = "confused",
                            isThinking = false
                        )
                    }
                    return@launch
                }

                val transcription = transcribeResult.getOrNull()?.text ?: ""
                if (transcription.isBlank()) {
                    _uiState.update { it.copy(error = "Couldn't understand", currentMood = "confused", isThinking = false) }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        lastTranscription = transcription,
                        messages = it.messages + ChatMessage(transcription, isUser = true)
                    )
                }

                // Chat
                val chatResult = repository.chat(transcription)
                chatResult.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = "Chat failed: ${e.message}",
                            currentMood = "confused",
                            isThinking = false
                        )
                    }
                    return@launch
                }

                val response = chatResult.getOrNull()?.response ?: ""
                _uiState.update {
                    it.copy(
                        lastResponse = response,
                        messages = it.messages + ChatMessage(response, isUser = false),
                        currentMood = "speaking",
                        isThinking = false
                    )
                }

                // Speak
                try {
                    val speakResult = repository.speak(response)
                    speakResult.onSuccess { audioBytes ->
                        audioPlayer.playAudio(audioBytes)
                    }
                } catch (e: Exception) {
                    // Speaking failed but response was received - that's okay
                }

                // Return to chill after speaking
                _uiState.update { it.copy(currentMood = "chill") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error: ${e.message}", currentMood = "chill", isThinking = false) }
            }
        }
    }

    fun stopListening() {
        audioRecorder.stopRecording()
    }

    fun stopSpeaking() {
        audioPlayer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stopRecording()
        audioPlayer.stop()
    }
}
