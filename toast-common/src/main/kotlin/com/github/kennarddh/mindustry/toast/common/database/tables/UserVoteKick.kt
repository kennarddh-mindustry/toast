package com.github.kennarddh.mindustry.toast.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object UserVoteKick : IntIdTable() {
    val UserPunishmentID = reference("UserPunishmentID", UserPunishments)
    val requiredVotes = short("requiredVotes")
}