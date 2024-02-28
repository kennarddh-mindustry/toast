package com.github.kennarddh.mindustry.toast.core.handlers.vote

import arc.util.Timer
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.toDisplayString
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mindustry.game.EventType.PlayerJoin
import mindustry.game.EventType.PlayerLeave
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import kotlin.time.Duration

abstract class AbstractVoteCommand<T : Any>(val name: String, protected val timeout: Duration) : Handler() {
    private var session: VoteSession<T>? = null

    private val sessionMutex = Mutex()

    protected suspend fun start(initiator: Player, objective: T): Boolean {
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

        sessionMutex.withLock {
            if (canPlayerVote(initiator, session!!))
                session!!.voted[initiator] = true

            Call.sendMessage(
                """
                [#00ff00]${initiator.plainName()} started $name vote.
                ${session!!.votes}/${getRequiredVotes()} votes are required.
                ${getSessionDetails(session!!)}
                """.trimIndent()
            )
        }

        return true
    }

    protected open fun getRequiredVotes(): Int = Groups.player.size() / 2 + 1

    protected open fun canPlayerVote(player: Player, session: VoteSession<T>): Boolean = true

    protected open fun canPlayerStart(player: Player, session: T): Boolean = true

    protected abstract suspend fun onSuccess(session: VoteSession<T>)

    protected abstract suspend fun getSessionDetails(session: VoteSession<T>): String

    protected suspend fun vote(player: Player, vote: Boolean): Boolean {
        sessionMutex.withLock {
            if (session == null) {
                player.sendMessage("[#ff0000]No $name vote is in progress.")

                return false
            }

            if (!canPlayerVote(player, session!!)) {
                player.sendMessage("[#ff0000]You cannot vote in $name vote.")

                return false
            }

            session!!.voted[player] = vote

            Call.sendMessage(
                """
                [#00ff00]${player.plainName()} voted ${vote.toDisplayString()} for $name vote.
                ${session!!.votes}/${getRequiredVotes()} votes are required.
                """.trimIndent()
            )
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

                onSuccess(session!!)

                cleanUp()
            }
        }
    }

    @EventHandler
    suspend fun onPlayerLeave(event: PlayerLeave) {
        sessionMutex.withLock {
            if (session == null) return

            session!!.voted.remove(event.player)
        }

        checkIsRequiredVoteReached()
    }

    @EventHandler
    suspend fun onPlayerJoin(event: PlayerJoin) {
        checkIsRequiredVoteReached()
    }
}