package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThread
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandlerTrigger
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.extensions.stripFooMessageInvisibleCharacters
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.*
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import mindustry.game.EventType
import mindustry.game.EventType.Trigger
import mindustry.gen.Groups
import mindustry.type.ItemStack
import mindustry.type.LiquidStack
import mindustry.world.blocks.production.GenericCrafter.GenericCrafterBuild
import mindustry.world.consumers.ConsumeItems
import mindustry.world.consumers.ConsumeLiquid
import mindustry.world.consumers.ConsumeLiquids
import java.time.Instant

class GameEventsHandler : Handler() {
    override suspend fun onDispose() {
        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server, Instant.now().toEpochMilli(),
                ServerStopGameEvent()
            )
        )
    }

    @EventHandler
    fun onPlayerJoin(event: EventType.PlayerJoin) {
        val player = event.player

        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server,
                Instant.now().toEpochMilli(),
                PlayerJoinGameEvent(player.name, player.uuid())
            )
        )
    }

    @EventHandler
    fun onPlayerLeave(event: EventType.PlayerLeave) {
        val player = event.player

        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server,
                Instant.now().toEpochMilli(),
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
            GameEvent(
                ToastVars.server, Instant.now().toEpochMilli(),
                PlayerChatGameEvent(player.name, player.uuid(), event.message.stripFooMessageInvisibleCharacters())
            )
        )
    }

    @EventHandler
    @EventHandlerTrigger(Trigger.update)
    fun onUpdate() {
        runOnMindustryThread {
            Groups.build.each { building ->
                if (building is GenericCrafterBuild) {
                    val items = mutableListOf<ItemStack>()
                    val liquids = mutableListOf<LiquidStack>()

                    building.block.consumers.forEach {
                        if (it is ConsumeItems) {
                            items.addAll(it.items)
                        } else if (it is ConsumeLiquid) {
                            liquids.add(LiquidStack(it.liquid, it.amount))
                        } else if (it is ConsumeLiquids) {
                            liquids.addAll(it.liquids)
                        }
                    }

                    items.forEach {
                        building.items.set(it.item, building.block.itemCapacity * 10)
                    }

                    liquids.forEach {
                        building.liquids.set(it.liquid, building.block.liquidCapacity * 10)
                    }
                }
            }
        }
    }
}