package com.trading.app.presentation.learn.flowbuilder

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoneyFlowBuilderViewModel @Inject constructor() : ViewModel() {

    val blocks = listOf(
        MoneyBlock(id = "rent", name = "Rent", kind = BlockKind.NEED, min = 900, max = 1200, defaultAmount = 900, color = Color(0xFF6C93F5)),
        MoneyBlock(id = "food", name = "Food", kind = BlockKind.NEED, min = 300, max = 600, defaultAmount = 400, color = Color(0xFFF59A5A)),
        MoneyBlock(id = "transport", name = "Transport", kind = BlockKind.NEED, min = 150, max = 350, defaultAmount = 250, color = Color(0xFF3CC7CC)),
        MoneyBlock(id = "debt", name = "Debt payments", kind = BlockKind.NEED, min = 200, max = 600, defaultAmount = 300, color = Color(0xFFA38BF0)),
        MoneyBlock(id = "savings", name = "Savings", kind = BlockKind.GOAL, min = 0, max = 800, defaultAmount = 300, color = Color(0xFF52C286)),
        MoneyBlock(id = "fun", name = "Fun", kind = BlockKind.WANT, min = 0, max = 400, defaultAmount = 150, color = Color(0xFFF072AE)),
        MoneyBlock(id = "repair", name = "Car repair", kind = BlockKind.SHOCK, min = 380, max = 380, defaultAmount = 380, color = Color(0xFFF26A6E))
    )

    val quizQuestions = listOf(
        QuizQuestion(
            question = "A $380 repair bill lands mid-month and every dollar already has a job. Which move protects you best?",
            options = listOf("Skip rent and catch up later", "Draw on savings, then trim fun", "Miss the debt minimum this once"),
            correctIndex = 1,
            explanation = "That is what a cushion is for. Late rent or a missed debt payment adds fees and makes next month harder."
        ),
        QuizQuestion(
            question = "All $2,400 has a job, then you spot a $150 purchase you want. What has to happen?",
            options = listOf("Nothing, it will fit somehow", "Another block shrinks by $150", "Your income rises by $150"),
            correctIndex = 1,
            explanation = "A fixed income is a closed system. Every yes to one block is a no, or a smaller yes, to another."
        ),
        QuizQuestion(
            question = "Why do traders only use money left over after needs and savings?",
            options = listOf("Brokers require it", "A trade can lose, and rent is still due", "It lets them trade bigger"),
            correctIndex = 1,
            explanation = "That leftover is risk capital. If a trade goes wrong, it should not touch rent, food or your cushion."
        )
    )

    private val _uiState = MutableStateFlow(MoneyFlowState())
    val uiState: StateFlow<MoneyFlowState> = _uiState.asStateFlow()

    init {
        updateCoachMessage()
    }

    fun startLesson() {
        _uiState.update { it.copy(stage = FlowStage.BUILD) }
        updateCoachMessage()
    }

    fun placeBlock(id: String) {
        val block = blocks.find { it.id == id } ?: return
        if (_uiState.value.isPlaced(id)) return

        val unassigned = _uiState.value.unassignedDollars.coerceAtLeast(0)
        val initialAmount = minOf(block.defaultAmount, unassigned, block.max)

        _uiState.update { state ->
            val updated = state.allocations.toMutableMap()
            updated[id] = (initialAmount / 10) * 10
            state.copy(allocations = updated)
        }
        updateCoachMessage()
    }

    fun setAmount(id: String, requestedAmount: Int) {
        val block = blocks.find { it.id == id } ?: return
        if (block.kind == BlockKind.SHOCK) return // Fixed shock amount

        val currentAmount = _uiState.value.allocations[id] ?: 0
        val unassigned = _uiState.value.unassignedDollars.coerceAtLeast(0)
        val maxPossible = currentAmount + unassigned

        val clamped = (requestedAmount / 10) * 10
        val finalAmount = if (clamped > maxPossible) {
            _uiState.update {
                it.copy(overrideMessage = "Out of unassigned money. To raise ${block.name}, lower another block first.")
            }
            maxPossible
        } else {
            clamped.coerceIn(0, block.max)
        }

        _uiState.update { state ->
            val updated = state.allocations.toMutableMap()
            updated[id] = finalAmount
            state.copy(allocations = updated)
        }
        updateCoachMessage()
    }

    fun adjustAmount(id: String, delta: Int) {
        val current = _uiState.value.allocations[id] ?: 0
        setAmount(id, current + delta)
    }

    fun lockPlan1() {
        if (!getLockStatus().isReady) return
        _uiState.update { state ->
            state.copy(
                stage = FlowStage.SURPRISE,
                plan1 = state.allocations.toMap(),
                xpEarned = state.xpEarned + 10,
                shockAlertOpen = true
            )
        }
        updateCoachMessage()
    }

    fun acknowledgeShock() {
        _uiState.update { state ->
            val updated = state.allocations.toMutableMap()
            updated["repair"] = state.shockAmount
            state.copy(
                allocations = updated,
                shockAlertOpen = false
            )
        }
        updateCoachMessage()
    }

    fun lockPlan2() {
        if (!getLockStatus().isReady) return
        _uiState.update { state ->
            state.copy(
                stage = FlowStage.REFLECT,
                plan2 = state.allocations.toMap(),
                xpEarned = state.xpEarned + 10
            )
        }
    }

    fun proceedToQuiz() {
        _uiState.update {
            it.copy(
                stage = FlowStage.QUIZ,
                currentQuizIndex = 0,
                quizSelectedOption = null,
                quizIsChecked = false,
                quizCorrectCount = 0
            )
        }
    }

    fun selectQuizOption(index: Int) {
        if (_uiState.value.quizIsChecked) return
        _uiState.update { it.copy(quizSelectedOption = index) }
    }

    fun checkQuizAnswer() {
        val state = _uiState.value
        val question = quizQuestions.getOrNull(state.currentQuizIndex) ?: return
        val selected = state.quizSelectedOption ?: return

        val isRight = selected == question.correctIndex
        _uiState.update {
            it.copy(
                quizIsChecked = true,
                quizCorrectCount = if (isRight) it.quizCorrectCount + 1 else it.quizCorrectCount,
                xpEarned = if (isRight) it.xpEarned + 5 else it.xpEarned
            )
        }
    }

    fun nextQuizQuestion() {
        val state = _uiState.value
        if (state.currentQuizIndex >= quizQuestions.size - 1) {
            _uiState.update { it.copy(stage = FlowStage.DONE) }
        } else {
            _uiState.update {
                it.copy(
                    currentQuizIndex = it.currentQuizIndex + 1,
                    quizSelectedOption = null,
                    quizIsChecked = false
                )
            }
        }
    }

    fun resetLesson() {
        _uiState.update {
            MoneyFlowState()
        }
        updateCoachMessage()
    }

    fun getRowImpact(block: MoneyBlock): RowImpact? {
        val state = _uiState.value
        val amount = state.allocations[block.id] ?: 0
        val isShockPhase = state.stage == FlowStage.SURPRISE
        val plan1 = state.plan1

        return when (block.kind) {
            BlockKind.SHOCK -> RowImpact(ImpactTone.BAD, "Due today. It cannot wait.")
            BlockKind.NEED -> {
                if (amount < block.min) {
                    val short = block.min - amount
                    val reason = when (block.id) {
                        "rent" -> "Short $$short. Late fee of $45 & notice."
                        "food" -> "Short $$short. Won't last the month."
                        "transport" -> "Short $$short. Transit becomes unreliable."
                        else -> "Short $$short. Missed payment: $35 fee."
                    }
                    RowImpact(ImpactTone.BAD, reason)
                } else if (isShockPhase && plan1 != null && amount < (plan1[block.id] ?: 0)) {
                    val trimmed = (plan1[block.id] ?: 0) - amount
                    RowImpact(ImpactTone.GOOD, "Trimmed $$trimmed. Still covered.")
                } else {
                    RowImpact(ImpactTone.GOOD, "Covered")
                }
            }
            BlockKind.GOAL -> {
                if (isShockPhase && plan1 != null) {
                    val before = plan1["savings"] ?: 0
                    if (before == 0) RowImpact(ImpactTone.WARN, "No cushion to draw on.")
                    else if (amount < before) RowImpact(ImpactTone.WARN, "Used $${before - amount} of your cushion.")
                    else RowImpact(ImpactTone.GOOD, "Cushion untouched.")
                } else {
                    if (amount > 0) RowImpact(ImpactTone.GOOD, "$$amount set aside for surprises.")
                    else RowImpact(ImpactTone.WARN, "Nothing set aside for surprises.")
                }
            }
            BlockKind.WANT -> {
                if (isShockPhase && plan1 != null && amount < (plan1[block.id] ?: 0)) {
                    val cut = (plan1[block.id] ?: 0) - amount
                    RowImpact(ImpactTone.WARN, "Skipping $$cut of fun.")
                } else null
            }
        }
    }

    fun getLockStatus(): LockStatus {
        val state = _uiState.value
        val unassigned = state.unassignedDollars

        if (state.stage == FlowStage.BUILD) {
            val needs = blocks.filter { it.kind == BlockKind.NEED }
            val missing = needs.filter { !state.isPlaced(it.id) }
            if (missing.isNotEmpty()) {
                val nextName = missing.first().name
                val more = if (missing.size > 1) " & ${missing.size - 1} more" else ""
                return LockStatus(false, "Add $nextName$more")
            }

            val underfunded = needs.find { (state.allocations[it.id] ?: 0) < it.min }
            if (underfunded != null) {
                return LockStatus(false, "Fund ${underfunded.name} to $${underfunded.min}")
            }

            if (unassigned > 0) {
                return LockStatus(false, "Assign the last $$unassigned")
            }

            return LockStatus(true, "Lock in my budget")
        }

        if (state.stage == FlowStage.SURPRISE) {
            if (!state.isPlaced("repair")) return LockStatus(false, "Waiting for repair bill")
            if (unassigned < 0) return LockStatus(false, "Find $${-unassigned} more")
            if (unassigned > 0) return LockStatus(false, "Assign the last $$unassigned")
            return LockStatus(true, "Lock in the new plan")
        }

        return LockStatus(true, "Continue")
    }

    private fun updateCoachMessage() {
        val state = _uiState.value
        val unassigned = state.unassignedDollars
        val placedCount = state.allocations.size

        val message = when (state.stage) {
            FlowStage.BUILD -> {
                val needs = blocks.filter { it.kind == BlockKind.NEED }
                val missingNeeds = needs.filter { !state.isPlaced(it.id) }

                if (placedCount == 0) {
                    "You earn $${state.totalIncome} a month. Tap blocks below to give those dollars a job. Start with what you can't skip."
                } else if (missingNeeds.isNotEmpty()) {
                    val nonNeedPlaced = state.allocations.keys.any { id -> blocks.find { it.id == id }?.kind != BlockKind.NEED }
                    if (nonNeedPlaced) {
                        "Wants are in before ${missingNeeds.first().name.lowercase()}. Bills arrive whether or not money is left, so cover them first."
                    } else {
                        "Good start. Still to place: ${missingNeeds.joinToString(", ") { it.name }}."
                    }
                } else {
                    val underfunded = needs.find { (state.allocations[it.id] ?: 0) < it.min }
                    if (underfunded != null) {
                        "${underfunded.name} is $${underfunded.min - (state.allocations[underfunded.id] ?: 0)} under its minimum. That bill still arrives in full."
                    } else if (unassigned > 0) {
                        if ((state.allocations["savings"] ?: 0) == 0) {
                            "Needs are covered with $$unassigned to spare. Dollars with no job tend to disappear. Where should they go?"
                        } else {
                            "$$unassigned still has no job. Give it one before you lock in."
                        }
                    } else {
                        if ((state.allocations["savings"] ?: 0) == 0) {
                            "Every dollar has a job, but nothing is set aside for surprises. Keep that in mind."
                        } else {
                            "Bills are covered and $${state.allocations["savings"]} is set aside. Lock it in when you're happy."
                        }
                    }
                }
            }
            FlowStage.SURPRISE -> {
                val needs = blocks.filter { it.kind == BlockKind.NEED }
                val underfunded = needs.filter { (state.allocations[it.id] ?: 0) < it.min }
                val plan1Savings = state.plan1?.get("savings") ?: 0
                val currentSavings = state.allocations["savings"] ?: 0

                if (unassigned < 0) {
                    if (underfunded.isNotEmpty()) {
                        "${underfunded.first().name} is now short. Missing a bill adds fees. What else could give?"
                    } else if (plan1Savings > 0 && currentSavings == plan1Savings) {
                        "You still need $${-unassigned}. Your savings exist for moments like this."
                    } else if (plan1Savings == 0) {
                        "You still need $${-unassigned}. With nothing saved, it has to come out of this month's plans."
                    } else {
                        "$${-unassigned} still unfunded. Keep trimming."
                    }
                } else if (unassigned > 0) {
                    "You freed $$unassigned more than needed. Give it back to a block."
                } else if (underfunded.isNotEmpty()) {
                    "Repair paid, but ${underfunded.joinToString(", ") { it.name }} fell short. Check the fees before locking in."
                } else {
                    "Repair paid and every bill is safe. Lock it in when you're ready."
                }
            }
            else -> ""
        }

        _uiState.update { it.copy(coachMessage = message) }
    }
}
