package com.lifeos.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.AppLockType
import com.lifeos.app.core.util.PinAttemptResult
import com.lifeos.app.core.util.RecoveryAttemptResult
import com.lifeos.app.core.util.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class RecoveryStep { NONE, ANSWER_QUESTION, NEW_PIN, NEW_PIN_CONFIRM }

@Composable
fun AppLockScreen(lockType: AppLockType, onUnlocked: () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()

    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var recoveryStep by remember { mutableStateOf(RecoveryStep.NONE) }
    var recoveryAnswerInput by remember { mutableStateOf("") }
    var recoveryQuestionText by remember { mutableStateOf<String?>(null) }
    var newPin by remember { mutableStateOf("") }
    var newPinConfirm by remember { mutableStateOf("") }

    fun submitPin() {
        scope.launch {
            when (val result = locator.settingsStore.attemptPinUnlock(pinInput)) {
                PinAttemptResult.Success -> {
                    error = null
                    onUnlocked()
                }
                is PinAttemptResult.Incorrect -> {
                    error = if (result.attemptsRemaining <= 0) "Incorrect PIN."
                    else "Incorrect PIN. ${result.attemptsRemaining} attempt(s) left."
                    pinInput = ""
                }
                is PinAttemptResult.LockedOut -> {
                    val seconds = (result.remainingMillis / 1000L).coerceAtLeast(1)
                    error = "Too many attempts. Try again in $seconds second(s)."
                    pinInput = ""
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.padding(bottom = 16.dp))
        Text("LifeOS is locked", style = MaterialTheme.typography.titleLarge)
        Text(
            "Enter your 4-digit LifeOS PIN to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
        }

        when (recoveryStep) {
            RecoveryStep.ANSWER_QUESTION -> {
                Text(recoveryQuestionText ?: "", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                OutlinedTextField(
                    value = recoveryAnswerInput,
                    onValueChange = { recoveryAnswerInput = it },
                    label = { Text("Your answer") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Button(
                    onClick = {
                        scope.launch {
                            when (val result = locator.settingsStore.attemptRecoveryAnswer(recoveryAnswerInput)) {
                                RecoveryAttemptResult.Success -> {
                                    error = null
                                    newPin = ""
                                    recoveryStep = RecoveryStep.NEW_PIN
                                }
                                is RecoveryAttemptResult.Incorrect -> {
                                    error = if (result.attemptsRemaining <= 0) "That doesn't match."
                                    else "That doesn't match. ${result.attemptsRemaining} attempt(s) left."
                                    recoveryAnswerInput = ""
                                }
                                is RecoveryAttemptResult.LockedOut -> {
                                    val seconds = (result.remainingMillis / 1000L).coerceAtLeast(1)
                                    error = "Too many attempts. Try again in $seconds second(s)."
                                    recoveryAnswerInput = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("Verify") }
                TextButton(onClick = { recoveryStep = RecoveryStep.NONE; error = null }) { Text("Cancel") }
            }

            RecoveryStep.NEW_PIN -> {
                Text("Choose a new 4-digit PIN", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                PinDots(newPin.length, Modifier.padding(top = 12.dp))
                PinKeypad(
                    onDigit = { digit -> if (newPin.length < SettingsStore.PIN_LENGTH) { newPin += digit; error = null } },
                    onBackspace = { newPin = newPin.dropLast(1) },
                    modifier = Modifier.padding(top = 16.dp)
                )
                LaunchedEffect(newPin) {
                    if (newPin.length == SettingsStore.PIN_LENGTH) {
                        newPinConfirm = ""
                        recoveryStep = RecoveryStep.NEW_PIN_CONFIRM
                    }
                }
            }

            RecoveryStep.NEW_PIN_CONFIRM -> {
                Text("Confirm your new PIN", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                PinDots(newPinConfirm.length, Modifier.padding(top = 12.dp))
                PinKeypad(
                    onDigit = { digit -> if (newPinConfirm.length < SettingsStore.PIN_LENGTH) { newPinConfirm += digit; error = null } },
                    onBackspace = { newPinConfirm = newPinConfirm.dropLast(1) },
                    modifier = Modifier.padding(top = 16.dp)
                )
                LaunchedEffect(newPinConfirm) {
                    if (newPinConfirm.length == SettingsStore.PIN_LENGTH) {
                        if (newPinConfirm != newPin) {
                            error = "PINs don't match. Try again."
                            newPinConfirm = ""
                        } else {
                            scope.launch {
                                locator.settingsStore.enablePinLock(
                                    newPin,
                                    recoveryQuestionText.orEmpty(),
                                    recoveryAnswerInput
                                )
                                onUnlocked()
                            }
                        }
                    }
                }
            }

            RecoveryStep.NONE -> {
                PinDots(pinInput.length, Modifier.padding(top = 24.dp), isError = error != null)
                PinKeypad(
                    onDigit = { digit -> if (pinInput.length < SettingsStore.PIN_LENGTH) { pinInput += digit; error = null } },
                    onBackspace = { pinInput = pinInput.dropLast(1) },
                    modifier = Modifier.padding(top = 24.dp)
                )
                LaunchedEffect(pinInput) {
                    if (pinInput.length == SettingsStore.PIN_LENGTH) submitPin()
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            val question = locator.settingsStore.recoveryQuestion.first()
                            if (question.isNullOrBlank()) error = "No recovery question was set up for this PIN."
                            else {
                                recoveryQuestionText = question
                                recoveryAnswerInput = ""
                                error = null
                                recoveryStep = RecoveryStep.ANSWER_QUESTION
                            }
                        }
                    }
                ) { Text("Forgot PIN?") }
            }
        }
    }
}
