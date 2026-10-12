package com.lifeos.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lifeos.app.ui.navigation.ROOT_TABS_GRAPH
import com.lifeos.app.ui.navigation.Screen
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSecondary

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
 * Ordered list backing the bottom navigation bar — the app's five-section IA:
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

    val pillShape = RoundedCornerShape(LifeOSRadius.xlarge)
    // Index after which the raised centre action is inserted, so the bar reads
    // Home · Notes · ＋ · Insights · Life.
    val centerAfterIndex = bottomNavItems.size / 2 - 1

    // Transparent outer slot: only the pill itself is painted, so it floats on
    // the page background with margin from the screen edges.
    Surface(color = Color.Transparent) {
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = pillShape,
                        tint = LifeOSPrimary.copy(alpha = 0.30f),
                        elevation = 16.dp
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
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

/** One tappable bottom-nav entry: stacked icon + label, with a pill when selected. */
@Composable
private fun BottomNavEntry(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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
    val entryShape = RoundedCornerShape(LifeOSRadius.large)

    Column(
        modifier = modifier
            .scale(pressedScale)
            .then(
                if (selected) {
                    // The active tab is a lit, brand-gradient pill — the loudest
                    // small surface in the app, so "where am I" is never a guess.
                    Modifier
                        .clip(entryShape)
                        .background(Brush.horizontalGradient(listOf(LifeOSPrimary, LifeOSSecondary)))
                } else {
                    Modifier
                        .clip(entryShape)
                        .background(if (pressed) LifeOSPrimary.copy(alpha = 0.08f) else Color.Transparent)
                }
            )
            .semantics { this.role = Role.Tab; this.selected = selected }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(23.dp)
        )
        Text(
            item.label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The raised centre action. A lit gradient disc, sized like a tab slot so it
 * lines up with the surrounding labels, opening the full-screen Capture hub.
 * Kept out of [bottomNavItems] on purpose: it navigates rather than selects.
 */
@Composable
private fun CaptureCenterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(stiffness = 900f),
        label = "captureCenterPress"
    )
    Box(
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(52.dp)
                .scale(pressedScale)
                .shadow(12.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(LifeOSPrimary, LifeOSSecondary)))
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
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
