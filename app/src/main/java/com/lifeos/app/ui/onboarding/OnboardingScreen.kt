package com.lifeos.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.components.GlassCard

private data class OnboardingPage(val emoji: String, val title: String, val body: String)

private val PAGES = listOf(
    OnboardingPage(
        "🧠", "Welcome to LifeOS",
        "Capture your life. Organize your life. Understand your life. Everything in one connected, local-first app."
    ),
    OnboardingPage(
        "🔒", "Your life. Your data.",
        "Notes, tasks, habits, expenses and diary entries stay on your device. Nothing is uploaded unless you explicitly export it."
    ),
    OnboardingPage(
        "🤖", "AI, on your terms",
        "Optional AI features (summaries, task extraction, weekly reviews) only run when you tap to use them, and every AI suggestion needs your approval before it's saved."
    ),
    OnboardingPage(
        "🔗", "Everything connects",
        "Your Timeline weaves together notes, tasks, habits, expenses and diary entries by date and time — one continuous story of your life."
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = PAGES[pageIndex]
    val isLast = pageIndex == PAGES.lastIndex

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(page.emoji, style = MaterialTheme.typography.displayLarge)
                Text(page.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
                Text(
                    page.body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
            Button(
                onClick = { if (isLast) onFinish() else pageIndex++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLast) "Get started" else "Next")
            }
            if (!isLast) {
                TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Skip") }
            }
        }
    }
}
