package com.github.kennarddh.mindustry.toast.core.handlers

import arc.util.Strings
import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.standard.extensions.kickWithoutLogging
import com.github.kennarddh.mindustry.toast.common.PunishmentType
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.UserPunishments
import com.github.kennarddh.mindustry.toast.common.database.tables.UserVoteKickVotes
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerPunishedGameEvent
import com.github.kennarddh.mindustry.toast.common.toDisplayString
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.getMindustryUser
import com.github.kennarddh.mindustry.toast.core.commons.getUserAndMindustryUserAndUserServerData
import com.github.kennarddh.mindustry.toast.core.handlers.vote.AbstractVoteCommand
import com.github.kennarddh.mindustry.toast.core.handlers.vote.VoteSession
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mindustry.gen.Call
import mindustry.gen.Player
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class VoteKickCommandHandler : AbstractVoteCommand<VoteKickVoteObjective>("vote kick", 1.minutes) {
    override suspend fun onInit() {
        GenesisAPI.commandRegistry.removeCommand("votekick")
        GenesisAPI.commandRegistry.removeCommand("vote")
    }

    @Command(["votekick", "voteKick"])
    @ClientSide
    fun startVoteKick(player: Player, target: Player, reason: String) {
        start(player, VoteKickVoteObjective(target, reason))
    }

    @Command(["vote"])
    @ClientSide
    suspend fun voteCommand(player: Player, vote: Boolean) {
        vote(player, vote)
    }

    @Command(["vote-cancel", "vote-kick-cancel", "votekick-cancel"])
    @ClientSide
    @MinimumRole(UserRole.Mod)
    suspend fun cancelCommand(player: Player) {
        cancel(player)
    }

    override suspend fun onSuccess(session: VoteSession<VoteKickVoteObjective>) {
        val duration = 5.hours

        session.objective.target.kickWithoutLogging(
            """
            [#ff0000]You were vote kicked with the reason
            []${session.objective.reason}
            [#00ff00]You can join again in ${duration.toDisplayString()}.
            [#00ff00]Appeal in Discord.
            """.trimIndent()
        )

        Call.sendMessage("Vote kick success. Kicked ${session.objective.target.name} for ${duration.toDisplayString()}.")

        Database.newTransaction {
            val mindustryUser = session.initiator.getMindustryUser()!!
            val targetMindustryUser = session.objective.target.getMindustryUser()!!
            val user = session.initiator.getUserAndMindustryUserAndUserServerData()
            val targetUser = session.objective.target.getUserAndMindustryUserAndUserServerData()

            val punishmentID = UserPunishments.insertAndGetId {
                it[this.server] = ToastVars.server
                it[this.reason] = session.objective.reason
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

            session.voted.forEach { voter ->
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
                    "${ToastVars.server.name}.punishment.vote-kick",
                    GameEvent(
                        ToastVars.server,
                        Clock.System.now(),
                        PlayerPunishedGameEvent(
                            punishmentID.value,
                            Strings.stripColors(session.initiator.name),
                            Strings.stripColors(session.objective.target.name)
                        )
                    )
                )
            }
        }
    }
}