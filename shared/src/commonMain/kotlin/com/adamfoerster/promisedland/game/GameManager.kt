package com.adamfoerster.promisedland.game

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.adamfoerster.promisedland.GameDatabase
import com.adamfoerster.promisedland.Player
import com.adamfoerster.promisedland.db.DatabaseDriverFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock

data class SavedGameSummary(
    val id: Long,
    val name: String,
    val startedAt: Long,
    val playerCount: Long
)

data class GameUIState(
    val gameId: Long? = null,
    val gameName: String = "",
    val currentRound: Long = 1,
    val currentPhase: Long = 1,
    val currentPlayer: Player? = null,
    val players: List<Player> = emptyList(),
    val isGameActive: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class GameManager(
    databaseDriverFactory: DatabaseDriverFactory,
    scope: CoroutineScope
) {
    private val database = GameDatabase(databaseDriverFactory.createDriver())
    private val queries = database.gameDatabaseQueries

    // The currently active game id (null = no active game / on welcome screen)
    private val activeGameId = MutableStateFlow<Long?>(null)

    val state: StateFlow<GameUIState> = activeGameId.flatMapLatest { gameId ->
        if (gameId == null) {
            flowOf(GameUIState())
        } else {
            combine(
                queries.selectPlayersForGame(gameId).asFlow().mapToList(Dispatchers.Default),
                queries.getGameState(gameId).asFlow().mapToOneOrNull(Dispatchers.Default)
            ) { players: List<Player>, gameState ->
                if (gameState == null || players.isEmpty()) {
                    GameUIState(gameId = gameId)
                } else {
                    val currentPlayer = players.find { it.id == gameState.currentPlayerId }
                    val gameName = queries.selectAllGames().executeAsList()
                        .find { row -> row.id == gameId }?.name ?: ""
                    GameUIState(
                        gameId = gameId,
                        gameName = gameName,
                        currentRound = gameState.currentRound,
                        currentPhase = gameState.currentPhase,
                        currentPlayer = currentPlayer,
                        players = players.sortedBy { it.turnOrder },
                        isGameActive = true
                    )
                }
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(), GameUIState())

    val savedGames: StateFlow<List<SavedGameSummary>> =
        queries.selectAllGames().asFlow().mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    SavedGameSummary(
                        id = row.id,
                        name = row.name,
                        startedAt = row.startedAt,
                        playerCount = row.playerCount
                    )
                }
            }.stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    fun setupGame(gameName: String, playersInfo: List<Pair<String, String>>) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.transaction {
            queries.insertGame(gameName.ifBlank { "Game" }, now)
            val gameId = queries.lastInsertRowId().executeAsOne()

            val shuffled = playersInfo.shuffled()
            shuffled.forEachIndexed { index, info ->
                queries.insertPlayer(gameId, info.first, info.second, index.toLong())
            }

            val allPlayers = queries.selectPlayersForGame(gameId).executeAsList()
            if (allPlayers.isNotEmpty()) {
                queries.initializeGame(gameId, allPlayers.first().id)
            }

            activeGameId.value = gameId
        }
    }

    fun loadGame(gameId: Long) {
        activeGameId.value = gameId
    }

    fun returnToWelcome() {
        activeGameId.value = null
    }

    fun nextTurn() {
        val gameId = activeGameId.value ?: return
        val players = queries.selectPlayersForGame(gameId).executeAsList().sortedBy { it.turnOrder }
        val gameState = queries.getGameState(gameId).executeAsOneOrNull()

        if (players.isEmpty() || gameState == null) return

        val m = players.size
        val currentRound = gameState.currentRound
        val currentPhase = gameState.currentPhase
        val currPlayerId = gameState.currentPlayerId

        val currIdx = players.indexOfFirst { it.id == currPlayerId }
        val startIndex = ((currentRound - 1) % m).toInt()
        val lastIndex = (startIndex + m - 1) % m

        if (currIdx == lastIndex) {
            if (currentPhase == 7L) {
                val newRound = currentRound + 1
                val newStartIndex = ((newRound - 1) % m).toInt()
                queries.updateGameState(newRound, 1L, players[newStartIndex].id, gameId)
            } else {
                queries.updateGameState(currentRound, currentPhase + 1, players[startIndex].id, gameId)
            }
        } else {
            val nextIdx = (currIdx + 1) % m
            queries.updateGameState(currentRound, currentPhase, players[nextIdx].id, gameId)
        }
    }
}
