package com.lifeos.app.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.SettingsStore
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSPageHeader
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.lifeOSIsDark
import com.lifeos.app.ui.theme.LifeOSMint
import com.lifeos.app.ui.theme.lifeOSCanvasBrush
import kotlinx.coroutines.launch

private enum class SetupStep { CREATE_PIN, CONFIRM_PIN, RECOVERY, DONE }

private val RECOVERY_QUESTION_PRESETS = listOf(
    "What was the name of your first school?",
    "What is your favorite childhood nickname?",
    "What city were you born in?",
    "Custom question…"
)

/**
 * The one-time App Lock setup: PIN, confirm, recovery question and answer.
 *
 * Everything is inset-free on purpose. This is a [androidx.navigation.compose]
 * destination, so `LifeOSNavHost` has already padded it for the status bar, the
 * bottom navigation and the system navigation area — adding `.statusBarsPadding()`
 * / `.navigationBarsPadding()` here applied those same insets a second time.
 *
 * Navigation is a single top-level Back that steps the flow backwards and leaves
 * it on the first step, instead of one Back per card plus a second at the top.
 * The recovery question and its answer are one step, because splitting them made
 * the user press Next to answer a question they had just been shown.
 */
@Composable
fun AppLockSetupScreen(onBack: () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(SetupStep.CREATE_PIN) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var recoveryQuestion by remember { mutableStateOf(RECOVERY_QUESTION_PRESETS.first()) }
    var customQuestion by remember { mutableStateOf("") }
    var recoveryAnswer by remember { mutableStateOf("") }
    var showQuestionMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun finalQuestion() = if (recoveryQuestion == "Custom question…") customQuestion.trim() else recoveryQuestion

    fun goBack() {
        error = null
        when (step) {
            SetupStep.CREATE_PIN -> onBack()
            SetupStep.CONFIRM_PIN -> {
                pin = ""
                confirmPin = ""
                step = SetupStep.CREATE_PIN
            }
            SetupStep.RECOVERY -> step = SetupStep.CONFIRM_PIN
            SetupStep.DONE -> onBack()
        }
    }

    val dark = lifeOSIsDark()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lifeOSCanvasBrush(dark))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // One Back, at the top, for the whole flow.
        if (step != SetupStep.DONE) {
            LifeOSPageHeader(
                eyebrow = "Privacy",
                title = "App Lock",
                onBack = ::goBack
            )
        } else {
            LifeOSPageHeader(
                eyebrow = "Privacy",
                title = "App Lock"
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "LifeOS uses a private 4-digit PIN only. No fingerprint or face unlock is used.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (step) {
                SetupStep.CREATE_PIN -> {
                    Text("Create your PIN", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "Choose exactly 4 digits. Your PIN is stored only as a salted hash.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LifeOSCard(Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PinDots(pin.length, Modifier.align(Alignment.CenterHorizontally))
                            PinKeypad(
                                onDigit = { digit ->
                                    if (pin.length < SettingsStore.PIN_LENGTH) { pin += digit; error = null }
                                },
                                onBackspace = { pin = pin.dropLast(1) }
                            )
                            LaunchedEffect(pin) {
                                if (pin.length == SettingsStore.PIN_LENGTH) step = SetupStep.CONFIRM_PIN
                            }
                        }
                    }
                }

                SetupStep.CONFIRM_PIN -> {
                    Text("Confirm your PIN", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "Re-enter the same 4 digits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    error?.let {
                        Text(
                            it,
                            color = LifeOSMint,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    LifeOSCard(Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PinDots(
                                confirmPin.length,
                                Modifier.align(Alignment.CenterHorizontally),
                                isError = error != null
                            )
                            PinKeypad(
                                onDigit = { digit ->
                                    if (confirmPin.length < SettingsStore.PIN_LENGTH) { confirmPin += digit; error = null }
                                },
                                onBackspace = { confirmPin = confirmPin.dropLast(1) }
                            )
                            LaunchedEffect(confirmPin) {
                                if (confirmPin.length == SettingsStore.PIN_LENGTH) {
                                    if (confirmPin == pin) {
                                        step = SetupStep.RECOVERY
                                    } else {
                                        error = "PINs don't match. Try again."
                                        confirmPin = ""
                                    }
                                }
                            }
                        }
                    }
                }

                SetupStep.RECOVERY -> {
                    Text("Set up recovery", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "If you forget your PIN, you must answer this question before creating a new one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LifeOSCard(Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            androidx.compose.foundation.layout.Column {
                                OutlinedButton(onClick = { showQuestionMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(finalQuestion())
                                }
                                DropdownMenu(expanded = showQuestionMenu, onDismissRequest = { showQuestionMenu = false }) {
                                    RECOVERY_QUESTION_PRESETS.forEach { question ->
                                        DropdownMenuItem(
                                            text = { Text(question) },
                                            onClick = { recoveryQuestion = question; showQuestionMenu = false }
                                        )
                                    }
                                }
                            }
                            if (recoveryQuestion == "Custom question…") {
                                OutlinedTextField(
                                    value = customQuestion,
                                    onValueChange = { customQuestion = it; error = null },
                                    label = { Text("Your question") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            OutlinedTextField(
                                value = recoveryAnswer,
                                onValueChange = { recoveryAnswer = it; error = null },
                                label = { Text("Your answer") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    error?.let {
                        Text(
                            it,
                            color = LifeOSMint,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // The button only earns a press once both halves of the step are
                    // filled, so "Finish" can never fire an empty question or answer.
                    LifeOSPrimaryButton(
                        text = "Finish setup",
                        onClick = {
                            scope.launch {
                                locator.settingsStore.enablePinLock(pin, finalQuestion(), recoveryAnswer)
                                step = SetupStep.DONE
                            }
                        },
                        enabled = finalQuestion().isNotBlank() && recoveryAnswer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SetupStep.DONE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = LifeOSMint)
                        Text(
                            "App Lock updated",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        LifeOSPrimaryButton(text = "Done", onClick = onBack, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
