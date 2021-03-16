package com.example.demo.view

import com.example.demo.Connection
import com.example.demo.app.LogLevel
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
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.stage.StageStyle
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
    var logs: ObservableList<LogViewModel> by singleAssign()
    var tags: ObservableList<String> by singleAssign()
    var users: ObservableList<String> by singleAssign()

    var connectionStatus = SimpleStringProperty()

    private val selectedUser = SimpleStringProperty()
    private val selectedLogLevel = SimpleObjectProperty<LogLevel>()
    private val searchText = SimpleStringProperty()

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

        top = menubar {
            //TODO This is not working correctly. Commenting it out for now
            //useSystemMenuBarProperty().value = true
            menu("File") {
                item("Exit") {
                    setOnAction {
                        close()
                    }
                }
                item("About") {
                    setOnAction {
                        //Show about dialog
                        val aboutDialog = dialog(title = "About", stageStyle = StageStyle.UTILITY) {
                            text = "Dev Log Server - Version 1.0"
                            stage.resizableProperty().value = false
                        }
                        aboutDialog?.show()
                    }
                }
            }
        }

        center = vbox {
            buttonbar {
                vboxConstraints {
                    marginRight = 8.0
                    marginTop = 4.0
                    marginBottom = 4.0
                }
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

            vbox {
                separator(Orientation.HORIZONTAL)

                filtersView()

                listview<LogViewModel> {
                    items = logs
                    cellFormat {
                        //For wrapping text
                        minWidth = width
                        maxWidth = width
                        prefWidth = width
                        isWrapText = true

                        val formattedTime = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(item.timeInMillis.value),
                                ZoneId.systemDefault()
                        ).format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh.mm:ss a"))
                        //TODO Bold the Tag, and maybe the time as well ? Use TextFlow
                        text = "$formattedTime - ${item.tag.value} - ${item.message.value}"

                        //Color coding for the various log levels
                        textFill = when (item.logLevel.value) {
                            2 -> Paint.valueOf(Color.GRAY.toString())
                            3 -> Paint.valueOf(Color.BLACK.toString())
                            4 -> Paint.valueOf(Color.BLUE.toString())
                            5 -> Paint.valueOf(Color.ORANGE.darker().toString())
                            6 -> Paint.valueOf(Color.RED.toString())
                            else -> Paint.valueOf(Color.GRAY.toString())
                        }
                    }
                }
            }

            bottom = appStatusView()
        }
    }

    override fun onDock() {
        currentWindow?.setOnCloseRequest {
            server.stop()
        }
    }

    private fun EventTarget.filtersView(spacing: Double? = null, alignment: Pos? = null) = hbox {
        spacing?.let {
            this.spacing = it
        }
        alignment?.let {
            this.alignment = it
        }

        //Filter user
        combobox<String> {
            items = users
            valueProperty().bindBidirectional(selectedUser)
            promptText = "Select Device"
            cellFormat {
                text = it
            }
            hboxConstraints {
                marginTop = 8.0
                marginBottom = 8.0
                marginLeft = 8.0
                marginRight = 20.0
                hGrow = Priority.ALWAYS
            }
        }
        selectedUser.onChange {
            dbController.filterLogs(it, selectedLogLevel.value, searchText.value) //Send logLevel here as well
        }

        combobox(values = LogLevel.values().toMutableList<LogLevel?>().apply {
            add(0, null)
        }) {
            bindSelected(selectedLogLevel)
            cellFormat {
                text = it?.name ?: ""
                textFill = when (it) {
                    LogLevel.VERBOSE -> Paint.valueOf(Color.GRAY.toString())
                    LogLevel.DEBUG -> Paint.valueOf(Color.BLACK.toString())
                    LogLevel.INFO -> Paint.valueOf(Color.BLUE.toString())
                    LogLevel.WARNING -> Paint.valueOf(Color.ORANGE.darker().toString())
                    LogLevel.ERROR -> Paint.valueOf(Color.RED.toString())
                    else -> Paint.valueOf(Color.WHITE.darker().toString())
                }
            }
            hboxConstraints {
                marginTop = 8.0
                marginBottom = 8.0
                marginRight = 20.0
                hGrow = Priority.ALWAYS
            }
        }
        selectedLogLevel.onChange {
            dbController.filterLogs(selectedUser.value, it, searchText.value)
        }

        //Search functionality
        combobox<String> {
            items = tags
            isEditable = true
            searchText.bind(editor.textProperty())
            promptText = "Enter TAG here"

            cellFormat {
                text = it
            }
            hboxConstraints {
                marginTop = 8.0
                marginBottom = 8.0
                marginRight = 8.0
                hGrow = Priority.ALWAYS
            }
        }
        searchText.onChange {
            dbController.filterLogs(
                    selectedUser.value,
                    selectedLogLevel.value,
                    it
            )
        }
    }

    private fun EventTarget.appStatusView(spacing: Double? = null, alignment: Pos? = null) = hbox {
        spacing?.let {
            this.spacing = it
        }
        alignment?.let {
            this.alignment = it
        }
        add(ConnectionStatusView(connectionStatus))
        spacer()

        fun logColorDef(color: Color, logLevel: LogLevel): StackPane {
            return stackpane {
                rectangle {
                    widthProperty().bind(this@hbox.heightProperty())
                    heightProperty().bind(this@hbox.heightProperty())
                    fill = Paint.valueOf(color.toString())
                }
                when (logLevel) {
                    LogLevel.VERBOSE -> text("V")
                    LogLevel.DEBUG -> text("D") {
                        fill = Paint.valueOf(Color.WHITE.toString())
                    }
                    LogLevel.INFO -> text("I") {
                        fill = Paint.valueOf(Color.WHITE.toString())
                    }
                    LogLevel.WARNING -> text("W")
                    LogLevel.ERROR -> text("E")
                }
            }
        }
        add(logColorDef(Color.GRAY, LogLevel.VERBOSE))
        add(logColorDef(Color.BLACK, LogLevel.DEBUG))
        add(logColorDef(Color.BLUE, LogLevel.INFO))
        add(logColorDef(Color.ORANGE.darker(), LogLevel.WARNING))
        add(logColorDef(Color.RED, LogLevel.ERROR))
    }

    //TODO Find ways to move this to its own class.
    // Currently the main issue with doing this is the [dbController] object.
    // DbController cannot be injected into a normal class. Is it okay to use constructor initialization?
    inner class Server {
        private val socketConnection by lazy {
            embeddedServer(Netty, port = 8080) {
                install(WebSockets)
                Platform.runLater {
                    connectionStatus.value = "Listening at ${InetAddress.getLocalHost()}"
                }
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

}
