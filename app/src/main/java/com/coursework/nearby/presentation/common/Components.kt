package com.coursework.nearby.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ButtonMinHeight = 54.dp

/** Главная кнопка экрана. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(onClick = onClick, enabled = enabled && !loading, modifier = modifier.heightIn(min = ButtonMinHeight)) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        } else {
            Text(text)
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = ButtonMinHeight),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(text)
    }
}

/** Плоская карточка с тонкой рамкой вместо тени. */
@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = MaterialTheme.shapes.medium
    val color = MaterialTheme.colorScheme.surfaceContainerLow
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border) { Column(content = content) }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = border) { Column(content = content) }
    }
}

/** Крупный заголовок раздела. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.headlineLarge, modifier = modifier.padding(top = 8.dp, bottom = 4.dp))
}

/** Знак приложения: круг радиуса и два человека внутри него. */
@Composable
fun LogoMark(size: Dp, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.run { if (primary.red > 0.5f) primary else tertiary }
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension
        drawCircle(color = color, radius = unit * 0.44f, style = Stroke(width = unit * 0.06f))
        drawCircle(color = color, radius = unit * 0.11f, center = Offset(unit * 0.37f, unit * 0.58f))
        drawCircle(color = color, radius = unit * 0.11f, center = Offset(unit * 0.64f, unit * 0.40f))
    }
}
