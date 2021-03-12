package com.example.demo.view

import com.example.demo.Connection
import com.example.demo.controller.DbController
import com.example.demo.viewModel.LogViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import tornadofx.*
import java.net.InetAddress
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.thread


class MainView : View("Dev Logs") {

    val dbController: DbController by inject()
    var logTable: TableViewEditModel<LogViewModel> by singleAssign()
    var logs: ObservableList<LogViewModel> by singleAssign()
    var tags: ObservableList<String> by singleAssign()
    var users: ObservableList<String> by singleAssign()

    var connectionStatus = SimpleStringProperty()

    private val selectedUser = SimpleStringProperty()

    private val server by lazy { Server() }

    override val root = borderpane {

        dbController.createTable()

        logs = dbController.logs
        tags = dbController.tags
        users = dbController.users

        connectionStatus.value = "Disconnected"
        connectionStatus.stringBinding {
            """Connection Status
                    |${connectionStatus.value}
                """.trimMargin()
        }
        center = vbox {
            buttonbar {
                button("Delete All") {
                    setOnAction {
                        dbController.deleteAll()
                    }
                }
                button("Start Server") {
                    setOnAction {
                        connectionStatus.value = "Connecting"
                        server.start()
                        isDisable = true
                    }
                }
            }

            add(ConnectionView())

            val filteredLogs = SortedFilteredList(logs)
            hbox {
                //Filter user
                combobox<String> {
                    valueProperty().bindBidirectional(selectedUser)
                    promptText = "Select Device"
                    selectionModel.selectedItemProperty()
                    isEditable = true
                    isEditable = false
                    items = users
                }.apply {
                    cellFormat {
                        text = it
                    }
                }

                selectedUser.onChange {
                    dbController.filterByUser(it)
                }

                //Search functionality
                combobox<String> {
                    isEditable = true
                    filteredLogs.filterWhen(editor.textProperty()) { query, item ->
                        item.tag.value.contains(query)
                    }
                    promptText = "Enter TAG here"
                    items = tags
                }.cellFormat {
                    text = it
                }
            }

            tableview<LogViewModel> {
                logTable = editModel
                items = filteredLogs.bindTo(this)

                column("Time", LogViewModel::timeInMillis).cellFormat {
                    val time = Instant.ofEpochMilli(it)
                    text = LocalDateTime.ofInstant(time, ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh.mm:ss a"))
                }
                column("Tag", LogViewModel::tag)
                column("Level", LogViewModel::logLevel)
                column("Message", LogViewModel::message) {
                    enableTextWrap()
                }
            }
        }
    }

    override fun onDock() {
        currentWindow?.setOnCloseRequest {
            server.stop()
        }
    }

    inner class Server {
        private val socketConnection by lazy {
            embeddedServer(Netty, port = 8080) {
                install(WebSockets)
                connectionStatus.value = "Listening at ${InetAddress.getLocalHost()}"
                routing {
                    val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
                    webSocket("/log/") {
                        val thisConnection = Connection(this)
                        connections += thisConnection
                        try {
                            send("You are connected! There are ${connections.count()} users here.")
                            for (frame in incoming) {
                                frame as? Frame.Text ?: continue
                                val receivedText = frame.readText()
                                val messageJson = Gson().fromJson(receivedText, JsonObject::class.java)
                                val type = messageJson["TYPE"]?.asString
                                if (type == "DEVICE_INFO") {
                                    thisConnection.name = messageJson["NAME"].asString
                                    Platform.runLater {
                                        selectedUser.value = thisConnection.name
                                    }
                                    dbController.addUser(thisConnection.name)
                                    println("Adding user! ${thisConnection.name}")
                                } else {
                                    dbController.insertLog(thisConnection.name, messageJson)
                                    println(messageJson)
                                }
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

    inner class ConnectionView : View() {
        override val root = textarea(connectionStatus) {
            maxHeight = (text.lines().count() * 2).toDouble()
            isEditable = false
        }

    }
}
