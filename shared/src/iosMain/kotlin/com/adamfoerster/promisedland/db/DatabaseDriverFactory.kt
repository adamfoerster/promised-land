package com.adamfoerster.promisedland.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.adamfoerster.promisedland.GameDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(GameDatabase.Schema, "game.db")
    }
}
