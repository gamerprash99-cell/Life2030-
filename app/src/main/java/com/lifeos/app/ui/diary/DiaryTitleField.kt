package com.lifeos.app.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.DiaryActionViolet

/**
 * The optional title line, sitting above the writing surface.
 *
 * `BasicTextField` rather than `OutlinedTextField` for the same reason the body
 * uses it: no floating label, no 56dp container, and — the part that matters
 * here — no second element for the IME to fight over. The editor scrolls with
 * `imeNestedScroll`, and a bordered field inside that viewport insets its own
 * padding, so the caret can end up visible while the title itself is scrolled
 * under the toolbar.
 *
 * ImeAction.Next, not Done: the next thing after the title is the body, so Done
 * should not dismiss the keyboard from the first field the user reaches.
 *
 * The hint is a placeholder rather than a permanent label, which means the field
 * looks empty when it is. That is the intent — a title is an embellishment, and
 * a permanently visible label would give an optional field the visual weight of
 * a required one. The placeholder disappears on first keystroke, and
 * `contentDescription` keeps the field named for screen readers once it does.
 */
@Composable
fun DiaryTitleField(
    title: String,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (title.isEmpty()) {
            Text(
                "Give it a name",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Memory title, optional" },
            textStyle = LocalTextStyle.current.merge(
                MaterialTheme.typography.titleLarge.copy(
                    color = DiaryActionViolet,
                    textAlign = TextAlign.Start
                )
            ),
            singleLine = true,
            cursorBrush = SolidColor(DiaryActionViolet),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            decorationBox = { inner ->
                if (title.isEmpty()) {
                    Text(
                        "Optional",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                inner()
            }
        )
    }
}