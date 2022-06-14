package com.quinze.realtimequiz

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.quinze.realtimequiz.models.HostViewModel

class NearbyHost() {

    //var mConnectionsClient: ConnectionsClient = connectionsClient

    //val mPlayers = hostViewModel.players
    //val hostViewModel :HostViewModel =

    @Composable
    fun QuizCreation(connectionsClient: ConnectionsClient, hostViewModel :HostViewModel = viewModel(factory = HostViewModel.HostViewModelFactory(connectionsClient))) {
        val players = hostViewModel.players
        val connected = hostViewModel.connected
        val advertising = hostViewModel.advertising
        //var question by remember { mutableStateOf("") }

        if (hostViewModel.connectionAlert) {

            Alert(connectionsClient, hostViewModel)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if(!advertising && !connected){
                ShowNameSelection(hostViewModel)
            }
            else if (!connected) {
                Card() {
                    Column(
                        Modifier.padding(all = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(text = "Connecting...")
                    }
                }
            }else {
                ShowParticipants(players, hostViewModel.winner)

                /*if(players.size<1){
                            Text(text = "Waiting for players...")
                        }else {
                         */
                Card(modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()) {
                    Column(
                        Modifier.padding(all = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ShowQuestionField(players, hostViewModel)
                    }
                }
            }
        }
    }

    @Composable
    fun ShowNameSelection(hostViewModel: HostViewModel){
        Row() {
            OutlinedTextField(
                value = hostViewModel.hostName,
                onValueChange = { hostViewModel.hostName = it },
                singleLine = true,
                label = { Text("Hostname") })
            Button(modifier = Modifier.padding(top = 5.dp), enabled = hostViewModel.hostName.trim().isNotEmpty(), onClick = {
                hostViewModel.startAdvertising()
            }) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.padding(vertical = 10.dp))
            }
        }
    }

