package com.github.kennarddh.mindustry.toast.core.handlers.users

import arc.util.Align
import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ServerSide
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResultStatus
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.timers.annotations.TimerTask
import com.github.kennarddh.mindustry.genesis.standard.extensions.infoPopup
import com.github.kennarddh.mindustry.toast.common.*
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.getMindustryUserAndUserServerData
import com.github.kennarddh.mindustry.toast.core.commons.mindustryServerUserDataWhereClause
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mindustry.game.EventType
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update


class UserStatsHandler : Handler() {
    // Constants
    private val minActionsPerWindowTimeToGetXP: Int = 30
    private val xpPerWindowTime: Int = 10

    private val playersActionsCounter: MutableMap<Player, Int> = mutableMapOf()

    // Unsaved xp
    private val playersXPDelta: MutableMap<Player, Int> = mutableMapOf()

    private val playersLastPlayTimeSave: MutableMap<Player, Instant> = mutableMapOf()

    @TimerTask(0f, 10f)
    private suspend fun savePlayerDelta() {
        GenesisAPI.getHandler<UserAccountHandler>()!!.users.forEach {
            val player = it.key

            if (!playersActionsCounter.containsKey(player)) return
            if (!playersLastPlayTimeSave.containsKey(player)) return

            val playerActionsCount = playersActionsCounter[player]!!
            val lastPlayTimeSave = playersLastPlayTimeSave[player]!!

            val now = Clock.System.now()
            val playTimeChanges = now - lastPlayTimeSave

            // It's like this for easier change if later xp can be incremented in other places
            if (playerActionsCount >= minActionsPerWindowTimeToGetXP)
                playersXPDelta[player] = playersXPDelta[player]!! + xpPerWindowTime

            val xpDelta = playersXPDelta[player]!!
            val isPlayerActive = xpDelta > 0

            Database.newTransaction {
                // TODO: Only update once even if the player is in multiple servers. Use redis with set ttl to lock which server will generate xp for each player
                MindustryUserServerData.join(
                    MindustryUser,
                    JoinType.INNER,
                    onColumn = MindustryUserServerData.mindustryUserID,
                    otherColumn = MindustryUser.id
                ).update({ player.mindustryServerUserDataWhereClause }) {
                    with(SqlExpressionBuilder) {
                        it[MindustryUserServerData.xp] = MindustryUserServerData.xp + xpDelta
                        it[MindustryUserServerData.playTime] = MindustryUserServerData.playTime + playTimeChanges

                        if (isPlayerActive)
                            it[MindustryUserServerData.activePlayTime] =
                                MindustryUserServerData.activePlayTime + playTimeChanges
                    }
                }

                updateStatsPopup(player)

                playersActionsCounter[player] = 0
                playersXPDelta[player] = 0
                playersLastPlayTimeSave[player] = now
            }
        }
    }

    private suspend fun updateStatsPopup(player: Player) {
        Database.newTransaction {
            val mindustryUserServerData = player.getMindustryUserAndUserServerData()!!

            val xp = mindustryUserServerData[MindustryUserServerData.xp]
            val playTime = mindustryUserServerData[MindustryUserServerData.playTime]

            player.infoPopup(
                """
                XP: $xp
                Rank: ${UserRank.getRank(xp)}
                Play Time: ${playTime.toDisplayString()}
                """.trimIndent(),
                10.5f, Align.topRight, 200, 0, 0, 10
            )
        }
    }

    private fun incrementActionsCounter(player: Player, value: Int) {
        playersActionsCounter[player] = playersActionsCounter[player]!! + value
    }

    @EventHandler
    private fun onPlayerJoin(event: EventType.PlayerJoin) {
        playersActionsCounter[event.player] = 0
        playersXPDelta[event.player] = 0

        playersLastPlayTimeSave[event.player] = Clock.System.now()
    }

    @EventHandler
    private fun onPlayerLeave(event: EventType.PlayerLeave) {
        playersActionsCounter.remove(event.player)
        playersXPDelta.remove(event.player)

        playersLastPlayTimeSave.remove(event.player)
    }

