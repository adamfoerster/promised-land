package com.adamfoerster.promisedland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adamfoerster.promisedland.game.GameUIState
import com.adamfoerster.promisedland.game.HexagonData

@Composable
fun GameScreen(
    state: GameUIState,
    onNextTurn: () -> Unit,
    onReturnToWelcome: () -> Unit
) {
    if (state.currentPlayer == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    var selectedHex by remember { mutableStateOf<HexagonData?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        // The Map occupies the center
        HexMap(
            modifier = Modifier.fillMaxSize(),
            onHexSelected = { selectedHex = it },
            selectedHex = selectedHex,
            hexagons = state.hexagons
        )

        // Overlay UI
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Current Player Info
                val colorInfo = playerColors.find { it.first == state.currentPlayer.color }
                Card(
                    backgroundColor = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(12.dp).clip(CircleShape)
                                    .background(colorInfo?.second ?: Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                state.currentPlayer.name,
                                style = MaterialTheme.typography.body2,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                state.gameName.ifBlank { "Promised Land" },
                                style = MaterialTheme.typography.subtitle1,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Round ${state.currentRound} - Phase ${state.currentPhase}",
                            style = MaterialTheme.typography.caption,
                            color = Color.LightGray
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            menuExpanded = false
                            onReturnToWelcome()
                        }) {
                            Text("Return to Welcome Screen")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(80.dp)) // Space for the bottom bar in HexMap
        }

        // Next Turn Button (floating)
        Button(
            onClick = onNextTurn,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFE91E63),
                contentColor = Color.White
            )
        ) {
            Text("Next Turn", style = MaterialTheme.typography.button)
        }
    }
}
