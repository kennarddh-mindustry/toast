package com.github.kennarddh.mindustry.toast.core.handlers.vote

import arc.util.Timer
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.extensions.toDisplayString
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import com.github.kennarddh.mindustry.toast.core.commons.entities.PlayerData
import com.github.kennarddh.mindustry.toast.core.handlers.users.PlayerDisconnected
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mindustry.game.EventType.PlayEvent
import mindustry.game.EventType.PlayerJoin
import mindustry.gen.Call
import mindustry.gen.Player
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

abstract class AbstractVoteCommand<T : Any>(
    protected val name: String,
    protected val timeout: Duration,
    protected val minDelayBetweenStart: Duration = 5.minutes,
    protected val resetOnPlay: Boolean = true
) : Handler {
    private val playersLastStartTime: MutableMap<Player, Instant> = ConcurrentHashMap()

    private var session: VoteSession<T>? = null

    private val sessionMutex = Mutex()

    protected suspend fun getIsVoting(): Boolean {
        sessionMutex.withLock {
            return session != null
        }
    }

    protected suspend fun start(initiator: PlayerData, objective: T): Boolean {
        val playerLastStartTime = playersLastStartTime[initiator.player]

        if (playerLastStartTime !== null && playerLastStartTime >= Clock.System.now() - minDelayBetweenStart) {
            initiator.player.sendMessage("[#ff0000]You must wait ${minDelayBetweenStart.toDisplayString()} before starting another '$name' vote. Wait ${(minDelayBetweenStart - (Clock.System.now() - playerLastStartTime)).toDisplayString()}.")

            return false
        }

        playersLastStartTime[initiator.player] = Clock.System.now()

        if (!canPlayerStart(initiator, objective)) {
            initiator.player.sendMessage("[#ff0000]You cannot start '$name' vote.")

            return false
        }

        sessionMutex.withLock {
            if (session != null) {
                initiator.player.sendMessage("[#ff0000]There is '$name' vote in progress.")

                return false
            }

            val task = Timer.schedule({
                CoroutineScopes.Main.launch {
                    timeout()
                }
            }, timeout.inWholeSeconds.toFloat())

            session = VoteSession(initiator, objective, task)

            if (canPlayerVote(initiator, session!!))
                session!!.voted[initiator.player] = true

            val sessionDetail = getSessionDetails(session!!)

            Call.sendMessage(
                """
                [#00ff00]'${initiator.player.plainName()}/${initiator.mindustryUserID}' started '$name' vote.
                ${session!!.votes}/${getRequiredVotes()} votes are required.
                """.trimIndent() + if (sessionDetail != "") "\n${sessionDetail}" else ""
            )
        }

        checkIsRequiredVoteReached()

        return true
    }

    protected open fun getRequiredVotes(): Int = Entities.players.size / 2 + 1

    protected open suspend fun canPlayerVote(playerData: PlayerData, session: VoteSession<T>): Boolean = true

    protected open suspend fun canPlayerStart(playerData: PlayerData, objective: T): Boolean = true

    protected abstract suspend fun onSuccess(session: VoteSession<T>)

    protected abstract suspend fun getSessionDetails(session: VoteSession<T>): String

    protected suspend fun vote(playerData: PlayerData, vote: Boolean): Boolean {
        sessionMutex.withLock {
            if (session == null) {
                playerData.player.sendMessage("[#ff0000]No '$name' vote is in progress.")

                return false
            }

            if (!canPlayerVote(playerData, session!!)) {
                playerData.player.sendMessage("[#ff0000]You cannot vote in '$name' vote.")

                return false
            }

            session!!.voted[playerData.player] = vote

            Call.sendMessage(
                """
                [#00ff00]'${playerData.player.plainName()}/${playerData.mindustryUserID}' voted ${vote.toDisplayString()} for '$name' vote.
                ${session!!.votes}/${getRequiredVotes()} votes are required.
                """.trimIndent()
            )
        }

        checkIsRequiredVoteReached()

        return true
    }

    protected open suspend fun cancel(playerData: PlayerData): Boolean {
        sessionMutex.withLock {
            if (session == null) {
                playerData.player.sendMessage("[#ff0000]No '$name' vote is in progress.")

                return false
            }

            Call.sendMessage("[#ff0000]The '$name' vote cancelled by '${playerData.player.plainName()}/${playerData.mindustryUserID}'.")

            cleanUp()

            return true
        }
    }

    protected open suspend fun timeout() {
        Call.sendMessage("[#ff0000]The '$name' vote timed out.")

        sessionMutex.withLock {
            cleanUp()
        }
    }

    /**
     * Must be called with locked sessionMutex
     */
    private fun cleanUp() {
        session?.task?.cancel()

        session = null
    }

    /**
     * Must be called with locked sessionMutex
     */
    protected suspend fun silentCancel() {
        sessionMutex.withLock {
        }
    }

    protected suspend fun checkIsRequiredVoteReached() {
        sessionMutex.withLock {
            if (session == null) throw IllegalStateException("Cannot check votes when session is null")

            if (session!!.votes >= getRequiredVotes()) {
                Call.sendMessage("[#00ff00]The '$name' vote succeeded.")

                onSuccess(session!!)

                cleanUp()
            }
        }
    }

    @EventHandler
    suspend fun onPlayerDisconnected(event: PlayerDisconnected) {
        playersLastStartTime.remove(event.player)

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

    @EventHandler
    suspend fun onPlay(event: PlayEvent) {
        silentCancel()
    }
}