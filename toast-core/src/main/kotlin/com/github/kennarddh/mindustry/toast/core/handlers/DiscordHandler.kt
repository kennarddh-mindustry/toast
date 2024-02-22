package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.extensions.openURI
import mindustry.gen.Player

class DiscordHandler : Handler() {
    @Command(["discord"])
    @ClientSide
    fun discord(player: Player) {
        player.openURI("https://discord.gg/eGD2jNAE3N")
    }
}