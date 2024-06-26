package com.github.kennarddh.mindustry.toast.core.commands.paramaters.types

import com.github.kennarddh.mindustry.genesis.core.commands.parameters.types.CommandParameter
import com.github.kennarddh.mindustry.genesis.core.commands.parameters.types.CommandParameterParsingException
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import mindustry.gen.Player
import kotlin.reflect.KClass

class ToastPlayerParameter : CommandParameter<Player> {
    override suspend fun parse(instance: KClass<Player>, input: String): Player {
        if (input.startsWith("#")) {
            try {
                val inputInt = input.drop(1).toInt()

                return Entities.players.keys.find { it.id == inputInt }
                    ?: throw CommandParameterParsingException("Cannot convert $input into player for parameter :parameterName:. Cannot find player with the mindustry id $inputInt.")
            } catch (error: NumberFormatException) {
                throw CommandParameterParsingException("Cannot convert $input into player for parameter :parameterName: because it's not a valid mindustry id, mindustry id must starts with '#' and followed with numbers.")
            }
        }

        val inputInt = input.toIntOrNull()
            ?: return Entities.players.keys.find { it.plainName() == input }
                ?: throw CommandParameterParsingException("Cannot convert $input into player for parameter :parameterName:. Cannot find player with the name $input.")

        val player = Entities.players.filter { it.value.userID == inputInt }.keys.firstOrNull()

        if (player == null)
            throw CommandParameterParsingException("Cannot convert $input into active player for parameter :parameterName:. Player with the id $inputInt is not here.")

        return player
    }

    override suspend fun toUsageType(input: KClass<Player>): String = "player"
}