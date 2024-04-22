package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.senders.CommandSender
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThreadSuspended
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.extensions.getName
import kotlinx.coroutines.TimeoutCancellationException
import mindustry.Vars
import kotlin.time.Duration.Companion.seconds

class JavascriptHandler : Handler {
    override suspend fun onInit() {
        Genesis.commandRegistry.removeCommand("js")
        Genesis.commandRegistry.removeCommand("javascript")
    }

    @Command(["javascript", "js"])
    @MinimumRole(UserRole.Admin)
    @Description("Run arbitrary Javascript on the server. This will run the code on the server. Please do not run code that takes long time to execute as it will blocks main thread and hang everything else.")
    suspend fun javascript(sender: CommandSender, script: String) {
        Logger.info("${sender.getName()} ran \"${script}\"")

        try {
            val output = runOnMindustryThreadSuspended(5.seconds) {
                Vars.mods.scripts.runConsole(script)
            }

            sender.sendSuccess(output)
        } catch (error: TimeoutCancellationException) {
            sender.sendError("Code took too long.")
        } catch (error: Exception) {
            Logger.error("Javascript code throws unknown error", error)

            sender.sendError("Unknown error occurred while running the code.")
        }
    }
}