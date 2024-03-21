package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ServerSide
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResultStatus
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThreadSuspended
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import kotlinx.coroutines.TimeoutCancellationException
import mindustry.Vars
import mindustry.gen.Player
import kotlin.time.Duration.Companion.seconds

class JavascriptHandler : Handler {
    override suspend fun onInit() {
        Genesis.commandRegistry.removeCommand("js")
        Genesis.commandRegistry.removeCommand("javascript")
    }

    @Command(["javascript", "js"])
    @ServerSide
    @ClientSide
    @MinimumRole(UserRole.Admin)
    @Description("Run arbitrary Javascript on the server. This will run the code on the server. Please do not run code that takes long time to execute as it will blocks main thread and hang everything else.")
    suspend fun javascript(player: Player? = null, script: String): CommandResult {
        Logger.info("${player?.name ?: "Server"} ran \"${script}\"")

        try {
            val output = runOnMindustryThreadSuspended(5.seconds) {
                Vars.mods.scripts.runConsole(script)
            }

            return CommandResult(output)
        } catch (error: TimeoutCancellationException) {
            return CommandResult("Code took too long.", CommandResultStatus.Failed)
        } catch (error: Exception) {
            Logger.error("Javascript code throws unknown error", error)

            return CommandResult("Unknown error occurred while running the code.", CommandResultStatus.Failed)
        }
    }
}