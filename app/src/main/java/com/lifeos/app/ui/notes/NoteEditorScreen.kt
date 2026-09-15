package com.lifeos.app.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.ai.NoteAiAction
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.domain.model.NoteBlock

@Composable
fun NoteEditorScreen(noteId: String?, onBack: () -> Unit) {
    val locator = LocalServiceLocator.current
    val viewModel: NoteEditorViewModel = viewModel(
        factory = LambdaViewModelFactory {
            NoteEditorViewModel(locator.noteRepository, locator.taskRepository, locator.aiRepository, noteId)
        }
    )

    val title by viewModel.title.collectAsState()
    val blocks by viewModel.blocks.collectAsState()
    val aiBusy by viewModel.aiBusy.collectAsState()
    val aiResult by viewModel.aiResult.collectAsState()
    val extractedTasks by viewModel.extractedTasks.collectAsState()

    var showAiMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.save(); onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.addChecklistBlock() }) {
                        Icon(Icons.Filled.Checklist, contentDescription = "Add checklist")
                    }
                    IconButton(onClick = { viewModel.addParagraphBlock() }) {
                        Icon(Icons.Filled.Notes, contentDescription = "Add paragraph")
                    }
                    IconButton(onClick = { showAiMenu = true }) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI actions")
                    }
                    DropdownMenu(expanded = showAiMenu, onDismissRequest = { showAiMenu = false }) {
                        NoteAiAction.entries.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.label) },
                                onClick = { showAiMenu = false; viewModel.runAiAction(action) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Extract tasks") },
                            onClick = { showAiMenu = false; viewModel.extractTasks() }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TextField(
                value = title,
                onValueChange = viewModel::updateTitle,
                placeholder = { Text("Title") },
                textStyle = MaterialTheme.typography.headlineMedium,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)) {
                items(blocks, key = { it.id }) { block ->
                    NoteBlockEditor(block, onTextChange = { viewModel.updateBlockText(block.id, it) }, onToggle = { viewModel.toggleChecklistItem(block.id) })
                }
            }

            if (aiBusy) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    Text("Thinking…")
                }
            }
        }

        aiResult?.let { result ->
            AlertDialog(
                onDismissRequest = viewModel::dismissAiResult,
                title = { Text("AI result") },
                text = { Text(result) },
                confirmButton = { TextButton(onClick = viewModel::dismissAiResult) { Text("Close") } }
            )
        }

        if (extractedTasks.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Extracted tasks") },
                text = {
                    Column {
                        extractedTasks.forEach { Text("• $it") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.approveExtractedTasks(extractedTasks) }) { Text("CREATE TASKS") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.approveExtractedTasks(emptyList()) }) { Text("Discard") }
                }
            )
        }
    }
}

@Composable
private fun NoteBlockEditor(block: NoteBlock, onTextChange: (String) -> Unit, onToggle: () -> Unit) {
    when (block) {
        is NoteBlock.ChecklistItem -> {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = block.checked, onCheckedChange = { onToggle() })
                OutlinedTextField(
                    value = block.text,
                    onValueChange = onTextChange,
                    placeholder = { Text("Checklist item") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        is NoteBlock.Heading -> {
            OutlinedTextField(
                value = block.text,
                onValueChange = onTextChange,
                placeholder = { Text("Heading") },
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
        is NoteBlock.BulletItem -> {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("•  ")
                OutlinedTextField(value = block.text, onValueChange = onTextChange, modifier = Modifier.fillMaxWidth())
            }
        }
        is NoteBlock.NumberedItem -> {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("${block.index}.  ")
                OutlinedTextField(value = block.text, onValueChange = onTextChange, modifier = Modifier.fillMaxWidth())
            }
        }
        is NoteBlock.Paragraph -> {
            OutlinedTextField(
                value = block.text,
                onValueChange = onTextChange,
                placeholder = { Text("Start writing…") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
    }
}
