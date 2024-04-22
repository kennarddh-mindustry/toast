package com.github.kennarddh.mindustry.toast.core.handlers.users

import arc.util.Align
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.senders.CommandSender
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.timers.annotations.TimerTask
import com.github.kennarddh.mindustry.genesis.standard.extensions.infoPopup
import com.github.kennarddh.mindustry.toast.common.Server
import com.github.kennarddh.mindustry.toast.common.UserRank
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import com.github.kennarddh.mindustry.toast.common.extensions.selectOne
import com.github.kennarddh.mindustry.toast.common.extensions.toDisplayString
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import com.github.kennarddh.mindustry.toast.core.commons.getMindustryUserAndUserServerData
import com.github.kennarddh.mindustry.toast.core.commons.safeGetPlayerData
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mindustry.game.EventType
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

private const val MAX_XP_PER_WINDOW_TIME: Int = 10

class UserStatsHandler : Handler {
    private val playersUnsavedXp = ConcurrentHashMap<Player, Int>()

    private val playersLastPlayTimeSave = ConcurrentHashMap<Player, Instant>()

    @TimerTask(0f, 10f)
    private suspend fun savePlayerStats() {
        Entities.players.forEach {
            val player = it.key

            val now = Clock.System.now()

            val lastPlayTimeSave = playersLastPlayTimeSave.getOrDefault(player, now)
            val playTimeChanges = now - lastPlayTimeSave

            val xpDelta = playersUnsavedXp.getOrDefault(player, 0)
            val isPlayerActive = xpDelta > 0

            Database.newTransaction {
                MindustryUserServerData.join(
                    MindustryUser,
                    JoinType.INNER,
                    onColumn = MindustryUserServerData.mindustryUserID,
                    otherColumn = MindustryUser.id
                ).update({
                    (MindustryUser.mindustryUUID eq player.uuid()) and (MindustryUserServerData.server eq ToastVars.server)
                }) {
                    with(SqlExpressionBuilder) {
                        it[MindustryUserServerData.xp] = MindustryUserServerData.xp + xpDelta
                        it[MindustryUserServerData.playTime] = MindustryUserServerData.playTime + playTimeChanges

                        if (isPlayerActive)
                            it[MindustryUserServerData.activePlayTime] =
                                MindustryUserServerData.activePlayTime + playTimeChanges
                    }
                }

                val updatedMindustryUserAndUserServerData = player.getMindustryUserAndUserServerData()

                Entities.players[player]?.xp =
                    updatedMindustryUserAndUserServerData?.get(MindustryUserServerData.xp) ?: 0
                Entities.players[player]?.playTime =
                    updatedMindustryUserAndUserServerData?.get(MindustryUserServerData.playTime) ?: Duration.ZERO

                playersLastPlayTimeSave[player] = now
                playersUnsavedXp[player] = 0
            }
        }
    }

    @TimerTask(1f, 1f)
    private fun updatePlayersStatsPopup() {
        Entities.players.keys.forEach {
            updateStatsPopup(it)
        }
    }

    private fun updateStatsPopup(player: Player) {
        val playerData = player.safeGetPlayerData() ?: return

        val now = Clock.System.now()
        val lastPlayTimeSave = playersLastPlayTimeSave.getOrDefault(player, now)
        val computedPlayTime = playerData.playTime + (now - lastPlayTimeSave)

        player.infoPopup(
            """
            XP: ${playerData.xp}
            Rank: ${UserRank.getRank(playerData.xp)}
            Play Time: ${computedPlayTime.toDisplayString()}
            """.trimIndent(),
            1f, Align.topRight, 200, 0, 0, 10
        )
    }

    private fun tryIncrementPlayerXP(player: Player, value: Int = 1) {
        val unsavedXp = playersUnsavedXp.getOrDefault(player, 0)

        // Limit xp per window time
        val coercedValue = (unsavedXp + value).coerceIn(0, MAX_XP_PER_WINDOW_TIME)

        playersUnsavedXp[player] = coercedValue

        val playerData = player.safeGetPlayerData() ?: return

        playerData.xp = playerData.xp - unsavedXp + coercedValue
    }

    @EventHandler
    private fun onPlayerDisconnected(event: PlayerDisconnected) {
        playersLastPlayTimeSave.remove(event.player)
        playersUnsavedXp.remove(event.player)
    }

    /**
     * This also get triggered when block destroy
     */
    @EventHandler
    private fun onBlockBuildEndEvent(event: EventType.BlockBuildEndEvent) {
        if (event.unit.player == null) return

        tryIncrementPlayerXP(event.unit.player)
    }

    @EventHandler
    private fun onPlayerChatEvent(event: EventType.PlayerChatEvent) {
        tryIncrementPlayerXP(event.player)
    }

    enum class XPCommandType {
        get, set, add, remove
    }

    @Command(["xp"])
    @MinimumRole(UserRole.Admin)
    @Description("Get and update player's xp. This will always return saved data.")
    suspend fun xp(
        sender: CommandSender,
        target: Player,
        server: Server? = null,
        type: XPCommandType,
        value: Int? = null
    ) {
        val computedServer = server ?: ToastVars.server

        Database.newTransaction {
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
                        return@newTransaction sender.sendError("${target.plainName()} doesn't have xp in ${computedServer.displayName} server.")
                    } else {
                        return@newTransaction sender.sendSuccess("${target.plainName()} has ${mindustryUserServerData[MindustryUserServerData.xp]} xp.")
                    }
                }

                XPCommandType.add -> {
                    if (value == null) return@newTransaction sender.sendError(
                        "Value cannot be empty for add subcommand",
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
                        return@newTransaction sender.sendError("${target.plainName()} doesn't have xp in ${computedServer.displayName} server.")
                    } else {
                        return@newTransaction sender.sendSuccess("Added $value xp to ${target.plainName()}.")
                    }
                }

                XPCommandType.set -> {
                    if (value == null) return@newTransaction sender.sendError(
                        "Value cannot be empty for set subcommand",
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
                        return@newTransaction sender.sendError("${target.plainName()} doesn't have xp in ${computedServer.displayName} server.")
                    } else {
                        return@newTransaction sender.sendSuccess("Set ${target.plainName()} xp to $value.")
                    }
                }

                XPCommandType.remove -> {
                    if (value == null) return@newTransaction sender.sendError(
                        "Value cannot be empty for remove subcommand",
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
                        return@newTransaction sender.sendError("${target.plainName()} doesn't have xp in ${computedServer.displayName} server.")
                    } else {
                        return@newTransaction sender.sendSuccess("Removed $value xp from ${target.plainName()}.")
                    }
                }
            }
        }
    }
}