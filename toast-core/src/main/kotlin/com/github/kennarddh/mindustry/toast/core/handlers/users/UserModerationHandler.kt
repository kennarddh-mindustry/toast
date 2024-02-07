package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ServerSide
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import mindustry.gen.Player
import kotlin.time.Duration

class UserModerationHandler : Handler() {
    override suspend fun onInit() {
        GenesisAPI.commandRegistry.removeCommand("kick")
    }

    @Command(["kick"])
    @MinimumRole(UserRole.Mod)
    @ClientSide
    @ServerSide
    fun kick(player: Player? = null, target: Player, duration: Duration, reason: String) {
        Logger.info("kicked ${target.name} for $duration with the reason $reason")
//        CoroutineScopes.Main.launch {
//            newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
//            }
//        }
    }
}