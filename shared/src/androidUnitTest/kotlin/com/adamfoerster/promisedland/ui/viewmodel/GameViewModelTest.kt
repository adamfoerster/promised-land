package com.adamfoerster.promisedland.ui.viewmodel

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.adamfoerster.promisedland.GameDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private lateinit var driver: SqlDriver
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameDatabase.Schema.create(driver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        driver.close()
    }

    private fun createViewModel(): GameViewModel {
        return GameViewModel(driver, testDispatcher)
    }

    @Test
    fun testInitialState() {
        val viewModel = createViewModel()
        assertEquals(Screen.Welcome, viewModel.currentScreen)
    }

    @Test
    fun testNavigation() {
        val viewModel = createViewModel()
        viewModel.navigateTo(Screen.Setup)
        assertEquals(Screen.Setup, viewModel.currentScreen)
    }

    @Test
    fun testSetupGameNavigatesToGame() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.state.test {
            awaitItem() // Initial
            
            viewModel.setupGame("Test", listOf("P1" to "Red"))
            
            assertEquals(Screen.Game, viewModel.currentScreen)
            val state = expectMostRecentItem()
            assertEquals("Test", state.gameName)
        }
    }

    @Test
    fun testReturnToWelcome() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.setupGame("Test", listOf("P1" to "Red"))
        
        viewModel.returnToWelcome()
        assertEquals(Screen.Welcome, viewModel.currentScreen)
    }
}
