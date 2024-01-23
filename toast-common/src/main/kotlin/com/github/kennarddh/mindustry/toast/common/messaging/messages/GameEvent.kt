package com.github.kennarddh.mindustry.toast.common.messaging.messages

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
data class PlayerJoinGameEvent(val playerMindustryName: String, val playerMindustryUUID: String) : GameEventBase()

@Serializable
@SerialName("PlayerLeave")
data class PlayerLeaveGameEvent(val playerMindustryName: String, val playerMindustryUUID: String) : GameEventBase()

@Serializable
data class GameEvent(val data: GameEventBase)

