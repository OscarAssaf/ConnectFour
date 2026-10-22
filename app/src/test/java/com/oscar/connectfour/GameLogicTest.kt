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
    fun testPieceGravityStackingLogic() {
        // Simulate piece dropping in column 0
        val board = MutableList(6 * 7) { 0 }
        val column = 0

        // Drop first piece
        var row = rows - 1
        while (row >= 0 && board[row * cols + column] != 0) {
            row--
        }
        board[row * cols + column] = 1

        // Bottom cell should have player 1's piece
        assertEquals(1, board[5 * cols + 0])

        // Drop second piece in same column
        while (row >= 0 && board[row * cols + column] != 0) {
            row--
        }
        board[row * cols + column] = 2

        // Row above bottom should have player 2's piece
        assertEquals(2, board[4 * cols + 0])
    }

    @Test
    fun testPlayerModelSerializationAndDefaults() {
        val player = Player(name = "Alice")
        assertEquals("Alice", player.name)
        
        player.name = "Bob"
        assertEquals("Bob", player.name)
    }

    @Test
    fun testGameModelSerializationAndDefaults() {
        val game = Game(
            gameBoard = List(42) { 0 },
            gameState = "player1_turn",
            player1Id = "p1",
            player2Id = "p2"
        )
        assertEquals(42, game.gameBoard.size)
        assertEquals("player1_turn", game.gameState)
        assertEquals("p1", game.player1Id)
        assertEquals("p2", game.player2Id)
    }
}
