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

    val logs = mutableListOf<LogViewModel>().asObservable()

    val tags = mutableListOf<String>().asObservable()

    val users: ObservableList<String> by lazy {
        transaction {
            LogMessage.all().distinctBy { it.userId }.map {
                it.userId
            }.asObservable()
        }
    }

    val processes = mutableListOf<String>().asObservable()

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

    fun getProcessesOfUser(userId: String?) {
        if (userId == null) {
            processes.clear()
            return
        }
        transaction {
            processes.clear()
            processes.addAll(LogMessage.find {
                LogTable.userId eq userId
            }.distinctBy { it.processName }.map {
                it.processName
            })
        }
    }

    fun getTagsOfProcess(selectedUser: String?, selectedProcess: String?) {
        if (selectedProcess == null || selectedUser == null) {
            tags.clear()
            return
        }
        transaction {
            tags.clear()
            tags.addAll(LogMessage.find {
                (LogTable.userId eq selectedUser) and (LogTable.processName eq selectedProcess)
            }.distinctBy { it.tag }.map {
                it.tag
            })
        }
    }

    //TODO Simplify query
    fun filterLogs(userId: String?, selectedProcess: String?, logLevel: LogLevel, searchText: String?) {
        if (userId == null || selectedProcess == null) {
            logs.clear()
            tags.clear()
            return
        }
        val filterLogLevel = if (logLevel == LogLevel.ALL) null else logLevel
        transaction {
            logs.clear()
            logs.addAll(
                    LogMessage.find {
                        val userIdOp = userId.let {
                            LogTable.userId eq it
                        }
                        val processOp = selectedProcess.let {
                            LogTable.processName eq it
                        }
                        val logLevelOp = filterLogLevel?.let {
                            LogTable.logLevel eq it.level
                        }
                        val searchOp = if (searchText.isNullOrBlank())
                            null
                        else
                            LogTable.tag like "%$searchText%"

                        logLevelOp?.let {
                            var filterOp = userIdOp and processOp and it
                            searchOp?.let { sOp ->
                                filterOp = filterOp.and(sOp)
                            }
                            filterOp
                        } ?: searchOp?.let {
                            userIdOp and processOp and it
                        } ?: userIdOp and processOp
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