package com.quinze.realtimequiz

import com.quinze.realtimequiz.models.Question

data class GameState(val answering:Boolean,
                     val problem:String = "",
                     val answers:List<String> = listOf(),
                     val mcq :Boolean,
                     val winner: String = "",
                     val hostName:String,
                     val players:Map<String, String> = mapOf())