package com.quinze.realtimequiz

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.quinze.realtimequiz.models.HostViewModel
import com.quinze.realtimequiz.models.Question
import java.util.function.UnaryOperator

class NearbyHost() {

    //var mConnectionsClient: ConnectionsClient = connectionsClient

    //val mPlayers = hostViewModel.players
    //val hostViewModel :HostViewModel =

    @Composable
    fun QuizCreation(connectionsClient: ConnectionsClient, hostViewModel :HostViewModel = viewModel(factory = HostViewModel.HostViewModelFactory(connectionsClient))) {
        val players = hostViewModel.players
        val connected = hostViewModel.connected

        val question = hostViewModel.problem
        //var question by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if(connected) {
                ShowParticipants(players, hostViewModel.winner)
            }
            if (!connected) {

                Card() {
                    Column(
                        Modifier.padding(all = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(text = "Connecting...")
                        hostViewModel.startAdvertising()
                    }
                }
            }else {
                /*if(players.size<1){
                            Text(text = "Waiting for players...")
                        }else {
                         */
                Card(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
                    Column(
                        Modifier.padding(all = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        //if (!hostViewModel.answering)
                        ShowQuestionField(players, hostViewModel)
                        /*else
                            ShowWinner(hostViewModel)*/
                    }
                }
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
        //var question by remember { mutableStateOf("") }

        val truth = remember { mutableStateListOf(0,1,2,3)}
        //val answers = remember { mutableStateListOf("","",null,null)}
        val (selectedOption, onOptionSelected) = remember { mutableStateOf(truth[0]) }


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
            for(i in 0..3){
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if(hostViewModel.mcq){
                        Checkbox(
                            checked = hostViewModel.truths[i],
                            onCheckedChange = { hostViewModel.truths[i] = it },
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
                        value = hostViewModel.answers[i],
                        onValueChange = { hostViewModel.answers[i] = it },
                        label = { Text("Answer ${truth[i]}")},
                        singleLine = true,
                        enabled = !hostViewModel.answering
                    )
                }
            }

            Spacer(Modifier.height(28.dp))


            Button(modifier = Modifier.padding(top = 5.dp), enabled = !hostViewModel.answering
                ,onClick = {
                    if(!hostViewModel.mcq){
                        hostViewModel.truths.replaceAll( {false}  )
                        hostViewModel.truths[selectedOption]=true
                    }
                    /*val question = Question(problem = hostViewModel.problem,
                        answers = hostViewModel.answers,
                        truths =  hostViewModel.truths,
                        multipleChoice = hostViewModel.mcq
                    )*/

                    hostViewModel.answering=true
                    hostViewModel.calculateNbCorrectAnswers()
                    hostViewModel.winner=""

                    val game = GameState(
                        answering = hostViewModel.answering,
                        problem = hostViewModel.problem,
                        answers = hostViewModel.answers,
                        mcq = hostViewModel.mcq,
                        winner = hostViewModel.players[hostViewModel.winner]?:"",
                        hostName = hostViewModel.hostName,
                        players = hostViewModel.players
                    )
                    val jsonGame = Gson().toJson(game)
                    val bytesPayload = Payload.fromBytes(jsonGame.encodeToByteArray())

                    //val bytesPayload = Payload.fromBytes(question.encodeToByteArray())
                    hostViewModel.mConnectionsClient.sendPayload(players.keys.toList(), bytesPayload)
                }) {
                Text(text="SEND QUESTION")
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