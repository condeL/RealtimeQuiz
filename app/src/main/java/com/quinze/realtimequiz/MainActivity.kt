package com.quinze.realtimequiz

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup

import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.quinze.realtimequiz.models.HostViewModel
import com.quinze.realtimequiz.ui.theme.RealtimeQuizTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectionsClient = Nearby.getConnectionsClient(this);

        mClient = NearbyClient(connectionsClient)
        mHost = NearbyHost()
        setContent {
            RealtimeQuizTheme {

                //QuizAnswer()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = "Quiz App") },
                            /*navigationIcon = {
                                IconButton(onClick = { /* doSomething() */ }) {
                                    Icon(Icons.Filled.Menu, contentDescription = null)
                                }
                            }*/
                        )
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colors.background)
                    ) {
                        RealtimeQuizApp()
                    }
                }
            }
        }
    }

    private lateinit var mClient :NearbyClient
    private lateinit var mHost :NearbyHost
    private lateinit var connectionsClient: ConnectionsClient


    @Preview(showBackground = true)
    @Composable
    fun RealtimeQuizApp(){
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "login") {
            composable("answer") { mClient.QuizAnswer(connectionsClient) }
            composable("create") { mHost.QuizCreation(connectionsClient) }
            composable("login") { Login(navController, connectionsClient) }
            /*...*/
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun Login(navController : NavController, connectionsClient: ConnectionsClient){
        val multiplePermissionsState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rememberMultiplePermissionsState(
                listOf(
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.ACCESS_WIFI_STATE,
                    android.Manifest.permission.CHANGE_WIFI_STATE,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.BLUETOOTH_ADVERTISE,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                )
            )
        } else {
            rememberMultiplePermissionsState(
                listOf(
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.ACCESS_WIFI_STATE,
                    android.Manifest.permission.CHANGE_WIFI_STATE,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = {
                if (multiplePermissionsState.allPermissionsGranted) {
                    /*val advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
                    connectionsClient
                        .startAdvertising(
                            "Host",
                            SERVICE_ID, mHost.hostViewModel.connectionLifecycleCallback, advertisingOptions
                        )
                        .addOnSuccessListener { unused: Void? ->
                            Log.d("Nearby Advertising: ", "Success!")
                            navController.navigate("create")
                        }
                        .addOnFailureListener { e: Exception? -> Log.d("Nearby Advertising: ", e.toString()) }*/
                    navController.navigate("create")

                } else{
                    multiplePermissionsState.launchMultiplePermissionRequest()
                }
            }) {
                Icon(
                    Icons.Filled.Create,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("CREATE SESSION")
            }
            Spacer(Modifier.height(56.dp))
            Button(onClick = {
                if (multiplePermissionsState.allPermissionsGranted) {
                    /*val discoveryOptions: DiscoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
                    connectionsClient
                        .startDiscovery(SERVICE_ID, mClient.endpointDiscoveryCallback, discoveryOptions)
                        .addOnSuccessListener { unused: Void? ->
                            Log.d("Nearby Discovery: ", "Success!")
                            navController.navigate("answer")
                        }
                        .addOnFailureListener { e: java.lang.Exception? -> Log.d("Nearby Discovery: ", e.toString()) }
                        */
                    navController.navigate("answer")
                }
                else{
                    multiplePermissionsState.launchMultiplePermissionRequest()

                }
            }) {
                Icon(
                    Icons.Filled.FormatListBulleted,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("JOIN SESSION")
            }
        }
    }

    /*@Composable
    fun QuizCreation(connectionsClient: ConnectionsClient){
        val player by mPlayer.observeAsState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card() {
                Column(
                    Modifier.padding(all = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Players: $player")
                }
            }
        }
    }

    @Composable
    fun QuizAnswer(connectionsClient: ConnectionsClient) {
        val radioOptions = listOf("A: Guinea", "B: Mali", "C: Liberia", "D: Togo")
        val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }
        val (winner, setWinner) = remember { mutableStateOf("")}
        val host by mHost.observeAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card() {
                Column(
                    Modifier.padding(all = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Question: ")
                    Text(text = "Where is Conakry located? ")
                }
            }
            Spacer(Modifier.height(56.dp))
            Card(Modifier.padding(horizontal = 24.dp)) {
                Column(Modifier.selectableGroup()) {
                    radioOptions.forEach { text ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (text == selectedOption),
                                    onClick = { onOptionSelected(text) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (text == selectedOption),
                                onClick = null // null recommended for accessibility with screenreaders
                            )
                            Text(
                                text = text,
                                style = MaterialTheme.typography.body1.merge(),
                                modifier = Modifier.padding(start = 16.dp)
                            )

                        }
                    }
                }
            }
            Spacer(Modifier.height(56.dp))
            Card() {
                Text(text = "Winner: $winner", Modifier.padding(all = 16.dp))
            }
            Button(onClick = {
                val bytesPayload = Payload .fromBytes(host!!.encodeToByteArray())
                Nearby.getConnectionsClient(applicationContext).sendPayload(host!!, bytesPayload)
            }
            ) {
                Text("SEND ANSWER")

            }

        }
    }

    private fun getLocalUserName():String {
        return "Lance"
    }

    private fun getOtherUserName():String {
        return "Lanciné"
    }
*/
    /*private fun startAdvertising() {
        val advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        Nearby.getConnectionsClient(applicationContext)
            .startAdvertising(
                getLocalUserName(),
                Companion.SERVICE_ID, connectionLifecycleCallback, advertisingOptions
            )
            .addOnSuccessListener { unused: Void? -> Log.d("Nearby Advertising: ", "Success!")}
            .addOnFailureListener { e: Exception? -> Log.d("Nearby Advertising: ", e.toString()) }
    }

    private fun startDiscovery() {
        val discoveryOptions: DiscoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        Nearby.getConnectionsClient(applicationContext)
            .startDiscovery(Companion.SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener { unused: Void? -> Log.d("Nearby Discovery: ", "Success!") }
            .addOnFailureListener { e: java.lang.Exception? -> Log.d("Nearby Discovery: ", e.toString()) }
    }




    private val endpointDiscoveryCallback: EndpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                // An endpoint was found. We request a connection to it.
                Nearby.getConnectionsClient(applicationContext)
                    .requestConnection(getOtherUserName(), endpointId, connectionLifecycleCallback)
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
                Nearby.getConnectionsClient(applicationContext).acceptConnection(endpointId, payloadCallback)
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        Log.d("Nearby Connection: ", "OK!")
                        mHost.postValue(endpointId)
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {Log.d("Nearby Connection: ", "Rejected")
                    }
                    ConnectionsStatusCodes.STATUS_ERROR -> {Log.d("Nearby Connection: ", "Error")
                    }
                    else -> {Log.d("Nearby Connection: ", "Something happened")
                    }
                }
            }

            override fun onDisconnected(endpointId: String) {
                // We've been disconnected from this endpoint. No more data can be
                // sent or received.
                Log.d("Nearby Connection: ", "Lost: $endpointId")

            }
        }

    private val payloadCallback : PayloadCallback =
        object : PayloadCallback(){
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                // This always gets the full data of the payload. Is null if it's not a BYTES payload.
                Log.d("Nearby Payload: ", "Payload received from $endpointId")

                if (payload.type == Payload.Type.BYTES) {
                    val receivedBytes = payload.asBytes()
                    mPlayer.postValue(String(receivedBytes!!))
                }
            }

            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
                // after the call to onPayloadReceived().
            }
        }*/

    companion object {
        val SERVICE_ID = "com.quinze.realtimequiz"
    }
}

