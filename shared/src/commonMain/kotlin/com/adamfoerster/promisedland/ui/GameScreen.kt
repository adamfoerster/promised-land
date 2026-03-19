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
    onPlaceGeneral: (generalId: Long, hexCol: Int, hexRow: Int) -> String?,
    onSelectActiveGeneral: (placementId: Long?) -> Unit,
    onMoveGeneral: (placementId: Long, hexCol: Int, hexRow: Int) -> String?
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
    var showHandModal by remember { mutableStateOf(false) }
    var showEndTurnConfirm by remember { mutableStateOf(false) }
    var selectedGeneralFromHand by remember { mutableStateOf<GeneralData?>(null) }
    var placementError by remember { mutableStateOf("") }
    var pendingMovePlacementId by remember { mutableStateOf<Long?>(null) }
    val selectedHexScroll = rememberScrollState()

    val isPlacementPhase = state.currentRound == 1L && state.currentPlayerGeneralCount < 2
    val needsPlacement = isPlacementPhase
    val placementsInSelectedHex = if (selectedHex != null) {
        state.generalPlacements.filter {
            it.hexCol == selectedHex!!.col &&
                it.hexRow == selectedHex!!.row &&
                it.playerId == state.currentPlayer.id
        }
    } else {
        emptyList()
    }
    val activeGeneralForMove =
        state.selectedActiveGeneralForMove
            ?: state.generalPlacements.find { it.id == pendingMovePlacementId }

    LaunchedEffect(state.selectedActiveGeneralForMove?.id) {
        if (state.selectedActiveGeneralForMove != null) {
            pendingMovePlacementId = state.selectedActiveGeneralForMove.id
        }
    }

    LaunchedEffect(state.currentRound, state.currentPhase, state.currentPlayer.id) {
        selectedHex = null
        pendingMovePlacementId = null
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        HexMap(
            modifier = Modifier.fillMaxSize(),
            turnKey = "${state.currentRound}-${state.currentPhase}-${state.currentPlayer.id}",
            onHexSelected = { hex ->
                placementError = ""
                val selectedGeneral = activeGeneralForMove
                val isMoveAttempt =
                    selectedGeneral != null &&
                        (selectedGeneral.hexCol != hex.col || selectedGeneral.hexRow != hex.row)

                if (isMoveAttempt) {
                    val error = onMoveGeneral(selectedGeneral!!.id, hex.col, hex.row)
                    if (error != null) {
                        placementError = error
                        selectedHex = hex
                    } else {
                        pendingMovePlacementId = null
                        selectedHex = null
                    }
                } else if (isPlacementPhase && selectedGeneralFromHand != null && hex.type == "village") {
                    selectedHex = hex
                    val error = onPlaceGeneral(selectedGeneralFromHand!!.id, hex.col, hex.row)
                    if (error != null) {
                        placementError = error
                    } else {
                        selectedGeneralFromHand = null
                        placementError = ""
                    }
                } else {
                    selectedHex = hex
                    pendingMovePlacementId = null
                    onSelectActiveGeneral(null)
                }
            },
            selectedHex = selectedHex,
            hexagons = state.hexagons,
            generalPlacements = state.generalPlacements,
            reachableHexes = state.reachableHexes
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                        DropdownMenuItem(
                            onClick = {
                                menuExpanded = false
                                onReturnToWelcome()
                            }
                        ) {
                            Text("Return to Welcome Screen")
                        }
                    }
                }
            }

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
            }

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

            Spacer(modifier = Modifier.weight(1f))

            val canShowMoveButtons = state.currentRound > 1L && placementsInSelectedHex.isNotEmpty()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.78f),
                elevation = 10.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    if (canShowMoveButtons) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(selectedHexScroll),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            placementsInSelectedHex.forEach { placement ->
                                val hasMoved = placement.lastMovedRound >= state.currentRound
                                val isSelectedForMove =
                                    activeGeneralForMove?.id == placement.id
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
                                            if (isSelectedForMove) Color(0xFF2E7D32) else Color(0xFFE91E63),
                                        contentColor = Color.White,
                                        disabledBackgroundColor = Color.Gray.copy(alpha = 0.5f),
                                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Place,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        placement.generalName + if (hasMoved) " (Moved)" else "",
                                        style = MaterialTheme.typography.button
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (state.currentRound > 1L) {
                        Text(
                            text = "Select one of your hexagons to choose a general.",
                            style = MaterialTheme.typography.body2,
                            color = Color.LightGray,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.currentRound > 1L && state.idleGenerals.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        val nextIdle = state.idleGenerals.first()
                                        selectedHex = state.hexagons[nextIdle.hexCol to nextIdle.hexRow]
                                        pendingMovePlacementId = nextIdle.id
                                        onSelectActiveGeneral(nextIdle.id)
                                    },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF2196F3),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Idle", style = MaterialTheme.typography.button)
                                }
                            }

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
