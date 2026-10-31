package com.trading.app.presentation.onboarding

data class OnboardingScreenData(
    val title: String,
    val cardHeading: String,
    val currentValueDisplay: String,
    val unitLabel: String,
    val defaultVal: Int,
    val minVal: Int,
    val maxVal: Int,
    val step: Int,
    val switchLabel: String,
    val defaultSwitchState: Boolean,
    val secondaryText: String? = null
)

val OnboardingScreensList = listOf(
    // Screen 1: Risk Rule Setup (Exact visual match to referenced image)
    OnboardingScreenData(
        title = "Set your trade\nrules and start\ntrading",
        cardHeading = "Get a risk alert to\ncut loss if trade hits\ndefined risk for",
        currentValueDisplay = "8",
        unitLabel = "hours",
        defaultVal = 8,
        minVal = 1,
        maxVal = 24,
        step = 1,
        switchLabel = "On",
        defaultSwitchState = true,
        secondaryText = "Strict 1% capital preservation rule"
    ),
    // Screen 2: Risk-to-Reward Ratio
    OnboardingScreenData(
        title = "Automate your\nprofit targets and\nloss limits",
        cardHeading = "Auto-calculate 2:1\nreward-to-risk ratio\nfor minimum target of",
        currentValueDisplay = "2",
        unitLabel = "x target",
        defaultVal = 2,
        minVal = 1,
        maxVal = 5,
        step = 1,
        switchLabel = "Active",
        defaultSwitchState = true,
        secondaryText = "Safeguard positive statistical expectancy"
    ),
    // Screen 3: Position Sizing & Max Drawdown
    OnboardingScreenData(
        title = "Control position\nsizing and leverage\nexposure",
        cardHeading = "Cap single-trade max\naccount exposure to\nstrict threshold of",
        currentValueDisplay = "5",
        unitLabel = "% equity",
        defaultVal = 5,
        minVal = 1,
        maxVal = 20,
        step = 1,
        switchLabel = "Locked",
        defaultSwitchState = true,
        secondaryText = "Prevent margin call liquidation spirals"
    ),
    // Screen 4: Market Signals & Learning Goal
    OnboardingScreenData(
        title = "Complete setup\nand launch your\nsimulator",
        cardHeading = "Daily simulated lessons\nand market drill alerts\nscheduled every",
        currentValueDisplay = "1",
        unitLabel = "day",
        defaultVal = 1,
        minVal = 1,
        maxVal = 7,
        step = 1,
        switchLabel = "Ready",
        defaultSwitchState = true,
        secondaryText = "Build trading mastery without financial risk"
    )
)
