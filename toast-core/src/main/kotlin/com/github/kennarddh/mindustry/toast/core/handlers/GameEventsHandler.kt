package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.extensions.stripFooMessageInvisibleCharacters
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerChatGameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerJoinGameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerLeaveGameEvent
import com.github.kennarddh.mindustry.toast.common.preventDiscordPings
import com.github.kennarddh.mindustry.toast.common.stripColors
import com.github.kennarddh.mindustry.toast.common.stripGlyphs
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kotlinx.datetime.Clock
import mindustry.game.EventType

class GameEventsHandler : Handler {
    @EventHandler
    suspend fun onPlayerJoin(event: EventType.PlayerJoin) {
        val player = event.player

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.player.join",
            GameEvent(
                ToastVars.server,
                Clock.System.now(),
                PlayerJoinGameEvent(player.plainName(), player.uuid())
            )
        )
    }

    @EventHandler
    suspend fun onPlayerLeave(event: EventType.PlayerLeave) {
        val player = event.player

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.player.leave",
            GameEvent(
                ToastVars.server,
                Clock.System.now(),
                PlayerLeaveGameEvent(player.plainName(), player.uuid())
            )
        )
    }

    @EventHandler
    suspend fun onPlayerChat(event: EventType.PlayerChatEvent) {
        val player = event.player

        // Ignore client command
        if (event.message.startsWith(Genesis.commandRegistry.clientPrefix)) return

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.player.chat",
            GameEvent(
                ToastVars.server, Clock.System.now(),
                PlayerChatGameEvent(
                    player.plainName(), player.uuid(),
                    event.message
                        .stripFooMessageInvisibleCharacters()
                        .stripGlyphs()
                        .stripColors()
                        .preventDiscordPings()
                )
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