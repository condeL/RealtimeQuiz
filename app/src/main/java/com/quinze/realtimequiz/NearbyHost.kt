package com.quinze.realtimequiz

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

    @Composable
    fun QuizCreation(connectionsClient: ConnectionsClient, hostViewModel :HostViewModel = viewModel(factory = HostViewModel.HostViewModelFactory(connectionsClient))) {
        val players = hostViewModel.players
        val connected = hostViewModel.connected
        val advertising = hostViewModel.advertising

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
                        Text(text = "${stringResource(R.string.connecting)}...")
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
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.height(TextFieldDefaults.MinHeight*1.25f)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxHeight(),
                    value = hostViewModel.hostName,
                    onValueChange = { hostViewModel.hostName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.hostname)) })
            }
            Column(modifier = Modifier.height(TextFieldDefaults.MinHeight*1.1f)) {
                Button(
                    modifier = Modifier.fillMaxHeight(),
                    enabled = hostViewModel.hostName.trim().isNotEmpty(),
                    onClick = {
                        hostViewModel.startAdvertising()
                    }) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = null,
                    )
                }
            }
        }
    }

    @Composable
    fun ShowParticipants(players: Map<String, String>, winnerID: String){
        Card(modifier = Modifier.fillMaxWidth(0.66f)) {
            Column(
                Modifier.padding(all = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "${stringResource(R.string.participants)}: ${players.size}")
                players.forEach { player ->
                    val winner = player.key == winnerID
                    Row(modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Text(text = player.key,  color = if (winner) Color.Green else Color.Unspecified)
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
                label = { Text(stringResource(R.string.question)) },
                enabled = !hostViewModel.answering
            )
            Row(verticalAlignment = Alignment.CenterVertically){
                Switch(
                    checked = hostViewModel.mcq,
                    onCheckedChange = { hostViewModel.mcq = it },
                    enabled = !hostViewModel.answering

                )
                Text(text="${stringResource(R.string.multiple_choice)}?")
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
                    val answerLabel = stringResource(R.string.answer)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = hostViewModel.answers[i].first,
                        onValueChange = { hostViewModel.answers[i] = hostViewModel.answers[i].copy(first=it)},
                        label = { Text(text = "$answerLabel ${letters[i]}")},
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
                    Text(text= stringResource(R.string.cancel_question))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = null,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            } else {
                val invalid = stringResource(R.string.invalid_answers)
                val canceled = stringResource(R.string.canceled_by_host)
                Button(modifier = Modifier.padding(top = 5.dp),
                    enabled = hostViewModel.problem.trim().isNotEmpty(),
                    onClick = {
                        if (!hostViewModel.mcq) {
                            hostViewModel.answers.replaceAll { it.copy(second = false) }
                            hostViewModel.answers[selectedOption] =
                                hostViewModel.answers[selectedOption].copy(second = true)
                        }

                        if (!hostViewModel.calculateNbCorrectAnswers()) {
                            Toast.makeText(context, invalid, Toast.LENGTH_LONG).show()
                        } else {
                            hostViewModel.answered = false
                            hostViewModel.answering = true
                            hostViewModel.winner = ""
                            hostViewModel.winningAnswers = listOf()
                            hostViewModel.winningProblem = "$canceled..."

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
                    Text(text = stringResource(R.string.send_question))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Icon(
                        Icons.Filled.Send,
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
                //hostViewModel.connectionAlert = false
            },
            title = {
                Text(text = "${stringResource(R.string.accept_connection_to)}:")
            },
            text = null,
            buttons = {

                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp)) {
                    for (i in 0 until hostViewModel.connectionAlertInfo.size) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(buildAnnotatedString {
                                append("ID: ")
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.primary
                                    )
                                ) {
                                    append(hostViewModel.connectionAlertInfo[i].first)
                                }
                            })

                            Text(buildAnnotatedString {
                                append("${stringResource(R.string.code)}: ")
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.primary
                                    )
                                ) {
                                    append(hostViewModel.connectionAlertInfo[i].second)
                                }
                            })
                            Box() {
                                Row(horizontalArrangement = Arrangement.End) {
                                    IconButton(

                                        onClick = {
                                            connectionsClient.rejectConnection(hostViewModel.connectionAlertInfo[i].first)
                                            hostViewModel.connectionAlertInfo.removeAt(i)
                                            hostViewModel.connectionAlert = hostViewModel.connectionAlertInfo.isNotEmpty()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Outlined.Cancel,
                                            tint = MaterialTheme.colors.error,
                                            contentDescription = null
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    IconButton(
                                        onClick = {
                                            connectionsClient.acceptConnection(
                                                hostViewModel.connectionAlertInfo[i].first,
                                                hostViewModel.payloadCallback
                                            )
                                            hostViewModel.connectionAlertInfo.removeAt(i)
                                            hostViewModel.connectionAlert = hostViewModel.connectionAlertInfo.isNotEmpty()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Outlined.TaskAlt,
                                            tint = MaterialTheme.colors.primary,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp, 0.dp, 8.dp, 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    val refused = stringResource(R.string.connection_refused)
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.error),
                        border= BorderStroke(1.dp, MaterialTheme.colors.error),
                        onClick = {
                            for(i in 0 until hostViewModel.connectionAlertInfo.size) {
                                connectionsClient.rejectConnection(hostViewModel.connectionAlertInfo[i].first)
                            }
                            hostViewModel.connectionAlertInfo.clear()
                            hostViewModel.connectionAlert = hostViewModel.connectionAlertInfo.isNotEmpty()
                            Toast.makeText(context, refused, Toast.LENGTH_LONG).show()

                        }
                    ) {
                        Text(stringResource(R.string.cancel_all), modifier = Modifier.padding(vertical = 8.dp))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Icon(Icons.Filled.Block, contentDescription = null)
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            for(i in 0 until hostViewModel.connectionAlertInfo.size) {
                                connectionsClient.acceptConnection(hostViewModel.connectionAlertInfo[i].first, hostViewModel.payloadCallback)
                            }
                            hostViewModel.connectionAlertInfo.clear()
                            hostViewModel.connectionAlert = hostViewModel.connectionAlertInfo.isNotEmpty()
                        }
                    ) {
                        Text(stringResource(R.string.accept_all), modifier = Modifier.padding(vertical = 8.dp))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Icon(Icons.Filled.DoneAll, contentDescription = null)
                    }
                }
            }
        )
    }
}