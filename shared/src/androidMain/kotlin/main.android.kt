import android.content.Context
import androidx.compose.runtime.Composable
import com.adamfoerster.promisedland.db.DatabaseDriverFactory

@Composable fun MainView(context: Context) = App(DatabaseDriverFactory(context))
