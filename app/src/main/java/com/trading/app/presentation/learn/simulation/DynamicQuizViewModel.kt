package com.trading.app.presentation.learn.simulation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trading.app.domain.quiz.model.QuizProgressSummary
import com.trading.app.domain.quiz.model.QuizQuestion
import com.trading.app.domain.quiz.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DynamicQuizUiState(
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val totalEarnedXp: Int = 0
) {
    val currentQuestion: QuizQuestion?
        get() = questions.getOrNull(currentIndex)

    val progressFraction: Float
        get() = if (questions.isEmpty()) 0f else (currentIndex + 1).toFloat() / questions.size.toFloat()
}

@HiltViewModel
class DynamicQuizViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialQuestionId: String? = savedStateHandle.get<String>("questionId")

    private val _uiState = MutableStateFlow(DynamicQuizUiState())
    val uiState: StateFlow<DynamicQuizUiState> = _uiState.asStateFlow()

    val progressSummary: StateFlow<QuizProgressSummary> = quizRepository.getProgressSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = QuizProgressSummary(0, 0, 0)
        )

    init {
        observeQuestions()
        syncWithServer()
    }

    private fun observeQuestions() {
        viewModelScope.launch {
            quizRepository.getQuestionsFlow().collect { questionsList ->
                _uiState.update { state ->
                    val resolvedIndex = if (initialQuestionId != null) {
                        val found = questionsList.indexOfFirst { it.id == initialQuestionId }
                        if (found >= 0) found else state.currentIndex
                    } else {
                        state.currentIndex
                    }.coerceIn(0, (questionsList.size - 1).coerceAtLeast(0))

                    state.copy(
                        questions = questionsList,
                        currentIndex = resolvedIndex,
                        isLoading = false,
                        totalEarnedXp = questionsList.sumOf { it.earnedXp }
                    )
                }
            }
        }
    }

    fun syncWithServer(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = quizRepository.syncQuestionsWithServer(force)
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }

    fun nextQuestion() {
        _uiState.update { state ->
            val next = (state.currentIndex + 1).coerceAtMost(state.questions.size - 1)
            state.copy(currentIndex = next)
        }
    }

    fun previousQuestion() {
        _uiState.update { state ->
            val prev = (state.currentIndex - 1).coerceAtLeast(0)
            state.copy(currentIndex = prev)
        }
    }

    fun jumpToQuestion(index: Int) {
        _uiState.update { state ->
            state.copy(currentIndex = index.coerceIn(0, (state.questions.size - 1).coerceAtLeast(0)))
        }
    }

    fun submitAnswer(userAnswer: String, isCorrect: Boolean, earnedXp: Int) {
        val current = _uiState.value.currentQuestion ?: return
        viewModelScope.launch {
            quizRepository.submitAnswer(current.id, userAnswer, isCorrect, earnedXp)
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            quizRepository.resetProgress()
        }
    }
}
