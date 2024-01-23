package com.github.kennarddh.mindustry.toast.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object MindustryUserIPAddresses : IntIdTable() {
    val ipAddressID = reference("ipAddressID", IPAddresses)
    val mindustryUserID = reference("mindustryUserID", MindustryUser)
}