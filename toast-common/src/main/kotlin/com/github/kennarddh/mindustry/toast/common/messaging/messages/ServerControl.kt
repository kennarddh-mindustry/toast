package com.github.kennarddh.mindustry.toast.common.messaging.messages

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class ServerControlBase

@Serializable
@SerialName("ServerCommand")
data class ServerCommandServerControl(val command: String) : ServerControlBase()

@Serializable
data class ServerControl(val time: Long, val data: ServerControlBase)

