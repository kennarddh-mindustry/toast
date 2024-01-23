package com.github.kennarddh.mindustry.toast.core.commons.messaging.messages

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

object Messenger {
    const val GameEventsQueueName = "GameEvents"
    val connection: Connection

    init {
        val uri = System.getenv("RABBITMQ_URI")

        val connectionFactory = ConnectionFactory()

        connectionFactory.setUri(uri)

        connection = connectionFactory.newConnection()

        val channel = connection.createChannel()

        channel.queueDeclare(GameEventsQueueName, true, false, false, null)
    }
}