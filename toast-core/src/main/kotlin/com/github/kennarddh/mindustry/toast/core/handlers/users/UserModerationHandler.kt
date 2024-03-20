package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ServerSide
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResultStatus
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.commons.priority.Priority
import com.github.kennarddh.mindustry.genesis.core.filters.FilterType
import com.github.kennarddh.mindustry.genesis.core.filters.annotations.Filter
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.extensions.kickWithoutLogging
import com.github.kennarddh.mindustry.toast.common.Permission
import com.github.kennarddh.mindustry.toast.common.PunishmentType
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.UserPunishments
import com.github.kennarddh.mindustry.toast.common.extensions.toDisplayString
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerPunishedGameEvent
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.entities.PlayerData
import com.github.kennarddh.mindustry.toast.core.commons.safeGetPlayerData
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mindustry.gen.Player
import org.jetbrains.exposed.sql.insertAndGetId
import kotlin.time.Duration

class UserModerationHandler : Handler {
    override suspend fun onInit() {
        Genesis.commandRegistry.removeCommand("kick")
        Genesis.commandRegistry.removeCommand("ban")
        Genesis.commandRegistry.removeCommand("bans")
        Genesis.commandRegistry.removeCommand("unban")
        Genesis.commandRegistry.removeCommand("pardon")
        Genesis.commandRegistry.removeCommand("subnet-ban")
    }

    @Filter(FilterType.Chat, Priority.Important)
    fun chatFilter(player: Player, message: String): String? {
        val playerData = player.safeGetPlayerData() ?: return null

        if (playerData.fullPermissions.contains(Permission.Chat)) return message

        player.sendMessage("[scarlet]You are not allowed to chat.")

        return null
    }

    @Command(["kick"])
    @MinimumRole(UserRole.Mod)
    @ClientSide
    @ServerSide
    @Description("Kick player.")
    suspend fun kick(player: Player? = null, target: Player, duration: Duration, reason: String): CommandResult? {
        val targetPlayerData = target.safeGetPlayerData() ?: return null

        var playerData: PlayerData? = null

        if (player != null) {
            val playerDataNonNull = player.safeGetPlayerData() ?: return null

            val playerRole = playerDataNonNull.role
            val targetRole = targetPlayerData.role

            if (
                targetPlayerData.userID != null &&
                playerDataNonNull.userID != null &&
                playerRole != null &&
                targetRole != null &&
                playerRole > targetRole
            )
                return CommandResult(
                    "Your role must be higher than target's role to kick.",
                    CommandResultStatus.Failed
                )

            playerData = playerDataNonNull
        }

        val playerMindustryUserID = playerData?.mindustryUserID
        val playerUserID = playerData?.userID
        val targetUserID = targetPlayerData.userID

        return Database.newTransaction {
            val punishmentID = UserPunishments.insertAndGetId {
                it[this.server] = ToastVars.server
                it[this.reason] = reason
                it[this.endAt] = Clock.System.now().plus(duration).toLocalDateTime(TimeZone.UTC)
                it[this.type] = PunishmentType.Kick

                if (playerMindustryUserID != null)
                    it[this.mindustryUserID] = playerMindustryUserID

                it[this.targetMindustryUserID] = targetPlayerData.mindustryUserID

                if (playerUserID != null)
                    it[this.userID] = playerUserID

                if (targetUserID != null)
                    it[this.targetUserID] = targetUserID
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
                            player?.plainName() ?: "Server",
                            target.name
                        )
                    )
                )
            }

            CommandResult("Successfully kicked ${target.plainName()}/${target.uuid()} for $duration with the reason \"$reason\"")
        }
    }

    @Command(["ban"])
    @MinimumRole(UserRole.Admin)
    @ClientSide
    @ServerSide
    @Description("Ban player.")
    suspend fun ban(player: Player? = null, target: Player, reason: String): CommandResult? {
        val targetPlayerData = target.safeGetPlayerData() ?: return null

        var playerData: PlayerData? = null

        if (player != null) {
            val playerDataNonNull = player.safeGetPlayerData() ?: return null

            val playerRole = playerDataNonNull.role
            val targetRole = targetPlayerData.role

            if (
                targetPlayerData.userID != null &&
                playerDataNonNull.userID != null &&
                playerRole != null &&
                targetRole != null &&
                playerRole > targetRole
            )
                return CommandResult(
                    "Your role must be higher than target's role to ban.",
                    CommandResultStatus.Failed
                )

            playerData = playerDataNonNull
        }

        val playerMindustryUserID = playerData?.mindustryUserID
        val playerUserID = playerData?.userID
        val targetUserID = targetPlayerData.userID

        return Database.newTransaction {
            val punishmentID = UserPunishments.insertAndGetId {
                it[this.server] = ToastVars.server
                it[this.reason] = reason
                it[this.type] = PunishmentType.Ban

                if (playerMindustryUserID != null)
                    it[this.mindustryUserID] = playerMindustryUserID

                it[this.targetMindustryUserID] = targetPlayerData.mindustryUserID

                if (playerUserID != null)
                    it[this.userID] = playerUserID

                if (targetUserID != null)
                    it[this.targetUserID] = targetUserID
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
                            player?.plainName() ?: "Server",
                            target.name
                        )
                    )
                )
            }

            CommandResult("Successfully banned ${target.plainName()}/${target.uuid()} with the reason \"$reason\"")
        }
    }
}