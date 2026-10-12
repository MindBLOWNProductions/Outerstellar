package com.mindblownproductions.union

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class GameType { NONE, TIC_TAC_TOE, SNAKE, MINESWEEPER }

@Composable
fun GamesScreen() {
    var selectedGame by remember { mutableStateOf(GameType.NONE) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (selectedGame) {
            GameType.NONE -> GameDashboard(onGameSelected = { selectedGame = it })
            GameType.TIC_TAC_TOE -> TicTacToeScreen(onBack = { selectedGame = GameType.NONE })
            GameType.SNAKE -> SnakeScreen(onBack = { selectedGame = GameType.NONE })
            GameType.MINESWEEPER -> MinesweeperScreen(onBack = { selectedGame = GameType.NONE })
        }
    }
}

@Composable
fun GameDashboard(onGameSelected: (GameType) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Arcade Dashboard",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GameCard("Tic Tac Toe", Modifier.weight(1f)) { onGameSelected(GameType.TIC_TAC_TOE) }
            GameCard("Snake", Modifier.weight(1f)) { onGameSelected(GameType.SNAKE) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GameCard("Minesweeper", Modifier.weight(1f)) { onGameSelected(GameType.MINESWEEPER) }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun GameCard(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ==========================================
// 1. TIC TAC TOE
// ==========================================
@Composable
fun TicTacToeScreen(onBack: () -> Unit) {
    var board by remember { mutableStateOf(List(9) { "" }) }
    var xTurn by remember { mutableStateOf(true) }
    var winner by remember { mutableStateOf<String?>(null) }

    fun checkWinner(b: List<String>): String? {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (line in lines) {
            if (b[line[0]].isNotEmpty() && b[line[0]] == b[line[1]] && b[line[0]] == b[line[2]]) {
                return b[line[0]]
            }
        }
        if (b.none { it.isEmpty() }) return "Draw"
        return null
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("Tic Tac Toe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = when {
            winner == "Draw" -> "It's a Draw!"
            winner != null -> "Winner: $winner"
            xTurn -> "Turn: X"
            else -> "Turn: O"
        }, style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.size(300.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(9) { index ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .clickable(enabled = board[index].isEmpty() && winner == null) {
                            val newBoard = board.toMutableList()
                            newBoard[index] = if (xTurn) "X" else "O"
                            board = newBoard
                            winner = checkWinner(board)
                            xTurn = !xTurn
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = board[index], fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            board = List(9) { "" }
            xTurn = true
            winner = null
        }) {
            Text("Restart Game")
        }
    }
}

// ==========================================
// 2. SNAKE (Dies on Wall Collision)
// ==========================================
data class Point(val x: Int, val y: Int)

@Composable
fun SnakeScreen(onBack: () -> Unit) {
    val gridSize = 15
    var snake by remember { mutableStateOf(listOf(Point(7, 7), Point(7, 8), Point(7, 9))) }
    var direction = remember { mutableStateOf(Point(0, -1)) }
    var food by remember { mutableStateOf(Point(3, 3)) }
    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }

    fun spawnFood() {
        var newFood: Point
        do {
            newFood = Point(Random.nextInt(gridSize), Random.nextInt(gridSize))
        } while (snake.contains(newFood))
        food = newFood
    }

    LaunchedEffect(isGameOver) {
        if (!isGameOver) spawnFood()
        while (!isGameOver) {
            delay(150L)
            val head = snake.first()
            val newX = head.x + direction.value.x
            val newY = head.y + direction.value.y

            // Check wall collision (Dies if out of bounds)
            if (newX < 0 || newX >= gridSize || newY < 0 || newY >= gridSize) {
                isGameOver = true
                break
            }

            val newHead = Point(newX, newY)

            // Check self collision
            if (snake.dropLast(1).contains(newHead)) {
                isGameOver = true
                break
            }

            val newSnake = mutableListOf(newHead)
            if (newHead == food) {
                score += 10
                newSnake.addAll(snake)
                spawnFood()
            } else {
                newSnake.addAll(snake.dropLast(1))
            }
            snake = newSnake
        }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("Snake • Score: $score", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(320.dp)
                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (y in 0 until gridSize) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (x in 0 until gridSize) {
                            val pt = Point(x, y)
                            val color = when {
                                snake.first() == pt -> Color.Green
                                snake.contains(pt) -> Color.LightGray
                                food == pt -> Color.Red
                                else -> Color.Black
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(1.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (isGameOver) {
            Text("Game Over!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                snake = listOf(Point(7, 7), Point(7, 8), Point(7, 9))
                direction.value = Point(0, -1)
                score = 0
                isGameOver = false
            }) { Text("Play Again") }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { if (direction.value.y == 0) direction.value = Point(0, -1) }) { Text("▲") }
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Button(onClick = { if (direction.value.x == 0) direction.value = Point(-1, 0) }) { Text("◄") }
                    Button(onClick = { if (direction.value.x == 0) direction.value = Point(1, 0) }) { Text("►") }
                }
                Button(onClick = { if (direction.value.y == 0) direction.value = Point(0, 1) }) { Text("▼") }
            }
        }
    }
}

// ==========================================
// 3. MINESWEEPER
// ==========================================
data class Cell(val isMine: Boolean, var isRevealed: Boolean, var adjacentMines: Int = 0)

@Composable
fun MinesweeperScreen(onBack: () -> Unit) {
    val size = 8
    val mineCount = 8
    var grid by remember {
        mutableStateOf(
            mutableListOf<MutableList<Cell>>().apply {
                for (i in 0 until size) {
                    add(mutableListOf<Cell>().apply {
                        for (j in 0 until size) add(Cell(false, false))
                    })
                }
            }
        )
    }
    var gameState by remember { mutableStateOf("Playing") }

    fun initGame() {
        val newGrid = mutableListOf<MutableList<Cell>>().apply {
            for (i in 0 until size) {
                add(mutableListOf<Cell>().apply {
                    for (j in 0 until size) add(Cell(false, false))
                })
            }
        }
        var placed = 0
        while (placed < mineCount) {
            val rx = Random.nextInt(size)
            val ry = Random.nextInt(size)
            if (!newGrid[rx][ry].isMine) {
                newGrid[rx][ry] = newGrid[rx][ry].copy(isMine = true)
                placed++
            }
        }
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (!newGrid[x][y].isMine) {
                    var count = 0
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in 0 until size && ny in 0 until size && newGrid[nx][ny].isMine) {
                                count++
                            }
                        }
                    }
                    newGrid[x][y].adjacentMines = count
                }
            }
        }
        grid = newGrid
        gameState = "Playing"
    }

    LaunchedEffect(Unit) { initGame() }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("Minesweeper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = when (gameState) {
            "Won" -> "You Won! 🎉"
            "Lost" -> "Boom! Game Over 💥"
            else -> "Find the safe tiles!"
        }, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(size),
            modifier = Modifier.size(320.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(size * size) { index ->
                val x = index / size
                val y = index % size
                val cell = grid[x][y]

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(
                            if (cell.isRevealed) {
                                if (cell.isMine) Color.Red else MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .clickable(enabled = gameState == "Playing" && !cell.isRevealed) {
                            if (cell.isMine) {
                                val revealedGrid = grid.map { row ->
                                    row.map { if (it.isMine) it.copy(isRevealed = true) else it }.toMutableList()
                                }.toMutableList()
                                grid = revealedGrid
                                gameState = "Lost"
                            } else {
                                val newGrid = grid.map { row -> row.map { it.copy() }.toMutableList() }.toMutableList()
                                newGrid[x][y].isRevealed = true
                                grid = newGrid

                                val unrevealedSafe = grid.flatten().count { !it.isMine && !it.isRevealed }
                                if (unrevealedSafe == 0) gameState = "Won"
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (cell.isRevealed) {
                        if (cell.isMine) {
                            Text("💣", fontSize = 14.sp)
                        } else if (cell.adjacentMines > 0) {
                            Text("${cell.adjacentMines}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { initGame() }) { Text("New Game") }
    }
}