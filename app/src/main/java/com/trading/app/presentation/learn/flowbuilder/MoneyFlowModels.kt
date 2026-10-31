package com.trading.app.presentation.learn.flowbuilder

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class BlockKind {
    NEED,
    GOAL,
    WANT,
    SHOCK
}

data class MoneyBlock(
    val id: String,
    val name: String,
    val kind: BlockKind,
    val min: Int,
    val max: Int,
    val defaultAmount: Int,
    val color: Color
)

enum class FlowStage(val stepIndex: Int, val title: String) {
    INTRO(0, "Intro"),
    BUILD(1, "Build"),
    SURPRISE(2, "Surprise"),
    REFLECT(3, "Reflect"),
    QUIZ(4, "Quiz"),
    DONE(5, "Done")
}

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class RowImpact(
    val tone: ImpactTone,
    val text: String
)

enum class ImpactTone {
    GOOD,
    WARN,
    BAD
}

data class LockStatus(
    val isReady: Boolean,
    val buttonText: String
)

data class MoneyFlowState(
    val stage: FlowStage = FlowStage.INTRO,
    val allocations: Map<String, Int> = emptyMap(),
    val plan1: Map<String, Int>? = null,
    val plan2: Map<String, Int>? = null,
    val totalIncome: Int = 2400,
    val shockAmount: Int = 380,
    val xpEarned: Int = 0,
    val shockAlertOpen: Boolean = false,
    val coachMessage: String = "",
    val overrideMessage: String? = null,
    val currentQuizIndex: Int = 0,
    val quizSelectedOption: Int? = null,
    val quizIsChecked: Boolean = false,
    val quizCorrectCount: Int = 0
) {
    val totalAllocated: Int
        get() = allocations.values.sum()

    val unassignedDollars: Int
        get() = totalIncome - totalAllocated

    fun isPlaced(id: String): Boolean = allocations.containsKey(id)
}
