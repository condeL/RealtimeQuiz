package com.quinze.realtimequiz.models


data class Question(var problem: String, val answers: List<String>, val truths:  List<Boolean>, var multipleChoice: Boolean) {

    //var mProblem = problem
    //val mAnswers: MutableList<Pair<String, Boolean>> = ArrayList()
    //var mMultipleChoice = multipleChoice
    var nbCorrectAnswers = 0
    private set


    init {
        val length = answers.size
        if (length in 2..4) {
            for (i in 0 until length) {
                if (truths[i] == true) {
                    nbCorrectAnswers++
                }
            }
        }
        this.nbCorrectAnswers = nbCorrectAnswers
    }
}