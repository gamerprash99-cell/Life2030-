package com.lifeos.app.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.AppLockType
import com.lifeos.app.core.util.PinAttemptResult
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

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
            Text("LifeOS is locked", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 18.dp))
            Text(
                if (recoveryStep == RecoveryStep.NONE) "Enter your LifeOS PIN to continue." else "Secure recovery stays on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            if (recoveryStep != RecoveryStep.NONE) {
                RecoveryContent(
                    step = recoveryStep,
                    question = recoveryQuestionText,
                    answer = recoveryAnswerInput,
                    onAnswer = { recoveryAnswerInput = it },
                    newPin = newPin,
                    onNewPin = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                    newPinConfirm = newPinConfirm,
                    onConfirm = { if (it.length <= 6 && it.all(Char::isDigit)) newPinConfirm = it },
                    error = error,
                    onVerifyAnswer = {
                        scope.launch {
                            if (locator.settingsStore.verifyRecoveryAnswer(recoveryAnswerInput)) {
                                error = null
                                newPin = ""
                                recoveryStep = RecoveryStep.NEW_PIN
                            } else error = "That doesn't match. Try again."
                        }
                    },
                    onNext = {
                        if (newPin.length < 4) error = "PIN must be at least 4 digits."
                        else { error = null; newPinConfirm = ""; recoveryStep = RecoveryStep.NEW_PIN_CONFIRM }
                    },
                    onReset = {
                        if (newPinConfirm != newPin) error = "PINs don't match. Try again."
                        else scope.launch {
                            locator.settingsStore.enablePinLock(newPin, recoveryQuestionText.orEmpty(), recoveryAnswerInput)
                            onUnlocked()
                        }
                    },
                    onCancel = { recoveryStep = RecoveryStep.NONE; error = null }
                )
            } else {
                PinDots(pinInput)
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(Modifier.weight(1f))
                Keypad(
                    pin = pinInput,
                    onDigit = { if (pinInput.length < 6) pinInput += it },
                    onBackspace = { pinInput = pinInput.dropLast(1) },
                    onUnlock = {
                        scope.launch {
                            when (val result = locator.settingsStore.attemptPinUnlock(pinInput)) {
                                PinAttemptResult.Success -> { error = null; onUnlocked() }
                                is PinAttemptResult.Incorrect -> {
                                    error = "Incorrect PIN. ${result.attemptsRemaining.coerceAtLeast(0)} attempt(s) left."
                                    pinInput = ""
                                }
                                is PinAttemptResult.LockedOut -> {
                                    error = "Too many attempts. Try again in ${(result.remainingMillis / 1000L).coerceAtLeast(1)}s."
                                    pinInput = ""
                                }
                            }
                        }
                    }
                )
                TextButton(onClick = {
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
                }) { Text("Forgot PIN?") }
            }

            Text("Verified • offline & secured on device", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PinDots(pin: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 24.dp)) {
        repeat(6) { index ->
            Box(
                Modifier.size(10.dp).background(
                    if (index < pin.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape
                )
            )
        }
    }
}

@Composable
private fun Keypad(
    pin: String,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onUnlock: () -> Unit
) {
    val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { digit ->
                    Surface(
                        onClick = { onDigit(digit) },
                        modifier = Modifier.weight(1f).size(58.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(digit, style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onBackspace, modifier = Modifier.weight(1f).size(58.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Backspace, contentDescription = "Delete digit") }
            }
            Surface(
                onClick = { onDigit("0") },
                modifier = Modifier.weight(1f).size(58.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) { Box(contentAlignment = Alignment.Center) { Text("0", style = MaterialTheme.typography.titleLarge) } }
            Surface(
                onClick = onUnlock,
                modifier = Modifier.weight(1f).size(58.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Box(contentAlignment = Alignment.Center) { Text("→", style = MaterialTheme.typography.headlineSmall) } }
        }
        Button(onClick = onUnlock, enabled = pin.length >= 4, modifier = Modifier.fillMaxWidth()) { Text("Unlock") }
    }
}

@Composable
private fun RecoveryContent(
    step: RecoveryStep,
    question: String?,
    answer: String,
    onAnswer: (String) -> Unit,
    newPin: String,
    onNewPin: (String) -> Unit,
    newPinConfirm: String,
    onConfirm: (String) -> Unit,
    error: String?,
    onVerifyAnswer: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (step) {
            RecoveryStep.ANSWER_QUESTION -> {
                Text(question.orEmpty(), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(answer, onAnswer, label = { Text("Your answer") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = onVerifyAnswer, modifier = Modifier.fillMaxWidth()) { Text("Verify") }
            }
            RecoveryStep.NEW_PIN -> {
                Text("Choose a new PIN", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(newPin, onNewPin, label = { Text("New PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Next") }
            }
            RecoveryStep.NEW_PIN_CONFIRM -> {
                Text("Confirm your new PIN", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(newPinConfirm, onConfirm, label = { Text("Re-enter PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Reset PIN and unlock") }
            }
            RecoveryStep.NONE -> Unit
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel") }
    }
}
