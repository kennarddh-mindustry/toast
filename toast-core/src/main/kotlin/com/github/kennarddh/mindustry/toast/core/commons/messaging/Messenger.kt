package com.github.kennarddh.mindustry.toast.core.commons.messaging

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

object Messenger {
    const val GAME_EVENTS_QUEUE_NAME = "GameEvents"
    val connection: Connection

    init {
        val uri = System.getenv("RABBITMQ_URI")

        val connectionFactory = ConnectionFactory()

        connectionFactory.setUri(uri)

        connection = connectionFactory.newConnection()

        val channel = connection.createChannel()

        channel.queueDeclare(GAME_EVENTS_QUEUE_NAME, true, false, false, null)
    }
}