package com.quinze.realtimequiz.models

import android.util.Log
import android.widget.CheckBox
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
    var answering by mutableStateOf(false)

    var problem by mutableStateOf("")
    //val question :Question? = null
    var answers = mutableStateListOf("","","","")
    var truths = mutableStateListOf(false,false,false,false)
    var nbCorrectAnswers = 0


    var mcq by mutableStateOf(false)
    var winner by mutableStateOf("")

    var hostName by mutableStateOf("Host")
        private set
    var hostID by mutableStateOf("")
        private set


    val players = mutableStateMapOf<String, String>()



    class HostViewModelFactory(private val connectionsClient: ConnectionsClient) :
        ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = HostViewModel(connectionsClient) as T
    }

    override fun onCleared() {
        super.onCleared()
        mConnectionsClient.stopAdvertising()
    }

    fun calculateNbCorrectAnswers() {
        nbCorrectAnswers = 0
        for (i in 0 until answers.size) {
            if (truths[i]) {
                nbCorrectAnswers++
            }
        }
    }

    fun startAdvertising() {
        val advertisingOptions: AdvertisingOptions =
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        //var result = false
        Log.d("Nearby Advertising: ", "Advertising...")

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
            }
        Log.d("Nearby Advertising Result: ", connected.toString())

    }

    private val endpointDiscoveryCallback: EndpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                // An endpoint was found. We request a connection to it.
                connectionsClient
                    .requestConnection("Host", endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener{ unused: Void? -> Log.d("Nearby Endpoint discovery: ", "Success!") }
                    .addOnFailureListener{ e: java.lang.Exception? -> Log.d("Nearby Endpoint discovery: ", e.toString()) }
            }

            override fun onEndpointLost(endpointId: String) {
                // A previously discovered endpoint has gone away.
                Log.d("Nearby Endpoint discovery: ", "Lost $endpointId")
            }
        }

    val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                // Automatically accept the connection on both sides.
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        Log.d("Nearby Connection: ", "OK!")
                        players[endpointId] = " "
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

    private val payloadCallback : PayloadCallback =
        object : PayloadCallback(){
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                // This always gets the full data of the payload. Is null if it's not a BYTES payload.
                Log.d("Nearby Payload: ", "Payload received from $endpointId")

                if (payload.type == Payload.Type.BYTES) {
                    val receivedBytes = payload.asBytes()
                    val jsonMove = String(receivedBytes!!)
                    val move = Gson().fromJson(jsonMove, GameMove::class.java)
                    players[endpointId] = move.playerName

                    if(answering){
                        evaluateMove(move)
                    }
                    val game = if(answering){

                        GameState(
                            answering = answering,
                            problem = problem,
                            answers = answers,
                            mcq = mcq,
                            winner = winner,
                            hostName = hostName,
                            players = players
                        )} else{
                        GameState(
                            answering = answering,
                            problem = problem,
                            answers = answers,
                            mcq = mcq,
                            winner = winner,
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

    fun evaluateMove(move: GameMove){
        if (!mcq) {
            if (truths[move.answer[0]]) {
                //correct answer
                answering = false
                winner = move.playerName
            } else {
                //messageResId = R.string.incorrect_toast
            }
        } else {
            var score = 0
            run loop@{
                move.answer.forEach {
                    if (truths[it]) {
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
                answering = false
                winner = move.playerName
            } else {
                //messageResId = R.string.incorrect_toast
            }
        }
    }
}