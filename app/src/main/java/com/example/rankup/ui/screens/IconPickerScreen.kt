package com.example.rankup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerScreen(
    onIconSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Categories using high-quality sports and team icons
    val categories = listOf("futsal", "padel", "player")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    
    // Using a consistent set of high-quality Flaticon IDs
    val iconIds = remember(selectedCategory) {
        when(selectedCategory) {
            "player" -> listOf("128/3840/3840481.png", "128/2906/2906401.png", "128/166/166344.png", "128/560/560178.png", "128/5025/5025661.png", "128/5210/5210542.png", "128/4216/4216530.png", "128/7039/7039027.png", "128/1499/1499853.png", "128/2348/2348789.png", "128/5281/5281619.png", "128/4389/4389644.png", "128/12524/12524844.png", "128/2173/2173430.png")
            "padel" -> listOf("128/8842/8842175.png", "128/19030/19030323.png", "128/19030/19030325.png", "128/19030/19030307.png", "128/15459/15459659.png", "128/16117/16117685.png", "128/8845/8845451.png", "128/19030/19030321.png", "128/19030/19030340.png", "128/19030/19030342.png")
            "futsal" -> listOf("128/17728/17728970.png", "128/919/919459.png", "128/10841/10841200.png", "128/8920/8920689.png", "128/4276/4276494.png", "128/8920/8920729.png", "128/9192/9192876.png", "128/5701/5701802.png", "128/3461/3461211.png", "128/9192/9192889.png", "128/824/824746.png")
            else -> listOf("")
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select Team Icon",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp)
                )
            }

            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                containerColor = Color.Transparent,
                contentColor = Color.Black,
                edgePadding = 0.dp,
                divider = {},
                indicator = { tabPositions ->
                    val selectedIndex = categories.indexOf(selectedCategory)
                    if (selectedIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                            color = Color.Black
                        )
                    }
                }
            ) {
                categories.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        text = { Text(category.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(80.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(iconIds) { id ->
                    val url = "https://cdn-icons-png.flaticon.com/$id"
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(Color.LightGray.copy(alpha = 0.2f))
                            .clickable { onIconSelected(url) },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}
