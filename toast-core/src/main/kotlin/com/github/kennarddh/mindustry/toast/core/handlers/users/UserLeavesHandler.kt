package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.commons.priority.Priority
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.server.packets.annotations.ServerPacketHandler
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import mindustry.net.NetConnection
import mindustry.net.Packets

class UserLeavesHandler : Handler {
    @ServerPacketHandler(Priority.Important, true)
    fun onPlayerDisconnect(con: NetConnection, packet: Packets.Disconnect): Boolean {
        Entities.players.remove(con.player)

        Logger.info("Player ${con.player.name}/${con.player.uuid()} removed from Entities.players")

        return true
    }
}