package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.extensions.stripFooMessageInvisibleCharacters
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.*
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kotlinx.datetime.Clock
import mindustry.game.EventType

class GameEventsHandler : Handler() {
    override suspend fun onDispose() {
        Messenger.publishGameEvent(
            "${ToastVars.server.name}.stop",
            GameEvent(
                ToastVars.server, Clock.System.now().toEpochMilliseconds(),
                ServerStopGameEvent()
            )
        )
    }

    @EventHandler
    fun onPlayerJoin(event: EventType.PlayerJoin) {
        val player = event.player

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.player.join",
            GameEvent(
                ToastVars.server,
                Clock.System.now().toEpochMilliseconds(),
                PlayerJoinGameEvent(player.name, player.uuid())
            )
        )
    }

    @EventHandler
    fun onPlayerLeave(event: EventType.PlayerLeave) {
        val player = event.player

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.player.leave",
            GameEvent(
                ToastVars.server,
                Clock.System.now().toEpochMilliseconds(),
                PlayerLeaveGameEvent(player.name, player.uuid())
            )
        )
    }

    @EventHandler
    fun onPlayerChat(event: EventType.PlayerChatEvent) {
        val player = event.player

        // Ignore client command
        if (event.message.startsWith(GenesisAPI.commandRegistry.clientPrefix)) return

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.player.chat",
            GameEvent(
                ToastVars.server, Clock.System.now().toEpochMilliseconds(),
                PlayerChatGameEvent(player.name, player.uuid(), event.message.stripFooMessageInvisibleCharacters())
            )
        )
    }

//    @EventHandler
//    @EventHandlerTrigger(Trigger.update)
//    fun onUpdate() {
//        runOnMindustryThread {
//            Groups.build.each { building ->
//                if (building is GenericCrafterBuild) {
//                    val items = mutableListOf<ItemStack>()
//                    val liquids = mutableListOf<LiquidStack>()
//
//                    building.block.consumers.forEach {
//                        if (it is ConsumeItems) {
//                            items.addAll(it.items)
//                        } else if (it is ConsumeLiquid) {
//                            liquids.add(LiquidStack(it.liquid, it.amount))
//                        } else if (it is ConsumeLiquids) {
//                            liquids.addAll(it.liquids)
//                        }
//                    }
//
//                    items.forEach {
//                        building.items.set(it.item, building.block.itemCapacity * 10)
//                    }
//
//                    liquids.forEach {
//                        building.liquids.set(it.liquid, building.block.liquidCapacity * 10)
//                    }
//                }
//            }
//        }
//    }
}