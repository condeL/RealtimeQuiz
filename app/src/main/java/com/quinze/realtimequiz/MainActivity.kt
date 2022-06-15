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
import androidx.compose.ui.res.stringResource
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
        connectionsClient = Nearby.getConnectionsClient(this)

        mClient = NearbyClient(connectionsClient)
        mHost = NearbyHost()
        setContent {
            RealtimeQuizTheme {
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
            Row(modifier = Modifier.width(ButtonDefaults.MinWidth*4)) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    if (multiplePermissionsState.allPermissionsGranted) {
                        navController.navigate("create")

                    } else {
                        multiplePermissionsState.launchMultiplePermissionRequest()
                    }
                }) {
                    Icon(
                        Icons.Filled.Create,
                        contentDescription = null,
                        modifier = Modifier.padding(vertical = 10.dp).size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.create_session))
                }
            }
            Spacer(Modifier.height(56.dp))
            Row(modifier = Modifier.width(ButtonDefaults.MinWidth*4)) {

                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    if (multiplePermissionsState.allPermissionsGranted) {
                        navController.navigate("answer")
                    } else {
                        multiplePermissionsState.launchMultiplePermissionRequest()

                    }
                }) {
                    Icon(
                        Icons.Filled.FormatListBulleted,
                        contentDescription = null,
                        modifier = Modifier.padding(vertical = 10.dp).size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.join_session))
                }
            }
        }
    }

    companion object {
        const val SERVICE_ID = "com.quinze.realtimequiz"
    }
}

