package com.github.kennarddh.mindustry.toast.common.messaging

import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerControl
import com.rabbitmq.client.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext


object Messenger {
    private const val GAME_EVENTS_EXCHANGE_NAME = "GameEvents"
    private const val SERVER_CONTROL_EXCHANGE_NAME = "ServerControl"
    private lateinit var connection: Connection
    private lateinit var channel: Channel
    private lateinit var coroutineContext: CoroutineContext

    fun init(coroutineContext: CoroutineContext) {
        this.coroutineContext = coroutineContext

        val uri = System.getenv("RABBITMQ_URI")

        val connectionFactory = ConnectionFactory()

        connectionFactory.setUri(uri)
        connectionFactory.connectionTimeout = 20000

        connection = connectionFactory.newConnection()

        channel = connection.createChannel()

        channel.exchangeDeclare(GAME_EVENTS_EXCHANGE_NAME, BuiltinExchangeType.TOPIC, true)
        channel.exchangeDeclare(SERVER_CONTROL_EXCHANGE_NAME, BuiltinExchangeType.TOPIC, true)
    }

    fun close() {
        channel.close()
        connection.close()
    }

    suspend fun publishGameEvent(routingKey: String, gameEvent: GameEvent) {
        withContext(coroutineContext) {
            val data = Json.encodeToString(gameEvent)

            channel.basicPublish(GAME_EVENTS_EXCHANGE_NAME, routingKey, null, data.toByteArray())
        }
    }

    suspend fun publishServerControl(routingKey: String, serverControl: ServerControl) {
        withContext(coroutineContext) {
            val data = Json.encodeToString(serverControl)

            channel.basicPublish(SERVER_CONTROL_EXCHANGE_NAME, routingKey, null, data.toByteArray())
        }
    }

    fun listenGameEvent(queueName: String, bindingKey: String, callback: (GameEvent) -> Unit) {
        channel.queueDeclare(queueName, true, false, false, mapOf("x-queue-type" to "quorum"))

        channel.queueBind(queueName, GAME_EVENTS_EXCHANGE_NAME, bindingKey)

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