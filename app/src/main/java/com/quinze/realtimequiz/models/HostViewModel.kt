package com.quinze.realtimequiz.models

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.quinze.realtimequiz.GameMove
import com.quinze.realtimequiz.GameState
import com.quinze.realtimequiz.MainActivity


class HostViewModel(connectionsClient: ConnectionsClient) : ViewModel(){


    val mConnectionsClient = connectionsClient

    var connected by mutableStateOf(false)
        private set
    var advertising by mutableStateOf(false)
        private set
    var answering by mutableStateOf(false)
    var answered by mutableStateOf(false)

    var connectionAlert by mutableStateOf(false)
    var connectionAlertInfo = mutableStateListOf<Pair<String, String>>()



    var problem by mutableStateOf("")
    var answers = mutableStateListOf(Pair("",false),Pair("",false),Pair("",false),Pair("",false))
    var winningAnswers  = listOf<String>()
    var winningProblem = ""

    var nbCorrectAnswers = 0


    var mcq by mutableStateOf(false)
    var winner by mutableStateOf("")

    var hostName by mutableStateOf("")


    val players = mutableStateMapOf<String, String>()



    class HostViewModelFactory(private val connectionsClient: ConnectionsClient) :
        ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = HostViewModel(connectionsClient) as T
    }

    override fun onCleared() {
        super.onCleared()
        advertising = false
        mConnectionsClient.stopAdvertising()
        mConnectionsClient.stopAllEndpoints()
    }

    private fun validateAnswerChoices():Int{
        val indexes = mutableListOf<Int>()

        for (i in 0 until answers.size) {
            if (answers[i].first.trim().isEmpty()) {
                indexes.add(i)
            }
        }

        indexes.reversed().map{answers.removeAt(it)}

        return answers.size
    }

    fun calculateNbCorrectAnswers() : Boolean {
        if(validateAnswerChoices() >= 2) {
            nbCorrectAnswers = 0
            for (i in 0 until answers.size) {
                if (answers[i].second) {
                    nbCorrectAnswers++
                }
            }
            val valid = if(mcq)
                nbCorrectAnswers>1
            else
                nbCorrectAnswers==1
            if(!valid){
                repopulateAnswerChoices()
            }
            return valid
        } else{
            repopulateAnswerChoices()
            return false
        }

    }

    fun repopulateAnswerChoices(){
        if(answers.size<4) {
            for (i in answers.size until 4) {
                answers += Pair("", false)
            }
        }
    }

    fun startAdvertising() {
        val advertisingOptions: AdvertisingOptions =
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        //var result = false
        Log.d("Nearby Advertising: ", "Advertising...")
        advertising = true
        mConnectionsClient
            .startAdvertising(
                "Host", MainActivity.SERVICE_ID, connectionLifecycleCallback, advertisingOptions
            )
            .addOnSuccessListener { unused: Void? ->
                connected = true
                Log.d("Nearby Advertising: ", "Success!")
            }
            .addOnFailureListener { e: Exception? ->
                Log.d("Nearby Advertising: ", e.toString())
                advertising=false
            }
        Log.d("Nearby Advertising Result: ", connected.toString())

    }

    val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {

            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                connectionAlertInfo.add(Pair(endpointId,connectionInfo.authenticationDigits))
                //connectionAlertCode = connectionInfo.authenticationDigits
                connectionAlert = true
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        Log.d("Nearby Connection: ", "OK!")
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        Log.d("Nearby Connection: ", "Rejected")
                    }
                    ConnectionsStatusCodes.STATUS_ERROR -> {
                        Log.d("Nearby Connection: ", "Error")
                    }
                    else -> {
                        Log.d("Nearby Connection:  ", "Something happened")
                    }
                }
            }

            override fun onDisconnected(endpointId: String) {
                // We've been disconnected from this endpoint. No more data can be
                // sent or received.
                Log.d("Nearby Connection: ", "Lost: $endpointId")
                players.remove(endpointId)
            }
        }

    val payloadCallback : PayloadCallback =
        object : PayloadCallback(){
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                // This always gets the full data of the payload. Is null if it's not a BYTES payload.
                Log.d("Nearby Payload: ", "Payload received from $endpointId")

                if (payload.type == Payload.Type.BYTES) {
                    val receivedBytes = payload.asBytes()
                    val jsonMove = String(receivedBytes!!)
                    val move = Gson().fromJson(jsonMove, GameMove::class.java)

                    if(answering && players.containsKey(endpointId)){
                        evaluateMove(move, endpointId)
                    } else{
                        players[endpointId] = move.playerName
                    }

                    val game = if(answering){

                        GameState(
                            answering = answering,
                            answered = answered,
                            problem = problem,
                            answers = answers.unzip().first,
                            mcq = mcq,
                            winner = "",
                            hostName = hostName,
                            players = players
                        )} else{
                        GameState(
                            answering = answering,
                            answered = answered,
                            problem = winningProblem,
                            answers = winningAnswers,
                            mcq = mcq,
                            winner = players[winner]?:"",
                            hostName = hostName,
                            players = players
                        )
                    }

                    val jsonGame = Gson().toJson(game)
                    val bytesPayload = Payload.fromBytes(jsonGame.encodeToByteArray())

                    mConnectionsClient.sendPayload(players.keys.toList(), bytesPayload)
                    //players[endpointId] = String(receivedBytes!!)

                    //players.postValue(players.value)
                }
            }

            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
                // after the call to onPayloadReceived().
            }
        }

    fun evaluateMove(move: GameMove, playerID: String){
        if (!mcq) {
            if (answers[move.answer[0]].second) {
                //correct answer
                setWiningAnswer()
                answered=true
                answering = false
                winner = playerID
                repopulateAnswerChoices()
            } else {
                //messageResId = R.string.incorrect_toast
            }
        } else {
            var score = 0
            run loop@{
                move.answer.forEach {
                    if (answers[it].second) {
                        score++
                    } else {
                        score = -10
                        return@loop
                    }
                }
            }
            Log.d("Answer", move.answer.joinToString(","))
            Log.d("Score", score.toString())

            if (score == nbCorrectAnswers) {
                setWiningAnswer()
                answered=true
                answering = false
                winner = playerID
                repopulateAnswerChoices()
            } else {
                //messageResId = R.string.incorrect_toast
            }
        }
    }

    private fun setWiningAnswer(){
        winningAnswers = answers.mapNotNull { if (it.second) it.first else null }
        winningProblem = problem
    }
}