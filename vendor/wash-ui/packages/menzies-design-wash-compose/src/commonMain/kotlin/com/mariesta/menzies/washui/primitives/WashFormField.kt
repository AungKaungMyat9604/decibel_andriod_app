package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariesta.menzies.washui.theme.WashTheme

@Composable
internal fun WashFormField(
    label: String?,
    hint: String?,
    error: String?,
    requiredMark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = WashTheme.colors
    val hasError = !error.isNullOrBlank()

    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank()) {
            val showMark = requiredMark
            WashText(
                text = if (showMark) "$label*" else label,
                color = colors.base_content,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        content()
        val helper = error ?: hint
        if (!helper.isNullOrBlank()) {
            WashText(
                text = helper,
                color = if (hasError) colors.error else colors.ink_muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
