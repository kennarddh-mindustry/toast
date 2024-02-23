package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResultStatus
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.extensions.kickWithoutLogging
import com.github.kennarddh.mindustry.toast.common.PunishmentType
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.UserPunishments
import com.github.kennarddh.mindustry.toast.common.database.tables.UserVoteKickVotes
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerPunishedGameEvent
import com.github.kennarddh.mindustry.toast.common.toDisplayString
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.VoteSession
import com.github.kennarddh.mindustry.toast.core.commons.getMindustryUser
import com.github.kennarddh.mindustry.toast.core.commons.getUserAndMindustryUserAndUserServerData
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class VoteKickCommandHandler : Handler() {
    private var voteSession: VoteSession? = null
    private var target: Player? = null
    private var starter: Player? = null
    private var reason: String? = null

    override suspend fun onInit() {
        GenesisAPI.commandRegistry.removeCommand("votekick")
        GenesisAPI.commandRegistry.removeCommand("vote")
    }

    @Command(["votekick", "voteKick"])
    @ClientSide
    fun startVoteKick(player: Player, target: Player, reason: String): CommandResult? {
        if (voteSession != null) return CommandResult("Vote kick is already in progress.", CommandResultStatus.Failed)

        voteSession = VoteSession(1.minutes, player, ::getRequiredVotes, ::onSuccess, ::onTimeout, ::onCancel)
        this.target = target
        starter = player
        this.reason = reason

        Call.sendMessage("Vote kick started to kick ${target.name} with the reason \"$reason\".")

        return null
    }

    @Command(["vote"])
    @ClientSide
    suspend fun vote(player: Player, vote: Boolean) {
        Call.sendMessage("${player.name} voted ${if (vote) "yes" else "no"} to kick ${target!!.name}.")

        voteSession!!.vote(player, vote)
    }

    @Command(["voteCancel", "voteKickCancel", "votekickCancel"])
    @ClientSide
    suspend fun cancel(player: Player): CommandResult? {
        val user = player.getUserAndMindustryUserAndUserServerData()

        if (player == starter || (user != null && user[Users.role] >= UserRole.Mod)) {
            Call.sendMessage("${player.name} canceled vote kick session.")

            voteSession!!.cancel()

            return null
        } else {
            return CommandResult("Cannot cancel vote kick session as you are not the starter or mod.")
        }
    }

    private fun getRequiredVotes(): Int = Groups.player.size() / 2 + 1

    private suspend fun onSuccess() {
        val duration = 5.hours

        target!!.kickWithoutLogging(
            """
            [#ff0000]You were vote kicked with the reason
            []$reason
            [#00ff00]You can join again in ${duration.toDisplayString()}.
            [#00ff00]Appeal in Discord.
            """.trimIndent()
        )

        Call.sendMessage("Vote kick success. Kicked ${target!!.name} for ${duration.toDisplayString()}.")

        newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
            val mindustryUser = starter!!.getMindustryUser()!!
            val targetMindustryUser = target!!.getMindustryUser()!!
            val user = starter!!.getUserAndMindustryUserAndUserServerData()
            val targetUser = target!!.getUserAndMindustryUserAndUserServerData()

            val punishmentID = UserPunishments.insertAndGetId {
                it[this.server] = ToastVars.server
                it[this.reason] = reason
                it[this.endAt] =
                    Clock.System.now().plus(duration).toLocalDateTime(TimeZone.UTC)
                it[this.type] = PunishmentType.VoteKick

                it[this.mindustryUserID] = mindustryUser[MindustryUser.id]
                it[this.targetMindustryUserID] = targetMindustryUser[MindustryUser.id]

                if (user != null)
                    it[this.userID] = user[Users.id]

                if (targetUser != null)
                    it[this.targetUserID] = targetUser[Users.id]
            }

            voteSession!!.voted.forEach { voter ->
                val voterMindustryUser = voter.key.getMindustryUser()!!
                val vote = voter.value

                UserVoteKickVotes.insert {
                    it[this.vote] = vote
                    it[this.userPunishmentID] = punishmentID
                    it[this.mindustryUserID] = voterMindustryUser[MindustryUser.id]
                }
            }

            CoroutineScopes.Main.launch {
                Messenger.publishGameEvent(
                    GameEvent(
                        ToastVars.server,
                        Clock.System.now().toEpochMilliseconds(),
                        PlayerPunishedGameEvent(
                            punishmentID.value,
                            starter!!.name,
                            target!!.name
                        )
                    )
                )
            }

            voteSession = null
            target = null
            starter = null
            reason = null
        }
    }

    private fun onTimeout() {
        Call.sendMessage("Vote kick session timed out. Failed to kick ${target!!.name}.")

        voteSession = null
        target = null
        starter = null
        reason = null
    }

    private fun onCancel() {
        Call.sendMessage("Vote kick session cancelled.")

        voteSession = null
        target = null
        starter = null
        reason = null
    }

    @EventHandler
    private suspend fun onPlayerJoin(event: EventType.PlayerJoin) {
        voteSession?.onPlayerJoin(event.player)
    }

    @EventHandler
    private suspend fun onPlayerLeave(event: EventType.PlayerLeave) {
        voteSession?.onPlayerLeave(event.player)

        if (event.player == target) {
            Call.sendMessage("Vote kick target left. Vote kick session will still continue.")
        }
    }
}