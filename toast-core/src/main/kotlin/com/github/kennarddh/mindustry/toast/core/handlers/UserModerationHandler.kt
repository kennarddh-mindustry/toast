package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import mindustry.gen.Player
import kotlin.time.Duration

class UserModerationHandler : Handler() {
    @Command(["kick"])
    @MinimumRole(UserRole.Mod)
    @ClientSide
    fun kick(player: Player, target: Player, duration: Duration, reason: String) {
        Logger.info("kicked ${target.name} for $duration with the reason $reason")
//        CoroutineScopes.Main.launch {
//            newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
//            }
//        }
    }
}