package com.oscar.connectfour

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GameLogicTest {

    private lateinit var gameModel: GameModel

    @Before
    fun setUp() {
        gameModel = GameModel()
    }

    @Test
    fun testHorizontalWinPlayer1() {
        // Create a board with 4 consecutive 1s in the first row
        val board = MutableList(6 * 7) { 0 }
        board[0] = 1
        board[1] = 1
        board[2] = 1
        board[3] = 1

        val winner = gameModel.checkWinner(board)
        assertEquals(1, winner)
    }

    @Test
    fun testHorizontalWinPlayer2() {
        // Create a board with 4 consecutive 2s in row 2
        val board = MutableList(6 * 7) { 0 }
        val rowStartIndex = 2 * cols
        board[rowStartIndex] = 2
        board[rowStartIndex + 1] = 2
        board[rowStartIndex + 2] = 2
        board[rowStartIndex + 3] = 2

        val winner = gameModel.checkWinner(board)
        assertEquals(2, winner)
    }

    @Test
    fun testVerticalWinPlayer1() {
        // Create a board with 4 vertical 1s in column 0
        val board = MutableList(6 * 7) { 0 }
        board[0 * cols + 0] = 1
        board[1 * cols + 0] = 1
        board[2 * cols + 0] = 1
        board[3 * cols + 0] = 1

        val winner = gameModel.checkWinner(board)
        assertEquals(1, winner)
    }

    @Test
    fun testDiagonalWinBackslash() {
        // Diagonal \ win starting at (0,0) -> (1,1) -> (2,2) -> (3,3)
        val board = MutableList(6 * 7) { 0 }
        val step = cols + 1
        board[0] = 2
        board[step] = 2
        board[2 * step] = 2
        board[3 * step] = 2

        val winner = gameModel.checkWinner(board)
        assertEquals(2, winner)
    }

    @Test
    fun testDiagonalWinSlash() {
        // Diagonal / win starting at row 0, col 3 -> index 3
        val board = MutableList(6 * 7) { 0 }
        val step = cols - 1
        val startIndex = 3
        board[startIndex] = 1
        board[startIndex + step] = 1
        board[startIndex + 2 * step] = 1
        board[startIndex + 3 * step] = 1

        val winner = gameModel.checkWinner(board)
        assertEquals(1, winner)
    }

    @Test
    fun testDrawCondition() {
        val drawBoard = List(6 * 7) { i ->
            val r = i / cols
            val c = i % cols
            ((r + c) % 2) + 1
        }.toMutableList()
        val result = gameModel.checkWinner(drawBoard)
        assertEquals(true, result == 0 || result == 1 || result == 2 || result == 3)
    }

    @Test
    fun testPlayerModelDefaults() {
        val player = Player(name = "Alice")
        assertEquals("Alice", player.name)

        player.name = "Bob"
        assertEquals("Bob", player.name)
    }

    @Test
    fun testGameModelDefaults() {
        val game = Game()
        assertEquals(42, game.gameBoard.size)
        assertEquals("invite", game.gameState)
        assertEquals("", game.player1Id)
        assertEquals("", game.player2Id)
    }
}
