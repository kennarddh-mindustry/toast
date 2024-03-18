package com.github.kennarddh.mindustry.toast.core.commands.paramaters.types

import com.github.kennarddh.mindustry.genesis.core.commands.parameters.types.CommandParameter
import com.github.kennarddh.mindustry.genesis.core.commands.parameters.types.CommandParameterParsingException
import mindustry.game.Team
import kotlin.reflect.KClass

class TeamParameter : CommandParameter<Team> {
    override suspend fun parse(instance: KClass<Team>, input: String): Team {
        return Team.all.find { it.name.equals(input, ignoreCase = true) }
            ?: throw CommandParameterParsingException("Cannot convert $input into team for parameter :parameterName:.")
    }

    override suspend fun toUsageType(input: KClass<Team>): String = "team"
}