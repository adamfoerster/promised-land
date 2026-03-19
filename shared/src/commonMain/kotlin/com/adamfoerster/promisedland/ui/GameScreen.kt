package com.adamfoerster.promisedland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adamfoerster.promisedland.game.GameUIState
import com.adamfoerster.promisedland.game.GeneralData
import com.adamfoerster.promisedland.game.HexagonData

@Composable
fun GameScreen(
    state: GameUIState,
    onNextTurn: () -> Unit,
    onReturnToWelcome: () -> Unit,
    onPlaceGeneral: (generalId: Long, hexCol: Int, hexRow: Int) -> String?
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
    
    // Dialog states
    var showHandModal by remember { mutableStateOf(false) }
    var showEndTurnConfirm by remember { mutableStateOf(false) }
    
    // Placement state
    var selectedGeneralFromHand by remember { mutableStateOf<GeneralData?>(null) }
    var placementError by remember { mutableStateOf("") }

    val isPlacementPhase = state.currentRound == 1L && state.currentPlayerGeneralCount < 2
    val needsPlacement = isPlacementPhase

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        // The Map occupies the center
        HexMap(
            modifier = Modifier.fillMaxSize(),
            onHexSelected = { hex ->
                selectedHex = hex
                placementError = ""
                // If in placement phase and a general is selected, try to place
                if (isPlacementPhase && selectedGeneralFromHand != null && hex.type == "village") {
                    val error = onPlaceGeneral(selectedGeneralFromHand!!.id, hex.col, hex.row)
                    if (error != null) {
                        placementError = error
                    } else {
                        selectedGeneralFromHand = null
                        placementError = ""
                    }
                }
            },
            selectedHex = selectedHex,
            hexagons = state.hexagons,
            generalPlacements = state.generalPlacements
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

            // Placement specific HUD Text
            if (isPlacementPhase) {
                Text(
                    text = "You must place your two generals",
                    style = MaterialTheme.typography.subtitle1,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(Color(0xFFE91E63).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp)
                )

                if (placementError.isNotEmpty()) {
                    Text(
                        text = placementError,
                        style = MaterialTheme.typography.body2,
                        color = Color(0xFFFF5252),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons Row (Hand / Next Turn)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp), // Clear HexMap's bottom bar
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Hand Button
                Button(
                    onClick = { showHandModal = true },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF3C3C3C),
                        contentColor = Color.White
                    )
                ) {
                    Text("Hand (${state.playerHand.size})", style = MaterialTheme.typography.button)
                }

                // Next Turn Button
                Button(
                    onClick = { showEndTurnConfirm = true },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    enabled = !needsPlacement,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (needsPlacement) Color.Gray else Color(0xFFE91E63),
                        contentColor = Color.White,
                        disabledBackgroundColor = Color.Gray.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        if (needsPlacement) "Place Generals" else "Next Turn",
                        style = MaterialTheme.typography.button
                    )
                }
            }
        }
    }

    // Hand Modal Dialog
    if (showHandModal) {
        Dialog(
            onDismissRequest = { showHandModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2C2C2C)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        "Your Hand",
                        style = MaterialTheme.typography.h5,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (state.playerHand.isEmpty()) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("Your hand is empty.", color = Color.LightGray)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(state.playerHand) { card ->
                                Card(
                                    backgroundColor = Color(0xFF3C3C3C),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 8.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            card.name,
                                            style = MaterialTheme.typography.h6,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Movements: ${card.movements}",
                                            style = MaterialTheme.typography.body2,
                                            color = Color.LightGray
                                        )
                                        Text(
                                            "Strength: ${card.strength}",
                                            style = MaterialTheme.typography.body2,
                                            color = Color.LightGray
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        // Only show the "Place" button if we are supposed to place generals
                                        if (isPlacementPhase) {
                                            Button(
                                                onClick = {
                                                    selectedGeneralFromHand = card
                                                    showHandModal = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    backgroundColor = Color(0xFFE91E63),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Text("Place")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showHandModal = false },
                        modifier = Modifier.align(Alignment.End).fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    // End Turn Confirmation Dialog
    if (showEndTurnConfirm) {
        AlertDialog(
            onDismissRequest = { showEndTurnConfirm = false },
            title = {
                Text("End your turn?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text("Are you sure you want to end your turn?", color = Color.LightGray)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndTurnConfirm = false
                        onNextTurn()
                    }
                ) {
                    Text("Yes", color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEndTurnConfirm = false }
                ) {
                    Text("No", color = Color.White)
                }
            },
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
