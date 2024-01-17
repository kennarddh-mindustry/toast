package com.github.kennarddh.mindustry.toast.core.commons.menus

import mindustry.gen.Player

class EmptyMenuException(message: String) : Exception(message)

class Menus(private val menus: Map<String, Menu>) {
    init {
        if (menus.isEmpty()) {
            throw EmptyMenuException("Menu must contains at least 1 menu")
        }
    }

    suspend fun open(player: Player): Map<String, String?> {
        val output: MutableMap<String, String?> = mutableMapOf()

        for ((menuID, menu) in menus) {
            val value = menu.open(player)

            output[menuID] = value
        }

        return output
    }
}