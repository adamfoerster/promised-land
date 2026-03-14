package com.adamfoerster.promisedland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val playerColors = listOf(
    "Red" to Color.Red,
    "Blue" to Color.Blue,
    "Green" to Color.Green,
    "Yellow" to Color.Yellow,
    "Orange" to Color(0xFFFFA500),
    "Purple" to Color(0xFF800080),
    "Black" to Color.Black,
    "White" to Color.White
)

@Composable
fun SetupScreen(onStartGame: (List<Pair<String, String>>) -> Unit) {
    var players by remember { mutableStateOf(List(3) { index -> "Player ${index + 1}" to playerColors[index].first }) }
    var errorMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)).padding(16.dp)) {
        Text("Setup Strategy Game", style = MaterialTheme.typography.h4, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))
        
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colors.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(players) { index, player ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), backgroundColor = Color(0xFF2C2C2C)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = player.first,
                            onValueChange = { newName ->
                                val newList = players.toMutableList()
                                newList[index] = newName to player.second
                                players = newList
                                errorMessage = ""
                            },
                            label = { Text("Name", color = Color.LightGray) },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            val colorInfo = playerColors.find { it.first == player.second }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(colorInfo?.second ?: Color.Gray)
                                    .clickable { expanded = true }
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                playerColors.forEach { (colorName, colorValue) ->
                                    DropdownMenuItem(onClick = {
                                        val newList = players.toMutableList()
                                        newList[index] = player.first to colorName
                                        players = newList
                                        expanded = false
                                        errorMessage = ""
                                    }) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(24.dp).clip(CircleShape).background(colorValue)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(colorName)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (players.size > 3) {
                            IconButton(onClick = {
                                val newList = players.toMutableList()
                                newList.removeAt(index)
                                players = newList
                                errorMessage = ""
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = {
                    if (players.size < 6) {
                        val usedColors = players.map { it.second }.toSet()
                        val availableColor = playerColors.find { !usedColors.contains(it.first) }?.first ?: playerColors.first().first
                        players = players + ("Player ${players.size + 1}" to availableColor)
                    }
                },
                enabled = players.size < 6,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF03A9F4), contentColor = Color.White)
            ) {
                Text("Add Player")
            }

            Button(
                onClick = {
                    val names = players.map { it.first.trim() }
                    val colors = players.map { it.second }
                    
                    if (names.any { it.isEmpty() }) {
                        errorMessage = "Names cannot be empty"
                        return@Button
                    }
                    if (names.size != names.toSet().size) {
                        errorMessage = "Names must be unique"
                        return@Button
                    }
                    if (colors.size != colors.toSet().size) {
                        errorMessage = "Colors cannot be repeated"
                        return@Button
                    }
                    
                    onStartGame(players)
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50), contentColor = Color.White)
            ) {
                Text("Start Game")
            }
        }
    }
}
