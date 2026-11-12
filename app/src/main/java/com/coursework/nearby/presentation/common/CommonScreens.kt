package com.coursework.nearby.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.coursework.nearby.R
import com.coursework.nearby.domain.error.AppError

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(error: AppError, onRetry: () -> Unit, onSignOut: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(error.messageRes()), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        if (onSignOut != null) {
            TextButton(onClick = onSignOut) { Text(stringResource(R.string.action_sign_out)) }
        }
    }
}

/** Фото профиля; без фото — круг с первой буквой имени. [model] — адрес или локальный Uri. */
@Composable
fun Avatar(model: Any?, name: String, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val accent = if (colors.primary.red > 0.5f) colors.primary else colors.tertiary
    val shape = modifier
        .size(size)
        .then(if (ring) Modifier.border(2.dp, accent, CircleShape).padding(5.dp) else Modifier)
        .clip(CircleShape)
    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = shape.background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            shape.background(Brush.linearGradient(listOf(colors.surfaceContainerHighest, colors.surfaceContainerLow))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.trim().take(1).uppercase().ifEmpty { "?" },
                style = if (size >= 72.dp) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleMedium,
                color = accent,
            )
        }
    }
}
