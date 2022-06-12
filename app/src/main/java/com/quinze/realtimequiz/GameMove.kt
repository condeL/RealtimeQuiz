package com.quinze.realtimequiz

data class GameMove(val playerName :String, val answer :List<Int> = listOf(-1)) {
}