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
    var player1Id: String = "", //fb ids
    var player2Id: String = ""
)

const val rows = 6
const val cols = 7

class GameModel: ViewModel() { //data stays after changing screens
    val db = Firebase.firestore //firestore
    var localPlayerId = mutableStateOf<String?>(null) //player ID
    val playerMap = MutableStateFlow<Map<String, Player>>(emptyMap()) //player list
    val gameMap = MutableStateFlow<Map<String, Game>>(emptyMap()) //all active games, Game IDs

    fun initGame() { //sync fb

        // listen players changes fb.
        db.collection("players")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value != null) { //document to map<string,player>
                    val updatedMap = value.documents.associate { doc ->
                        doc.id to doc.toObject(Player::class.java)!!
                    }
                    playerMap.value = updatedMap //updates when new player added
                }
            }

        // listen games changes fb
        db.collection("games")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value != null) {
                    val updatedMap = value.documents.associate { doc ->
                        doc.id to doc.toObject(Game::class.java)!!
                    }
                    gameMap.value = updatedMap //updates when new game created or modified.
                }
            }
    }

//rules: 0 = continue, 1 = player1 , 2 = player 2, 3 = draw
    private fun checkWinner(board: List<Int>): Int {
        // horizontal by looping row
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

        // vertical by lopping column
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

        // Diagonal \
        for (row in 0 until rows - 3) {
            for (col in 0 until cols - 3) {
                val index = row * cols + col
                if (board[index] != 0 && //moves down with cols + 1
                    board[index] == board[index + cols + 1] &&
                    board[index] == board[index + 2 * (cols + 1)] &&
                    board[index] == board[index + 3 * (cols + 1)]) {
                    return board[index]
                }
            }
        }

        // Diagonal 2 /
        for (row in 0 until rows - 3) {
            for (col in 3 until cols) {
                val index = row * cols + col
                if (board[index] != 0 && //moves with col -1
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

    //Places piece + update game status
    fun checkGameState(gameId: String?, column: Int) {
        if (gameId != null) {
            val game: Game? = gameMap.value[gameId]
            if (game != null) {
                //double check turn
                val myTurn = game.gameState == "player1_turn" && game.player1Id == localPlayerId.value || //player1 can move when its player1
                        game.gameState == "player2_turn" && game.player2Id == localPlayerId.value  //player 2 when its player 2's turn
                if (!myTurn) return //exit if not their player's turn

                val list: MutableList<Int> = game.gameBoard.toMutableList()

                // lowest empty row in column
                var row = rows - 1 //bottom
                while (row >= 0 && list[row * cols + column] != 0) {
                    row-- //move up if not empty
                }

                if (row >= 0) { // space empty?
                    //places piece
                    if (game.gameState == "player1_turn") {
                        list[row * cols + column] = 1
                    } else if (game.gameState == "player2_turn") {
                        list[row * cols + column] = 2
                    }
                    //logic for next players turn
                    var turn: String
                    turn = if (game.gameState == "player1_turn") {
                        "player2_turn"
                    } else {
                        "player1_turn"
                    }
                        //check winner after every move
                    val winner = checkWinner(list.toList())
                    when (winner) {
                        1 -> {
                            turn = "player1_won"
                        }
                        2 -> {
                            turn = "player2_won"
                        }
                        3 -> {
                            turn = "draw"
                        }
                    }
                    //updates database
                    db.collection("games").document(gameId)
                        .update(
                            "gameBoard", list, //new piece
                            "gameState", turn //game state
                        )
                }
            }
        }
    }


}