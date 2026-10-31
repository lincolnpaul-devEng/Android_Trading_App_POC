package com.trading.app.presentation.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trading.app.domain.quiz.model.QuizQuestion
import com.trading.app.domain.quiz.model.QuizQuestionType
import com.trading.app.domain.quiz.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChallengeItem(
    val id: String,
    val title: String,
    val minXp: Int,
    val maxXp: Int,
    val iconType: String, // "quiz", "trade", "chart", "sentiment"
    val isCompleted: Boolean = false,
    val question: String? = null,
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String? = null,
    val questionType: QuizQuestionType = QuizQuestionType.MULTIPLE_CHOICE,
    val simulationType: String? = null
)

data class LearnPracticeUiState(
    val totalXp: Int = 780,
    val todayXp: Int = 0,
    val streakDays: Int = 4,
    val todayLessons: Int = 0,
    val todayChallenges: Int = 1,
    val searchQuery: String = "",
    val showStreakDialog: Boolean = false,
    val activeChallenge: ChallengeItem? = null,
    val activeSessionDialog: Boolean = false,
    val sessionQuizAnswered: Boolean = false,
    val quizSelectedOption: Int? = null,
    val quizIsCorrect: Boolean? = null,
    val challenges: List<ChallengeItem> = emptyList()
)

@HiltViewModel
class LearnPracticeViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnPracticeUiState())
    val uiState: StateFlow<LearnPracticeUiState> = _uiState.asStateFlow()

    init {
        observeDynamicQuizData()
        syncWithServer()
    }

    private fun syncWithServer() {
        viewModelScope.launch {
            quizRepository.syncQuestionsWithServer(force = false)
        }
    }

    private fun observeDynamicQuizData() {
        viewModelScope.launch {
            quizRepository.getQuestionsFlow().collect { questions ->
                val dynamicChallenges = if (questions.isNotEmpty()) {
                    questions.map { q -> q.toChallengeItem() }
                } else {
                    getFallbackChallenges()
                }

                _uiState.update { state ->
                    val completedCount = dynamicChallenges.count { it.isCompleted }
                    val earnedXpFromDb = dynamicChallenges.filter { it.isCompleted }.sumOf { it.maxXp }
                    state.copy(
                        challenges = dynamicChallenges,
                        todayChallenges = completedCount,
                        todayXp = earnedXpFromDb,
                        totalXp = 780 + earnedXpFromDb
                    )
                }
            }
        }
    }

    private fun QuizQuestion.toChallengeItem(): ChallengeItem {
        val icon = when (type) {
            QuizQuestionType.SIMULATION -> if (simulationType == "projectile_motion") "chart" else "quiz"
            QuizQuestionType.PREDICTION_EXPERIMENT -> "sentiment"
            QuizQuestionType.NUMERICAL -> "trade"
            QuizQuestionType.MULTIPLE_CHOICE -> "quiz"
        }

        return ChallengeItem(
            id = id,
            title = title,
            minXp = minXp,
            maxXp = maxXp,
            iconType = icon,
            isCompleted = isCompleted,
            question = prompt,
            options = options,
            correctIndex = correctAnswer?.toIntOrNull() ?: 0,
            explanation = explanation,
            questionType = type,
            simulationType = simulationType
        )
    }

    private fun getFallbackChallenges(): List<ChallengeItem> {
        return listOf(
            ChallengeItem(
                id = "money_flow_builder_poc",
                title = "Quiz of the week: Money Flow Builder",
                minXp = 50,
                maxXp = 150,
                iconType = "quiz",
                questionType = QuizQuestionType.SIMULATION,
                simulationType = "money_flow_builder"
            ),
            ChallengeItem(
                id = "risk_management_drag_drop",
                title = "Risk Management: Interactive Stop Placement",
                minXp = 25,
                maxXp = 50,
                iconType = "chart",
                questionType = QuizQuestionType.SIMULATION,
                simulationType = "risk_management"
            ),
            ChallengeItem(
                id = "margin_call_prediction",
                title = "Leverage & Margin Call Prediction",
                minXp = 15,
                maxXp = 35,
                iconType = "sentiment",
                questionType = QuizQuestionType.PREDICTION_EXPERIMENT
            ),
            ChallengeItem(
                id = "position_sizing_numerical",
                title = "Position Sizing 1% Risk Formula",
                minXp = 10,
                maxXp = 30,
                iconType = "trade",
                questionType = QuizQuestionType.NUMERICAL
            )
        )
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openStreakDialog() {
        _uiState.update { it.copy(showStreakDialog = true) }
    }

    fun closeStreakDialog() {
        _uiState.update { it.copy(showStreakDialog = false) }
    }

    fun openChallenge(challenge: ChallengeItem) {
        _uiState.update {
            it.copy(
                activeChallenge = challenge,
                quizSelectedOption = null,
                quizIsCorrect = null
            )
        }
    }

    fun closeChallenge() {
        _uiState.update {
            it.copy(
                activeChallenge = null,
                quizSelectedOption = null,
                quizIsCorrect = null
            )
        }
    }

    fun submitQuizAnswer(selectedIndex: Int) {
        val currentChallenge = _uiState.value.activeChallenge ?: return
        val isCorrect = selectedIndex == currentChallenge.correctIndex
        val earnedXp = if (isCorrect) currentChallenge.maxXp else currentChallenge.minXp

        viewModelScope.launch {
            quizRepository.submitAnswer(
                questionId = currentChallenge.id,
                answer = selectedIndex.toString(),
                isCorrect = isCorrect,
                earnedXp = earnedXp
            )
        }

        _uiState.update { state ->
            val updatedChallenges = state.challenges.map { item ->
                if (item.id == currentChallenge.id) item.copy(isCompleted = true) else item
            }
            state.copy(
                quizSelectedOption = selectedIndex,
                quizIsCorrect = isCorrect,
                totalXp = if (!currentChallenge.isCompleted) state.totalXp + earnedXp else state.totalXp,
                todayXp = if (!currentChallenge.isCompleted) state.todayXp + earnedXp else state.todayXp,
                todayChallenges = if (!currentChallenge.isCompleted) state.todayChallenges + 1 else state.todayChallenges,
                challenges = updatedChallenges
            )
        }
    }

    fun openSessionDialog() {
        _uiState.update { it.copy(activeSessionDialog = true) }
    }

    fun closeSessionDialog() {
        _uiState.update { it.copy(activeSessionDialog = false) }
    }

    fun completeSession() {
        val xpGain = 100
        _uiState.update { state ->
            if (!state.sessionQuizAnswered) {
                state.copy(
                    sessionQuizAnswered = true,
                    totalXp = state.totalXp + xpGain,
                    todayXp = state.todayXp + xpGain,
                    todayLessons = state.todayLessons + 1,
                    activeSessionDialog = false
                )
            } else {
                state.copy(activeSessionDialog = false)
            }
        }
    }
}
