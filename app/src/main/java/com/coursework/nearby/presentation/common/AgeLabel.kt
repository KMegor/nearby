package com.coursework.nearby.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.coursework.nearby.R

/**
 * «21 год», «22 года», «25 лет». Интерфейс всегда на русском, поэтому склонение выбирается
 * здесь, а не через plurals: те зависят от языка устройства.
 */
@Composable
fun ageLabel(age: Int): String = stringResource(
    when {
        age % 100 in 11..14 -> R.string.age_many
        age % 10 == 1 -> R.string.age_one
        age % 10 in 2..4 -> R.string.age_few
        else -> R.string.age_many
    },
    age,
)
