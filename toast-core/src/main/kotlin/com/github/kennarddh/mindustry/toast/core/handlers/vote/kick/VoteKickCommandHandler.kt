package com.github.kennarddh.mindustry.toast.core.handlers.vote.kick

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
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
import com.github.kennarddh.mindustry.toast.core.commons.safeGetPlayerData
import com.github.kennarddh.mindustry.toast.core.handlers.vote.AbstractVoteCommand
import com.github.kennarddh.mindustry.toast.core.handlers.vote.VoteSession
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class VoteKickCommandHandler : AbstractVoteCommand<VoteKickVoteObjective>("vote kick", 1.minutes) {
    override suspend fun onInit() {
        Genesis.commandRegistry.removeCommand("votekick")
        Genesis.commandRegistry.removeCommand("vote")
    }

    @Command(["votekick", "vote-kick"])
    @ClientSide
    suspend fun startVoteKick(player: Player, target: Player, reason: String): CommandResult? {
        if (Groups.player.size() < 3) CommandResult("There must be more than or equal to 3 players to start $name vote.")

        start(player, VoteKickVoteObjective(target, reason))

        return null
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

    override fun canPlayerStart(player: Player, session: VoteKickVoteObjective): Boolean {
        val playerData = player.safeGetPlayerData() ?: return false
        val targetPlayerData = session.target.safeGetPlayerData() ?: return false

        // If the player is public it's equivalent to UserRole.Player role
        val playerComputedRole = playerData.role ?: UserRole.Player
        val targetComputedRole = targetPlayerData.role ?: UserRole.Player

        if (playerComputedRole < targetComputedRole) {
            player.sendMessage("[#ff0000]Your role must be higher than target's role to vote kick them.")

            return false
        }

        return true
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

        Call.sendMessage("[#00ff00]Vote kick success. Kicked ${session.objective.target.plainName()} for ${duration.toDisplayString()}.")

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
                            session.initiator.plainName(),
                            session.objective.target.plainName()
                        )
                    )
                )
            }
        }
    }

    override suspend fun getSessionDetails(session: VoteSession<VoteKickVoteObjective>): String {
        return "Type [accent]/vote y[] or [accent]/vote n[] to vote."
    }
}