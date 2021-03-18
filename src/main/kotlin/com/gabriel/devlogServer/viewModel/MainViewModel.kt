package com.gabriel.devlogServer.viewModel

import com.gabriel.devlogServer.Connection
import com.gabriel.devlogServer.controller.DbController
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import tornadofx.ViewModel
import tornadofx.onChange
import tornadofx.singleAssign
import tornadofx.stringBinding
import java.net.InetAddress
import java.util.*
import kotlin.concurrent.thread

class MainViewModel : ViewModel() {
    val dbController: DbController by inject()
    var logs: ObservableList<LogViewModel> by singleAssign()
    var tags: ObservableList<String> by singleAssign()
    var users: ObservableList<String> by singleAssign()
    var processes: ObservableList<String> by singleAssign()

    var connectionStatus = SimpleStringProperty()

    val selectedUser = SimpleStringProperty()
    val selectedProcess = SimpleStringProperty()
    val selectedLogLevel = SimpleObjectProperty<LogLevel>()
    val searchText = SimpleStringProperty()

    private val server by lazy { Server() }

    init {
        selectedUser.onChange {
            dbController.getProcessesOfUser(it)
            dbController.filterLogs(
                    it,
                    selectedProcess.value,
                    selectedLogLevel.value ?: LogLevel.ALL,
                    searchText.value
            )
        }

        selectedProcess.onChange {
            dbController.getTagsOfProcess(selectedUser.value, it)
            dbController.filterLogs(
                    selectedUser.value,
                    it,
                    selectedLogLevel.value ?: LogLevel.ALL,
                    searchText.value
            )
        }

        selectedLogLevel.onChange {
            dbController.filterLogs(selectedUser.value, selectedProcess.value, it ?: LogLevel.ALL, searchText.value)
        }

        searchText.onChange {
            dbController.filterLogs(
                    selectedUser.value,
                    selectedProcess.value,
                    selectedLogLevel.value ?: LogLevel.ALL,
                    it
            )
        }

        connectionStatus.value = "Disconnected"
        connectionStatus.stringBinding {
            """Connection Status
                    |${connectionStatus.value}
                """.trimMargin()
        }
    }

    fun createTable() {
        dbController.createTable()
        logs = dbController.logs
        tags = dbController.tags
        users = dbController.users
        processes = dbController.processes
    }

    fun deleteAll() {
        dbController.deleteAll()
    }

    fun startServer() {
        connectionStatus.value = "Connecting"
        server.start()
    }

    fun stopServer() {
        server.stop()
    }


    inner class Server {
        private val socketConnection by lazy {
            embeddedServer(Netty, port = 8080) {
                install(WebSockets)
                Platform.runLater {
                    connectionStatus.value = "Listening at ${InetAddress.getLocalHost()}"
                }
                routing {
                    val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
                    webSocket("/log/{deviceName}/{processName}") {
                        println(call.parameters["deviceName"])
                        println(call.parameters["processName"])
                        val thisConnection = Connection(this)
                        connections += thisConnection
                        try {
                            thisConnection.name = call.parameters["deviceName"].toString()
                            Platform.runLater {
                                selectedUser.value = thisConnection.name
                            }
                            dbController.addUser(thisConnection.name)
                            thisConnection.process = call.parameters["processName"].toString()
                            Platform.runLater {
                                selectedProcess.value = thisConnection.process
                            }
                            dbController.addProcess(thisConnection.process)
                            send("You are connected! There are ${connections.count()} users here.")
                            for (frame in incoming) {
                                frame as? Frame.Text ?: continue
                                val receivedText = frame.readText()
                                val messageJson = Gson().fromJson(receivedText, JsonObject::class.java)
                                dbController.insertLog(thisConnection.name, thisConnection.process, messageJson)
                                println(messageJson)
                            }
                        } catch (e: Exception) {
                            println(e.localizedMessage)
                        } finally {
                            println("Removing ${thisConnection.name}!")
                            connections -= thisConnection
                        }
                    }
                }
            }
        }
        private val serverThread by lazy {
            thread(start = false) {
                socketConnection.start(wait = true)
            }

        }

        fun start() {
            serverThread.start()
        }

        fun stop() {
            socketConnection.stop(0, 0)
            serverThread.interrupt()
        }
    }

    enum class LogLevel(val level: Int) {
        ALL(1),
        VERBOSE(2),
        DEBUG(3),
        INFO(4),
        WARNING(5),
        ERROR(6);

        fun color(isDarkMode: Boolean): Color {
            return when (this) {
                ALL -> if (!isDarkMode) Color.GRAY.darker() else Color.LIGHTGRAY
                VERBOSE -> if (!isDarkMode) Color.GRAY else Color.GRAY.brighter()
                DEBUG -> if (!isDarkMode) Color.BLACK else Color.WHITE
                INFO -> if (!isDarkMode) Color.BLUE else Color.LIGHTBLUE
                WARNING -> if (!isDarkMode) Color.ORANGE.darker() else Color.ORANGE.brighter()
                ERROR -> if (!isDarkMode) Color.RED else Color.INDIANRED
            }
        }
    }
}