package com.lifeos.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSGradientButton
import kotlinx.coroutines.launch

private enum class SetupStep { CHOOSE, EXPLAIN_BIOMETRIC, CREATE_PIN, CONFIRM_PIN, RECOVERY_QUESTION, RECOVERY_ANSWER, DONE }

private val RECOVERY_QUESTION_PRESETS = listOf(
    "What was the name of your first pet?",
    "What city were you born in?",
    "What was your childhood nickname?",
    "What's your favorite book?",
    "Custom question…"
)

/**
 * Section 3/4 of the security pass: a real setup flow with clear
 * explanations, rather than a single opaque toggle. See
 * core/util/SettingsStore.kt for how the PIN/recovery answer are hashed
 * (never stored in plaintext) and core/security/AppLockManager.kt for the
 * biometric path and its documented platform limits.
 */
@Composable
fun AppLockSetupScreen(onBack: () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var step by remember { mutableStateOf(SetupStep.CHOOSE) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var recoveryQuestion by remember { mutableStateOf(RECOVERY_QUESTION_PRESETS.first()) }
    var customQuestion by remember { mutableStateOf("") }
    var showQuestionMenu by remember { mutableStateOf(false) }
    var recoveryAnswer by remember { mutableStateOf("") }
    var biometricError by remember { mutableStateOf<String?>(null) }

    fun finalQuestion() = if (recoveryQuestion == "Custom question…") customQuestion else recoveryQuestion

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Lock") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                SetupStep.CHOOSE -> {
                    Text("Choose how LifeOS should be protected", style = MaterialTheme.typography.titleLarge)

                    OptionCard(
                        icon = Icons.Filled.LockOpen,
                        title = "No App Lock",
                        description = "LifeOS opens immediately, like most apps. Anyone with your unlocked phone can open it.",
                        onClick = {
                            scope.launch { locator.settingsStore.disableAppLock() }
                            step = SetupStep.DONE
                        }
                    )
                    OptionCard(
                        icon = Icons.Filled.Fingerprint,
                        title = "Biometric App Lock",
                        description = "Uses your phone's fingerprint/face unlock. Note: this is the same biometric that unlocks your phone itself. Android doesn't allow apps to have a separate fingerprint enrollment.",
                        onClick = { step = SetupStep.EXPLAIN_BIOMETRIC }
                    )
                    OptionCard(
                        icon = Icons.Filled.Pin,
                        title = "App PIN",
                        description = "A separate PIN just for LifeOS, independent of your phone's own lock screen. Recommended if you share your phone or want LifeOS protected even when your phone is unlocked.",
                        onClick = { pin = ""; confirmPin = ""; pinError = null; step = SetupStep.CREATE_PIN }
                    )
                }

                SetupStep.EXPLAIN_BIOMETRIC -> {
                    Text("Biometric App Lock", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "LifeOS uses Android's official biometric prompt. Your fingerprint or face data stays with Android; LifeOS only receives the authentication result.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider()
                    val biometricReady = locator.appLockManager.isBiometricAvailable()
                    if (!biometricReady) {
                        Text(
                            "No usable biometric is enrolled on this device yet. Enroll a fingerprint or face in Android settings, then return here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                val act = activity
                                if (act == null || !locator.appLockManager.openBiometricEnrollment(act)) {
                                    biometricError = "Android could not open biometric enrollment. Open your device Security settings and enroll a biometric."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Set up fingerprint / biometric") }
                    } else {
                        Text(
                            "A biometric is ready. Verify it once to enable LifeOS App Lock.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                val act = activity
                                if (act == null) {
                                    biometricError = "Couldn't start biometric verification here."
                                } else {
                                    locator.appLockManager.authenticate(
                                        activity = act,
                                        onSuccess = {
                                            scope.launch { locator.settingsStore.enableBiometricLock() }
                                            step = SetupStep.DONE
                                        },
                                        onError = { message -> biometricError = message }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Verify and enable") }
                    }
                    biometricError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    TextButton(onClick = { step = SetupStep.CHOOSE }) { Text("Back") }
                }

                SetupStep.CREATE_PIN -> {
                    Text("Create a PIN", style = MaterialTheme.typography.titleLarge)
                    Text("Choose a 4-6 digit PIN.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                        label = { Text("PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    pinError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Button(
                        onClick = {
                            if (pin.length < 4) {
                                pinError = "PIN must be at least 4 digits."
                            } else {
                                pinError = null
                                confirmPin = ""
                                step = SetupStep.CONFIRM_PIN
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Next") }
                    TextButton(onClick = { step = SetupStep.CHOOSE }) { Text("Back") }
                }

                SetupStep.CONFIRM_PIN -> {
                    Text("Confirm your PIN", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                        label = { Text("Re-enter PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    pinError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Button(
                        onClick = {
                            if (confirmPin != pin) {
                                pinError = "PINs don't match. Try again."
                            } else {
                                pinError = null
                                step = SetupStep.RECOVERY_QUESTION
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Next") }
                    TextButton(onClick = { step = SetupStep.CREATE_PIN }) { Text("Back") }
                }

                SetupStep.RECOVERY_QUESTION -> {
                    Text("Set up recovery", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "If you forget your PIN, you'll answer this question to reset it. Your answer is stored securely — LifeOS never keeps it as plain text.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Column {
                        OutlinedButton(onClick = { showQuestionMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(recoveryQuestion)
                        }
                        DropdownMenu(expanded = showQuestionMenu, onDismissRequest = { showQuestionMenu = false }) {
                            RECOVERY_QUESTION_PRESETS.forEach { q ->
                                DropdownMenuItem(text = { Text(q) }, onClick = { recoveryQuestion = q; showQuestionMenu = false })
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
                        modifier = Modifier.fillMaxWidth(),
                        enabled = finalQuestion().isNotBlank()
                    ) { Text("Next") }
                    TextButton(onClick = { step = SetupStep.CONFIRM_PIN }) { Text("Back") }
                }

                SetupStep.RECOVERY_ANSWER -> {
                    Text("Your answer", style = MaterialTheme.typography.titleLarge)
                    Text(finalQuestion(), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = recoveryAnswer,
                        onValueChange = { recoveryAnswer = it },
                        label = { Text("Answer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (recoveryAnswer.isNotBlank()) {
                                scope.launch {
                                    locator.settingsStore.enablePinLock(pin, finalQuestion(), recoveryAnswer)
                                }
                                step = SetupStep.DONE
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = recoveryAnswer.isNotBlank()
                    ) { Text("Finish setup") }
                    TextButton(onClick = { step = SetupStep.RECOVERY_QUESTION }) { Text("Back") }
                }

                SetupStep.DONE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
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
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    LifeOSCard(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 10.dp), tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(description, style = MaterialTheme.typography.bodySmall)
            LifeOSGradientButton(text = "Choose", modifier = Modifier.fillMaxWidth(), onClick = onClick)
        }
    }
}
