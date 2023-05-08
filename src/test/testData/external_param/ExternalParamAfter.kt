package com.theapache64.rebuggersample

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import com.theapache64.rebugger.Rebugger
import androidx.compose.runtime.Composable

@Composable
fun MyFunction(
    param1: String,
    param2: Int,
    myClass: MyClass = MyClass()
) {
    val state0 by remember { mutableStateOf("") }
    Text(text = "myDirectState is ${myClass.state1}")
    val state2 = myClass.state2 + myClass.state3
    Rebugger(
            trackMap = mapOf(
                    "param1" to param1,
                    "param2" to param2,
                    "myClass" to myClass,
                    "state0" to state0,
                    "myClass.state1" to myClass.state1,
                    "state2" to state2,
            ),
    )
    Column {
        Text(text = "Param 1 is $param1 and Params 2 is $param2")
    }
}