package com.adamfoerster.promisedland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adamfoerster.promisedland.game.GameUIState
import com.adamfoerster.promisedland.game.GeneralData
import com.adamfoerster.promisedland.game.GeneralPlacementInfo
import com.adamfoerster.promisedland.game.HexagonData

private val HudBarColor = Color(0xFF151515)
private val HudPanelColor = Color(0xFF242424)

@Composable
fun GameScreen(
    state: GameUIState,
    onNextTurn: () -> Unit,
    onReturnToWelcome: () -> Unit,
    onPlaceGeneral: (generalId: Long, hexCol: Int, hexRow: Int) -> String?,
    onSelectActiveGeneral: (placementId: Long?) -> Unit,
    onMoveGeneral: (placementId: Long, hexCol: Int, hexRow: Int) -> String?
) {
    if (state.currentPlayer == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            Text("No active game found", color = Color.White)
        }
        return
    }

    var showHandModal by remember { mutableStateOf(false) }
    var showEndTurnConfirm by remember { mutableStateOf(false) }
    var selectedHex by remember { mutableStateOf<HexagonData?>(null) }
    var selectedGeneralFromHand by remember { mutableStateOf<GeneralData?>(null) }
    var placementError by remember { mutableStateOf("") }
    var pendingMovePlacementId by remember { mutableStateOf<Long?>(null) }

    val isPlacementPhase = state.currentPhase == 1L
    val needsPlacement = isPlacementPhase && state.currentPlayerGeneralCount < 2

    val moveSelectionScroll = rememberScrollState()

    val placementsOnSelectedHex = if (selectedHex != null) {
        state.generalPlacements.filter { it.hexCol == selectedHex?.col && it.hexRow == selectedHex?.row }
    } else emptyList()

    val currentPlayerPlacementsOnSelectedHex = placementsOnSelectedHex.filter { it.playerId == state.currentPlayer.id }

    val activeGeneralForMove = state.selectedActiveGeneralForMove

    Box(modifier = Modifier.fillMaxSize()) {
        HexMap(
            hexagons = state.hexagons,
            selectedHex = selectedHex,
            generalPlacements = state.generalPlacements,
            onHexSelected = { hex ->
                if (selectedGeneralFromHand != null) {
                    val error = onPlaceGeneral(selectedGeneralFromHand!!.id, hex.col, hex.row)
                    if (error == null) {
                        selectedGeneralFromHand = null
                        placementError = ""
                    } else {
                        placementError = error
                    }
                } else if (activeGeneralForMove != null) {
                    val error = onMoveGeneral(activeGeneralForMove.id, hex.col, hex.row)
                    if (error == null) {
                        onSelectActiveGeneral(null)
                        pendingMovePlacementId = null
                        placementError = ""
                    } else {
                        placementError = error
                    }
                } else {
                    selectedHex = if (selectedHex?.id == hex.id) null else hex
                    placementError = ""
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = HudBarColor,
            elevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.widthIn(min = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showHandModal = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = HudPanelColor,
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedBorder.copy(width = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✋",
                                color = Color.White,
                                style = MaterialTheme.typography.caption,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            state.playerHand.size.toString(),
                            style = MaterialTheme.typography.button
                        )
                    }

                    if (state.currentRound > 1L && state.idleGenerals.isNotEmpty()) {
                        Button(
                            onClick = {
                                val nextIdle = state.idleGenerals.first()
                                selectedHex = state.hexagons[nextIdle.hexCol to nextIdle.hexRow]
                                pendingMovePlacementId = nextIdle.id
                                onSelectActiveGeneral(nextIdle.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF1976D2),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text("Idle", style = MaterialTheme.typography.button)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isPlacementPhase) {
                        Text(
                            text = "Place your two generals in villages.",
                            color = Color(0xFFFF80AB),
                            style = MaterialTheme.typography.body2,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (placementError.isNotEmpty()) {
                        Text(
                            text = placementError,
                            color = Color(0xFFFF6E6E),
                            style = MaterialTheme.typography.body2
                        )
                    }

                    HexInfoSection(
                        selectedHex = selectedHex,
                        placementsOnSelectedHex = placementsOnSelectedHex,
                        activeGeneralForMove = activeGeneralForMove
                    )

                    if (state.currentRound > 1L) {
                        if (currentPlayerPlacementsOnSelectedHex.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(moveSelectionScroll),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentPlayerPlacementsOnSelectedHex.forEach { placement ->
                                    val hasMoved = placement.lastMovedRound >= state.currentRound
                                    val isSelectedForMove = activeGeneralForMove?.id == placement.id
                                    Button(
                                        onClick = {
                                            if (!hasMoved) {
                                                val newSelection =
                                                    if (isSelectedForMove) null else placement.id
                                                pendingMovePlacementId = newSelection
                                                onSelectActiveGeneral(newSelection)
                                            }
                                        },
                                        enabled = !hasMoved,
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor =
                                                if (isSelectedForMove) Color(0xFF2E7D32) else Color(
                                                    0xFF6A1B9A
                                                ),
                                            contentColor = Color.White,
                                            disabledBackgroundColor = Color.Gray.copy(alpha = 0.45f),
                                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = placement.generalName + if (hasMoved) " moved" else "",
                                            style = MaterialTheme.typography.caption
                                        )
                                    }
                                }
                            }
                        } else if (selectedHex == null) {
                            Text(
                                text = "Select a hex to inspect it or choose a general to move.",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { showEndTurnConfirm = true },
                    enabled = !needsPlacement,
                    modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (needsPlacement) Color.Gray else Color(0xFFD32F2F),
                        contentColor = Color.White,
                        disabledBackgroundColor = Color.Gray.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (needsPlacement) "Place\nGenerals" else "Next Turn",
                        style = MaterialTheme.typography.button
                    )
                }
            }
        }
    }

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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        "Your Hand",
                        style = MaterialTheme.typography.h5,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (state.playerHand.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
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
                        modifier = Modifier
                            .align(Alignment.End)
                            .fillMaxWidth(),
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
                TextButton(onClick = { showEndTurnConfirm = false }) {
                    Text("No", color = Color.White)
                }
            },
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun HexInfoSection(
    selectedHex: HexagonData?,
    placementsOnSelectedHex: List<GeneralPlacementInfo>,
    activeGeneralForMove: GeneralPlacementInfo?
) {
    if (selectedHex == null) {
        Text(
            text = "No hex selected.",
            color = Color.White,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tap a hex to view its details and any generals on it.",
            color = Color.LightGray,
            style = MaterialTheme.typography.caption
        )
        if (activeGeneralForMove != null) {
            Text(
                text = "Selected to move: ${activeGeneralForMove.generalName}",
                color = Color(0xFFA5D6A7),
                style = MaterialTheme.typography.caption
            )
        }
        return
    }

    val typeText = selectedHex.type?.replaceFirstChar { it.uppercase() } ?: "None"
    val terrainText = selectedHex.terrain?.replaceFirstChar { it.uppercase() } ?: "Unknown"

    Text(
        text = "${selectedHex.id}",
        color = Color.White,
        style = MaterialTheme.typography.body1,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "$typeText | $terrainText",
        color = Color.LightGray,
        style = MaterialTheme.typography.caption
    )

    if (placementsOnSelectedHex.isEmpty()) {
        Text(
            text = "Generals: none",
            color = Color.LightGray,
            style = MaterialTheme.typography.caption
        )
    } else {
        Text(
            text = buildAnnotatedString {
                append("\uD83D\uDEE1\uFE0F ")
                placementsOnSelectedHex.forEachIndexed { index, placement ->
                    val color = playerColors.find { it.first == placement.playerColor }?.second ?: Color.LightGray
                    withStyle(style = SpanStyle(color = color)) {
                        append(placement.generalName)
                    }
                    if (index < placementsOnSelectedHex.size - 1) {
                        append(" - ")
                    }
                }
            },
            color = Color.LightGray,
            style = MaterialTheme.typography.caption
        )
    }

    if (activeGeneralForMove != null) {
        Text(
            text = "Selected to move: ${activeGeneralForMove.generalName}",
            color = Color(0xFFA5D6A7),
            style = MaterialTheme.typography.caption
        )
    }
}
