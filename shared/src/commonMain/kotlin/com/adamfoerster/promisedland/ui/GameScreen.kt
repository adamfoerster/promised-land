package com.adamfoerster.promisedland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adamfoerster.promisedland.game.GameUIState

@Composable
fun GameScreen(state: GameUIState, onNextTurn: () -> Unit) {
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

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Promised Land", style = MaterialTheme.typography.h4, color = Color.White, modifier = Modifier.padding(bottom = 32.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
