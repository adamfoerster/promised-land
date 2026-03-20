package com.adamfoerster.promisedland.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adamfoerster.promisedland.game.GeneralData

@Composable
fun HandModal(
    showHandModal: Boolean,
    onDismiss: () -> Unit,
    playerHand: List<GeneralData>,
    isPlacementPhase: Boolean,
    onGeneralSelected: (GeneralData) -> Unit
) {
    if (showHandModal) {
        Dialog(
            onDismissRequest = onDismiss,
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

                    if (playerHand.isEmpty()) {
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
                            items(playerHand) { card ->
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
                                                    onGeneralSelected(card)
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
                        onClick = onDismiss,
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
}