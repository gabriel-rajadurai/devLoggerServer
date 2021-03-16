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

    val processes: ObservableList<String> by lazy {
        transaction {
            LogMessage.all().distinctBy { it.processName }.map {
                it.processName
            }.asObservable()
        }
    }

    fun addUser(userId: String) {
        if (users.contains(userId)) return
        Platform.runLater {
            users.add(userId)
        }
    }

    fun addProcess(processName: String) {
        if (processes.contains(processName)) return
        Platform.runLater {
            processes.add(processName)
        }
    }

    fun insertLog(userId: String, processName: String, messageJson: JsonObject) {
        transaction {
            val log = LogMessage.new {
                this.userId = userId
                this.processName = processName
                logLevel = messageJson["logLevel"].asInt
                tag = messageJson["tag"].asString
                timeInMillis = messageJson["timeMills"].asLong
                message = messageJson["message"].asString
            }
            Platform.runLater {
                if (!tags.contains(log.tag)) {
                    tags.add(log.tag)
                }
                logs.add(LogViewModel().apply {
                    item = log
                })
            }
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

    //TODO Simplify query
    fun filterLogs(userId: String?, selectedProcess: String?, logLevel: LogLevel, searchText: String?) {
        val filterLogLevel = if (logLevel == LogLevel.ALL) null else logLevel
        transaction {
            logs.clear()
            logs.addAll(
                    when {
                        userId == null && filterLogLevel == null && searchText.isNullOrBlank() && selectedProcess == null -> {
                            LogMessage.all()
                        }
                        else -> {
                            LogMessage.find {
                                val userIdOp = userId?.let {
                                    LogTable.userId eq it
                                }
                                val processOp = selectedProcess?.let {
                                    LogTable.processName eq it
                                }
                                val logLevelOp = filterLogLevel?.let {
                                    LogTable.logLevel eq it.level
                                }
                                val searchOp = if (searchText.isNullOrBlank())
                                    null
                                else
                                    LogTable.tag like "%$searchText%"

                                userIdOp?.let {
                                    var filterOp = it
                                    processOp?.let { pOp ->
                                        filterOp = filterOp.and(pOp)
                                    }
                                    logLevelOp?.let { lOp ->
                                        filterOp = filterOp.and(lOp)
                                    }
                                    searchOp?.let { sOp ->
                                        filterOp = filterOp.and(sOp)
                                    }
                                    filterOp
                                } ?: processOp?.let {
                                    var filterOp = it
                                    logLevelOp?.let { lOp ->
                                        filterOp = filterOp.and(lOp)
                                    }
                                    searchOp?.let { sOp ->
                                        filterOp = filterOp.and(sOp)
                                    }
                                    filterOp
                                } ?: logLevelOp?.let {
                                    var filterOp = it
                                    searchOp?.let { sOp ->
                                        filterOp = filterOp.and(sOp)
                                    }
                                    filterOp
                                } ?: searchOp ?: throw IllegalArgumentException("")
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