import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.adamfoerster.promisedland.db.DatabaseDriverFactory
import com.adamfoerster.promisedland.game.GameManager
import com.adamfoerster.promisedland.ui.ContinueGameScreen
import com.adamfoerster.promisedland.ui.GameScreen
import com.adamfoerster.promisedland.ui.SetupScreen
import com.adamfoerster.promisedland.ui.WelcomeScreen

sealed class Screen {
    object Welcome : Screen()
    object Setup : Screen()
    object ContinueGame : Screen()
    object Game : Screen()
}

@Composable
fun App(driverFactory: DatabaseDriverFactory) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val gameManager = remember { GameManager(driverFactory, scope) }
        val state by gameManager.state.collectAsState()
        val savedGames by gameManager.savedGames.collectAsState()

        var screen by remember { mutableStateOf<Screen>(Screen.Welcome) }

        when (screen) {
            is Screen.Welcome -> WelcomeScreen(
                hasSavedGames = savedGames.isNotEmpty(),
                onNewGame = { screen = Screen.Setup },
                onContinueGame = { screen = Screen.ContinueGame }
            )

            is Screen.Setup -> SetupScreen(
                onStartGame = { gameName, players ->
                    gameManager.setupGame(gameName, players)
                    screen = Screen.Game
                }
            )

            is Screen.ContinueGame -> ContinueGameScreen(
                savedGames = savedGames,
                onSelectGame = { gameId ->
                    gameManager.loadGame(gameId)
                    screen = Screen.Game
                },
                onDeleteGame = { gameId ->
                    gameManager.deleteGame(gameId)
                },
                onBack = { screen = Screen.Welcome }
            )

            is Screen.Game -> GameScreen(
                state = state,
                onNextTurn = { gameManager.nextTurn() },
                onReturnToWelcome = {
                    gameManager.returnToWelcome()
                    screen = Screen.Welcome
                }
            )
        }
    }
}