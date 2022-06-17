package com.quinze.realtimequiz

import android.content.Context
import android.content.IntentSender
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.tasks.Task
import com.quinze.realtimequiz.ui.theme.RealtimeQuizTheme


class MainActivity : ComponentActivity() {

    private lateinit var mClient :NearbyClient
    private lateinit var mHost :NearbyHost
    private lateinit var connectionsClient: ConnectionsClient

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
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager;
        val context = LocalContext.current
        val gpsMessage = stringResource(R.string.please_activate_the_GPS)

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY}

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true)

        val client: SettingsClient = LocationServices.getSettingsClient(this)

        val createLocationResultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                navController.navigate("create")
            }
        }

        val answerLocationResultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                navController.navigate("answer")
            }
        }


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

                        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
                        task.addOnSuccessListener { locationSettingsResponse ->
                            // All location settings are satisfied
                            navController.navigate("create")
                        }
                        task.addOnFailureListener { exception ->
                            if (exception is ResolvableApiException){
                                // Location settings are not satisfied, but this can be fixed
                                // by showing the user a dialog.
                                Toast.makeText(context, gpsMessage, Toast.LENGTH_SHORT).show();

                                try {
                                    // Show the dialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                                    createLocationResultLauncher.launch(intentSenderRequest)
                                } catch (sendEx: IntentSender.SendIntentException) {
                                    // Ignore the error.
                                }
                            }
                        }

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
                        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
                        task.addOnSuccessListener { locationSettingsResponse ->
                            // All location settings are satisfied
                            navController.navigate("answer")
                        }
                        task.addOnFailureListener { exception ->
                            if (exception is ResolvableApiException){
                                // Location settings are not satisfied, but this can be fixed
                                // by showing the user a dialog.
                                Toast.makeText(context, gpsMessage, Toast.LENGTH_SHORT).show();

                                try {
                                    // Show the dialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                                    answerLocationResultLauncher.launch(intentSenderRequest)
                                } catch (sendEx: IntentSender.SendIntentException) {
                                    // Ignore the error.
                                }
                            }
                        }
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
        const val REQUEST_CHECK_SETTINGS = 123
    }
}

