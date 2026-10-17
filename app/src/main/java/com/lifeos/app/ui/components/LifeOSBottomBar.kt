package com.lifeos.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lifeos.app.ui.navigation.ROOT_TABS_GRAPH
import com.lifeos.app.ui.navigation.Screen
import com.lifeos.app.ui.theme.FigmaAccent
import com.lifeos.app.ui.theme.FigmaBottomNav
import com.lifeos.app.ui.theme.FigmaMuted
import com.lifeos.app.ui.theme.FigmaOnAccent
import com.lifeos.app.ui.theme.FigmaStroke
import com.lifeos.app.ui.theme.LifeOSElevation
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors

/**
 * A single item of the primary bottom navigation.
 * [route] is the navigation route of the top-level destination.
 */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Ordered list backing the bottom navigation bar — the spec's five-section IA:
 * Home · Notes · (Capture) · Insights · Life. Capture is not a stateful tab; it
 * is the raised centre action that opens the full-window Capture hub, so it is
 * rendered separately (see [CaptureCenterButton]) and must not appear here.
 *
 * Labels are always visible: icon-only navigation is a UX trap.
 */
internal val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Notes, "Notes", Icons.Filled.Description, Icons.Outlined.Description),
    BottomNavItem(Screen.Insights, "Insights", Icons.Filled.Insights, Icons.Outlined.Insights),
    BottomNavItem(Screen.Life, "Life", Icons.Filled.Timeline, Icons.Outlined.Timeline)
)

@Composable
fun LifeOSBottomBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    // True while the visible destination lives INSIDE the tabs graph. False on
    // an overlay stacked above the tabs (Profile, Habit Detail, Expenses, …).
    val isOnTab = destination?.hierarchy?.any { it.route == ROOT_TABS_GRAPH } == true

    // Index after which the raised centre action is inserted, so the bar reads
    // Home · Notes · ＋ · Insights · Life.
    val centerAfterIndex = bottomNavItems.size / 2 - 1

    // The redesign's nav is a full-width slate plate with a single 1px top
    // hairline (not a floating pill): `rgba(16,32,42,0.94)` over the canvas.
    Surface(color = FigmaBottomNav) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(FigmaStroke)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(LifeOSSpacing.bottomNavHeight)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    val selected = destination?.hierarchy?.any { it.route == item.screen.route } == true
                    BottomNavEntry(item = item, selected = selected, onClick = {
                        if (isOnTab) {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else if (!navController.popBackStack(item.screen.route, false)) {
                            navController.navigate(item.screen.route) { launchSingleTop = true }
                        }
                    }, modifier = Modifier.weight(1f))

                    // The Capture action sits at the centre of the primary tabs.
                    // It is intentionally NOT a [BottomNavItem]: it opens a
                    // full-screen flow rather than switching tabs, so it must
                    // not join the tab selection model.
                    if (index == centerAfterIndex) {
                        CaptureCenterButton(
                            onClick = { navController.navigate(Screen.Capture.route) { launchSingleTop = true } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/** One tappable bottom-nav entry: the 40×30 icon plate over a 9px uppercase label. */
@Composable
private fun BottomNavEntry(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) FigmaAccent else FigmaMuted,
        animationSpec = spring(stiffness = 500f),
        label = "bottomNavColor"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 900f),
        label = "bottomNavPress"
    )
    // The selected destination wears the mint plate; unselected ones are bare.
    val plateColor by animateColorAsState(
        targetValue = if (selected) FigmaAccent.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = spring(stiffness = 500f),
        label = "bottomNavPlate"
    )

    Column(
        modifier = modifier
            .scale(pressedScale)
            .semantics { this.role = Role.Tab; this.selected = selected }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .defaultMinSize(minHeight = 56.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(width = 40.dp, height = 30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(plateColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            item.label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The centre action. A flat **mint** 50×42 plate (radius 15) with dark on-accent
 * ink, opening the full-screen Capture hub. Kept out of [bottomNavItems] on
 * purpose: it navigates rather than selects.
 */
@Composable
private fun CaptureCenterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 900f),
        label = "captureCenterPress"
    )
    Box(
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(width = 50.dp, height = 42.dp)
                .scale(pressedScale)
                .clip(RoundedCornerShape(15.dp))
                .background(FigmaAccent)
                .semantics {
                    role = Role.Button
                    contentDescription = "Open capture"
                }
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = FigmaOnAccent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
