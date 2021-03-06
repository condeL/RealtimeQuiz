package com.quinze.realtimequiz

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.quinze.realtimequiz.models.ClientViewModel

class NearbyClient(connectionsClient: ConnectionsClient) {

    @Composable
    fun QuizAnswer(connectionsClient: ConnectionsClient, clientViewModel : ClientViewModel = viewModel(factory = ClientViewModel.ClientViewModelFactory(connectionsClient))) {

        if (clientViewModel.connectionAlert) {
            Alert(connectionsClient, clientViewModel)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            if(!clientViewModel.discovering && !clientViewModel.connected){
                ShowNameSelection(clientViewModel)
            }

            else if (!clientViewModel.connected) {
                clientViewModel.startDiscovery()
                Card() {
                    Text(modifier = Modifier.padding(all = 16.dp), text = "${stringResource(R.string.connecting)}...")
                }
            }
            else {
                ShowQuestion(clientViewModel)
                if(!clientViewModel.answering) {
                    ShowWinner(clientViewModel.winner)
                }
            }
        }
    }

    @Composable
    fun ShowNameSelection(clientViewModel: ClientViewModel){
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.height(TextFieldDefaults.MinHeight*1.25f)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxHeight(),
                    value = clientViewModel.playerName,
                    onValueChange = { clientViewModel.playerName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.username)) })
            }
            Column(modifier = Modifier.height(TextFieldDefaults.MinHeight*1.1f)) {
                Button(
                    modifier = Modifier.fillMaxHeight(),
                    enabled = clientViewModel.playerName.trim().isNotEmpty(),
                    onClick = {
                        clientViewModel.startDiscovery()
                    }) {
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
    fun ShowQuestion(clientViewModel: ClientViewModel) {


        val answers = remember { mutableStateListOf(0, 1, 2, 3) }
        val letters = listOf("A", "B", "C", "D")

        val mcq = clientViewModel.mcq
        val (selectedOption, onOptionSelected) = remember {
            mutableStateOf(answers[0])
        }

        Card() {
            Column(
                Modifier.padding(all = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "${stringResource(R.string.host)}: ${clientViewModel.hostName}")
            }
        }

        Spacer(Modifier.height(56.dp))

        Card() {
            Column(
                Modifier.padding(all = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "${stringResource(R.string.question)}: ")
                Text(text = clientViewModel.problem)
            }
        }

        Spacer(Modifier.height(56.dp))

        Card(Modifier.padding(horizontal = 24.dp)) {
            Column(Modifier.selectableGroup()) {
                for (i in 0 until clientViewModel.answers.size) {
                    Row(
                        modifier = if (mcq) {
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .toggleable(
                                    value = clientViewModel.answerChoice.contains(i),
                                    onValueChange = {
                                        if (it) clientViewModel.answerChoice.add(i) else clientViewModel.answerChoice.remove(
                                            i
                                        )
                                    },
                                    role = Role.Checkbox
                                )
                                .padding(horizontal = 16.dp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (answers[i] == selectedOption),
                                    onClick = { onOptionSelected(answers[i]) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (mcq) {
                            Checkbox(
                                checked = if (!clientViewModel.answered) clientViewModel.answerChoice.contains(
                                    i
                                ) else true,
                                onCheckedChange = {
                                    if (it) clientViewModel.answerChoice.add(i) else clientViewModel.answerChoice.remove(
                                        i
                                    )
                                },
                            )
                        } else {
                            RadioButton(
                                selected = if (!clientViewModel.answered) (answers[i] == selectedOption) else true,
                                onClick = null // null recommended for accessibility with screenreaders
                            )
                        }
                        Text(
                            text = clientViewModel.answers[i],
                            style = MaterialTheme.typography.body1.merge(),
                            modifier = Modifier.padding(start = 16.dp)
                        )

                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        if (clientViewModel.answering) {
            Button(onClick = {

                if (!clientViewModel.mcq) {
                    clientViewModel.answerChoice.clear()
                    clientViewModel.answerChoice.add(selectedOption)
                }

                val move = GameMove(
                    playerName = clientViewModel.playerName,
                    answer = clientViewModel.answerChoice
                )
                val jsonMove = Gson().toJson(move)
                val bytesPayload = Payload.fromBytes(jsonMove.encodeToByteArray())
                clientViewModel.mConnectionsClient.sendPayload(clientViewModel.hostID, bytesPayload)
            }) {
                Text(stringResource(R.string.send_answer))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Icon(
                    Icons.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
    @Composable
    fun ShowWinner(winner: String){
        Card() {
            Text(text = "${stringResource(R.string.winner)}: $winner", Modifier.padding(all = 16.dp))
        }
        Spacer(Modifier.height(14.dp))
    }

    @Composable
    fun Alert(connectionsClient: ConnectionsClient, clientViewModel: ClientViewModel){
        AlertDialog(
            onDismissRequest = {
                //clientViewModel.connectionAlert = false
            },
            title = {
                Text(text = "${stringResource(R.string.accept_connection_to)} " + clientViewModel.connectionAlertID)
            },
            text = {
                Text(modifier = Modifier.fillMaxWidth(),
                    text = buildAnnotatedString {
                        withStyle(style = ParagraphStyle(textAlign = TextAlign.Center)) {

                            append("${stringResource(R.string.confirm_the_code_matches)}:\n\n")

                            withStyle(
                                style = SpanStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.primary
                                )
                            ) {
                                append(clientViewModel.connectionAlertCode)
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
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.error),
                        border= BorderStroke(1.dp, MaterialTheme.colors.error),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            clientViewModel.connectionAlert = false
                            connectionsClient.rejectConnection(clientViewModel.connectionAlertID)
                        }
                    ) {
                        Text(stringResource(R.string.reject), modifier = Modifier.padding(vertical = 10.dp))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Icon(Icons.Filled.Block, contentDescription = null)
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            clientViewModel.connectionAlert = false
                            connectionsClient.acceptConnection(clientViewModel.connectionAlertID, clientViewModel.payloadCallback)
                        }
                    ) {
                        Text(stringResource(R.string.accept), modifier = Modifier.padding(vertical = 10.dp))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Icon(Icons.Filled.Done, contentDescription = null)
                    }
                }
            }
        )
    }
}