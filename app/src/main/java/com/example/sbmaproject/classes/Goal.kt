package com.example.sbmaproject.classes

import java.util.*

//Default values are for the firebase so we can create an exercise object from the document snapshot
data class Goal(
    val username: String = "",
    val uid: String = "",
    val distance: String = "",
) {}