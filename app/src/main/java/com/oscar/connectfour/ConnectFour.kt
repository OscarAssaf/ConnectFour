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
    val navController = rememberNavController()
    val model = GameModel().apply { initGame() }

    NavHost(navController = navController, startDestination = "player") {
        composable("player") { NewPlayerScreen(navController, model) }
        composable("lobby")  { LobbyScreen(navController, model) }
        composable("game/{gameId}") { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId")
            GameScreen(navController, model, gameId)
        }
    }
}

@Composable
fun NewPlayerScreen(navController: NavController, model: GameModel) {
    val sharedPreferences = LocalContext.current
        .getSharedPreferences("ConnectFourPrefs", Context.MODE_PRIVATE)

    // player logic
    LaunchedEffect(Unit) {
        model.localPlayerId.value = sharedPreferences.getString("playerId", null)
        if (model.localPlayerId.value != null) {
            navController.navigate("lobby")
        }
    }

    if (model.localPlayerId.value == null) {
        var playerName by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                    if (playerName.isNotBlank()) {
                        // Creates new player
                        val newPlayer = Player(name = playerName)
                        model.db.collection("players")
                            .add(newPlayer)
                            .addOnSuccessListener { docRef ->
                                val newPlayerId = docRef.id
                                sharedPreferences.edit()
                                    .putString("playerId", newPlayerId)
                                    .apply()
                                model.localPlayerId.value = newPlayerId
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
@Composable
fun LobbyScreen(navController: NavController, model: GameModel) {
    val players by model.playerMap.asStateFlow().collectAsStateWithLifecycle()
    val games   by model.gameMap.asStateFlow().collectAsStateWithLifecycle()

    LaunchedEffect(games) {
        games.forEach { (gameId, game) ->
            if ((game.player1Id == model.localPlayerId.value ||
                        game.player2Id == model.localPlayerId.value) &&
                (game.gameState == "player1_turn" ||
                        game.gameState == "player2_turn")) {
                navController.navigate("game/$gameId")
            }
        }
    }

    val playerName = players[model.localPlayerId.value]?.name ?: "Unknown"

    Scaffold(
        topBar = { TopAppBar(title = { Text("Connect Four - $playerName") }) }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(players.entries.toList()) { (id, player) ->
                if (id != model.localPlayerId.value) {
                    ListItem(
                        headlineContent  = { Text("Player: ${player.name}") },
                        supportingContent= { Text("Status: …") },
                        trailingContent  = {
                            var hasGame = false
                            games.forEach { (gameId, game) ->
                                when {
                                    game.player1Id == model.localPlayerId.value &&
                                            game.gameState == "invite" -> {
                                        Text("Waiting for accept…")
                                        hasGame = true
                                    }
                                    game.player2Id == model.localPlayerId.value &&
                                            game.gameState == "invite" -> {
                                        Button(onClick = {
                                            model.db.collection("games")
                                                .document(gameId)
                                                .update("gameState","player1_turn")
                                                .addOnSuccessListener {
                                                    navController.navigate("game/$gameId")
                                                }
                                                .addOnFailureListener {
                                                    Log.e("ConnectFourError",
                                                        "Error updating game: $gameId")
                                                }
                                        }) { Text("Accept invite") }
                                        hasGame = true
                                    }
                                }
                            }
                            if (!hasGame) {
                                Button(onClick = {
                                    model.db.collection("games")
                                        .add(Game(gameState = "invite",
                                            player1Id = model.localPlayerId.value!!,
                                            player2Id = id))
                                }) {
                                    Text("Challenge")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(navController: NavController, model: GameModel, gameId: String?) {
    val players by model.playerMap.asStateFlow().collectAsStateWithLifecycle()
    val games   by model.gameMap.asStateFlow().collectAsStateWithLifecycle()
    val playerName = players[model.localPlayerId.value]?.name ?: "Unknown"

    if (gameId == null || !games.containsKey(gameId)) {
        Log.e("ConnectFourError", "Game not found: $gameId")
        navController.navigate("lobby")
        return
    }

    val game = games[gameId]!!
    if (game.gameState.endsWith("_won") || game.gameState == "draw") {
        AlertDialog(
            onDismissRequest = {},
            title   = { Text("Game Over") },
            text    = {
                Text(
                    if (game.gameState == "draw") "Draw!"
                    else "${game.gameState.removeSuffix("_won")} wins!"
                )
            },
            confirmButton = {
                Button(onClick = { navController.navigate("lobby") }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Connect Four - $playerName") }) }
    ) { innerPadding ->
        Column(
            modifier            = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drop Row
            Row {
                for (col in 0 until cols) {
                    val myTurn = (game.gameState == "player1_turn" &&
                            game.player1Id == model.localPlayerId.value) ||
                            (game.gameState == "player2_turn" &&
                                    game.player2Id == model.localPlayerId.value)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(enabled = myTurn) {
                                model.checkGameState(gameId, col)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (myTurn) {
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
                        val idx = row * cols + col
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, Color.Black)
                                .background(Color.Blue.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            when (game.gameBoard[idx]) {
                                1 -> Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                                2 -> Box(
                                    Modifier
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
