package com.github.kennarddh.mindustry.toast.core.commons

import arc.util.Timer
import mindustry.gen.Player
import kotlin.time.Duration

class VoteSession(
    val timeout: Duration,
    val starter: Player,
    val getRequiredVotes: suspend () -> Int,
    val onSuccess: suspend () -> Unit,
    val onTimeout: () -> Unit,
    val onCancel: suspend () -> Unit,
) {
    private val backingVoted: MutableMap<Player, Boolean> = mutableMapOf()

    val voted
        get() = backingVoted.toMap()

    val votes: Int
        get() {
            var votes = 0

            backingVoted.values.forEach {
                if (it)
                    votes += 1
                else
                    votes -= 1
            }

            return votes
        }

    private lateinit var task: Timer.Task

    init {
        backingVoted[starter] = true

        task = Timer.schedule({
            onTimeout()

            task.cancel()
        }, timeout.inWholeSeconds.toFloat())
    }

    suspend fun vote(player: Player, vote: Boolean) {
        backingVoted[player] = vote

        if (votes >= getRequiredVotes())
            onSuccess()
    }

    suspend fun cancel() {
        onCancel()
    }

    suspend fun onPlayerLeave(player: Player) {
        backingVoted.remove(player)

        if (votes >= getRequiredVotes())
            onSuccess()
    }

    suspend fun onPlayerJoin(player: Player) {
        if (votes >= getRequiredVotes())
            onSuccess()
    }
}