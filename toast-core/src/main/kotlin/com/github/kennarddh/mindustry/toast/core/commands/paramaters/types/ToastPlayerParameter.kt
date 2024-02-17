package com.github.kennarddh.mindustry.toast.core.commands.paramaters.types

import com.github.kennarddh.mindustry.genesis.core.commands.parameters.types.CommandParameter
import com.github.kennarddh.mindustry.genesis.core.commands.parameters.types.CommandParameterParsingException
import mindustry.gen.Groups
import mindustry.gen.Player
import kotlin.reflect.KClass

class ToastPlayerParameter : CommandParameter<Player> {
    override suspend fun parse(instance: KClass<Player>, input: String): Player {
        return try {
            Groups.player.find { it.id == input.toInt() }
        } catch (error: NumberFormatException) {
            Groups.player.find { it.name == input }
                ?: throw CommandParameterParsingException("Cannot convert $input into player for parameter :parameterName:. Either it's not a valid player name or player id.")
        }
    }

    override suspend fun toUsageType(input: KClass<Player>): String = "player"
}