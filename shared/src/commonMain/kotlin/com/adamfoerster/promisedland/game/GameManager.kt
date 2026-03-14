package com.adamfoerster.promisedland.game

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.adamfoerster.promisedland.GameDatabase
import com.adamfoerster.promisedland.Player
import com.adamfoerster.promisedland.db.DatabaseDriverFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GameUIState(
    val currentRound: Long = 1,
    val currentPhase: Long = 1,
    val currentPlayer: Player? = null,
    val players: List<Player> = emptyList(),
    val isSetupFinished: Boolean = false
)

class GameManager(
    databaseDriverFactory: DatabaseDriverFactory,
    scope: CoroutineScope
) {
    private val database = GameDatabase(databaseDriverFactory.createDriver())
    private val queries = database.gameDatabaseQueries

    val state: StateFlow<GameUIState> = combine(
        queries.selectAllPlayers().asFlow().mapToList(Dispatchers.Default),
        queries.getGameState().asFlow().mapToOneOrNull(Dispatchers.Default)
    ) { players, gameState ->
        if (gameState == null || players.isEmpty()) {
            GameUIState(isSetupFinished = false, players = players)
        } else {
            val currentPlayer = players.find { it.id == gameState.currentPlayerId }
            GameUIState(
                currentRound = gameState.currentRound,
                currentPhase = gameState.currentPhase,
                currentPlayer = currentPlayer,
                players = players.sortedBy { it.turnOrder },
                isSetupFinished = true
            )
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(), GameUIState())

    fun setupGame(playersInfo: List<Pair<String, String>>) {
        queries.transaction {
            queries.deleteAllPlayers()
            queries.deleteGameState()
            
            val shuffled = playersInfo.shuffled()
            shuffled.forEachIndexed { index, info ->
                queries.insertPlayer(info.first, info.second, index.toLong())
            }
            
            val allPlayers = queries.selectAllPlayers().executeAsList()
            if (allPlayers.isNotEmpty()) {
                val firstPlayer = allPlayers.first()
                queries.initializeGame(firstPlayer.id)
            }
        }
    }

    fun nextTurn() {
        val players = queries.selectAllPlayers().executeAsList().sortedBy { it.turnOrder }
        val gameState = queries.getGameState().executeAsOneOrNull()
        
        if (players.isEmpty() || gameState == null) return
        
        val M = players.size
        val currentRound = gameState.currentRound
        val currentPhase = gameState.currentPhase
        val currPlayerId = gameState.currentPlayerId
        
        val currIdx = players.indexOfFirst { it.id == currPlayerId }
        val startIndex = ((currentRound - 1) % M).toInt()
        val lastIndex = (startIndex + M - 1) % M
        
        if (currIdx == lastIndex) {
            // End of phase
            if (currentPhase == 7L) {
                // End of round
                val newRound = currentRound + 1
                val newPhase = 1L
                val newStartIndex = ((newRound - 1) % M).toInt()
                val newPlayerId = players[newStartIndex].id
                queries.updateGameState(newRound, newPhase, newPlayerId)
            } else {
                // Next phase, same round
                val newPhase = currentPhase + 1
                val newPlayerId = players[startIndex].id
                queries.updateGameState(currentRound, newPhase, newPlayerId)
            }
        } else {
            // Next player in the same phase
            val nextIdx = (currIdx + 1) % M
            val newPlayerId = players[nextIdx].id
            queries.updateGameState(currentRound, currentPhase, newPlayerId)
        }
    }
}
