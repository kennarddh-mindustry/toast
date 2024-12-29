package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.senders.PlayerCommandSender
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.extensions.openURI

class DiscordHandler : Handler {
    @Command(["discord"])
    @Description("Open Toast Mindustry Server Discord.")
    fun discord(sender: PlayerCommandSender) {
        sender.player.openURI("https://discord.gg/27gcRJDH23")
    }
}
