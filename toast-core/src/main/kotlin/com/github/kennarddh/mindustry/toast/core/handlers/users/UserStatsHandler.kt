package com.github.kennarddh.mindustry.toast.core.handlers.users

import arc.util.Align
import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.timers.annotations.TimerTask
import com.github.kennarddh.mindustry.genesis.standard.extensions.infoPopup
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import com.github.kennarddh.mindustry.toast.common.toDisplayString
import com.github.kennarddh.mindustry.toast.core.commons.getMindustryUserAndUserServerData
import com.github.kennarddh.mindustry.toast.core.commons.mindustryServerUserDataWhereClause
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mindustry.game.EventType
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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

            newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
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
        newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
            val mindustryUserServerData = player.getMindustryUserAndUserServerData()!!

            val xp = mindustryUserServerData[MindustryUserServerData.xp]
            val playTime = mindustryUserServerData[MindustryUserServerData.playTime]

            player.infoPopup(
                "XP: ${xp}\nRank: Duo 1\nPlay Time: ${playTime.toDisplayString()}",
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
}