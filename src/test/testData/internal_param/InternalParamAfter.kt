package com.theapache64.rebuggersample

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import com.theapache64.rebugger.Rebugger
import androidx.compose.runtime.Composable

@Composable
fun MyFunction(
    param1: String,
    param2: Int
) {
    val state0 by remember { mutableStateOf("") }
    Column {
        var state1 by remember { mutableStateOf(0) }
        Rebugger(
            trackMap = mapOf(
                "param1" to param1,
                "param2" to param2,
                "state0" to state0,
                "state1" to state1,
            ),
        )
        Text(text = "Param 1 is $param1 and Params 2 is $param2")
    }
}