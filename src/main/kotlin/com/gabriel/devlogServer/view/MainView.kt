package com.gabriel.devlogServer.view

import com.gabriel.devlogServer.app.MyApp
import com.gabriel.devlogServer.app.res.SVGIcons
import com.gabriel.devlogServer.viewModel.LogViewModel
import com.gabriel.devlogServer.viewModel.MainViewModel
import com.gabriel.devlogServer.viewModel.MainViewModel.LogLevel
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.paint.Paint
import javafx.stage.StageStyle
import jfxtras.styles.jmetro.JMetroStyleClass
import tornadofx.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class MainView : View("Dev Logs") {

    private val viewModel: MainViewModel by inject()

    override val root = borderpane {

        styleClass.add(JMetroStyleClass.BACKGROUND)

        viewModel.createTable()

        minWidth = 600.0
        minHeight = 600.0

        top = hbox {
            menubar {
                //TODO This is not working correctly. Commenting it out for now
                //useSystemMenuBarProperty().value = true
                menu("File") {
                    item("Exit").action {
                        viewModel.stopServer()
                        close()
                    }
                    item("About").action {
                        //Show about dialog
                        val aboutDialog = dialog(title = "About", stageStyle = StageStyle.UTILITY) {
                            text = "Dev Log Server - Version 1.0"
                            stage.resizableProperty().value = false
                        }
                        aboutDialog?.show()
                    }
                }
            }
            spacer()

            button {
                styleClass.add("menu-bar")
                fitToParentHeight()
                graphic = SVGIcons.lightMode
                setOnAction {
                    graphic = if ((app as MyApp).isDarkMode.value) {
                        SVGIcons.lightMode
                    } else {
                        SVGIcons.darkMode
                    }
                    try {
                        (app as MyApp).toggleDarkMode()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        center = vbox {
            separator(Orientation.HORIZONTAL) {
                fitToParentWidth()
            }

            filtersView()

            listview<LogViewModel> {
                items = viewModel.logs
                cellFormat { logViewModel ->
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

                    val logLevel = when (logViewModel.logLevel.value) {
                        1 -> LogLevel.ALL
                        2 -> LogLevel.VERBOSE
                        3 -> LogLevel.DEBUG
                        4 -> LogLevel.INFO
                        5 -> LogLevel.WARNING
                        6 -> LogLevel.ERROR
                        else -> LogLevel.ALL
                    }
                    style {
                        //Color coding for the various log levels
                        textFill = Paint.valueOf(logLevel.color((app as MyApp).isDarkMode.value).toString())
                    }

                    //TODO Is this okay?
                    (app as MyApp).isDarkMode.onChange {
                        style {
                            textFill = Paint.valueOf(logLevel.color(it).toString())
                        }
                    }
                }
                fitToParentHeight()
            }
        }

        bottom = appStatusView()
    }

    override fun onDock() {
        currentWindow?.setOnCloseRequest {
            viewModel.stopServer()
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
            items = viewModel.users
            valueProperty().bindBidirectional(viewModel.selectedUser)
            promptText = "Select Device"
            cellFormat {
                text = it
            }
            hboxConstraints {
                marginTop = 8.0
                marginBottom = 8.0
                marginLeft = 8.0
                marginRight = 20.0
            }
            fitToParentWidth()
        }

        //Filter process
        combobox<String> {
            items = viewModel.processes
            valueProperty().bindBidirectional(viewModel.selectedProcess)
            promptText = "Select Process"
            cellFormat {
                text = it
            }
            hboxConstraints {
                marginTop = 8.0
                marginBottom = 8.0
                marginRight = 20.0
            }
            fitToParentWidth()
        }

        combobox(values = LogLevel.values().toList()) {
            bindSelected(viewModel.selectedLogLevel)
            promptText = "Select Log Level"
            cellFormat { logLevel ->
                text = item.name
                style {
                    textFill = Paint.valueOf(logLevel.color((app as MyApp).isDarkMode.value).toString())
                }
                (app as MyApp).isDarkMode.onChange {
                    style {
                        textFill = Paint.valueOf(logLevel.color(it).toString())
                    }
                }
            }
            hboxConstraints {
                marginTop = 8.0
                marginBottom = 8.0
                marginRight = 20.0
            }
            fitToParentWidth()
        }

        //Search functionality
        combobox<String> {
            items = viewModel.tags
            isEditable = true
            viewModel.searchText.bind(editor.textProperty())
            promptText = "Enter TAG here"

            cellFormat {
                text = it
            }
            hboxConstraints {
                marginTop = 8.0
                marginBottom = 8.0
                marginRight = 8.0
            }
            fitToParentWidth()
        }
    }

    private fun EventTarget.appStatusView(spacing: Double? = null, alignment: Pos? = null) = hbox {
        spacing?.let {
            this.spacing = it
        }
        alignment?.let {
            this.alignment = it
        }

        add(ConnectionStatusView(viewModel.connectionStatus))

        spacer()

        hbox {
            hboxConstraints {
                marginTop = 8.0
                marginBottom = 8.0
                marginLeft = 8.0
                marginRight = 8.0
            }
            button("Delete All") {
                setOnAction {
                    viewModel.deleteAll()
                }
                hboxConstraints {
                    marginRight = 8.0
                }
            }
            button("Start Server") {
                setOnAction {
                    viewModel.startServer()
                    isDisable = true
                }
            }
        }
    }
}