    @Composable
    fun ShowParticipants(players: Map<String, String>, winnerID: String){
        Card() {
            Column(
                Modifier.padding(all = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Participants: ${players.size}")
                players.forEach { player ->
                    val winner = player.key == winnerID
                    Row() {
                        Text(text = player.key,  color = if (winner) Color.Green else Color.Unspecified)
                        Spacer(Modifier.width(56.dp))
                        Text(text = player.value, color = if (winner) Color.Green else Color.Unspecified)
                    }
                }
            }
        }
        Spacer(Modifier.height(56.dp))
    }

    @Composable
    fun ShowQuestionField(players: Map<String, String>, hostViewModel: HostViewModel){

        val problem = hostViewModel.problem

        val truth = remember { mutableStateListOf(0,1,2,3)}
        val letters = listOf("A", "B", "C", "D")
        val (selectedOption, onOptionSelected) = remember { mutableStateOf(truth[0]) }

        val context = LocalContext.current



        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = problem,
                onValueChange = { hostViewModel.problem = it },
                label = { Text("Question") },
                enabled = !hostViewModel.answering
            )
            Row(verticalAlignment = Alignment.CenterVertically){
                Switch(
                    checked = hostViewModel.mcq,
                    onCheckedChange = { hostViewModel.mcq = it },
                    enabled = !hostViewModel.answering

                )
                Text(text="Multiple choice?")
            }

            for(i in 0 until hostViewModel.answers.size){
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if(hostViewModel.mcq){
                        Checkbox(
                            checked = hostViewModel.answers[i].second,
                            onCheckedChange = { hostViewModel.answers[i] = hostViewModel.answers[i].copy(second=it)},
                            enabled = !hostViewModel.answering
                        )
                    }else {
                        RadioButton(
                            selected = (truth[i] == selectedOption),
                            onClick = { onOptionSelected(truth[i]) },
                            enabled = !hostViewModel.answering
                        )
                    }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = hostViewModel.answers[i].first,
                        onValueChange = { hostViewModel.answers[i] = hostViewModel.answers[i].copy(first=it)},
                        label = { Text("Answer ${letters[i]}")},
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !hostViewModel.answering
                    )
                }
            }


            Spacer(Modifier.height(28.dp))


            if(hostViewModel.answering){
                OutlinedButton(modifier = Modifier.padding(top = 5.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.error),
                    border= BorderStroke(1.dp, MaterialTheme.colors.error),
                    onClick = {
                        if(hostViewModel.answering){
                            hostViewModel.repopulateAnswerChoices()
                            hostViewModel.answering=false
                            hostViewModel.answered = true

                        }
                        val game = GameState(
                            answering = hostViewModel.answering,
                            answered = hostViewModel.answered,
                            problem = hostViewModel.winningProblem,
                            answers = hostViewModel.winningAnswers,
                            mcq = hostViewModel.mcq,
                            winner = hostViewModel.players[hostViewModel.winner] ?: "",
                            hostName = hostViewModel.hostName,
                            players = hostViewModel.players
                        )
                        val jsonGame = Gson().toJson(game)
                        val bytesPayload = Payload.fromBytes(jsonGame.encodeToByteArray())

                        hostViewModel.mConnectionsClient.sendPayload(
                            players.keys.toList(),
                            bytesPayload
                        )


                    }
                ) {
                    Text(text=if(hostViewModel.answering)"CANCEL QUESTION" else "SEND QUESTION")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Icon(
                        imageVector = if(hostViewModel.answering) Icons.Filled.Cancel else Icons.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            } else {

                Button(modifier = Modifier.padding(top = 5.dp),
                    enabled = hostViewModel.problem.trim().isNotEmpty(),
                    onClick = {
                        if (!hostViewModel.mcq) {
                            hostViewModel.answers.replaceAll { it.copy(second = false) }
                            hostViewModel.answers[selectedOption] =
                                hostViewModel.answers[selectedOption].copy(second = true)
                        }

                        if (!hostViewModel.calculateNbCorrectAnswers()) {
                            Toast.makeText(context, "Invalid answers", Toast.LENGTH_LONG).show()
                        } else {
                            hostViewModel.answered = false
                            hostViewModel.answering = true
                            hostViewModel.winner = ""
                            hostViewModel.winningAnswers = listOf()
                            hostViewModel.winningProblem = "Canceled by host..."

                            val game = GameState(
                                answering = hostViewModel.answering,
                                answered = hostViewModel.answered,
                                problem = hostViewModel.problem,
                                answers = hostViewModel.answers.unzip().first,
                                mcq = hostViewModel.mcq,
                                winner = hostViewModel.players[hostViewModel.winner] ?: "",
                                hostName = hostViewModel.hostName,
                                players = hostViewModel.players
                            )
                            val jsonGame = Gson().toJson(game)
                            val bytesPayload = Payload.fromBytes(jsonGame.encodeToByteArray())

                            hostViewModel.mConnectionsClient.sendPayload(
                                players.keys.toList(),
                                bytesPayload
                            )
                        }
                    }
                ) {
                    Text(text = if (hostViewModel.answering) "CANCEL QUESTION" else "SEND QUESTION")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Icon(
                        imageVector = if (hostViewModel.answering) Icons.Filled.Cancel else Icons.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }

        }
    }

    @Composable
    fun Alert(connectionsClient: ConnectionsClient ,hostViewModel: HostViewModel){

        val context = LocalContext.current

        AlertDialog(
            modifier = Modifier.fillMaxWidth(),
            onDismissRequest = {
                hostViewModel.connectionAlert = false
            },
            title = {
                Text(text = "Accept connection to " + hostViewModel.connectionAlertID)
            },
            text = {
                Text(modifier = Modifier.fillMaxWidth(),
                    text = buildAnnotatedString {
                        withStyle(style = ParagraphStyle(textAlign = TextAlign.Center)) {

                            append("Confirm the code matches on both devices:\n\n")
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.primary
                                )
                            ) {
                                append(hostViewModel.connectionAlertCode)
                            }
                        }
                    }
                )
            },
            buttons = {
                Row(
                    modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            hostViewModel.connectionAlert = false
                            connectionsClient.acceptConnection(hostViewModel.connectionAlertID, hostViewModel.payloadCallback)
                        }
                    ) {
                        Text("ACCEPT", modifier = Modifier.padding(vertical = 10.dp))
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            hostViewModel.connectionAlert = false
                            connectionsClient.rejectConnection(hostViewModel.connectionAlertID)
                            Toast.makeText(context, "Connection refused!", Toast.LENGTH_LONG).show()

                        }
                    ) {
                        Text("CANCEL", modifier = Modifier.padding(vertical = 10.dp))
                    }
                }
            }
        )
    }
}