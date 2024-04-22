package com.github.kennarddh.mindustry.toast.core.handlers

import arc.Core
import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.senders.CommandSender
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThreadSuspended
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.commands.parameters.validations.numbers.GTE
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastState
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.extensions.getName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import mindustry.Vars
import mindustry.gen.Call
import mindustry.gen.KickCallPacket2
import mindustry.net.Packets.KickReason
import kotlin.time.Duration.Companion.seconds

class ShutdownHandler : Handler {
    private var gracefulStopJob: Job? = null

    override suspend fun onInit() {
        Genesis.commandRegistry.removeCommand("exit")
        Genesis.commandRegistry.removeCommand("stop")
    }

    @Command(["graceful_shutdown"])
    @MinimumRole(UserRole.Admin)
    @Description("Shutdown server with countdown then reconnect players.")
    suspend fun gracefulShutdown(sender: CommandSender, @GTE(0) countdown: Int = 5) {
        Logger.info("${sender.getName()} ran graceful_shutdown command")

        try {
            shutdown(countdown)
        } catch (_: CancellationException) {
            // Ignore if shutdown job got canceled
        }
    }

    suspend fun shutdown(countdown: Int = 5) {
        ToastVars.stateLock.withLock {
            ToastVars.state = ToastState.ShuttingDown
        }

        gracefulStopJob?.cancel()

        gracefulStopJob = CoroutineScopes.Main.launch {
            if (countdown > 0) {
                for (i in countdown downTo 1) {
                    Call.sendMessage("[scarlet]Server stopping in: ${i}s. Same map state will continue after restart.")

                    delay(1.seconds)
                }
            }

            runOnMindustryThreadSuspended {
                Call.sendMessage("[scarlet]Stopping server.")

                Logger.info("Kicking all players")

                // Kick every player
                val packet = KickCallPacket2()
                packet.reason = KickReason.serverRestarting
                Vars.net.send(packet, true)

                Logger.info("Kicked all players")

                Logger.info("Gracefully exiting.")

                Vars.net.dispose()
                Core.app.exit()

                Logger.info("Gracefully exited.")
            }
        }

        gracefulStopJob?.join()
    }
}