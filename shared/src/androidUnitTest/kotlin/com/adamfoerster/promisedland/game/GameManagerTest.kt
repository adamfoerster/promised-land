package com.adamfoerster.promisedland.game

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.adamfoerster.promisedland.GameDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameManagerTest {
    private lateinit var driver: SqlDriver

    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameDatabase.Schema.create(driver)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun testSetupGame() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val gameManager = GameManager(driver, backgroundScope, dispatcher, skipMapSync = true)
        
        gameManager.state.test {
            // Initial state (gameId is null)
            assertEquals(null, awaitItem().gameId)
            
            val players = listOf("Alice" to "#FF0000", "Bob" to "#00FF00")
            gameManager.setupGame("Test Game", players)
            
            // setupGame updates activeGameId, which triggers state flow via combine
            // We need to wait for all triggered jobs to finish
            advanceUntilIdle()
            
            val state = awaitItem()
            assertTrue(state.isGameActive, "Game should be active after setup. State: $state")
            assertEquals("Test Game", state.gameName)
            assertEquals(2, state.players.size)
            assertNotNull(state.currentPlayer)
        }
    }

    @Test
    fun testNextTurn() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val gameManager = GameManager(driver, backgroundScope, dispatcher, skipMapSync = true)
        
        gameManager.state.test {
            awaitItem() // Initial
            
            val players = listOf("Alice" to "#FF0000", "Bob" to "#00FF00")
            gameManager.setupGame("Test Game", players)
            advanceUntilIdle()
            
            val stateAfterSetup = awaitItem()
            val firstPlayerId = stateAfterSetup.currentPlayer?.id
            assertNotNull(firstPlayerId)
            
            gameManager.nextTurn()
            advanceUntilIdle()
            
            val stateAfterTurn = awaitItem()
            val secondPlayerId = stateAfterTurn.currentPlayer?.id
            assertNotNull(secondPlayerId)
            assertTrue(firstPlayerId != secondPlayerId, "Current player should have changed")
        }
    }

    @Test
    fun testDeleteGame() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val gameManager = GameManager(driver, backgroundScope, dispatcher, skipMapSync = true)
        
        gameManager.savedGames.test {
            assertEquals(emptyList(), awaitItem()) // Initial
            
            gameManager.setupGame("To Delete", listOf("Alice" to "#FF0000"))
            advanceUntilIdle()
            
            val gamesWithOne = awaitItem()
            val gameId = gamesWithOne.firstOrNull()?.id
            assertNotNull(gameId)
            
            gameManager.deleteGame(gameId)
            advanceUntilIdle()
            
            assertEquals(emptyList<SavedGameSummary>(), awaitItem())
        }
    }

    @Test
    fun testLoadGame() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val gameManager = GameManager(driver, backgroundScope, dispatcher, skipMapSync = true)
        
        gameManager.state.test {
            awaitItem() // Initial
            
            gameManager.setupGame("Game 1", listOf("Alice" to "#FF0000"))
            advanceUntilIdle()
            
            val stateAfterSetup = awaitItem()
            val gameId = stateAfterSetup.gameId
            assertNotNull(gameId)
            
            gameManager.returnToWelcome()
            advanceUntilIdle()
            assertNull(awaitItem().gameId)
            
            gameManager.loadGame(gameId)
            advanceUntilIdle()
            assertEquals(gameId, awaitItem().gameId)
        }
    }

    @Test
    fun testNextGameName() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val gameManager = GameManager(driver, backgroundScope, dispatcher, skipMapSync = true)
        
        assertEquals("Game #1", gameManager.nextGameName())
        
        gameManager.setupGame("Game #1", listOf("Alice" to "#FF0000"))
        advanceUntilIdle()
        
        assertEquals("Game #2", gameManager.nextGameName())
    }

    @Test
    fun testRoundAndPhaseProgression() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val gameManager = GameManager(driver, backgroundScope, dispatcher, skipMapSync = true)
        
        gameManager.state.test {
            awaitItem() // Initial
            
            val players = listOf("P1" to "C1", "P2" to "C2") 
            gameManager.setupGame("Progression Test", players)
            advanceUntilIdle()
            
            val state1 = awaitItem()
            assertEquals(1, state1.currentRound)
            assertEquals(1, state1.currentPhase)
            
            // Player 1 turn -> Player 2 turn (Phase 1)
            gameManager.nextTurn() 
            advanceUntilIdle()
            assertEquals(1, awaitItem().currentPhase)
            
            // Player 2 turn -> Player 1 turn (Phase 2)
            gameManager.nextTurn() 
            advanceUntilIdle()
            assertEquals(2, awaitItem().currentPhase)
        }
    }
}
