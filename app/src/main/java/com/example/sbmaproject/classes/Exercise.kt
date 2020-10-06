package com.example.sbmaproject.classes

import java.util.*

//Default values are for the firebase so we can create an exercise object from the document snapshot
data class Exercise(
    val username: String = "",
    val uid: String = "",
    val route: String = "",
    val distance: String = "",
    val time: String = "",
    val date: Date = Date(),
    val steps: String = "",
    val highestSpeed: Double = 0.00,
    val averageSpeed: Double = 0.00
) {}