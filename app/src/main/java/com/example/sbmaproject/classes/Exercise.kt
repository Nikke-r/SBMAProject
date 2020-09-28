package com.example.sbmaproject.classes

import java.util.*

data class Exercise(
    val uid: String,
    val distance: String,
    val time: String,
    val date: Date,
    val steps: String,
    val highestSpeed: Double
)