package com.lifeos.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lifeos.app.ui.navigation.Screen

private data class BottomItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val items = listOf(
    BottomItem(Screen.Home, "Today", Icons.Filled.Home, Icons.Outlined.Home),
    BottomItem(Screen.Habits, "Habits", Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment),
    BottomItem(Screen.Tasks, "Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    BottomItem(Screen.Insights, "Insights", Icons.Filled.Psychology, Icons.Outlined.Psychology),
)

@Composable
fun LifeOSBottomBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = destination?.hierarchy?.any { it.route == item.screen.route } == true
                val background by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                    else MaterialTheme.colorScheme.surface,
                    label = "bottomBarBackground"
                )
                val horizontalPadding by animateDpAsState(
                    if (selected) 13.dp else 10.dp,
                    spring(stiffness = 700f),
                    label = "bottomBarPadding"
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(background)
                        .clickable {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(horizontal = horizontalPadding, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selected) {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
