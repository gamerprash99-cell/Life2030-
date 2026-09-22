package com.lifeos.app.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.life.LifeController
import com.lifeos.app.core.life.LifeDestination
import com.lifeos.app.core.ai.AiResult
import com.lifeos.app.core.ai.ChatMessage
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.GlassCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.lifeos.app.core.life.LifeConversation
import com.lifeos.app.core.life.LifeAppContext
import com.lifeos.app.core.life.LifeSection
import com.lifeos.app.core.life.LifeActionFeedback
import com.lifeos.app.core.life.LifeSessionMemory
import com.lifeos.app.core.ai.runtime.LifeVoiceInputController
import com.lifeos.app.core.util.LifeOSPermissions
import com.lifeos.app.core.util.PermissionStatus
import com.lifeos.app.core.util.rememberPermissionState

class AiAssistantViewModel(private val lifeController: LifeController) : ViewModel() {
    private val conversation = LifeConversation()
    private val sessionMemory = LifeSessionMemory()
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("assistant", "Hi! I'm LIFE — your private, offline LifeOS assistant. Tell me what to save, find, or change."))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _requiresConfirmation = MutableStateFlow(false)
    val requiresConfirmation: StateFlow<Boolean> = _requiresConfirmation

    private val _latestDestination = MutableStateFlow<LifeDestination?>(null)

    private val _latestFeedback = MutableStateFlow<LifeActionFeedback?>(null)
    val latestFeedback: StateFlow<LifeActionFeedback?> = _latestFeedback
    val latestDestination: StateFlow<LifeDestination?> = _latestDestination

    fun send(text: String, context: LifeAppContext = LifeAppContext()) {
        if (text.isBlank() || _busy.value) return
        conversation.addUser(text)
        _messages.value = _messages.value + ChatMessage("user", text)
        viewModelScope.launch {
            _busy.value = true
            runCatching { lifeController.handle(text, context, conversation.previousUserText()) }
                .onSuccess { result ->
                    conversation.addAssistant(result.message)
                    sessionMemory.remember(result)
                    _messages.value = _messages.value + ChatMessage("assistant", result.message)
                    _requiresConfirmation.value = result.requiresConfirmation
                    _latestDestination.value = result.destination
                    _latestFeedback.value = result.feedback
                }
                .onFailure { error ->
                    val message = "Sorry, LIFE hit an error: ${error.message ?: "Unknown error"}"
                    conversation.addAssistant(message)
                    _messages.value = _messages.value + ChatMessage("assistant", message)
                    _requiresConfirmation.value = false
                }
            _busy.value = false
        }
    }

    fun confirm(context: LifeAppContext = LifeAppContext()) {
        _requiresConfirmation.value = false
        send("confirm", context)
    }

    fun cancel(context: LifeAppContext = LifeAppContext()) {
        lifeController.cancelPendingAction()
        _requiresConfirmation.value = false
        send("cancel", context)
    }

    fun clearConversation() {
        lifeController.cancelPendingAction()
        conversation.clear()
        sessionMemory.clear()
        _requiresConfirmation.value = false
        _latestDestination.value = null
        _latestFeedback.value = null
        _messages.value = listOf(ChatMessage("assistant", "Conversation cleared. I'm still here, fully offline."))
    }
}

@Composable
fun AiAssistantScreen(
    onOpenDestination: (LifeDestination) -> Unit = {},
    onOpenFeedback: (LifeActionFeedback) -> Unit = {},
    onBack: () -> Unit = {},
    appContext: LifeAppContext = LifeAppContext()
) {
    val locator = LocalServiceLocator.current
    val viewModel: AiAssistantViewModel = viewModel(factory = LambdaViewModelFactory { AiAssistantViewModel(locator.lifeController) })
    val messages by viewModel.messages.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val requiresConfirmation by viewModel.requiresConfirmation.collectAsState()
    val latestDestination by viewModel.latestDestination.collectAsState()
    val latestFeedback by viewModel.latestFeedback.collectAsState()
    var input by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf<String?>(null) }
    var voiceLanguage by remember { mutableStateOf("en-IN") }
    var voiceBusy by remember { mutableStateOf(false) }
    val androidContext = androidx.compose.ui.platform.LocalContext.current
    val microphone = rememberPermissionState(LifeOSPermissions.RECORD_AUDIO)
    val voice = remember(androidContext) { LifeVoiceInputController(androidContext) }
    DisposableEffect(Unit) { onDispose { voice.stop() } }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("LIFE") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            }
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    val isUser = message.role == "user"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                        GlassCard {
                            Text(message.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (busy) {
                    item {
                        Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                            Text("Thinking…")
                        }
                    }
                }
                latestFeedback?.let { feedback ->
                    item {
                        androidx.compose.material3.Button(
                            onClick = { onOpenFeedback(feedback) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(feedback.label) }
                    }
                } ?: latestDestination?.let { destination ->
                    item {
                        androidx.compose.material3.Button(
                            onClick = { onOpenDestination(destination) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Open ${destination.javaClass.simpleName.removeSuffix("Kt")}") }
                    }
                }
            }

            if (requiresConfirmation) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { viewModel.cancel(appContext) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                    Spacer(modifier = Modifier.height(0.dp))
                    androidx.compose.material3.Button(onClick = { viewModel.confirm(appContext) }) { Text("Confirm") }
                }
            }

            voiceStatus?.let { status ->
                Text(status, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.material3.TextButton(
                    onClick = {
                        voiceLanguage = when (voiceLanguage) {
                            "en-IN" -> "hi-IN"
                            "hi-IN" -> "gu-IN"
                            else -> "en-IN"
                        }
                    },
                    enabled = !listening
                ) {
                    Text(when (voiceLanguage) {
                        "hi-IN" -> "हिन्दी"
                        "gu-IN" -> "ગુજરાતી"
                        else -> "English"
                    })
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    placeholder = { Text("Ask LIFE…") },
                    modifier = Modifier.weight(1f),
                    singleLine = false,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    keyboardActions = KeyboardActions(onDone = {
                        if (!voiceBusy && input.isNotBlank()) {
                            viewModel.send(input, appContext)
                            input = ""
                        }
                    })
                )
                IconButton(onClick = {
                    if (listening) {
                        voice.stop()
                        listening = false
                        voiceBusy = false
                        voiceStatus = null
                    } else when (microphone.status) {
                        PermissionStatus.GRANTED -> {
                            if (!voice.isAvailable()) {
                                voiceStatus = "On-device voice recognition isn't available on this device."
                            } else {
                                listening = true
                                voiceBusy = true
                                voiceStatus = "Listening offline…"
                                voice.start(
                                    languageTags = listOf(voiceLanguage),
                                    onPartial = { partial -> input = partial },
                                    onResult = { result ->
                                        input = result
                                        listening = false
                                        voiceBusy = false
                                        voiceStatus = "Voice command received — sending to LIFE…"
                                        viewModel.send(result, appContext)
                                        input = ""
                                        voiceStatus = null
                                    },
                                    onError = { message ->
                                        listening = false
                                        voiceBusy = false
                                        voiceStatus = message
                                    }
                                )
                            }
                        }
                        PermissionStatus.PERMANENTLY_DENIED -> microphone.openSettings()
                        PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE -> microphone.request()
                    }
                }) {
                    Icon(if (listening) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = if (listening) "Stop voice input" else "Voice input")
                }
                IconButton(onClick = { viewModel.send(input, appContext); input = "" }) {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
