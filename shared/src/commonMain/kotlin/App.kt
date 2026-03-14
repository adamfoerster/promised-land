import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.adamfoerster.promisedland.db.DatabaseDriverFactory
import com.adamfoerster.promisedland.game.GameManager
import com.adamfoerster.promisedland.ui.GameScreen
import com.adamfoerster.promisedland.ui.SetupScreen

@Composable
fun App(driverFactory: DatabaseDriverFactory) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val gameManager = remember { GameManager(driverFactory, scope) }
        val state by gameManager.state.collectAsState()

        if (!state.isSetupFinished) {
            SetupScreen(onStartGame = { players ->
                gameManager.setupGame(players)
            })
        } else {
            GameScreen(
                state = state,
                onNextTurn = { gameManager.nextTurn() }
            )
        }
    }
}