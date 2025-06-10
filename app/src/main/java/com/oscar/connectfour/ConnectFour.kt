package com.oscar.connectfour

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.asStateFlow


import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.ArrowDropDown


// Foundation
import androidx.compose.foundation.background

// Material Design
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
@Composable
fun ConnectFourApp() {
    val navController = rememberNavController() //navigation screens
    val model = GameModel().apply { initGame() }
        //routes
    NavHost(navController = navController, startDestination = "player") {
        composable("player") { NewPlayerScreen(navController, model) } //new player
        composable("lobby")  { LobbyScreen(navController, model) } //lobby
        composable("game/{gameId}") { backStackEntry ->  //gamescreen but dynamic
            val gameId = backStackEntry.arguments?.getString("gameId")
            GameScreen(navController, model, gameId)
        }
    }
}
//For new players
@Composable
fun NewPlayerScreen(navController: NavController, model: GameModel) {
    val sharedPreferences = LocalContext.current //store user locally
        .getSharedPreferences("ConnectFourPrefs", Context.MODE_PRIVATE)

    // IS player already registered?
    LaunchedEffect(Unit) {
        model.localPlayerId.value = sharedPreferences.getString("playerId", null) //gets the reg player
        if (model.localPlayerId.value != null) { //if exists
            navController.navigate("lobby") //sends to lobby
        }
    }
        //player doesnt exist = register player
    if (model.localPlayerId.value == null) {
        var playerName by remember { mutableStateOf("") }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to Connect Four!")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                label = { Text("Enter your name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (playerName.isNotBlank()) { //nam not empty
                        // create new player
                        val newPlayer = Player(name = playerName)
                        model.db.collection("players") //saves user to firestore
                            .add(newPlayer)
                            .addOnSuccessListener { docRef ->
                                val newPlayerId = docRef.id
                                sharedPreferences.edit() //saves playerID for future
                                    .putString("playerId", newPlayerId)
                                    .apply()
                                model.localPlayerId.value = newPlayerId //puts ID in the model
                                navController.navigate("lobby")
                            }
                            .addOnFailureListener { error ->
                                Log.e("ConnectFourError", "Error creating player: ${error.message}")
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Player")
            }
        }
    } else {
        Text("Loading…")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable //lobby, challenge etv
fun LobbyScreen(navController: NavController, model: GameModel) {
    val players by model.playerMap.asStateFlow().collectAsStateWithLifecycle() //players + game from MAP
    val games   by model.gameMap.asStateFlow().collectAsStateWithLifecycle()

            //if in already a game, send user to it
    LaunchedEffect(games) {
        games.forEach { (gameId, game) ->
            if ((game.player1Id == model.localPlayerId.value || //user in game that is active?
                        game.player2Id == model.localPlayerId.value) &&
                (game.gameState == "player1_turn" ||
                        game.gameState == "player2_turn")) {
                navController.navigate("game/$gameId") //send that user to the game
            }
        }
    }

    val playerName = players[model.localPlayerId.value]?.name ?: "Unknown"

    fun inGame(playerId: String): Boolean { //helper func if in game
        return games.values.any { game ->
            (game.player1Id == playerId || game.player2Id == playerId) &&
                    (game.gameState == "player1_turn" || game.gameState == "player2_turn")
        }
    }

    // Status
    fun playerStatus(playerId: String): String {
        return if (inGame(playerId)) "In Game" else "Available"
    }
    //lobby screen layout
    Scaffold(
        topBar = { TopAppBar(title = { Text("Connect Four - $playerName") }) }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) { //list all players except urself
            items(players.entries.toList()) { (id, player) -> //map to list
                if (id != model.localPlayerId.value) {
                    val status = playerStatus(id)
                    val isInGame = inGame(id)

                    ListItem(
                        headlineContent  = { Text("Player: ${player.name}") },
                        supportingContent= { Text("Status: $status") },
                        trailingContent  = {
                            var hasGame = false
                            var gameIdPlayer: String? = null


                            games.forEach { (gameId, game) -> //Checks if u got challenged
                                when {
                                    game.player1Id == model.localPlayerId.value &&
                                            game.player2Id == id &&
                                            game.gameState == "invite" -> {
                                        Text("Waiting for accept…")
                                        hasGame = true
                                        gameIdPlayer = gameId
                                    }
                                    game.player2Id == model.localPlayerId.value &&
                                            game.player1Id == id &&
                                            game.gameState == "invite" -> {
                                        hasGame = true
                                        gameIdPlayer = gameId
                                    }
                                }
                            }
                                //buttons based on gamerequest
                            when { //we got an invite
                                hasGame && gameIdPlayer != null -> {
                                    val gameId = gameIdPlayer!!
                                    val game = games[gameId]!!

                                    if (game.player2Id == model.localPlayerId.value) {
                                        // accept/decline
                                        Row {
                                            Button(
                                                onClick = {
                                                    model.db.collection("games")
                                                        .document(gameId)
                                                        .update("gameState","player2_turn")
                                                        .addOnSuccessListener {
                                                            navController.navigate("game/$gameId")
                                                        }
                                                        .addOnFailureListener {
                                                            Log.e("ConnectFourError",
                                                                "Error accepting game: $gameId")
                                                        }
                                                },
                                                modifier = Modifier.padding(end = 4.dp)
                                            ) {
                                                Text("Accept")
                                            }
                                                //Decline
                                            Button(
                                                onClick = { //Delete the invite
                                                    model.db.collection("games")
                                                        .document(gameId)
                                                        .delete()
                                                        .addOnFailureListener {
                                                            Log.e("ConnectFourError",
                                                                "Error declining game: $gameId")
                                                        }
                                                }
                                            ) {
                                                Text("Decline")
                                            }
                                        }
                                    } else {
                                        Text("Waiting for accept…")
                                    }
                                }
                                isInGame -> { //player busy
                                    Button(
                                        onClick = {},
                                        enabled = false
                                    ) {
                                        Text("Challenge")
                                    }
                                }
                                !hasGame -> { //available
                                    Button(onClick = {
                                        model.db.collection("games") //
                                            .add(Game(gameState = "invite",
                                                player1Id = model.localPlayerId.value!!,
                                                player2Id = id))
                                    }) {
                                        Text("Challenge")
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}




//Gamescreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(navController: NavController, model: GameModel, gameId: String?) { //state + players
    val players by model.playerMap.asStateFlow().collectAsStateWithLifecycle()
    val games   by model.gameMap.asStateFlow().collectAsStateWithLifecycle()
    val playerName = players[model.localPlayerId.value]?.name ?: "Unknown"
//returns to lobby if game not exist
    if (gameId == null || !games.containsKey(gameId)) {
        Log.e("ConnectFourError", "Game not found: $gameId")
        navController.navigate("lobby")
        return
    }

    val game = games[gameId]!!

    // Current turn name
    val currentTurnPlayerName = when (game.gameState) {
        "player1_turn" -> players[game.player1Id]?.name ?: "Unknown"
        "player2_turn" -> players[game.player2Id]?.name ?: "Unknown"
        else -> ""
    }
    //game over
    if (game.gameState.endsWith("_won") || game.gameState == "draw") {
        val winnerName = when (game.gameState) {
            "player1_won" -> players[game.player1Id]?.name ?: "Unknown"
            "player2_won" -> players[game.player2Id]?.name ?: "Unknown"
            else -> ""
        }
       //shows who won
        AlertDialog(
            onDismissRequest = {},
            title   = { Text("Game Over") },
            text    = {
                Text(
                    if (game.gameState == "draw") "Draw!"
                    else "$winnerName wins!"
                )
            },
            confirmButton = {
                Button(onClick = { navController.navigate("lobby") }) {
                    Text("OK")
                }
            }
        )
    }

    //layout of the gamescreen
    Scaffold(
        topBar = { TopAppBar(title = { Text("Connect Four - $playerName") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // current turn indicator
            if (currentTurnPlayerName.isNotEmpty()) {
                Text(
                    text = "$currentTurnPlayerName's turn to play",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Drop Row
            Row {
                for (col in 0 until cols) {

                    val myTurn = (game.gameState == "player1_turn" &&
                            game.player1Id == model.localPlayerId.value) ||
                            (game.gameState == "player2_turn" &&
                                    game.player2Id == model.localPlayerId.value)
                    Box(
                        modifier = Modifier.size(48.dp).clickable(enabled = myTurn) {
                                model.checkGameState(gameId, col)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (myTurn) { //if its ur turn, show the arrows to you only
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop here")
                        }
                    }
                }
            }

            // Column numbers
            Row {
                for (col in 0 until cols) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = col.toString(),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Board
            for (row in 0 until rows) {
                Row {
                    for (col in 0 until cols) {
                        val idx = row * cols + col //array index
                        Box( //cell
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, Color.Black)
                                .background(Color.Blue.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            when (game.gameBoard[idx]) { //shows the boar based on its state
                                1 -> Box( //p1 piece
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                                2 -> Box(
                                    Modifier //p2 piece
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Yellow)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}