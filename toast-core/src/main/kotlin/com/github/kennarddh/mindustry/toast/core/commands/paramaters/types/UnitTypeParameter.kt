package com.github.kennarddh.mindustry.toast.core.commands.paramaters.types

import com.github.kennarddh.mindustry.genesis.core.commands.parameters.types.CommandParameter
import com.github.kennarddh.mindustry.genesis.core.commands.parameters.types.CommandParameterParsingException
import mindustry.Vars
import mindustry.type.UnitType
import kotlin.reflect.KClass

class UnitTypeParameter : CommandParameter<UnitType> {
    override suspend fun parse(instance: KClass<UnitType>, input: String): UnitType {
        return Vars.content.units().toArray().find { it.name.equals(input, ignoreCase = true) }
            ?: throw CommandParameterParsingException("Cannot convert $input into unit type for parameter :parameterName:.")
    }

    override suspend fun toUsageType(input: KClass<UnitType>): String = "unit"
}