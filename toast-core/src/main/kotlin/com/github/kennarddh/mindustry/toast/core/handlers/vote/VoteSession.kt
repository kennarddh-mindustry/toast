package com.github.kennarddh.mindustry.toast.core.handlers.vote

import arc.util.Timer
import mindustry.gen.Player
import java.util.concurrent.ConcurrentHashMap

data class VoteSession<T>(
    val initiator: Player,
    val objective: T,
    val task: Timer.Task,
    val voted: MutableMap<Player, Boolean> = ConcurrentHashMap()
) {
    val votes: Int
        get() {
            var votes = 0

            voted.values.forEach {
                votes += if (it) 1 else -1
            }

            return votes
        }
}
