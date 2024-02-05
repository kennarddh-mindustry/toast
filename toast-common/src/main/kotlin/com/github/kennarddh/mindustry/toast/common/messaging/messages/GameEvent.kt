package com.github.kennarddh.mindustry.toast.common.messaging.messages

import com.github.kennarddh.mindustry.toast.common.Server
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
    val playerMindustryUUID: String,
    val message: String
) :
    GameEventBase()

@Serializable
@SerialName("PlayerKicked")
data class PlayerKickedGameEvent(
    val userKickID: Int,
    val targetPlayerMindustryName: String,
) : GameEventBase()

@Serializable
@SerialName("PlayerBanned")
data class PlayerBannedGameEvent(
    val userBanID: Int,
    val targetPlayerMindustryName: String,
) : GameEventBase()

@Serializable
@SerialName("PlayerVoteKickStart")
data class PlayerVoteKickStartGameEvent(
    val playerMindustryName: String,
    val playerMindustryUUID: String,
    val targetPlayerMindustryName: String,
    val targetPlayerMindustryUUID: String
) : GameEventBase()

@Serializable
@SerialName("PlayerVoteKickVote")
data class PlayerVoteKickVoteGameEvent(
    val playerMindustryName: String,
    val playerMindustryUUID: String,
    val targetPlayerMindustryName: String,
    val targetPlayerMindustryUUID: String,
    val vote: Boolean
) : GameEventBase()

@Serializable
@SerialName("PlayerVoteKickCancel")
data class PlayerVoteKickCancelGameEvent(
    val playerMindustryName: String,
    val playerMindustryUUID: String,
    val targetPlayerMindustryName: String,
    val targetPlayerMindustryUUID: String,
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
data class GameEvent(val server: Server, val time: Long, val data: GameEventBase)

