package com.github.kennarddh.mindustry.toast.discord

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object CoroutineScopes {
    val Main = CoroutineScope(Dispatchers.Default)
    val IO = CoroutineScope(Dispatchers.IO)
}