package com.quinze.realtimequiz.models

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.quinze.realtimequiz.GameMove
import com.quinze.realtimequiz.GameState
import com.quinze.realtimequiz.MainActivity

class ClientViewModel(connectionsClient: ConnectionsClient): ViewModel() {
    val mConnectionsClient = connectionsClient


    var connected  by mutableStateOf(false)
    var discovering by mutableStateOf(false)
    var answering  by mutableStateOf(false)

    var connectionAlert by mutableStateOf(false)
    var connectionAlertID by mutableStateOf("")
    var connectionAlertCode by mutableStateOf("")

    var problem by mutableStateOf("")
    var mcq by mutableStateOf(false)
    var answers = mutableStateListOf("","","","")
    var winner by mutableStateOf("")
    var answerChoice = mutableStateListOf<Int>()

    var playerName by mutableStateOf("Player")
    var hostID by mutableStateOf("")
    var hostName by mutableStateOf("")

    class ClientViewModelFactory(private val connectionsClient: ConnectionsClient) :
        ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = ClientViewModel(connectionsClient) as T
    }

    override fun onCleared() {
        super.onCleared()
        discovering = false
        mConnectionsClient.stopDiscovery()
        mConnectionsClient.stopAllEndpoints()
    }

    fun startDiscovery() {
        discovering=true
        val discoveryOptions: DiscoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        mConnectionsClient
            .startDiscovery(MainActivity.SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                    unused: Void? ->
                Log.d("Nearby Discovery: ", "Success!")

            }
            .addOnFailureListener { e: java.lang.Exception? -> Log.d("Nearby Discovery: ", e.toString()) }
    }

    val endpointDiscoveryCallback: EndpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                // An endpoint was found. We request a connection to it.
                mConnectionsClient
                    .requestConnection("Client", endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener{ unused: Void? -> Log.d("Nearby Endpoint discovery: ", "Success!") }
                    .addOnFailureListener{ e: java.lang.Exception? -> Log.d("Nearby Endpoint discovery: ", e.toString()) }
            }

            override fun onEndpointLost(endpointId: String) {
                // A previously discovered endpoint has gone away.
                Log.d("Nearby Endpoint discovery: ", "Lost $endpointId")
            }
        }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                // Automatically accept the connection on both sides.
                //mConnectionsClient.acceptConnection(endpointId, payloadCallback)
                connectionAlertID = endpointId
                connectionAlertCode = connectionInfo.authenticationDigits
                connectionAlert = true
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        Log.d("Nearby Connection: ", "OK!")
                        connected = true
                        hostID = endpointId

                        val move = GameMove(playerName = playerName)
                        val jsonMove = Gson().toJson(move)
                        val bytesPayload = Payload.fromBytes(jsonMove.encodeToByteArray())
                        mConnectionsClient.sendPayload(hostID, bytesPayload)
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        Log.d("Nearby Connection: ", "Rejected")
                        connected=false
                        discovering=false

                    }
                    ConnectionsStatusCodes.STATUS_ERROR -> {
                        Log.d("Nearby Connection: ", "Error")
                        connected=false
                        discovering=false
                    }
                    else -> {
                        Log.d("Nearby Connection: ", "Something happened")
                        connected=false
                        discovering=false
                    }
                }
            }

            override fun onDisconnected(endpointId: String) {
                // We've been disconnected from this endpoint. No more data can be
                // sent or received.
                Log.d("Nearby Connection: ", "Lost: $endpointId")
                connected = false
                startDiscovery()
            }
        }

    val payloadCallback : PayloadCallback =
        object : PayloadCallback(){
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                // This always gets the full data of the payload. Is null if it's not a BYTES payload.
                Log.d("Nearby Payload: ", "Payload received from $endpointId")

                if (payload.type == Payload.Type.BYTES) {

                    val receivedBytes = payload.asBytes()
                    val jsonGame = String(receivedBytes!!)
                    val game = Gson().fromJson(jsonGame, GameState::class.java)

                    answering = game.answering
                    problem = game.problem
                    mcq=game.mcq
                    answers.clear()
                    game.answers.let { answers.addAll(it) }
                    winner = game.winner
                    hostName = game.hostName

                }
            }

            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
                // after the call to onPayloadReceived().
            }
        }
}