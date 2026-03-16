package com.adamfoerster.promisedland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    hasSavedGames: Boolean,
    onNewGame: () -> Unit,
    onContinueGame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D1A), Color(0xFF1A1A2E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚔",
                fontSize = 72.sp,
                modifier = Modifier.padding(bottom = 16.dp),
                color = Color.White
            )

            Text(
                text = "Promised Land",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "A strategy board game",
                style = MaterialTheme.typography.subtitle1,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 8.dp, bottom = 64.dp)
            )

            Button(
                onClick = onNewGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFE91E63),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Text(
                    "New Game",
                    style = MaterialTheme.typography.button,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onContinueGame,
                enabled = hasSavedGames,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF3D5AFE),
                    contentColor = Color.White,
                    disabledBackgroundColor = Color(0xFF2A2A3A),
                    disabledContentColor = Color(0xFF5A5A6A)
                ),
                elevation = ButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Text(
                    "Continue Game",
                    style = MaterialTheme.typography.button,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
