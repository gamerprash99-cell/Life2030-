package com.lifeos.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.lifeos.app.ui.theme.LifeOSCardStrokeDark
import com.lifeos.app.ui.theme.LifeOSCardStrokeLight
import com.lifeos.app.ui.theme.LifeOSAccentLavender
import com.lifeos.app.ui.theme.LifeOSMint
import com.lifeos.app.ui.theme.LifeOSMintInk
import com.lifeos.app.ui.theme.LifeOSNavBarDark
import com.lifeos.app.ui.theme.LifeOSNavBarLight
import com.lifeos.app.ui.theme.LifeOSNavPillSelectedDark
import com.lifeos.app.ui.theme.LifeOSNavPillSelectedLight
import com.lifeos.app.ui.theme.LifeOSTextPrimaryLight

/**
 * A single item of the primary bottom navigation.
 * [screen] is the navigation route of the top-level destination.
 */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Ordered list backing the bottom navigation bar — Home · Notes · (Capture) ·
 * Life. Capture is not a stateful tab; it is the raised centre action that
 * opens the full-window Capture hub, so it is rendered separately (see
 * [CaptureCenterButton]) and must not appear here.
 *
 * Labels are always visible: icon-only navigation is a UX trap.
 */
internal val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Outlined.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Notes, "Notes", Icons.AutoMirrored.Outlined.StickyNote2, Icons.AutoMirrored.Outlined.StickyNote2),
    BottomNavItem(Screen.Life, "Life", Icons.Outlined.Explore, Icons.Outlined.Explore)
)

/**
 * The Lavender Dream primary navigation bar: a full-width bar (plus the
 * gesture-area padding) painted over the canvas with a hairline top edge,
 * holding the primary destinations around a raised centre Capture action.
 * The selected one gets a soft-lavender pill behind its 24dp icon; every
 * entry keeps a ≥48dp touch target. Icons are 24dp per the Material spec.
 */
@Composable
fun LifeOSBottomBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    // True while the visible destination lives INSIDE the tabs graph. False on
    // an overlay stacked above the tabs (Profile, Habit Detail, Expenses, …).
    val isOnTab = destination?.hierarchy?.any { it.route == ROOT_TABS_GRAPH } == true

    // Index after which the raised centre action is inserted, so the bar reads
    // Home · Notes · ＋ · Life.
    val centerAfterIndex = bottomNavItems.size / 2 - 1

    Box(
        Modifier
            .fillMaxWidth()
            .background(if (lifeOSIsDark()) LifeOSNavBarDark else LifeOSNavBarLight)
            .border(
                width = 1.dp,
                color = if (lifeOSIsDark()) LifeOSCardStrokeDark else LifeOSCardStrokeLight,
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(84.dp)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
                })

                // The Capture action sits at the centre of the primary tabs.
                // It is intentionally NOT a [BottomNavItem]: it opens a
                // full-screen flow rather than switching tabs, so it must
                // not join the tab selection model.
                if (index == centerAfterIndex) {
                    CaptureCenterButton(
                        onClick = { navController.navigate(Screen.Capture.route) { launchSingleTop = true } }
                    )
                }
            }
        }
    }
}

/**
 * One tappable bottom-nav entry: a lavender pill behind a 24dp icon with a
 * 9sp uppercase label. The whole column is the hit region and is held to at
 * least 48x48dp so the touch target never shrinks with the label.
 */
@Composable
private fun BottomNavEntry(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = lifeOSIsDark()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pillColor by animateColorAsState(
        targetValue = when {
            selected -> if (dark) LifeOSNavPillSelectedDark else LifeOSNavPillSelectedLight
            pressed -> Color.White.copy(alpha = 0.06f)
            else -> Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "bottomNavPill"
    )
    // Selected ink sits ON the pill: dark lavender ink on the light pill, the
    // soft lavender accent on the dark pill. Unselected keeps the scheme's
    // quiet on-surface-variant grey.
    val selectedInk = if (dark) LifeOSAccentLavender else LifeOSTextPrimaryLight
    val iconColor by animateColorAsState(
        targetValue = if (selected) selectedInk else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "bottomNavColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) selectedInk else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "bottomNavLabelColor"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { this.role = Role.Tab; this.selected = selected }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            // The full 48x48dp minimum touch target (Material accessibility).
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(width = 44.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            item.label.uppercase(),
            color = labelColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The centre Capture action: a solid lavender 50x42 r16 tile with deep ink —
 * the one filled surface on the bar, so the app's primary action is always
 * the most present thing on screen. Kept out of [bottomNavItems] on purpose:
 * it navigates rather than selects.
 */
@Composable
private fun CaptureCenterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "captureCenterPress"
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(width = 50.dp, height = 42.dp)
                .scale(pressedScale)
                .clip(RoundedCornerShape(16.dp))
                .background(LifeOSMint)
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
                tint = LifeOSMintInk,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            "Capture",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}
