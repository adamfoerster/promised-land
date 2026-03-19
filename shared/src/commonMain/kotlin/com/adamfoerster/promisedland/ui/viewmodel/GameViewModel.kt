package com.adamfoerster.promisedland.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.promisedland.db.DatabaseDriverFactory
import com.adamfoerster.promisedland.game.GameManager
import com.adamfoerster.promisedland.game.GameUIState
import com.adamfoerster.promisedland.game.SavedGameSummary
import kotlinx.coroutines.flow.StateFlow

sealed class Screen {
    object Welcome : Screen()
    object Setup : Screen()
    object ContinueGame : Screen()
    object Game : Screen()
}

class GameViewModel(driverFactory: DatabaseDriverFactory) : ViewModel() {
    private val gameManager = GameManager(driverFactory, viewModelScope)

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

    fun returnToWelcome() {
        gameManager.returnToWelcome()
        navigateTo(Screen.Welcome)
    }
}
