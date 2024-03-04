package com.github.kennarddh.mindustry.toast.common

enum class Permission {
    ViewMindustryNamesHistory,
    ViewUUID,
    ViewIP,
    Join,
    Chat;

    companion object {
        val all = entries.toSet()
    }
}