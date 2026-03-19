package com.adamfoerster.promisedland.game

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.db.SqlDriver
import com.adamfoerster.promisedland.GameDatabase
import com.adamfoerster.promisedland.Player
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

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
        val isGameActive: Boolean = false,
        val hexagons: Map<Pair<Int, Int>, HexagonData> = emptyMap(),
        val generals: List<GeneralData> = emptyList(),
        val generalPlacements: List<GeneralPlacementInfo> = emptyList(),
        val currentPlayerGeneralCount: Int = 0,
        val playerHand: List<GeneralData> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalResourceApi::class)
class GameManager(
    val driver: SqlDriver, 
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val skipMapSync: Boolean = false
) {
    private val database = GameDatabase(driver)
    private val queries = database.gameDatabaseQueries

    // The currently active game id (null = no active game / on welcome screen)
    private val activeGameId = MutableStateFlow<Long?>(null)

    // Increment this whenever you update hexagons.csv or generals.csv
    private val MAP_DATA_VERSION = 36L

    init {
        if (!skipMapSync) {
            scope.launch(dispatcher) { syncMapData() }
        }
    }

    private suspend fun syncMapData() {
        try {
            val currentVersion =
                    queries.getMetadata("map_version").executeAsOneOrNull()?.toLongOrNull() ?: 0L
            if (MAP_DATA_VERSION > currentVersion) {
                val csvContent = try {
                    resource("hexagons.csv").readBytes().decodeToString()
                } catch (e: Exception) {
                    null
                }
                
                if (csvContent != null) {
                    val lines = csvContent.lines().drop(1).filter { it.isNotBlank() }

                    queries.transaction {
                        queries.deleteAllHexagons()
                        lines.forEach { line ->
                            val parts = line.split(",")
                            if (parts.size >= 6) {
                                val col = parts[0].trim().lowercase()
                                val row = parts[1].trim().toLongOrNull() ?: return@forEach
                                val name = parts[2].trim().takeIf { it.isNotEmpty() }
                                val active = parts[3].trim().lowercase() == "true" || parts[3].trim() == "1"
                                val type =
                                        parts[4].trim().lowercase().takeIf {
                                            it != "none" && it.isNotEmpty()
                                        }
                                val terrain = parts[5].trim().lowercase().takeIf { it.isNotEmpty() }

                                queries.insertHexagon(colCharToLong(col), row - 1, name, active, type, terrain)
                            }
                        }
                    }
                }

                // Sync generals CSV
                val generalsCsv = try {
                    resource("generals.csv").readBytes().decodeToString()
                } catch (e: Exception) {
                    null
                }

                if (generalsCsv != null) {
                    val generalLines = generalsCsv.lines().drop(1).filter { it.isNotBlank() }

                    queries.transaction {
                        queries.deleteAllGenerals()
                        generalLines.forEach { line ->
                            val parts = line.split(",")
                            if (parts.size >= 3) {
                                val name = parts[0].trim()
                                val movements = parts[1].trim().toLongOrNull() ?: 2L
                                val strength = parts[2].trim().toLongOrNull() ?: 1L
                                if (name.isNotEmpty()) {
                                    queries.insertGeneral(name, movements, strength)
                                }
                            }
                        }
                    }
                }

                queries.setMetadata("map_version", MAP_DATA_VERSION.toString())
            }
        } catch (e: Exception) {
            println("Error syncing map data: ${e.message}")
        }
    }

    val hexagons: StateFlow<Map<Pair<Int, Int>, HexagonData>> =
            queries.selectAllHexagons()
                    .asFlow()
                    .mapToList(dispatcher)
                    .map { rows ->
                        rows.associate { row ->
                            (row.col.toInt() to row.row.toInt()) to
                                    HexagonData(
                                            col = row.col.toInt(),
                                            row = row.row.toInt(),
                                            name = row.name ?: "",
                                            isActive = row.isActive,
                                            type = row.type,
                                            terrain = row.terrain
                                    )
                        }
                    }
                    .stateIn(scope, SharingStarted.WhileSubscribed(), emptyMap())

    val generals: StateFlow<List<GeneralData>> =
            queries.selectAllGenerals()
                    .asFlow()
                    .mapToList(dispatcher)
                    .map { rows ->
                        rows.map { row ->
                            GeneralData(
                                    id = row.id,
                                    name = row.name,
                                    movements = row.movements.toInt(),
                                    strength = row.strength.toInt()
                            )
                        }
                    }
                    .stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    val state: StateFlow<GameUIState> =
            combine(activeGameId, hexagons, generals) { gameId, hexMap, generalsList ->
                Triple(gameId, hexMap, generalsList)
            }
                    .flatMapLatest { (gameId, hexMap, generalsList) ->
                        if (gameId == null) {
                            flowOf(GameUIState(hexagons = hexMap, generals = generalsList))
                        } else {
                            combine(
                                    queries.selectPlayersForGame(gameId)
                                            .asFlow()
                                            .mapToList(dispatcher),
                                    queries.getGameState(gameId)
                                            .asFlow()
                                            .mapToOneOrNull(dispatcher),
                                    queries.selectGeneralPlacementsForGame(gameId)
                                            .asFlow()
                                            .mapToList(dispatcher)
                            ) { players: List<Player>, gameState, placements ->
                                if (gameState == null || players.isEmpty()) {
                                    GameUIState(gameId = gameId, hexagons = hexMap, generals = generalsList)
                                } else {
                                    val currentPlayer =
                                            players.find { it.id == gameState.currentPlayerId }
                                    val gameName =
                                            queries.selectAllGames()
                                                    .executeAsList()
                                                    .find { row -> row.id == gameId }
                                                    ?.name
                                                    ?: ""
                                    val placementInfos = placements.map { p ->
                                        GeneralPlacementInfo(
                                                generalId = p.generalId,
                                                generalName = p.generalName,
                                                playerId = p.playerId,
                                                playerColor = p.playerColor,
                                                hexCol = p.hexCol.toInt(),
                                                hexRow = p.hexRow.toInt()
                                        )
                                    }
                                    val currentPlayerCount = placementInfos.count {
                                        it.playerId == gameState.currentPlayerId
                                    }
                                    
                                    val playerHandRows = queries.selectPlayerHand(gameId, gameState.currentPlayerId).executeAsList()
                                    val hand = playerHandRows.map { row -> 
                                        GeneralData(row.id, row.name, row.movements.toInt(), row.strength.toInt())
                                    }

                                    GameUIState(
                                            gameId = gameId,
                                            gameName = gameName,
                                            currentRound = gameState.currentRound,
                                            currentPhase = gameState.currentPhase,
                                            currentPlayer = currentPlayer,
                                            players = players.sortedBy { it.turnOrder },
                                            isGameActive = true,
                                            hexagons = hexMap,
                                            generals = generalsList,
                                            generalPlacements = placementInfos,
                                            currentPlayerGeneralCount = currentPlayerCount,
                                            playerHand = hand
                                    )
                                }
                            }
                        }
                    }
                    .stateIn(scope, SharingStarted.WhileSubscribed(), GameUIState())

    val savedGames: StateFlow<List<SavedGameSummary>> =
            queries.selectAllGames()
                    .asFlow()
                    .mapToList(dispatcher)
                    .map { rows ->
                        rows.map { row ->
                            SavedGameSummary(
                                    id = row.id,
                                    name = row.name,
                                    startedAt = row.startedAt,
                                    playerCount = row.playerCount
                            )
                        }
                    }
                    .stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    private fun colCharToLong(col: String): Long {
        return when (col) {
            "a" -> 0
            "b" -> 1
            "c" -> 2
            "d" -> 3
            "e" -> 4
            "f" -> 5
            "g" -> 6
            "h" -> 7
            "i" -> 8
            "j" -> 9
            else -> 0
        }
    }

    fun setupGame(gameName: String, playersInfo: List<Pair<String, String>>) {
        val now = Clock.System.now().toEpochMilliseconds()
        var newGameId: Long? = null
        queries.transaction {
            queries.insertGame(gameName.ifBlank { "Game" }, now)
            val gameId = queries.lastInsertRowId().executeAsOne()
            newGameId = gameId

            val shuffled = playersInfo.shuffled()
            val insertedPlayers = mutableListOf<Long>()
            shuffled.forEachIndexed { index, info ->
                queries.insertPlayer(gameId, info.first, info.second, index.toLong())
                insertedPlayers.add(queries.lastInsertRowId().executeAsOne())
            }

            val allPlayers = queries.selectPlayersForGame(gameId).executeAsList()
            if (allPlayers.isNotEmpty()) {
                queries.initializeGame(gameId, allPlayers.first().id)
            }

            // Distribute 2 random generals to each player
            val allGenerals = queries.selectAllGenerals().executeAsList().shuffled()
            var generalIndex = 0
            insertedPlayers.forEach { playerId ->
                for (i in 0 until 2) {
                    if (generalIndex < allGenerals.size) {
                        queries.insertPlayerHand(gameId, playerId, allGenerals[generalIndex].id)
                        generalIndex++
                    }
                }
            }
        }
        activeGameId.value = newGameId
    }

    fun loadGame(gameId: Long) {
        activeGameId.value = gameId
    }

    fun returnToWelcome() {
        activeGameId.value = null
    }

    fun nextGameName(): String {
        val pattern = Regex("""^Game #(\d+)$""")
        val maxNumber = queries.selectAllGames()
            .executeAsList()
            .mapNotNull { pattern.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 0
        return "Game #${maxNumber + 1}"
    }

    fun deleteGame(gameId: Long) {
        queries.transaction {
            queries.deletePlayerHandsForGame(gameId)
            queries.deleteGeneralPlacementsForGame(gameId)
            queries.deleteGameStateForGame(gameId)
            queries.deletePlayersForGame(gameId)
            queries.deleteGame(gameId)
        }
    }

    /**
     * Place a general on a hex for the current player.
     * Rules:
     * - Only allowed during round 1
     * - Each player can place at most 2 generals
     * - Target hex must be a village
     * - A village can have multiple generals but only from the same player
     *   (no village can have generals from different players)
     * - The player must have the general in their hand
     *
     * @return null on success, or an error message string on failure
     */
    fun placeGeneral(generalId: Long, hexCol: Int, hexRow: Int): String? {
        val gameId = activeGameId.value ?: return "No active game"
        val gameState = queries.getGameState(gameId).executeAsOneOrNull() ?: return "No game state"
        val currentPlayerId = gameState.currentPlayerId

        if (gameState.currentRound != 1L) {
            return "Generals can only be placed in round 1"
        }

        val placedCount = queries.countGeneralPlacementsByPlayer(gameId, currentPlayerId)
                .executeAsOne()
        if (placedCount >= 2L) {
            return "You have already placed 2 generals"
        }

        // Check if general is in hand
        val handRows = queries.selectPlayerHand(gameId, currentPlayerId).executeAsList()
        if (handRows.none { it.id == generalId }) {
            return "You do not have this general in your hand"
        }

        // Check hex is a village
        val hex = queries.selectHexagon(hexCol.toLong(), hexRow.toLong()).executeAsOneOrNull()
        if (hex == null || hex.type != "village") {
            return "Generals can only be placed in villages"
        }

        // Check no other player has generals in this village
        val playersAtHex = queries.selectGeneralPlacementsAtHex(gameId, hexCol.toLong(), hexRow.toLong())
                .executeAsList()
        if (playersAtHex.any { it != currentPlayerId }) {
            return "This village already has generals from another player"
        }

        queries.transaction {
            queries.insertGeneralPlacement(gameId, generalId, currentPlayerId, hexCol.toLong(), hexRow.toLong())
            queries.deleteFromPlayerHand(gameId, generalId)
        }
        return null
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
                queries.updateGameState(
                        currentRound,
                        currentPhase + 1,
                        players[startIndex].id,
                        gameId
                )
            }
        } else {
            val nextIdx = (currIdx + 1) % m
            queries.updateGameState(currentRound, currentPhase, players[nextIdx].id, gameId)
        }
    }
}
