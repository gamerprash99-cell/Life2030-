package com.lifeos.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.components.LifeOSGradientButton
import com.lifeos.app.ui.theme.LifeOSPrimary

private data class OnboardingPage(val title: String, val body: String, val icon: @Composable () -> Unit)

private val pages = listOf(
    OnboardingPage("Welcome to LifeOS", "Capture your life. Organize your life. Understand your life.") { Icon(Icons.Filled.AutoAwesome, null, tint = LifeOSPrimary, modifier = Modifier.size(54.dp)) },
    OnboardingPage("Your life. Your data.", "Your notes, tasks, habits, diary and expenses stay on your device unless you explicitly export them.") { Icon(Icons.Filled.Lock, null, tint = LifeOSPrimary, modifier = Modifier.size(54.dp)) },
    OnboardingPage("AI, on your terms", "LifeOS Intelligence runs locally on your device. No cloud AI is required.") { Icon(Icons.Filled.AutoAwesome, null, tint = LifeOSPrimary, modifier = Modifier.size(54.dp)) },
    OnboardingPage("Everything connects", "Notes, tasks, habits, expenses and diary entries become one chronological LifeOS timeline.") { Icon(Icons.Filled.AutoAwesome, null, tint = LifeOSPrimary, modifier = Modifier.size(54.dp)) },
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onRestoreBackup: () -> Unit = {},
    restoreStatus: String? = null
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]
    val isLast = pageIndex == pages.lastIndex
    val indicatorScale by animateFloatAsState(if (isLast) 1.08f else 1f, tween(240), label = "indicator")

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFDF8FF), Color(0xFFF7F0FC)))
        )
    ) {
        Box(Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 18.dp).size(150.dp).background(Color(0xFFEADDFF).copy(alpha = .55f), CircleShape))
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!isLast) {
                    Text("Skip", color = LifeOSPrimary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable { onFinish() }.padding(12.dp))
                }
            }

            Spacer(Modifier.height(26.dp))
            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = {
                    (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(220)) + fadeOut(tween(220)))
                },
                label = "onboardingPage"
            ) { index ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.size(250.dp).background(
                            Brush.radialGradient(listOf(Color(0xFFEADDFF), Color(0xFFFCE7F3), Color.Transparent)),
                            CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(136.dp).background(Color.White.copy(alpha = .82f), CircleShape), contentAlignment = Alignment.Center) {
                            pages[index].icon()
                        }
                    }
                    Text(pages[index].title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 34.dp))
                    Text(pages[index].body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                }
            }

            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = true) {
                Column(Modifier.fillMaxWidth()) {
                    LifeOSGradientButton(
                        text = if (isLast) "Get started" else "Next",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { if (isLast) onFinish() else pageIndex++ }
                    )
                    Box(
                        Modifier.fillMaxWidth().padding(top = 10.dp).height(50.dp).background(Color.White, RoundedCornerShape(18.dp)).clickable { onRestoreBackup() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Backup, contentDescription = "Restore backup", tint = LifeOSPrimary, modifier = Modifier.size(18.dp))
                            Text("Restore a LifeOS backup", color = LifeOSPrimary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Text(restoreStatus ?: "Restore from Android storage", color = if (restoreStatus?.startsWith("Restore failed") == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                    Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        pages.indices.forEach { i ->
                            Box(Modifier.scale(if (i == pageIndex) indicatorScale else 1f).size(if (i == pageIndex) 24.dp else 7.dp, 7.dp).background(if (i == pageIndex) LifeOSPrimary else Color(0xFFD9CDE3), RoundedCornerShape(50)))
                        }
                    }
                }
            }
        }
    }
}
