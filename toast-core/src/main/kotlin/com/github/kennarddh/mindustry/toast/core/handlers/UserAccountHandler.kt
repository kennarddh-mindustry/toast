package com.github.kennarddh.mindustry.toast.core.handlers

import arc.util.Strings
import com.github.kennarddh.mindustry.toast.core.commons.Server
import com.github.kennarddh.mindustry.toast.core.commons.UserRole
import com.github.kennarddh.mindustry.toast.core.commons.database.tables.*
import com.github.kennarddh.mindustry.toast.core.commons.packIP
import kennarddh.genesis.core.commands.annotations.ClientSide
import kennarddh.genesis.core.commands.annotations.Command
import kennarddh.genesis.core.commands.result.CommandResult
import kennarddh.genesis.core.events.annotations.EventHandler
import kennarddh.genesis.core.handlers.Handler
import mindustry.game.EventType
import mindustry.gen.Player
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class UserAccountHandler : Handler() {
    @EventHandler
    fun onPlayerJoin(event: EventType.PlayerJoin) {
        val player = event.player
        val ip = player.con.address.packIP()
        val name = player.name
        val strippedName = Strings.stripColors(name)

        transaction {
            val mindustryUser = MindustryUser.selectAll().where {
                MindustryUser.mindustryUUID eq player.uuid()
            }.firstOrNull()

            val mindustryUserID = if (mindustryUser == null) {
                MindustryUser.insertAndGetId {
                    it[this.mindustryUUID] = player.uuid()
                }
            } else {
                mindustryUser[MindustryUser.id]
            }

            val ipAddress = IPAddresses.selectAll().where {
                IPAddresses.ipAddress eq ip
            }.firstOrNull()

            val ipAddressID = if (ipAddress == null) {
                IPAddresses.insertAndGetId {
                    it[this.ipAddress] = ip
                }
            } else {
                ipAddress[IPAddresses.id]
            }

            val mindustryName = MindustryNames.selectAll().where {
                MindustryNames.name eq name
            }.firstOrNull()

            val mindustryNameID = if (mindustryName == null) {
                MindustryNames.insertAndGetId {
                    it[this.name] = name
                    it[this.strippedName] = strippedName
                }
            } else {
                mindustryName[MindustryNames.id]
            }

            val mindustryUserIPAddresses = MindustryUserIPAddresses.selectAll().where {
                MindustryUserIPAddresses.mindustryUserID eq mindustryUserID
                MindustryUserIPAddresses.ipAddressID eq ipAddressID
            }.firstOrNull()

            if (mindustryUserIPAddresses == null) {
                MindustryUserIPAddresses.insertIgnore {
                    it[this.mindustryUserID] = mindustryUserID
                    it[this.ipAddressID] = ipAddressID
                }
            }

            val mindustryUserMindustryNames = MindustryUserMindustryNames.selectAll().where {
                MindustryUserMindustryNames.mindustryUserID eq mindustryUserID
                MindustryUserMindustryNames.mindustryNameID eq mindustryNameID
            }.firstOrNull()

            if (mindustryUserMindustryNames == null) {
                MindustryUserMindustryNames.insertIgnore {
                    it[this.mindustryUserID] = mindustryUserID
                    it[this.mindustryNameID] = mindustryNameID
                }
            }

            val mindustryUserServerData = MindustryUserServerData.selectAll().where {
                MindustryUserServerData.mindustryUserID eq mindustryUserID
            }.firstOrNull()

            if (mindustryUserServerData == null) {
                MindustryUserServerData.insertIgnore {
                    it[this.mindustryUserID] = mindustryUserID
                    it[this.server] = Server.Survival
                }
            }

            // TODO: Check for kick and ban

            val userID = mindustryUser?.get(MindustryUser.userID)

            if (userID != null) {
                val user = Users.selectAll().where {
                    Users.id eq userID
                }.first()

                // TODO: Handle more role
                when (user[Users.role]) {
                    UserRole.Owner -> player.admin = true
                    UserRole.CoOwner -> player.admin = true
                    UserRole.Admin -> player.admin = true
                    UserRole.Mod -> TODO()
                    UserRole.Player -> Unit
                }
            }
        }
    }

    @Command(["register"])
    @ClientSide
    fun register(player: Player): CommandResult {
        return CommandResult("Test ${player.admin}")
    }
}