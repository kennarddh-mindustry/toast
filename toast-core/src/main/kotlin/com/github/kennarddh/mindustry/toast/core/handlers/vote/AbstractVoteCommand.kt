package com.github.kennarddh.mindustry.toast.core.handlers.vote

import arc.util.Timer
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import kotlin.time.Duration

abstract class AbstractVoteCommand<T : Any>(val name: String, protected val timeout: Duration) : Handler() {
    private var session: VoteSession<T>? = null

    private val sessionMutex = Mutex()

    protected fun start(initiator: Player, objective: T): Boolean {
        if (session != null) {
            initiator.sendMessage("[#ff0000]There is $name vote in progress.")

            return false
        }

        if (!canPlayerStart(initiator, objective)) {
            initiator.sendMessage("[#ff0000]You cannot start $name vote.")

            return false
        }

        val task = Timer.schedule({
            CoroutineScopes.Main.launch {
                timeout()
            }
        }, timeout.inWholeSeconds.toFloat())

        session = VoteSession(initiator, objective, task)

        return true
    }

    protected open fun getRequiredVotes(): Int = Groups.player.size() / 2 + 1

    protected open fun canPlayerVote(player: Player, session: VoteSession<T>): Boolean = true

    protected open fun canPlayerStart(player: Player, session: T): Boolean = true

    protected abstract fun onSuccess()

    protected suspend fun vote(player: Player, vote: Boolean): Boolean {
        sessionMutex.withLock {
            if (session == null) {
                player.sendMessage("[#ff0000]No $name vote is in progress.")

                return false
            }

            if (canPlayerVote(player, session!!)) {
                player.sendMessage("[#ff0000]You cannot vote in $name vote.")

                return false
            }

            session!!.voted[player] = vote
        }

        checkIsRequiredVoteReached()

        return true
    }

    protected open suspend fun cancel(player: Player) {
        Call.sendMessage("[#ff0000]The $name vote cancelled by ${player.plainName()}.")

        cleanUp()
    }

    protected open suspend fun timeout() {
        Call.sendMessage("[#ff0000]The $name vote timed out.")

        cleanUp()
    }

    protected suspend fun cleanUp() {
        sessionMutex.withLock {
            session!!.task.cancel()

            session = null
        }
    }

    protected suspend fun checkIsRequiredVoteReached() {
        sessionMutex.withLock {
            if (session == null) throw IllegalStateException("Cannot check votes when session is null")

            if (session!!.votes >= getRequiredVotes()) {
                Call.sendMessage("[#00ff00]The $name vote succeeded.")

                onSuccess()
            }
        }
    }

    suspend fun onPlayerLeave(player: Player) {
        sessionMutex.withLock {
            if (session == null) return

            session!!.voted.remove(player)
        }

        checkIsRequiredVoteReached()
    }

    suspend fun onPlayerJoin(player: Player) {
        checkIsRequiredVoteReached()
    }
}