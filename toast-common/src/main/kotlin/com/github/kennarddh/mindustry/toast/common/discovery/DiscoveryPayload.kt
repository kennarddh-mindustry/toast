package com.github.kennarddh.mindustry.toast.common.discovery

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class DiscoveryPayload(
    val time: Instant,
    val players: Array<String>,
    val tps: Int,
    val uptime: Duration,
    val map: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DiscoveryPayload

        if (time != other.time) return false
        if (!players.contentEquals(other.players)) return false
        if (tps != other.tps) return false
        if (uptime != other.uptime) return false
        if (map != other.map) return false

        return true
    }

    override fun hashCode(): Int {
        var result = time.hashCode()
        result = 31 * result + players.contentHashCode()
        result = 31 * result + tps
        result = 31 * result + uptime.hashCode()
        result = 31 * result + map.hashCode()
        return result
    }
}
