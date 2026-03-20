package com.adamfoerster.promisedland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.adamfoerster.promisedland.game.GameUIState
import com.adamfoerster.promisedland.game.GeneralData
import com.adamfoerster.promisedland.game.GeneralPlacementInfo
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
    var scrollToHexTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var zoomCycleCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    val isPlacementPhase = state.currentPhase == 3L
    val needsPlacement = isPlacementPhase && state.currentPlayerGeneralCount < 2

    val placementsOnSelectedHex = if (selectedHex != null) {
        state.generalPlacements.filter { it.hexCol == selectedHex?.col && it.hexRow == selectedHex?.row }
    } else emptyList()

    val currentPlayerPlacementsOnSelectedHex = placementsOnSelectedHex.filter { it.playerId == state.currentPlayer.id }

    val activeGeneralForMove = state.selectedActiveGeneralForMove

    Box(modifier = Modifier.fillMaxSize()) {
        // Hex Map as background (fills entire screen)
        HexMap(
            hexagons = state.hexagons,
            selectedHex = selectedHex,
            generalPlacements = state.generalPlacements,
            reachableHexes = state.reachableHexes,
            scrollToHexTarget = scrollToHexTarget,
            onZoomCycleReady = { callback -> zoomCycleCallback = callback },
            onHexSelected = { hex ->
                if (selectedGeneralFromHand != null) {
                    val error = onPlaceGeneral(selectedGeneralFromHand!!.id, hex.col, hex.row)
                    if (error == null) {
                        selectedGeneralFromHand = null
                        selectedHex = hex
                        placementError = ""
                    } else {
                        placementError = error
                    }
                } else if (activeGeneralForMove != null) {
                    val error = onMoveGeneral(activeGeneralForMove.id, hex.col, hex.row)
                    if (error == null) {
                        selectedHex = hex
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

        // Top Bar (always on top)
        TopBar(state = state, onReturnToWelcome = onReturnToWelcome, onZoomCycle = zoomCycleCallback)

        // Bottom Bar (always on top)
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomBar(
                state = state,
                showHandModal = showHandModal,
                onShowHandModal = { showHandModal = it },
                isPlacementPhase = isPlacementPhase,
                needsPlacement = needsPlacement,
                placementError = placementError,
                selectedHex = selectedHex,
                placementsOnSelectedHex = placementsOnSelectedHex,
                activeGeneralForMove = activeGeneralForMove,
                currentPlayerPlacementsOnSelectedHex = currentPlayerPlacementsOnSelectedHex,
                onSelectActiveGeneral = onSelectActiveGeneral,
                onIdleClicked = { hexCol, hexRow, placementId ->
                    selectedHex = state.hexagons[hexCol to hexRow]
                    scrollToHexTarget = hexCol to hexRow
                    pendingMovePlacementId = placementId
                    onSelectActiveGeneral(placementId)
                },
                onMoveGeneral = onMoveGeneral,
                onShowEndTurnConfirm = { showEndTurnConfirm = it }
            )
        }
    }

    HandModal(
        showHandModal = showHandModal,
        onDismiss = { showHandModal = false },
        playerHand = state.playerHand,
        isPlacementPhase = isPlacementPhase,
        onGeneralSelected = { card ->
            selectedGeneralFromHand = card
            showHandModal = false
        }
    )

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
fun HexInfoSection(
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
