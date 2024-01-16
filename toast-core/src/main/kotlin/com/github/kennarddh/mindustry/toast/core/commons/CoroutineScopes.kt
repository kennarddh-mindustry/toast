package com.github.kennarddh.mindustry.toast.core.commons

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object CoroutineScopes {
    val Main = CoroutineScope(Dispatchers.Default)
}