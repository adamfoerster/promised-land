package com.adamfoerster.promisedland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adamfoerster.promisedland.game.GameUIState
import com.adamfoerster.promisedland.game.GeneralPlacementInfo
import com.adamfoerster.promisedland.game.HexagonData

private val HudBarColor = Color(0xFF151515)
private val HudPanelColor = Color(0xFF242424)

@Composable
fun BottomBar(
    state: GameUIState,
    showHandModal: Boolean,
    onShowHandModal: (Boolean) -> Unit,
    onShowAcquisitionsModal: (Boolean) -> Unit,
    isPlacementPhase: Boolean,
    needsPlacement: Boolean,
    placementError: String,
    selectedHex: HexagonData?,
    placementsOnSelectedHex: List<GeneralPlacementInfo>,
    activeGeneralForMove: GeneralPlacementInfo?,
    currentPlayerPlacementsOnSelectedHex: List<GeneralPlacementInfo>,
    onSelectActiveGeneral: (Long?) -> Unit,
    onIdleClicked: (hexCol: Int, hexRow: Int, placementId: Long) -> Unit,
    onMoveGeneral: (placementId: Long, hexCol: Int, hexRow: Int) -> String?,
    onShowEndTurnConfirm: (Boolean) -> Unit
) {
    val moveSelectionScroll = rememberScrollState()
    val showPlacementInfo = remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = HudBarColor,
        elevation = 12.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Placement phase info banner
            if (needsPlacement && showPlacementInfo.value) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = Color(0xFFFF80AB).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Place your two generals in villages.",
                            color = Color(0xFFFF80AB),
                            style = MaterialTheme.typography.body2,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showPlacementInfo.value = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFFF80AB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Main content row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Button(
                        onClick = { onShowAcquisitionsModal(true) },
                        enabled = state.currentPhase == 4L,
                        modifier = Modifier.height(40.dp).fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (state.currentPhase == 4L) Color(0xFF1976D2) else Color.Gray.copy(
                                alpha = 0.5f
                            ),
                            contentColor = Color.White,
                            disabledBackgroundColor = Color.Gray.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "🛒$${state.currentPlayer?.balance ?: 0}",
                            style = MaterialTheme.typography.button
                        )
                    }

                    Button(
                        onClick = { onShowHandModal(true) },
                        modifier = Modifier.height(40.dp).fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1976D2),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "✋ ${state.playerHand.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.caption,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (state.currentPhase == 5L && state.idleGenerals.isNotEmpty()) {
                        Button(
                            onClick = {
                                val nextIdle = state.idleGenerals.first()
                                onIdleClicked(nextIdle.hexCol, nextIdle.hexRow, nextIdle.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF1976D2),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(40.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "\uD83D\uDD14 Idle",
                                color = Color.White,
                                style = MaterialTheme.typography.caption,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(2f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (placementError.isNotEmpty()) {
                        Text(
                            text = placementError,
                            color = Color(0xFFFF6E6E),
                            style = MaterialTheme.typography.body2
                        )
                    }

                    val improvementOnSelectedHex = if (selectedHex != null) {
                        state.hexImprovements.find { it.hexCol == selectedHex.col && it.hexRow == selectedHex.row }
                    } else null

                    HexInfoSection(
                        selectedHex = selectedHex,
                        placementsOnSelectedHex = placementsOnSelectedHex,
                        improvement = improvementOnSelectedHex,
                        activeGeneralForMove = activeGeneralForMove
                    )

                    if (state.currentPhase == 5L) {
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

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .weight(1f),
                ) {
                    Button(
                        onClick = { onShowEndTurnConfirm(true) },
                        enabled = !needsPlacement,
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (needsPlacement) Color.Gray else Color(0xFFD32F2F),
                            contentColor = Color.White,
                            disabledBackgroundColor = Color.Gray.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (needsPlacement) "Place\nGenerals" else "Next Turn",
                            style = MaterialTheme.typography.button
                        )
                    }
                }
            }
        }
    }
}
