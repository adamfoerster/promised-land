import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adamfoerster.promisedland.db.DatabaseDriverFactory
import com.adamfoerster.promisedland.ui.ContinueGameScreen
import com.adamfoerster.promisedland.ui.GameScreen
import com.adamfoerster.promisedland.ui.SetupScreen
import com.adamfoerster.promisedland.ui.WelcomeScreen
import com.adamfoerster.promisedland.ui.viewmodel.GameViewModel
import com.adamfoerster.promisedland.ui.viewmodel.Screen

@Composable
fun App(driverFactory: DatabaseDriverFactory) {
    val viewModel: GameViewModel = viewModel { GameViewModel(driverFactory) }
    
    MaterialTheme {
        val state by viewModel.state.collectAsState()
        val savedGames by viewModel.savedGames.collectAsState()
        val screen = viewModel.currentScreen

        when (screen) {
            is Screen.Welcome -> WelcomeScreen(
                hasSavedGames = savedGames.isNotEmpty(),
                onNewGame = { viewModel.navigateTo(Screen.Setup) },
                onContinueGame = { viewModel.navigateTo(Screen.ContinueGame) }
            )

            is Screen.Setup -> SetupScreen(
                defaultGameName = viewModel.nextGameName(),
                onStartGame = { gameName, players ->
                    viewModel.setupGame(gameName, players)
                }
            )

            is Screen.ContinueGame -> ContinueGameScreen(
                savedGames = savedGames,
                onSelectGame = { gameId ->
                    viewModel.loadGame(gameId)
                },
                onDeleteGame = { gameId ->
                    viewModel.deleteGame(gameId)
                },
                onBack = { viewModel.navigateTo(Screen.Welcome) }
            )

            is Screen.Game -> GameScreen(
                state = state,
                onNextTurn = { viewModel.nextTurn() },
                onReturnToWelcome = {
                    viewModel.returnToWelcome()
                }
            )
        }
    }
}
