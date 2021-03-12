package com.example.demo.controller

import com.example.demo.db.LogMessage
import com.example.demo.db.LogTable
import com.example.demo.viewModel.LogViewModel
import com.google.gson.JsonObject
import javafx.application.Platform
import javafx.beans.property.Property
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.Controller
import tornadofx.asObservable
import tornadofx.observableListOf
import java.sql.Connection

class DbController : Controller() {

    val logs: ObservableList<LogViewModel> by lazy {
        transaction {
            LogMessage.all().map {
                LogViewModel().apply {
                    item = it
                }
            }.asObservable()
        }
    }

    val tags: ObservableList<String> by lazy {
        transaction {
            LogMessage.all().map {
                LogViewModel().apply {
                    item = it
                }
            }.distinctBy { it.tag.value }.map { it.tag.value }.asObservable()
        }
    }

    val users: ObservableList<String> by lazy {
        transaction {
            LogMessage.all().distinctBy { it.userId }.map {
                it.userId
            }.asObservable()
        }
    }

    fun addUser(userId: String) {
        if (users.contains(userId)) return
        Platform.runLater {
            users.add(userId)
        }
    }

    fun insertLog(userId: String, messageJson: JsonObject) {
        transaction {
            val log = LogMessage.new {
                this.userId = userId
                logLevel = messageJson["logLevel"].asInt
                tag = messageJson["tag"].asString
                timeInMillis = messageJson["timeMills"].asLong
                message = messageJson["message"].asString
            }
            if (!tags.contains(log.tag)) {
                tags.add(log.tag)
            }
            logs.add(LogViewModel().apply {
                item = log
            })
        }
    }

    fun createTable() {
        transaction {
            addLogger(StdOutSqlLogger)
            // create the table
            SchemaUtils.create(LogTable)
        }
    }

    fun deleteAll() {
        transaction {
            LogTable.deleteAll()
            logs.clear()
            tags.clear()
            users.clear()
        }
    }

    fun filterByUser(userId: String?) {
        userId?.let {
            transaction {
                logs.clear()
                logs.addAll(
                        LogMessage.find {
                            LogTable.userId eq userId
                        }.map {
                            LogViewModel().apply {
                                item = it
                            }
                        }
                )
            }
        }
    }


    init {
        Database.connect("jdbc:sqlite:file:data.sqlite", driver = "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    }
}