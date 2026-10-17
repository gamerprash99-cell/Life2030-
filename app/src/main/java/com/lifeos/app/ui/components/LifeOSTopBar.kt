package com.lifeos.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.ui.theme.FigmaAccent
import com.lifeos.app.ui.theme.FigmaStroke
import com.lifeos.app.ui.theme.FigmaText
import com.lifeos.app.ui.theme.FigmaWash
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * The shared page header for a pushed screen — the redesign's **86dp band**, not
 * a floating card: a leading back affordance, an identity column (a 9px mint
 * kicker over a 24px title) and one 42×42 action circle on the trailing edge.
 *
 * [subtitle] is rendered as the uppercase kicker above [title]; [onBack] and
 * [onSearch] each render as the shared [HeaderActionCircle], so every screen's
 * chrome is the same object.
 */
@Composable
fun LifeOSTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = LifeOSSpacing.pageHeaderHeight)
            .padding(horizontal = LifeOSSpacing.screenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            HeaderActionCircle(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        }
        Column(Modifier.weight(1f)) {
            subtitle?.let {
                Text(
                    it.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = FigmaAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 28.sp),
                color = FigmaText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onSearch != null) {
            HeaderActionCircle(Icons.Filled.Search, "Search", onSearch)
        }
        actions()
    }
}

/**
 * The page header's 42×42 action circle: a faint white wash
 * (`rgba(255,255,255,0.03)`) inside the hairline stroke, radius 15, with a 22dp
 * icon. Its touch target is widened to 48dp for accessibility without changing
 * the visible size.
 */
@Composable
fun HeaderActionCircle(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(LifeOSRadius.medium - 1.dp)
    Box(
        modifier
            .size(LifeOSSpacing.minTouchTarget)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription; role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(LifeOSSpacing.headerActionSize)
                .clip(shape)
                .background(FigmaWash)
                .border(BorderStroke(1.dp, FigmaStroke), shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = FigmaText, modifier = Modifier.size(22.dp))
        }
    }
}
