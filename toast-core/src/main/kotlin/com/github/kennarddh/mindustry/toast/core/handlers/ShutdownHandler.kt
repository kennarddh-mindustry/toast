package com.github.kennarddh.mindustry.toast.core.handlers

import arc.Core
import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ServerSide
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThreadSuspended
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.commands.parameters.validations.numbers.GTE
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import kotlinx.coroutines.*
import mindustry.Vars
import mindustry.gen.Call
import mindustry.gen.KickCallPacket2
import mindustry.gen.Player
import mindustry.net.Packets.KickReason
import kotlin.time.Duration.Companion.seconds

class ShutdownHandler : Handler {
    private var gracefulStopJob: Job? = null

    override suspend fun onInit() {
        Genesis.commandRegistry.removeCommand("exit")
        Genesis.commandRegistry.removeCommand("stop")
    }

    @Command(["graceful-shutdown"])
    @ServerSide
    @ClientSide
    @MinimumRole(UserRole.Admin)
    @Description("Shutdown server with countdown then reconnect players.")
    suspend fun gracefulShutdown(player: Player? = null, @GTE(0) countdown: Int = 5) {
        Logger.info("${player?.name ?: "Server"} ran graceful-shutdown command")

        try {
            shutdown(countdown)
        } catch (_: CancellationException) {
            // Ignore if shutdown job got canceled
        }
    }

    suspend fun shutdown(countdown: Int = 5) {
        gracefulStopJob?.cancel()

        gracefulStopJob = CoroutineScopes.Main.launch {
            if (countdown > 0) {
                for (i in countdown downTo 1) {
                    Call.sendMessage("[scarlet]Server stopping in: ${i}s. Same map state will continue after restart.")

                    delay(1.seconds)
                }
            }

            runOnMindustryThreadSuspended {
                runBlocking {
                    Call.sendMessage("[scarlet]Stopping server.")

                    Genesis.getHandler<AutoSaveHandler>()?.autoSave()

                    // Kick every player
                    val packet = KickCallPacket2()
                    packet.reason = KickReason.serverRestarting
                    Vars.net.send(packet, true)

                    Logger.info("Kicked all players")

                    Logger.info("Gracefully exiting.")

                    // Exit
                    Vars.net.dispose()
                    Core.app.exit()

                    Logger.info("Gracefully exited.")
                }
            }
        }

        gracefulStopJob?.join()
    }
}