package me.melijn.melijnbot.internals.arguments

import java.lang.reflect.Method
import java.lang.reflect.Parameter

data class MethodArgumentInfo(
    val method: Method,
    val list: Map<Parameter, ArgumentInfo>
)