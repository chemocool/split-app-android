package com.chemocool.split.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

fun Modifier.tappable(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
