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

@Composable
fun GameScreen(
    state: GameUIState,
    onNextTurn: () -> Unit,
    onReturnToWelcome: () -> Unit
) {
    if (state.currentPlayer == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val phaseNames = listOf(
        "Announcements",
        "Draw cards",
        "Income",
        "Building and mustering",
        "Movements",
        "Combat",
        "Check victory"
    )
    val phaseName = phaseNames.getOrNull((state.currentPhase - 1).toInt()) ?: "Unknown"

    var menuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

        // Title row with menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    state.gameName.ifBlank { "Promised Land" },
                    style = MaterialTheme.typography.h5,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (state.gameName.isNotBlank()) {
                    Text("Promised Land", style = MaterialTheme.typography.caption, color = Color(0xFF9E9E9E))
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
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

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            elevation = 8.dp,
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Round ${state.currentRound}", style = MaterialTheme.typography.h5, color = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Phase ${state.currentPhase}", style = MaterialTheme.typography.subtitle1, color = Color.Gray)
                Text(phaseName, style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        val colorInfo = playerColors.find { it.first == state.currentPlayer.color }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp,
            backgroundColor = Color(0xFF333333),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(colorInfo?.second ?: Color.Gray))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Current Player", style = MaterialTheme.typography.subtitle2, color = Color.Gray)
                    Text(state.currentPlayer.name, style = MaterialTheme.typography.h5, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNextTurn,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE91E63), contentColor = Color.White)
        ) {
            Text("Complete Turn & Next Player", style = MaterialTheme.typography.button, fontWeight = FontWeight.Bold)
        }
    }
}
