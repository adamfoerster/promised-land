package com.adamfoerster.promisedland.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.db.SqlDriver
import com.adamfoerster.promisedland.game.GameManager
import com.adamfoerster.promisedland.game.GameUIState
import com.adamfoerster.promisedland.game.SavedGameSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

sealed class Screen {
    object Welcome : Screen()
    object Setup : Screen()
    object ContinueGame : Screen()
    object Game : Screen()
}

class GameViewModel(
    driver: SqlDriver,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val gameManager = GameManager(driver, viewModelScope, dispatcher)

    val state: StateFlow<GameUIState> = gameManager.state
    val savedGames: StateFlow<List<SavedGameSummary>> = gameManager.savedGames

    var currentScreen by mutableStateOf<Screen>(Screen.Welcome)
        private set

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun nextGameName(): String = gameManager.nextGameName()

    fun setupGame(gameName: String, players: List<Pair<String, String>>) {
        gameManager.setupGame(gameName, players)
        navigateTo(Screen.Game)
    }

    fun loadGame(gameId: Long) {
        gameManager.loadGame(gameId)
        navigateTo(Screen.Game)
    }

    fun deleteGame(gameId: Long) {
        gameManager.deleteGame(gameId)
    }

    fun nextTurn() {
        gameManager.nextTurn()
    }

    fun placeGeneral(generalId: Long, hexCol: Int, hexRow: Int): String? {
        return gameManager.placeGeneral(generalId, hexCol, hexRow)
    }

    fun selectActiveGeneral(placementId: Long?) {
        gameManager.selectActiveGeneral(placementId)
    }

    fun moveGeneral(placementId: Long, hexCol: Int, hexRow: Int): String? {
        return gameManager.moveGeneral(placementId, hexCol, hexRow)
    }

    fun returnToWelcome() {
        gameManager.returnToWelcome()
        navigateTo(Screen.Welcome)
    }
}

