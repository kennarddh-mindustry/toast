package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.toast.core.commons.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.core.commons.database.tables.MindustryUserServerData
import kennarddh.genesis.core.events.annotations.EventHandler
import kennarddh.genesis.core.handlers.Handler
import kennarddh.genesis.core.timers.annotations.TimerTask
import mindustry.game.EventType
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class UserXPHandler : Handler() {
    // Constants
    private val minActionsPerWindowTimeToGetXP: Int = 30
    private val xpPerWindowTime: Int = 10

    private val playersActionsCounter: MutableMap<Player, Int> = mutableMapOf()

    // Unsaved xp
    private val playersXPDelta: MutableMap<Player, Int> = mutableMapOf()

    @TimerTask(0f, 10f)
    private fun saveXPDelta() {
        playersActionsCounter.forEach {
            // It's like this for easier change if later xp can be incremented in other places
            if (it.value >= minActionsPerWindowTimeToGetXP)
                playersXPDelta[it.key] = playersXPDelta[it.key]!! + xpPerWindowTime

            val xpDelta = playersXPDelta[it.key]!!

            if (xpDelta != 0) {
                transaction {
                    MindustryUserServerData.join(
                        MindustryUser,
                        JoinType.INNER,
                        onColumn = MindustryUserServerData.mindustryUserID,
                        otherColumn = MindustryUser.id
                    ).update({ MindustryUser.mindustryUUID eq it.key.uuid() }) {
                        with(SqlExpressionBuilder) {
                            it[MindustryUserServerData.xp] = MindustryUserServerData.xp + xpDelta
                        }
                    }

                    playersActionsCounter[it.key] = 0
                    playersXPDelta[it.key] = 0
                }
            }
        }
    }

    private fun incrementActionsCounter(player: Player, value: Int) {
        playersActionsCounter[player] = playersActionsCounter[player]!! + value
    }

    @EventHandler
    private fun onPlayerJoin(event: EventType.PlayerJoin) {
        playersActionsCounter[event.player] = 0
        playersXPDelta[event.player] = 0
    }

    @EventHandler
    private fun onPlayerLeave(event: EventType.PlayerLeave) {
        playersActionsCounter.remove(event.player)
        playersXPDelta.remove(event.player)
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