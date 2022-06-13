package com.quinze.realtimequiz.models

import android.R
import android.app.AlertDialog
import android.content.DialogInterface
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.nearby.Nearby
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

    var connectionAlert by mutableStateOf(false)
    var connectionAlertID by mutableStateOf("")
    var connectionAlertCode by mutableStateOf("")



    var problem by mutableStateOf("")
    var answers = mutableStateListOf(Pair("",false),Pair("",false),Pair("",false),Pair("",false))

    var nbCorrectAnswers = 0


    var mcq by mutableStateOf(false)
    var winner by mutableStateOf("")

    var hostName by mutableStateOf("Host")


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

    fun calculateNbCorrectAnswers() {
        nbCorrectAnswers = 0
        for (i in 0 until answers.size) {
            if (answers[i].second) {
                nbCorrectAnswers++
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
                /*AlertDialog.Builder(context)
                    .setTitle("Accept connection to " + info.endpointName)
                    .setMessage("Confirm the code matches on both devices: " + info.authenticationDigits)
                    .setPositiveButton(
                        "Accept"
                    ) { dialog: DialogInterface?, which: Int ->  // The user confirmed, so we can accept the connection.
                        mConnectionsClient.acceptConnection(endpointId!!, payloadCallback)
                    }
                    .setNegativeButton(
                        "cancel"
                    ) { dialog: DialogInterface?, which: Int ->  // The user canceled, so we should reject the connection.
                        mConnectionsClient.rejectConnection(endpointId!!)
                    }
                    .setIcon(R.drawable.ic_dialog_alert)
                    .show()*/
                connectionAlertID = endpointId
                connectionAlertCode = connectionInfo.authenticationDigits
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
                            problem = problem,
                            answers = answers.map{ it.first },
                            mcq = mcq,
                            winner = "",
                            hostName = hostName,
                            players = players
                        )} else{
                        GameState(
                            answering = answering,
                            problem = problem,
                            answers = answers.map{ it.first },
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
                answering = false
                winner = playerID
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
                answering = false
                winner = playerID
            } else {
                //messageResId = R.string.incorrect_toast
            }
        }
    }
}