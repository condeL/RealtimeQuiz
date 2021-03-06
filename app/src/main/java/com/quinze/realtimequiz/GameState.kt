package com.quinze.realtimequiz

data class GameState(val answering:Boolean,
                     val answered :Boolean = false,
                     val problem:String = "",
                     val answers:List<String> = listOf(),
                     val mcq :Boolean,
                     val winner: String = "",
                     val hostName:String,
                     val players:Map<String, String> = mapOf())