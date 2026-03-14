import androidx.compose.ui.window.ComposeUIViewController
import com.adamfoerster.promisedland.db.DatabaseDriverFactory

fun MainViewController() = ComposeUIViewController { App(DatabaseDriverFactory()) }