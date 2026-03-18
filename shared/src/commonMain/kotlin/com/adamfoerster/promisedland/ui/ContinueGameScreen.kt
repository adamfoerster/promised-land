package com.adamfoerster.promisedland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamfoerster.promisedland.game.SavedGameSummary
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun ContinueGameScreen(
    savedGames: List<SavedGameSummary>,
    onSelectGame: (Long) -> Unit,
    onDeleteGame: (Long) -> Unit,
    onBack: () -> Unit
) {
    var gameToDelete by remember { mutableStateOf<SavedGameSummary?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D1A), Color(0xFF1A1A2E))
                )
            )
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Continue Game",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Divider(color = Color(0xFF2A2A3A))

        if (savedGames.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No saved games found.", color = Color(0xFF9E9E9E))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedGames) { game ->
                    SavedGameCard(
                        game = game,
                        onClick = { onSelectGame(game.id) },
                        onDelete = { gameToDelete = game }
                    )
                }
            }
        }
    }

    // Confirmation dialog
    gameToDelete?.let { game ->
        AlertDialog(
            onDismissRequest = { gameToDelete = null },
            title = {
                Text(
                    text = "Delete Game",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${game.name}\"? This action cannot be undone.",
                    color = Color(0xFFCCCCCC)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteGame(game.id)
                    gameToDelete = null
                }) {
                    Text("Delete", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { gameToDelete = null }) {
                    Text("Cancel", color = Color(0xFF9E9E9E))
                }
            },
            backgroundColor = Color(0xFF1E1E30),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SavedGameCard(game: SavedGameSummary, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color(0xFF1E1E30),
        elevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game icon / accent
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFF3D5AFE), Color(0xFF1C2A80))),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("⚔", fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(game.startedAt),
                    style = MaterialTheme.typography.caption,
                    color = Color(0xFF9E9E9E)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${game.playerCount} player${if (game.playerCount != 1L) "s" else ""}",
                        style = MaterialTheme.typography.caption,
                        color = Color(0xFF9E9E9E)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete game",
                    tint = Color(0xFF9E9E9E)
                )
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        "$month ${local.dayOfMonth}, ${local.year}"
    } catch (_: Exception) {
        "Unknown date"
    }
}
