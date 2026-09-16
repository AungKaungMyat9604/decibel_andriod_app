package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariesta.menzies.washui.theme.WashTheme

@Composable
fun WashTextarea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    hint: String? = null,
    error: String? = null,
    requiredMark: Boolean = false,
    enabled: Boolean = true,
    minLines: Int = 3,
    placeholder: String? = null,
) {
    val colors = WashTheme.colors
    val hasError = !error.isNullOrBlank()
    val shape = RoundedCornerShape(colors.radiusField)
    val borderColor = if (hasError) colors.error else colors.ink_border
    val textColor = if (enabled) colors.base_content else colors.base_content.copy(alpha = 0.5f)

    WashFormField(
        label = label,
        hint = hint,
        error = error,
        requiredMark = requiredMark,
        modifier = modifier,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = TextStyle(color = textColor),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (minLines * 24).dp)
                .semantics {
                    if (hasError) contentDescription = error.orEmpty()
                }
                .clip(shape)
                .background(colors.base_200, shape)
                .border(BorderStroke(1.dp, borderColor), shape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && !placeholder.isNullOrBlank()) {
                        WashText(
                            text = placeholder,
                            color = colors.ink_muted,
                            fontSize = 16.sp,
                        )
                    }
                    inner()
                }
            },
        )
    }
}
