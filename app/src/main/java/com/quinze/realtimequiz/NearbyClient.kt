package com.quinze.realtimequiz

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.quinze.realtimequiz.models.ClientViewModel

class NearbyClient(connectionsClient: ConnectionsClient) {

    //var mConnectionsClient: ConnectionsClient = connectionsClient
    //var mPlayer = MutableLiveData<String>("Me")
    //var mHost = MutableLiveData<String>("")


    @Composable
    fun QuizAnswer(connectionsClient: ConnectionsClient, clientViewModel : ClientViewModel = viewModel(factory = ClientViewModel.ClientViewModelFactory(connectionsClient))) {


        val connected = clientViewModel.connected
        val discovering = clientViewModel.discovering

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            if(!discovering && !connected){
                ShowNameSelection(clientViewModel)
            }

            else if (!connected) {
                clientViewModel.startDiscovery()
                Card() {
                    Text(modifier = Modifier.padding(all = 16.dp), text = "Connecting...")
                }
            }
            else if(clientViewModel.answering) {
                ShowQuestion(clientViewModel)
            } else {
                ShowWinner(clientViewModel.winner)
            }
        }
    }

    @Composable
    fun ShowNameSelection(clientViewModel: ClientViewModel){
        Row() {
            OutlinedTextField(
                value = clientViewModel.playerName,
                onValueChange = { clientViewModel.playerName = it },
                singleLine = true,
                label = { Text("Username") })
            Button(modifier = Modifier.padding(top = 5.dp), onClick = {
                clientViewModel.startDiscovery()
            }) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.padding(vertical = 10.dp))
            }
        }
    }

    @Composable
    fun ShowQuestion(clientViewModel: ClientViewModel){

        val answers = clientViewModel.answers
        /*val (selectedOption, onOptionSelected) = remember { mutableStateOf(answers[0]) }*/

        //val answers = listOf("A: Guinea", "B: Mali", "C: Liberia", "D: Togo")
        val mcq = clientViewModel.mcq
        val (selectedOption, onOptionSelected) = remember { mutableStateOf(/*clientViewModel.*/answers[0]) }

        Card() {
            Column(
                Modifier.padding(all = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Host: ${clientViewModel.hostName}")
            }
        }

        Spacer(Modifier.height(56.dp))

        Card() {
            Column(
                Modifier.padding(all = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Question: ")
                Text(text = clientViewModel.problem)
            }
        }

        Spacer(Modifier.height(56.dp))

        Card(Modifier.padding(horizontal = 24.dp)) {
            Column(Modifier.selectableGroup()) {
                for(i in 0 until clientViewModel.answers.size){
                    Row(
                        modifier = if(mcq){
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .toggleable(
                                    value = clientViewModel.answerChoice.contains(i),
                                    onValueChange = {if(it) clientViewModel.answerChoice.add(i) else clientViewModel.answerChoice.remove(i) },
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if(mcq){
                            Checkbox(
                                checked = clientViewModel.answerChoice.contains(i),
                                onCheckedChange = { if(it) clientViewModel.answerChoice.add(i) else clientViewModel.answerChoice.remove(i) },
                            )
                        }else {
                            RadioButton(
                                selected = (answers[i] == selectedOption),
                                onClick = null // null recommended for accessibility with screenreaders
                            )
                        }
                        Text(
                            text = answers[i],
                            style = MaterialTheme.typography.body1.merge(),
                            modifier = Modifier.padding(start = 16.dp)
                        )

                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(onClick = {

            if(!clientViewModel.mcq){
                val index = clientViewModel.answers.indexOf(selectedOption)
                clientViewModel.answerChoice.clear()
                clientViewModel.answerChoice.add(index)
            }
            Log.d("Answer", clientViewModel.answerChoice.joinToString(","))
            val move = GameMove(playerName = clientViewModel.playerName, answer = clientViewModel.answerChoice)
            val jsonMove = Gson().toJson(move)
            val bytesPayload = Payload.fromBytes(jsonMove.encodeToByteArray())
            clientViewModel.mConnectionsClient.sendPayload(clientViewModel.hostID, bytesPayload)
        }) {
            Text("SEND ANSWER")

        }
    }
    @Composable
    fun ShowWinner(winner: String){
        Card() {
            Text(text = "Winner: $winner", Modifier.padding(all = 16.dp))
        }
        Spacer(Modifier.height(14.dp))
    }
}