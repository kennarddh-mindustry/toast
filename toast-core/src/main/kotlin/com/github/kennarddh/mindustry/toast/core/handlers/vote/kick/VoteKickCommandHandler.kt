package com.github.kennarddh.mindustry.toast.core.handlers.vote.kick

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.parameters.Vararg
import com.github.kennarddh.mindustry.genesis.core.commands.senders.PlayerCommandSender
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.standard.extensions.kickWithoutLogging
import com.github.kennarddh.mindustry.toast.common.PunishmentType
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.UserPunishments
import com.github.kennarddh.mindustry.toast.common.database.tables.UserVoteKickVotes
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.extensions.toDisplayString
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerPunishedGameEvent
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
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

    @Command(["votekick", "vote_kick", "vk"])
    @Description("Start a 'vote kick' vote.")
    suspend fun startVoteKick(sender: PlayerCommandSender, target: Player, @Vararg reason: String) {
        start(sender.player, VoteKickVoteObjective(target, reason))
    }

    @Command(["vote", "vote_kick_vote", "vkv"])
    @Description("Vote for 'vote kick' vote.")
    suspend fun voteCommand(sender: PlayerCommandSender, vote: Boolean) {
        vote(sender.player, vote)
    }

    @Command(["vote_cancel", "vote_kick_cancel", "votekick_cancel", "vkc"])
    @MinimumRole(UserRole.Mod)
    @Description("Cancel a 'vote kick' vote.")
    suspend fun cancelCommand(sender: PlayerCommandSender) {
        cancel(sender.player)
    }

    override fun getRequiredVotes(): Int = if (Entities.players.size <= 3) 2 else 3

    override fun canPlayerStart(player: Player, objective: VoteKickVoteObjective): Boolean {
        if (Entities.players.size < 3) {
            player.sendMessage("[#ff0000]Minimum of 3 players to start a '$name' vote.")

            return false
        }

        if (player == objective.target) {
            player.sendMessage("[#ff0000]Cannot start a '$name' vote against yourself.")

            return false
        }

        val playerData = player.safeGetPlayerData() ?: return false
        val targetPlayerData = objective.target.safeGetPlayerData() ?: return false

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

        val punishmentID = Database.newTransaction {
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

            punishmentID
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

    override suspend fun getSessionDetails(session: VoteSession<VoteKickVoteObjective>): String {
        return """
            Voting to kick '${session.objective.target.plainName()}' with the reason '${session.objective.reason}'
            Type [accent]/vote y[] or [accent]/vote n[] to vote.
        """.trimIndent()
    }
}