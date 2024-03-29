package com.github.kennarddh.mindustry.toast.core.handlers.users

import arc.Events
import com.github.kennarddh.mindustry.genesis.core.commons.priority.Priority
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThread
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.server.packets.annotations.ServerPacketHandler
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import mindustry.gen.Player
import mindustry.net.NetConnection
import mindustry.net.Packets

data class PlayerDisconnected(val player: Player)

class UserLeavesHandler : Handler {
    @ServerPacketHandler(Priority.Important, true)
    fun onPlayerDisconnect(con: NetConnection, packet: Packets.Disconnect): Boolean {
        if (con.player == null) return false

        Entities.players.remove(con.player)

        Logger.info("Player ${con.player.name}/${con.player.uuid()} removed from Entities.players")

        runOnMindustryThread {
            Events.fire(PlayerDisconnected(con.player))
        }

        return true
    }
}