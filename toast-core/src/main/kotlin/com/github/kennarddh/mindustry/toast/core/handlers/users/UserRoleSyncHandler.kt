package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerRoleChangedGameEvent
import com.github.kennarddh.mindustry.toast.common.selectOne
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import kotlinx.coroutines.launch

class UserRoleSyncHandler : Handler {
    override suspend fun onInit() {
        Messenger.listenGameEvent("${ToastVars.server.name}ServerRoleSync", "*.player.role.changed") { gameEvent ->
            val data = gameEvent.data

            if (gameEvent.server == ToastVars.server) return@listenGameEvent
            if (data !is PlayerRoleChangedGameEvent) return@listenGameEvent

            // If the user is not here just ignore this event
            val playerData = Entities.players.values.find { it.userID == data.userID }
                ?: return@listenGameEvent

            CoroutineScopes.IO.launch {
                Database.newTransaction {
                    val user = Users.selectOne { Users.id eq data.userID }

                    if (user == null) {
                        Logger.error("Cannot find user with the id ${data.userID} for UserRoleSyncHandler")

                        return@newTransaction
                    }

                    playerData.role = user[Users.role]
                }
            }
        }
    }
}