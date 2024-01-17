package com.github.kennarddh.mindustry.toast.core.commons.menus

import mindustry.gen.Player

class EmptyMenuException(message: String) : Exception(message)

class Menus(private val menus: Map<String, Menu>) {
    init {
        if (menus.isEmpty()) {
            throw EmptyMenuException("Menu must contains at least 1 menu")
        }
    }

    suspend fun open(player: Player): Map<String, String?>? {
        val output: MutableMap<String, String?> = mutableMapOf()

        var currentMenuIndex = 0

        while (true) {
            if (currentMenuIndex >= menus.size) return output

            val menuID = menus.keys.elementAt(currentMenuIndex)
            val menu = menus[menuID]!!

            val value = menu.open(player)

            if (value == null) {
                currentMenuIndex -= 1

                if (currentMenuIndex < 0)
                    return null
            } else {
                output[menuID] = value

                currentMenuIndex += 1
            }
        }
    }
}