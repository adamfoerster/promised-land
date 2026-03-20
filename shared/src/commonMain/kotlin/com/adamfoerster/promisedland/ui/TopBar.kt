package com.adamfoerster.promisedland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import com.adamfoerster.promisedland.game.GameUIState

private val phaseNames = listOf(
    "1. Announcements",
    "2. Draw Cards",
    "3. Income",
    "4. Acquisitions",
    "5. Movement",
    "6. Combat",
    "7. Check Victory"
)

@Composable
fun TopBar(
    state: GameUIState,
    onReturnToWelcome: () -> Unit,
    onZoomCycle: (() -> Unit)? = null
) {
    if (state.currentPlayer == null) {
        return
    }

    var menuExpanded = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Black)
                .align(Alignment.TopCenter),
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
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
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
                        "Round ${state.currentRound} - ${phaseNames.getOrElse(state.currentPhase.toInt() - 1) { " ${state.currentPhase}" }}",
                        style = MaterialTheme.typography.caption,
                        color = Color.LightGray
                    )
                }
            }

            // Right side buttons: Zoom and Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onZoomCycle != null) {
                    IconButton(
                        onClick = onZoomCycle,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Zoom",
                            tint = Color.White
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded.value = true },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded.value,
                        onDismissRequest = { menuExpanded.value = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            menuExpanded.value = false
                            onReturnToWelcome()
                        }) {
                            Text("Return to Welcome Screen")
                        }
                    }
                }
            }
        }
    }
}