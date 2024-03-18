package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ServerSide
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import mindustry.Vars
import mindustry.gen.Player

class JavascriptHandler : Handler {
    override suspend fun onInit() {
        Genesis.commandRegistry.removeCommand("js")
        Genesis.commandRegistry.removeCommand("javascript")
    }

    @Command(["javascript", "js"])
    @ServerSide
    @ClientSide
    @MinimumRole(UserRole.Admin)
    @Description("Run arbitrary Javascript on the server.")
    fun javascript(player: Player? = null, script: String): CommandResult {
        return CommandResult(Vars.mods.scripts.runConsole(script))
    }
}