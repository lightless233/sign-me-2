package me.lightless.burp.models

import me.lightless.burp.models.SignRuleModel
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Paths
import java.sql.Connection
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

/**
 * 连接数据库
 */
fun connectDatabase(): String {
    val homeDir = System.getProperty("user.home")
    val pluginHomePath = Paths.get(homeDir, ".sign-me-2")
    if (pluginHomePath.notExists()) {
        pluginHomePath.createDirectories()
    }
    val dbFile = pluginHomePath.resolve("sign-me-2.sqlite")

    // 连接
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    org.jetbrains.exposed.sql.Database.connect("jdbc:sqlite:$dbFile", "org.sqlite.JDBC")

    // 初始化数据表
    transaction {
        SchemaUtils.create(SignRuleModel)
        SchemaUtils.createMissingTablesAndColumns(SignRuleModel)
    }

    return dbFile.toString()
}