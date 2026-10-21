package com.lifeos.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.ai.AiConsent
import com.lifeos.app.core.data.ThemeMode
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.Pill
import com.lifeos.app.ui.components.SectionHeader

/**
 * Settings.
 *
 * The AI section is the security-relevant surface: consent is explicit, graded,
 * revocable in one tap, and the panel states the actual provider and whether it
 * needs the network. No key is stored and no remote provider is wired.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val currencies = listOf("INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "SGD", "AED")
    val context = LocalContext.current
    val shareExport: (String, String) -> Unit = { payload, mime ->
        // The payload is built on-device and handed to whatever app the user picks.
        // LifeOS itself never uploads anything.
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mime
            putExtra(android.content.Intent.EXTRA_SUBJECT, "LifeOS export")
            putExtra(android.content.Intent.EXTRA_TEXT, payload)
        }
        context.startActivity(
            android.content.Intent.createChooser(intent, "Share LifeOS export"),
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(title = "Settings", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { SectionHeader(title = "Appearance") }
            item {
                LifeOsCard {
                    Column {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            ThemeMode.entries.forEach { mode ->
                                Pill(
                                    text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                    selected = settings.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = "Money") }
            item {
                LifeOsCard {
                    Column {
                        Text(
                            text = "Currency",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Amounts are stored as whole minor units in this currency.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            items(currencies) { code ->
                                Pill(
                                    text = code,
                                    selected = settings.currencyCode == code,
                                    onClick = { viewModel.setCurrency(code) },
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = "Habits") }
            item {
                LifeOsCard {
                    Column {
                        Text(
                            text = "Heatmap window",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "How many weeks the habit heatmap shows (5–26).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            listOf(8, 12, 16, 26).forEach { weeks ->
                                Pill(
                                    text = "$weeks w",
                                    selected = settings.heatmapWeeks == weeks,
                                    onClick = { viewModel.setHeatmapWeeks(weeks) },
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = "Notifications") }
            item {
                LifeOsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Reminders",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Task and habit reminders you set. Nothing is sent off-device.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = viewModel::setNotifications,
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "LifeOS AI",
                    subtitle = "Consent is explicit, graded and revocable at any time",
                )
            }
            item {
                LifeOsCard {
                    Column {
                        Text(
                            text = "What is running",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${state.providerName} (${state.providerId}) — " +
                                if (state.providerNeedsNetwork) {
                                    "uses the network"
                                } else {
                                    "runs fully on this device, no network"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            text = "Level",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            listOf(
                                AiConsent.OFF to "Off — nothing is computed",
                                AiConsent.SUMMARIES to "Summaries — extractive, on-device",
                                AiConsent.TAGGING to "Summaries + tags",
                                AiConsent.SUGGESTIONS to "Summaries + tags + suggestions",
                            ).forEach { (level, label) ->
                                Pill(
                                    text = label,
                                    selected = settings.aiConsentLevel == level,
                                    onClick = { viewModel.setAiConsentLevel(level) },
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = "Current: ${AiConsent.levelName(settings.aiConsentLevel)}. " +
                                "Lowering the level disables every AI-derived string immediately.",
                            style = MaterialTheme.typography.labelSmall,
                            color = LifeOs.tokens.beigeAccent,
                        )
                    }
                }
            }

            item { SectionHeader(title = "Data") }
            item {
                LifeOsCard {
                    Column {
                        Text(
                            text = "Everything is on this device",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "LifeOS has no account and no server. Your notes, tasks, " +
                                "habits, expenses, diary, captures and timeline live in a local " +
                                "database you can export and back up yourself.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            androidx.compose.material3.Button(
                                onClick = { viewModel.exportJson { payload -> shareExport(payload, "application/json") } },
                                enabled = !state.busy,
                            ) {
                                Text("Export JSON")
                            }
                            androidx.compose.material3.OutlinedButton(
                                onClick = { viewModel.exportText { payload -> shareExport(payload, "text/plain") } },
                                enabled = !state.busy,
                            ) {
                                Text("Export text")
                            }
                        }
                        Text(
                            text = "Export builds a file on-device and opens the system share " +
                                "sheet. Nothing is uploaded by LifeOS.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(Spacing.sm)) }
        }
    }

    if (state.statusMessage != null) {
        val message = state.statusMessage!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::clearStatus,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Done") },
            text = { Text(message) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::clearStatus) {
                    Text("OK")
                }
            },
        )
    }
}
