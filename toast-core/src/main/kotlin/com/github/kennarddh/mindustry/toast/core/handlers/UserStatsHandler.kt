package com.github.kennarddh.mindustry.toast.core.handlers

import arc.util.Align
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.timers.annotations.TimerTask
import com.github.kennarddh.mindustry.genesis.standard.extensions.infoPopup
import com.github.kennarddh.mindustry.toast.common.CoroutineScopes
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import com.github.kennarddh.mindustry.toast.common.selectOne
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kotlinx.coroutines.launch
import mindustry.game.EventType
import mindustry.gen.Groups
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant


class UserStatsHandler : Handler() {
    // Constants
    private val minActionsPerWindowTimeToGetXP: Int = 30
    private val xpPerWindowTime: Int = 10

    private val playersActionsCounter: MutableMap<Player, Int> = mutableMapOf()

    // Unsaved xp
    private val playersXPDelta: MutableMap<Player, Int> = mutableMapOf()

    private val playersLastPlayTimeSave: MutableMap<Player, Long> = mutableMapOf()

    @TimerTask(0f, 10f)
    private fun savePlayerDelta() {
        Groups.player.forEach {
            val playerActionsCount = playersActionsCounter[it]!!
            val lastPlayTimeSave = playersLastPlayTimeSave[it]!!

            val now = Instant.now().toEpochMilli()
            val playTimeChanges = now - lastPlayTimeSave

            // It's like this for easier change if later xp can be incremented in other places
            if (playerActionsCount >= minActionsPerWindowTimeToGetXP)
                playersXPDelta[it] = playersXPDelta[it]!! + xpPerWindowTime

            val xpDelta = playersXPDelta[it]!!
            val isPlayerActive = xpDelta > 0

            CoroutineScopes.Main.launch {
                newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
                    // TODO: Only update once even if the player is in multiple servers. Use redis with set ttl to lock which server will generate xp for each player
                    MindustryUserServerData.join(
                        MindustryUser,
                        JoinType.INNER,
                        onColumn = MindustryUserServerData.mindustryUserID,
                        otherColumn = MindustryUser.id
                    ).update({
                        MindustryUser.mindustryUUID eq it.uuid()
                        MindustryUserServerData.server eq ToastVars.server
                    }) {
                        with(SqlExpressionBuilder) {
                            it[MindustryUserServerData.xp] = MindustryUserServerData.xp + xpDelta
                            it[MindustryUserServerData.playTime] = MindustryUserServerData.playTime + playTimeChanges

                            if (isPlayerActive)
                                it[MindustryUserServerData.activePlayTime] =
                                    MindustryUserServerData.activePlayTime + playTimeChanges
                        }
                    }

                    updateStatsPopup(it)

                    playersActionsCounter[it] = 0
                    playersXPDelta[it] = 0
                    playersLastPlayTimeSave[it] = now
                }
            }
        }
    }

    private suspend fun updateStatsPopup(player: Player) {
        newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
            val mindustryUserServerData =
                MindustryUserServerData.join(
                    MindustryUser,
                    JoinType.INNER,
                    onColumn = MindustryUserServerData.mindustryUserID,
                    otherColumn = MindustryUser.id
                ).selectOne { MindustryUser.mindustryUUID eq player.uuid() }!!

            val xp = mindustryUserServerData[MindustryUserServerData.xp]
            val playTimeMillis = mindustryUserServerData[MindustryUserServerData.playTime]

            player.infoPopup(
                "XP: ${xp}\nRank: Duo 1\nPlay Time: ${playTimeMillis / 1000}s",
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

        playersLastPlayTimeSave[event.player] = Instant.now().toEpochMilli()
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