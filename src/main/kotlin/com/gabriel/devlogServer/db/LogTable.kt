package com.gabriel.devlogServer.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object LogTable : IntIdTable() {
    val userId = varchar("userId", 50)
    val processName = varchar("processName", 50)
    val logLevel = integer("logLevel")
    val tag = varchar("tag", 23)
    val timeInMillis = long("timeInMillis")
    val message = varchar("message", 256)
}

class LogMessage(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LogMessage>(LogTable)

    var userId by LogTable.userId
    var processName by LogTable.processName
    var logLevel by LogTable.logLevel
    var tag by LogTable.tag
    var timeInMillis by LogTable.timeInMillis
    var message by LogTable.message

    override fun toString(): String {
        return "LogMessage(userId=${userId}, processName=${processName}, logLevel=${logLevel}, tag=${tag}, timeInMillis=${timeInMillis}, message=${message}))"
    }
}