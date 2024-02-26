package com.github.kennarddh.mindustry.toast.core.handlers.users

import arc.util.Strings
import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ServerSide
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResultStatus
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.extensions.kickWithoutLogging
import com.github.kennarddh.mindustry.toast.common.PunishmentType
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.UserPunishments
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerPunishedGameEvent
import com.github.kennarddh.mindustry.toast.common.toDisplayString
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.getMindustryUser
import com.github.kennarddh.mindustry.toast.core.commons.getUserAndMindustryUserAndUserServerData
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mindustry.gen.Player
import org.jetbrains.exposed.sql.insertAndGetId
import kotlin.time.Duration

class UserModerationHandler : Handler() {
    override suspend fun onInit() {
        GenesisAPI.commandRegistry.removeCommand("kick")
        GenesisAPI.commandRegistry.removeCommand("ban")
        GenesisAPI.commandRegistry.removeCommand("bans")
        GenesisAPI.commandRegistry.removeCommand("unban")
        GenesisAPI.commandRegistry.removeCommand("pardon")
        GenesisAPI.commandRegistry.removeCommand("subnet-ban")
    }

    @Command(["kick"])
    @MinimumRole(UserRole.Mod)
    @ClientSide
    @ServerSide
    @Description("Kick player.")
    suspend fun kick(player: Player? = null, target: Player, duration: Duration, reason: String): CommandResult {
        return Database.newTransaction {
            val mindustryUser = player?.getMindustryUser()
            val targetMindustryUser = target.getMindustryUser()!!
            val user = player?.getUserAndMindustryUserAndUserServerData()
            val targetUser = target.getUserAndMindustryUserAndUserServerData()

            if (targetUser != null && user != null && user[Users.role] > targetUser[Users.role])
                return@newTransaction CommandResult(
                    "Your role must be higher than target's role to kick.",
                    CommandResultStatus.Failed
                )

            val punishmentID = UserPunishments.insertAndGetId {
                it[this.server] = ToastVars.server
                it[this.reason] = reason
                it[this.endAt] =
                    Clock.System.now().plus(duration).toLocalDateTime(TimeZone.UTC)
                it[this.type] = PunishmentType.Kick

                if (mindustryUser != null)
                    it[this.mindustryUserID] = mindustryUser[MindustryUser.id]

                it[this.targetMindustryUserID] = targetMindustryUser[MindustryUser.id]

                if (user != null)
                    it[this.userID] = user[Users.id]

                if (targetUser != null)
                    it[this.targetUserID] = targetUser[Users.id]
            }

            Logger.info("${if (player == null) "Server" else player.name} kicked ${target.name}/${target.uuid()} for $duration with the reason \"$reason\"")

            target.kickWithoutLogging(
                """
                [#ff0000]You were kicked for the reason
                []$reason
                [#00ff00]You can join again in ${duration.toDisplayString()}.
                [#00ff00]Appeal in Discord.
                """.trimIndent()
            )

            CoroutineScopes.Main.launch {
                Messenger.publishGameEvent(
                    "${ToastVars.server.name}.punishment.kick",
                    GameEvent(
                        ToastVars.server,
                        Clock.System.now(),
                        PlayerPunishedGameEvent(
                            punishmentID.value,
                            Strings.stripColors(player?.name ?: "Server"),
                            target.name
                        )
                    )
                )
            }

            CommandResult("Successfully kicked ${target.name}/${target.uuid()} for $duration with the reason \"$reason\"")
        }
    }

    @Command(["ban"])
    @MinimumRole(UserRole.Admin)
    @ClientSide
    @ServerSide
    @Description("Ban player.")
    suspend fun ban(player: Player? = null, target: Player, reason: String): CommandResult {
        return Database.newTransaction {
            val mindustryUser = player?.getMindustryUser()
            val targetMindustryUser = target.getMindustryUser()!!
            val user = player?.getUserAndMindustryUserAndUserServerData()
            val targetUser = target.getUserAndMindustryUserAndUserServerData()

            if (targetUser != null && user != null && user[Users.role] > targetUser[Users.role])
                return@newTransaction CommandResult(
                    "Your role must be higher than target's role to kick.",
                    CommandResultStatus.Failed
                )

            val punishmentID = UserPunishments.insertAndGetId {
                it[this.server] = ToastVars.server
                it[this.reason] = reason
                it[this.type] = PunishmentType.Ban

                if (mindustryUser != null)
                    it[this.mindustryUserID] = mindustryUser[MindustryUser.id]

                it[this.targetMindustryUserID] = targetMindustryUser[MindustryUser.id]

                if (user != null)
                    it[this.userID] = user[Users.id]

                if (targetUser != null)
                    it[this.targetUserID] = targetUser[Users.id]
            }

            Logger.info("${if (player == null) "Server" else player.name} banned ${target.name}/${target.uuid()} for $duration with the reason \"$reason\"")

            target.kickWithoutLogging(
                """
                [#ff0000]You were banned for the reason
                []$reason
                [#00ff00]Appeal in Discord.
                """.trimIndent()
            )

            CoroutineScopes.Main.launch {
                Messenger.publishGameEvent(
                    "${ToastVars.server.name}.punishment.ban",
                    GameEvent(
                        ToastVars.server,
                        Clock.System.now(),
                        PlayerPunishedGameEvent(
                            punishmentID.value,
                            Strings.stripColors(player?.name ?: "Server"),
                            target.name
                        )
                    )
                )
            }

            CommandResult("Successfully banned ${target.name}/${target.uuid()} with the reason \"$reason\"")
        }
    }
}