package com.lifeos.app.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.ai.AiRepository
import com.lifeos.app.core.ai.AiResult
import com.lifeos.app.core.ai.ChatMessage
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.GlassCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AiAssistantViewModel(private val aiRepository: AiRepository) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("assistant", "Hi! I'm your LifeOS AI Assistant — I run fully offline on your device. Ask me about your tasks, habits, spending, or mood."))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun send(text: String) {
        if (text.isBlank()) return
        val updated = _messages.value + ChatMessage("user", text)
        _messages.value = updated
        viewModelScope.launch {
            _busy.value = true
            when (val result = aiRepository.chat(updated)) {
                is AiResult.Success -> _messages.value = _messages.value + ChatMessage("assistant", result.text)
                is AiResult.Error -> _messages.value = _messages.value + ChatMessage("assistant", "Sorry, I hit an error: ${result.message}")
            }
            _busy.value = false
        }
    }
}

@Composable
fun AiAssistantScreen() {
    val locator = LocalServiceLocator.current
    val viewModel: AiAssistantViewModel = viewModel(factory = LambdaViewModelFactory { AiAssistantViewModel(locator.aiRepository) })
    val messages by viewModel.messages.collectAsState()
    val busy by viewModel.busy.collectAsState()
    var input by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("LifeOS AI") }) }) { padding ->
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
            }

            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    placeholder = { Text("Ask LifeOS AI…") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.send(input); input = "" }) {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
