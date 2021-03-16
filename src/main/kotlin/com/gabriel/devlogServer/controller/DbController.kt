package com.gabriel.devlogServer.controller

import com.gabriel.devlogServer.app.LogLevel
import com.gabriel.devlogServer.db.LogMessage
import com.gabriel.devlogServer.db.LogTable
import com.gabriel.devlogServer.viewModel.LogViewModel
import com.google.gson.JsonObject
import javafx.application.Platform
import javafx.collections.ObservableList
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.Controller
import tornadofx.asObservable
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

    private fun filterOnlyByUser(userId: String?) {
        transaction {
            logs.clear()
            logs.addAll(
                    if (userId == null) {
                        LogMessage.all()
                    } else {
                        LogMessage.find {
                            LogTable.userId eq userId
                        }
                    }.map {
                        LogViewModel().apply {
                            item = it
                        }
                    }
            )
        }
    }

    fun filterLogs(userId: String?, logLevel: LogLevel?, searchText: String?) {
        transaction {
            logs.clear()
            logs.addAll(
                    when {
                        userId == null && logLevel == null && searchText.isNullOrBlank() -> {
                            LogMessage.all()
                        }
                        else -> {
                            LogMessage.find {
                                when {
                                    userId == null && logLevel == null && !searchText.isNullOrBlank() -> LogTable.tag like "%$searchText%"
                                    userId == null && logLevel != null && searchText.isNullOrBlank() -> LogTable.logLevel eq logLevel.level
                                    userId != null && logLevel == null && searchText.isNullOrBlank() -> LogTable.userId eq userId
                                    userId != null && logLevel == null && !searchText.isNullOrBlank() -> (LogTable.userId eq userId) and (LogTable.tag like "%$searchText%")
                                    userId != null && logLevel != null && searchText.isNullOrBlank() -> (LogTable.userId eq userId) and (LogTable.logLevel eq logLevel.level)
                                    userId == null && logLevel != null && !searchText.isNullOrBlank() -> (LogTable.logLevel eq logLevel.level) and (LogTable.tag like "%$searchText%")
                                    else -> (LogTable.logLevel eq logLevel!!.level) and
                                            (LogTable.userId eq userId!!) and (LogTable.tag like "%$searchText%")
                                }
                            }
                        }
                    }.map {
                        LogViewModel().apply {
                            item = it
                        }
                    }
            )
        }
    }


    init {
        Database.connect("jdbc:sqlite:file:data.sqlite", driver = "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    }
}

class InsensitiveLikeOp(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "ILIKE")

infix fun <T : String?> ExpressionWithColumnType<T>.ilike(pattern: String): Op<Boolean> = InsensitiveLikeOp(this, QueryParameter(pattern, columnType))