    /**
     * This also get triggered when block destroy
     */
    @EventHandler
    private fun onBlockBuildEndEvent(event: EventType.BlockBuildEndEvent) {
        if (event.unit.player != null)
            incrementActionsCounter(event.unit.player, 1)
    }

    @EventHandler
    private fun onPlayerChatEvent(event: EventType.PlayerChatEvent) {
        incrementActionsCounter(event.player, 1)
    }

    enum class XPCommandType {
        get, set, add, remove
    }

    @Command(["xp"])
    @MinimumRole(UserRole.Admin)
    @ClientSide
    @ServerSide
    suspend fun xp(
        player: Player? = null,
        target: Player,
        server: Server? = null,
        type: XPCommandType,
        value: Int? = null
    ): CommandResult {
        val computedServer = server ?: ToastVars.server

        return Database.newTransaction {
            when (type) {
                XPCommandType.get -> {
                    val mindustryUserServerData = MindustryUserServerData
                        .join(
                            MindustryUser,
                            JoinType.INNER,
                            onColumn = MindustryUserServerData.mindustryUserID,
                            otherColumn = MindustryUser.id
                        )
                        .selectOne {
                            (MindustryUser.mindustryUUID eq target.uuid()) and (MindustryUserServerData.server eq computedServer)
                        }

                    if (mindustryUserServerData == null) {
                        CommandResult("${target.name} doesn't have xp in ${computedServer.displayName} server.")
                    } else {
                        CommandResult("${target.name} has ${mindustryUserServerData[MindustryUserServerData.xp]} xp.")
                    }
                }

                XPCommandType.add -> {
                    if (value == null) return@newTransaction CommandResult(
                        "Value cannot be empty for add subcommand",
                        CommandResultStatus.Failed
                    )

                    val updatedCount = MindustryUserServerData.join(
                        MindustryUser,
                        JoinType.INNER,
                        onColumn = MindustryUserServerData.mindustryUserID,
                        otherColumn = MindustryUser.id
                    ).update({
                        (MindustryUser.mindustryUUID eq target.uuid()) and (MindustryUserServerData.server eq computedServer)
                    }) {
                        with(SqlExpressionBuilder) {
                            it[MindustryUserServerData.xp] = MindustryUserServerData.xp + value
                        }
                    }

                    if (updatedCount == 0) {
                        CommandResult("${target.name} doesn't have xp in ${computedServer.displayName} server.")
                    } else {
                        CommandResult("Added $value xp to ${target.name}.")
                    }
                }

                XPCommandType.set -> {
                    if (value == null) return@newTransaction CommandResult(
                        "Value cannot be empty for set subcommand",
                        CommandResultStatus.Failed
                    )

                    val updatedCount = MindustryUserServerData.join(
                        MindustryUser,
                        JoinType.INNER,
                        onColumn = MindustryUserServerData.mindustryUserID,
                        otherColumn = MindustryUser.id
                    ).update({
                        (MindustryUser.mindustryUUID eq target.uuid()) and (MindustryUserServerData.server eq computedServer)
                    }) {
                        it[MindustryUserServerData.xp] = value
                    }

                    if (updatedCount == 0) {
                        CommandResult("${target.name} doesn't have xp in ${computedServer.displayName} server.")
                    } else {
                        CommandResult("Set ${target.name} xp to $value.")
                    }
                }

                XPCommandType.remove -> {
                    if (value == null) return@newTransaction CommandResult(
                        "Value cannot be empty for remove subcommand",
                        CommandResultStatus.Failed
                    )

                    val updatedCount = MindustryUserServerData.join(
                        MindustryUser,
                        JoinType.INNER,
                        onColumn = MindustryUserServerData.mindustryUserID,
                        otherColumn = MindustryUser.id
                    ).update({
                        (MindustryUser.mindustryUUID eq target.uuid()) and (MindustryUserServerData.server eq computedServer)
                    }) {
                        with(SqlExpressionBuilder) {
                            it[MindustryUserServerData.xp] = MindustryUserServerData.xp - value
                        }
                    }

                    if (updatedCount == 0) {
                        CommandResult("${target.name} doesn't have xp in ${computedServer.displayName} server.")
                    } else {
                        CommandResult("Removed $value xp from ${target.name}.")
                    }
                }
            }
        }
    }
}