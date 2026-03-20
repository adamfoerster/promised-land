package com.adamfoerster.promisedland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.adamfoerster.promisedland.game.*

fun getControlledCities(state: GameUIState): List<HexagonData> {
    if (state.currentPlayer == null) return emptyList()
    val playerId = state.currentPlayer.id
    val controlledHexes = mutableSetOf<Pair<Int, Int>>()

    state.generalPlacements.forEach { 
        if (it.playerId == playerId) controlledHexes.add(it.hexCol to it.hexRow)
    }
    state.hexImprovements.forEach {
        if (it.playerId == playerId && it.troops > 0) controlledHexes.add(it.hexCol to it.hexRow)
    }

    return controlledHexes.mapNotNull { (col, row) ->
        val hex = state.hexagons[col to row]
        if (hex != null && (hex.type == "city" || hex.type == "village")) hex else null
    }
}

@Composable
fun IncomeModal(
    state: GameUIState,
    onDismiss: () -> Unit
) {
    val controlledCities = getControlledCities(state)
    val regularIncome = controlledCities.size * 2
    val totalIncome = regularIncome + if (state.currentRound == 1L) 6 else 0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2C2C2C),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Income Phase", style = MaterialTheme.typography.h6, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(controlledCities) { city ->
                        val label = city.name.ifBlank { "(${city.col}, ${city.row})" }
                        Text("• $label (2 coins)", color = Color.LightGray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (state.currentRound == 1L) {
                    Text("+ 6 coins (First Round Bonus)", color = Color(0xFFA5D6A7), fontWeight = FontWeight.Bold)
                }
                Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                Text("Total Income: $totalIncome coins", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1976D2), contentColor = Color.White)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AcquisitionsModal(
    state: GameUIState,
    onBuy: (List<PurchaseItem>) -> String?,
    onDismiss: () -> Unit
) {
    if (state.currentPlayer == null) return
    val controlledCities = getControlledCities(state)
    var cart by remember { mutableStateOf(listOf<PurchaseItem>()) }
    var errorMessage by remember { mutableStateOf("") }

    val totalCost = cart.sumOf {
        when (it.itemType) {
            "troop" -> 2
            "development" -> 6
            "wall" -> 10
            else -> 0
        }.toInt()
    }
    
    val balanceRemaining = state.currentPlayer.balance - totalCost

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2C2C2C),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Acquisitions", style = MaterialTheme.typography.h6, color = Color.White)
                Text("Balance: ${state.currentPlayer.balance} coins", color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 300.dp)) {
                    items(controlledCities) { city ->
                        val improvements = state.hexImprovements.find { it.hexCol == city.col && it.hexRow == city.row }
                        val cityCart = cart.filter { it.hexCol == city.col && it.hexRow == city.row }
                        val label = city.name.ifBlank { "(${city.col}, ${city.row})" }
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            // Troop
                            Button(
                                onClick = { cart = cart + PurchaseItem(city.col, city.row, "troop") },
                                enabled = balanceRemaining >= 2,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                                contentPadding = PaddingValues(2.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Troop(2)", style = MaterialTheme.typography.overline) }
                            // Wall
                            val wallCount = (improvements?.walls ?: 0) + cityCart.count { it.itemType == "wall" }
                            Button(
                                onClick = { cart = cart + PurchaseItem(city.col, city.row, "wall") },
                                enabled = balanceRemaining >= 10 && wallCount < 1,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF9800)),
                                contentPadding = PaddingValues(2.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Wall(10)", style = MaterialTheme.typography.overline) }
                            // Dev
                            val devCount = (improvements?.developments ?: 0) + cityCart.count { it.itemType == "development" }
                            Button(
                                onClick = { cart = cart + PurchaseItem(city.col, city.row, "development") },
                                enabled = balanceRemaining >= 6 && devCount < 2,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF9C27B0)),
                                contentPadding = PaddingValues(2.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Dev(6)", style = MaterialTheme.typography.overline) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = Color.Red, style = MaterialTheme.typography.caption)
                }
                Text("Cart Total: $totalCost coins", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Balance after purchase: $balanceRemaining", color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { cart = emptyList(); onDismiss() }) { Text("Cancel", color = Color(0xFFFF6E6E)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val error = onBuy(cart)
                            if (error != null) {
                                errorMessage = error
                            } else {
                                onDismiss()
                            }
                        },
                        enabled = cart.isNotEmpty() && balanceRemaining >= 0,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1976D2), contentColor = Color.White)
                    ) {
                        Text("Buy", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
