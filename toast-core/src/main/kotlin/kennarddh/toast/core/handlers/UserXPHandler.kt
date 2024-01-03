package kennarddh.toast.core.handlers

import kennarddh.genesis.core.events.annotations.EventHandler
import kennarddh.genesis.core.handlers.Handler
import kennarddh.genesis.core.timers.annotations.TimerTask
import mindustry.game.EventType
import mindustry.gen.Player

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
            if (it.value >= minActionsPerWindowTimeToGetXP)
                playersXPDelta[it.key] = playersXPDelta[it.key]!! + xpPerWindowTime

            println("xp: ${playersXPDelta[it.key]}")

            playersActionsCounter[it.key] = 0
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