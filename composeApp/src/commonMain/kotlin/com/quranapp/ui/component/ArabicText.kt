package com.quranapp.ui.component

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import com.quranapp.ui.theme.getQuranFontFamily

@Composable
fun ArabicText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    color: Color = LocalContentColor.current,
    textAlign: TextAlign = TextAlign.Right,
    lineHeight: TextUnit = fontSize * 1.5,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        Text(
            text = text,
            modifier = modifier,
            style = TextStyle(
                fontFamily = getQuranFontFamily(),
                fontSize = fontSize,
                lineHeight = lineHeight,
                textAlign = textAlign,
                color = color
            )
        )
    }
}
