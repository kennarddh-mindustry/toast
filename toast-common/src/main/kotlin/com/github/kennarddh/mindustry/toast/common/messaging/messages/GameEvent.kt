package com.github.kennarddh.mindustry.toast.common.messaging.messages

import com.github.kennarddh.mindustry.toast.common.Server
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class GameEventBase

@Serializable
@SerialName("PlayerJoin")
data class PlayerJoinGameEvent(val playerMindustryName: String, val playerMindustryUUID: String) :
    GameEventBase()

@Serializable
@SerialName("PlayerLeave")
data class PlayerLeaveGameEvent(val playerMindustryName: String, val playerMindustryUUID: String) :
    GameEventBase()

@Serializable
@SerialName("PlayerChat")
data class PlayerChatGameEvent(
    val playerMindustryName: String,
    val playerMindustryUUID: String?,
    val message: String
) :
    GameEventBase()

@Serializable
@SerialName("PlayerPunished")
data class PlayerPunishedGameEvent(
    val userPunishmentID: Int,
    val name: String,
    val targetPlayerMindustryName: String,
) : GameEventBase()

@Serializable
@SerialName("PlayerRoleChanged")
data class PlayerRoleChangedGameEvent(
    val userID: Int,
) : GameEventBase()

@Serializable
@SerialName("PlayerReported")
data class PlayerReportedGameEvent(
    val userReportID: Int,
    val name: String,
    val targetPlayerMindustryName: String,
) : GameEventBase()

// TODO: Implement map in database first. MapStart, MapEnd.
//@Serializable
//@SerialName("MapStart")
//data class MapStartGameEvent() : GameEventBase()

@Serializable
@SerialName("ServerStart")
class ServerStartGameEvent : GameEventBase()

@Serializable
@SerialName("ServerStop")
class ServerStopGameEvent : GameEventBase()

@Serializable
@SerialName("ServerRestart")
class ServerRestartGameEvent : GameEventBase()

@Serializable
data class GameEvent(val server: Server, val time: Instant, val data: GameEventBase)

