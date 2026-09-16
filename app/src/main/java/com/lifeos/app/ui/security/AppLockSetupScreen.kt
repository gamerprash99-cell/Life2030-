package com.lifeos.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSGradientButton
import kotlinx.coroutines.launch

private enum class SetupStep { CREATE_PIN, CONFIRM_PIN, RECOVERY_QUESTION, RECOVERY_ANSWER, DONE }

private val RECOVERY_QUESTION_PRESETS = listOf(
    "What was the name of your first school?",
    "What is your favorite childhood nickname?",
    "What city were you born in?",
    "Custom question…"
)

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

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("App Lock", style = MaterialTheme.typography.headlineMedium)
        Text(
            "LifeOS uses a private PIN only. No fingerprint or face unlock is used.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when (step) {
            SetupStep.CREATE_PIN -> {
                LifeOSCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Create your PIN", style = MaterialTheme.typography.titleLarge)
                        Text("Choose 4–6 digits. Your PIN is stored only as a salted hash.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                            label = { Text("PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        LifeOSGradientButton(
                            text = "Continue",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (pin.length < 4) error = "PIN must be at least 4 digits."
                                else { error = null; step = SetupStep.CONFIRM_PIN }
                            }
                        )
                    }
                }
            }

            SetupStep.CONFIRM_PIN -> {
                LifeOSCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Confirm your PIN", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                            label = { Text("Re-enter PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Button(
                            onClick = {
                                if (confirmPin != pin) error = "PINs don't match. Try again."
                                else { error = null; step = SetupStep.RECOVERY_QUESTION }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Next") }
                        TextButton(onClick = { step = SetupStep.CREATE_PIN }) { Text("Back") }
                    }
                }
            }

            SetupStep.RECOVERY_QUESTION -> {
                LifeOSCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Set up recovery", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "If you forget your PIN, you must answer this question before creating a new one.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Column {
                            OutlinedButton(onClick = { showQuestionMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(recoveryQuestion)
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
                                onValueChange = { customQuestion = it },
                                label = { Text("Your question") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Button(
                            onClick = { if (finalQuestion().isNotBlank()) step = SetupStep.RECOVERY_ANSWER },
                            enabled = finalQuestion().isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Next") }
                        TextButton(onClick = { step = SetupStep.CONFIRM_PIN }) { Text("Back") }
                    }
                }
            }

            SetupStep.RECOVERY_ANSWER -> {
                LifeOSCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Recovery answer", style = MaterialTheme.typography.titleLarge)
                        Text(finalQuestion(), style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = recoveryAnswer,
                            onValueChange = { recoveryAnswer = it },
                            label = { Text("Answer") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (recoveryAnswer.isBlank()) error = "Enter an answer."
                                else {
                                    scope.launch {
                                        locator.settingsStore.enablePinLock(pin, finalQuestion(), recoveryAnswer)
                                        step = SetupStep.DONE
                                    }
                                }
                            },
                            enabled = recoveryAnswer.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Finish setup") }
                        TextButton(onClick = { step = SetupStep.RECOVERY_QUESTION }) { Text("Back") }
                    }
                }
            }

            SetupStep.DONE -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("App Lock updated", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onBack) { Text("Done") }
                }
            }
        }
    }
}
