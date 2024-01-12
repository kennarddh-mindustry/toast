package com.github.kennarddh.mindustry.toast.core.handlers

import kennarddh.genesis.core.commands.annotations.ClientSide
import kennarddh.genesis.core.commands.annotations.Command
import kennarddh.genesis.core.commands.result.CommandResult
import kennarddh.genesis.core.handlers.Handler
import mindustry.gen.Player

class UserAccountHandler : Handler() {
    @Command(["register"])
    @ClientSide
    fun register(player: Player): CommandResult {
        return CommandResult("Test")
    }
}