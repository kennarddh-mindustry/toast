package com.github.kennarddh.mindustry.toast.core.commons.menus

import arc.Core
import mindustry.gen.Call
import mindustry.gen.Player
import mindustry.ui.Menus
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Menu(val title: String, val message: String, val maxLength: Int, val default: String, val numeric: Boolean) {
    private val continuations: MutableMap<Player, Continuation<String?>> = mutableMapOf()

    val id: Int = Menus.registerTextInput { player: Player, text: String? ->
        if (!continuations.contains(player)) return@registerTextInput

        val continuation = continuations[player]!!

        continuations.remove(player)

        if (text != null && text.length > maxLength)
            continuation.resume(null)
        else
            continuation.resume(text)
    }

    suspend fun open(player: Player) = suspendCoroutine<String?> { continuation ->
        if (continuations.contains(player)) return@suspendCoroutine continuation.resume(null)

        continuations[player] = continuation

        Core.app.post {
            Call.textInput(player.con, id, title, message, maxLength, default, numeric)
        }
    }
}