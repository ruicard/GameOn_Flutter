package com.example.rankup.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.rankup.ui.theme.RankUpTheme

@Composable
fun TeamsScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "My Teams Screen")
    }
}

@Preview(showBackground = true)
@Composable
fun TeamsScreenPreview() {
    RankUpTheme {
        TeamsScreen()
    }
}
