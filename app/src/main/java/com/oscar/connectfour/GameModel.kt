package com.oscar.connectfour


import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow


data class Player(
    var name: String = ""
)

data class Game(
    var gameBoard: List<Int> = List(6 * 7) { 0 }, // 0: empty, 1: player1's move, 2: player2's move
    var gameState: String = "invite", //  "invite", "player1_turn", "player2_turn" "player1_won", "player2_won", "draw"
    var player1Id: String = "",
    var player2Id: String = ""
)

const val rows = 6
const val cols = 7

class GameModel: ViewModel() {
    val db = Firebase.firestore
    var localPlayerId = mutableStateOf<String?>(null)
    val playerMap = MutableStateFlow<Map<String, Player>>(emptyMap())
    val gameMap = MutableStateFlow<Map<String, Game>>(emptyMap())

    fun initGame() {

        // listen players
        db.collection("players")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value != null) {
                    val updatedMap = value.documents.associate { doc ->
                        doc.id to doc.toObject(Player::class.java)!!
                    }
                    playerMap.value = updatedMap
                }
            }

        // listen games
        db.collection("games")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value != null) {
                    val updatedMap = value.documents.associate { doc ->
                        doc.id to doc.toObject(Game::class.java)!!
                    }
                    gameMap.value = updatedMap
                }
            }
    }

    fun checkWinner(board: List<Int>): Int {
        // horizontal
        for (row in 0 until rows) {
            for (col in 0 until cols - 3) {
                val index = row * cols + col
                if (board[index] != 0 &&
                    board[index] == board[index + 1] &&
                    board[index] == board[index + 2] &&
                    board[index] == board[index + 3]) {
                    return board[index]
                }
            }
        }

        // vertical
        for (row in 0 until rows - 3) {
            for (col in 0 until cols) {
                val index = row * cols + col
                if (board[index] != 0 &&
                    board[index] == board[index + cols] &&
                    board[index] == board[index + 2 * cols] &&
                    board[index] == board[index + 3 * cols]) {
                    return board[index]
                }
            }
        }

        // diagonal (topleft to bottomright)
        for (row in 0 until rows - 3) {
            for (col in 0 until cols - 3) {
                val index = row * cols + col
                if (board[index] != 0 &&
                    board[index] == board[index + cols + 1] &&
                    board[index] == board[index + 2 * (cols + 1)] &&
                    board[index] == board[index + 3 * (cols + 1)]) {
                    return board[index]
                }
            }
        }

        // diagonal (topright to bottomleft)
        for (row in 0 until rows - 3) {
            for (col in 3 until cols) {
                val index = row * cols + col
                if (board[index] != 0 &&
                    board[index] == board[index + cols - 1] &&
                    board[index] == board[index + 2 * (cols - 1)] &&
                    board[index] == board[index + 3 * (cols - 1)]) {
                    return board[index]
                }
            }
        }

        // draw
        if (!board.contains(0)) {
            return 3
        }


        return 0
    }
    fun checkGameState(gameId: String?, column: Int) {
        if (gameId != null) {
            val game: Game? = gameMap.value[gameId]
            if (game != null) {
                val myTurn = game.gameState == "player1_turn" && game.player1Id == localPlayerId.value ||
                        game.gameState == "player2_turn" && game.player2Id == localPlayerId.value
                if (!myTurn) return

                val list: MutableList<Int> = game.gameBoard.toMutableList()

                // lowest empty row in column
                var row = rows - 1
                while (row >= 0 && list[row * cols + column] != 0) {
                    row--
                }

                if (row >= 0) { // space in the column
                    if (game.gameState == "player1_turn") {
                        list[row * cols + column] = 1
                    } else if (game.gameState == "player2_turn") {
                        list[row * cols + column] = 2
                    }

                    var turn = ""
                    if (game.gameState == "player1_turn") {
                        turn = "player2_turn"
                    } else {
                        turn = "player1_turn"
                    }

                    val winner = checkWinner(list.toList())
                    if (winner == 1) {
                        turn = "player1_won"
                    } else if (winner == 2) {
                        turn = "player2_won"
                    } else if (winner == 3) {
                        turn = "draw"
                    }

                    db.collection("games").document(gameId)
                        .update(
                            "gameBoard", list,
                            "gameState", turn
                        )
                }
            }
        }
    }


}