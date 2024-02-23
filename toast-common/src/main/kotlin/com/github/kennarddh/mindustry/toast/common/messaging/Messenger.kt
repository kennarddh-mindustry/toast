package com.github.kennarddh.mindustry.toast.common.messaging

import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerControl
import com.rabbitmq.client.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


object Messenger {
    const val GAME_EVENTS_EXCHANGE_NAME = "GameEvents"
    const val SERVER_CONTROL_EXCHANGE_NAME = "ServerControl"
    private lateinit var connection: Connection
    private lateinit var channel: Channel

    fun init() {
        val uri = System.getenv("RABBITMQ_URI")

        val connectionFactory = ConnectionFactory()

        connectionFactory.setUri(uri)

        connection = connectionFactory.newConnection()

        channel = connection.createChannel()

        channel.exchangeDeclare(GAME_EVENTS_EXCHANGE_NAME, BuiltinExchangeType.FANOUT, true)
        channel.exchangeDeclare(SERVER_CONTROL_EXCHANGE_NAME, BuiltinExchangeType.TOPIC, true)
    }

    fun close() {
        channel.close()
        connection.close()
    }

    fun publishGameEvent(gameEvent: GameEvent) {
        val data = Json.encodeToString(gameEvent)

        channel.basicPublish(GAME_EVENTS_EXCHANGE_NAME, "", null, data.toByteArray())
    }

    fun publishServerControl(routingKey: String, serverControl: ServerControl) {
        val data = Json.encodeToString(serverControl)

        channel.basicPublish(SERVER_CONTROL_EXCHANGE_NAME, routingKey, null, data.toByteArray())
    }

    fun listenGameEvent(queueName: String, callback: (GameEvent) -> Unit) {
        channel.queueDeclare(queueName, true, false, false, mapOf("x-queue-type" to "quorum"))

        channel.queueBind(queueName, GAME_EVENTS_EXCHANGE_NAME, "")

        val deliverCallback = DeliverCallback { _, delivery ->
            val message = delivery.body.toString(Charsets.UTF_8)
            val data = Json.decodeFromString<GameEvent>(message)

            callback(data)
        }

        channel.basicConsume(queueName, true, deliverCallback) { _ -> }
    }

    fun listenServerControl(queueName: String, bindingKey: String, callback: (ServerControl) -> Unit) {
        channel.queueDeclare(queueName, true, false, false, mapOf("x-queue-type" to "quorum"))

        channel.queueBind(queueName, SERVER_CONTROL_EXCHANGE_NAME, bindingKey)

        val deliverCallback = DeliverCallback { _, delivery ->
            val message = delivery.body.toString(Charsets.UTF_8)
            val data = Json.decodeFromString<ServerControl>(message)

            callback(data)
        }

        channel.basicConsume(queueName, true, deliverCallback) { _ -> }
    }
}