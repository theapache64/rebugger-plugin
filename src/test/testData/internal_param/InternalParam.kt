package com.theapache64.rebuggersample

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun MyFunction(
    param1: String,
    param2: Int
) {
    val state0 by remember { mutableStateOf("") }
    <caret>
    Column {
        Text(text = "Param 1 is $param1 and Params 2 is $param2")
    }
}