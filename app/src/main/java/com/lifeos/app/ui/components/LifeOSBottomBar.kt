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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lifeos.app.ui.navigation.ROOT_TABS_GRAPH
import com.lifeos.app.ui.navigation.Screen
import com.lifeos.app.ui.theme.LifeOSElevation
import com.lifeos.app.ui.theme.LifeOSRadius
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

    val pillShape = RoundedCornerShape(LifeOSRadius.xlarge)
    // Index after which the raised centre action is inserted, so the bar reads
    // Home · Notes · ＋ · Life · You.
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
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
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
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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

    // The pill fades in and the icon lifts a touch when a tab becomes active, so
    // selection reads as a movement rather than a jump between two stills.
    //
    // It stays SOLID clay, deliberately not the identity gradient: that is
    // reserved for Capture, the hero and the AI orb so it keeps meaning "this is
    // LifeOS". Solid clay also holds the label at 5.4:1, where the gradient's
    // gold end would drop white text under 4.5:1.
    val pillColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            pressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        animationSpec = spring(stiffness = 500f),
        label = "bottomNavPill"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "bottomNavIconScale"
    )

    Column(
        modifier = modifier
            .scale(pressedScale)
            .clip(entryShape)
            .background(pillColor)
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
            modifier = Modifier
                .size(23.dp)
                .scale(iconScale)
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
    // A slow, calm breath, so the app's one primary action invites a tap instead
    // of sitting dead still. It fades out as it expands, which reads as a soft
    // pulse rather than a flicker.
    val breathTransition = rememberInfiniteTransition(label = "captureBreath")
    val breath by breathTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "captureBreathLevel"
    )
    Box(
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        // The halo sits behind the disc and never receives the tap.
        Box(
            Modifier
                .size(52.dp)
                .graphicsLayer {
                    val s = 1f + 0.45f * breath
                    scaleX = s
                    scaleY = s
                    alpha = 0.40f * (1f - breath)
                }
                .clip(CircleShape)
                .background(prismBrush())
        )
        Box(
            Modifier
                .size(52.dp)
                .scale(pressedScale)
                .shadow(
                    elevation = LifeOSElevation.floating,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = LocalLifeOSSemanticColors.current.shadow.copy(alpha = LifeOSElevation.shadowAlphaRest),
                    spotColor = LocalLifeOSSemanticColors.current.shadow.copy(alpha = LifeOSElevation.shadowAlphaLift)
                )
                .clip(CircleShape)
                .background(prismBrush())
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
