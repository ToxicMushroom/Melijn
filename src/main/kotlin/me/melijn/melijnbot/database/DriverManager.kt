package me.melijn.melijnbot.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.objects.utils.sendInGuild
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import javax.sql.DataSource
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class DriverManager(mysqlSettings: Settings.MySQL) {

    private val tableRegistrationQueries = ArrayList<String>()
    private val dataSource: DataSource
    private val logger = LoggerFactory.getLogger(DriverManager::class.java.name)
    private val connectorPattern = "mysql-connector-java-(\\d+\\.\\d+\\.\\d+).*".toRegex()
    private val mysqlPattern = "(\\d+\\.\\d+\\.\\d+)-.*".toRegex()

    init {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:mysql://${mysqlSettings.host}:${mysqlSettings.port}/${mysqlSettings.database}"
        config.username = mysqlSettings.user
        config.password = mysqlSettings.password
        config.maximumPoolSize = 40

        config.addDataSourceProperty("autoReconnect", "true")
        config.addDataSourceProperty("useUnicode", "true")
        config.addDataSourceProperty("useSSL", "false")
        config.addDataSourceProperty("useLegacyDatetimeCode", "false")
        config.addDataSourceProperty("serverTimezone", "UTC")
        //https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
        config.addDataSourceProperty("allowMultiQueries", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "350")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("useServerPrepStmts", "true")
        config.addDataSourceProperty("rewriteBatchedStatements", "true")
        config.addDataSourceProperty("useLocalTransactionState", "true")

        this.dataSource = HikariDataSource(config)
    }

    fun getUsableConnection(connection: (Connection) -> Unit) {
        dataSource.connection.use { connection.invoke(it) }
    }

    suspend fun getUsableConnection(): Connection = suspendCoroutine {
        dataSource.connection.use { conn -> it.resume(conn) }
    }

    fun registerTable(table: String, tableStructure: String, keys: String) {
        val hasKeys = keys != ""
        tableRegistrationQueries.add(
            "CREATE TABLE IF NOT EXISTS $table ($tableStructure${if (hasKeys) ", $keys" else ""})"
        )
    }

    fun executeTableRegistration() {
        getUsableConnection { connection ->
            connection.createStatement().use { statement ->
                tableRegistrationQueries.forEach { tableRegistrationQuery ->
                    statement.addBatch(tableRegistrationQuery)
                }
                statement.executeBatch()
            }
        }
    }


    /** returns the amount of rows affected by the query
     * [query] the sql query that needs execution
     * [objects] the arguments of the query
     * [Int] returns the amount of affected rows
     * example:
     *   query: "UPDATE apples SET bad = ? WHERE id = ?"
     *   objects: true, 6
     *   return value: 1
     * **/
    suspend fun executeUpdate(query: String, vararg objects: Any?): Int = suspendCoroutine {
        try {
            getUsableConnection { connection ->
                connection.prepareStatement(query).use { preparedStatement ->
                    for ((index, value) in objects.withIndex()) {
                        preparedStatement.setObject(index + 1, value)
                    }
                    val rows = preparedStatement.executeUpdate()
                    it.resume(rows)
                }
            }
        } catch (e: SQLException) {
            logger.error("Something went wrong when executing the query: $query")
            e.sendInGuild()
            e.printStackTrace()
        }
    }


    /**
     * [query] the sql query that needs execution
     * [resultset] The consumer that will contain the resultset after executing the query
     * [objects] the arguments of the query
     * example:
     *   query: "SELECT * FROM apples WHERE id = ?"
     *   objects: 5
     *   resultset: Consumer object to handle the resultset
     * **/
    fun executeQuery(query: String, resultset: (ResultSet) -> Unit, vararg objects: Any) {
        try {
            getUsableConnection { connection ->
                connection.prepareStatement(query).use { preparedStatement ->
                    for ((index, value) in objects.withIndex()) {
                        preparedStatement.setObject(index + 1, value)
                    }
                    preparedStatement.executeQuery().use { resultSet -> resultset.invoke(resultSet) }
                }
            }
        } catch (e: SQLException) {
            logger.error("Something went wrong when executing the query: $query")
            e.sendInGuild()
            e.printStackTrace()
        }
    }

    /**
     * [query] the sql query that needs execution
     * [ResultSet] The resultset after executing the query
     * [objects] the arguments of the query
     * example:
     *   query: "SELECT * FROM apples WHERE id = ?"
     *   objects: 5
     *   resultset: Consumer object to handle the resultset
     * **/
    suspend fun executeQuery(
        query: String,
        vararg objects: Any
    ): ResultSet = suspendCoroutine {
        try {
            getUsableConnection { connection ->
                connection.prepareStatement(query).use { preparedStatement ->
                    for ((index, value) in objects.withIndex()) {
                        preparedStatement.setObject(index + 1, value)
                    }
                    it.resume(preparedStatement.executeQuery())
                }
            }
        } catch (t: Exception) {
            logger.error("Something went wrong when executing the query: $query")
            t.sendInGuild()
        }
    }

    suspend fun awaitQueryExecution(
        query: String,
        vararg objects: Any
    ): ResultSet = suspendCoroutine {
        try {
            getUsableConnection { connection ->
                connection.prepareStatement(query).use { preparedStatement ->
                    for ((index, value) in objects.withIndex()) {
                        preparedStatement.setObject(index + 1, value)
                    }
                    it.resume(preparedStatement.executeQuery())
                }
            }
        } catch (e: SQLException) {
            logger.error("Something went wrong when executing the query: $query")
            e.sendInGuild()
            e.printStackTrace()
        }
    }

    suspend fun getMySQLVersion(): String {
        return try {
            val connection = getUsableConnection()
            val version = connection.metaData.databaseProductVersion.replace(mysqlPattern, "$1")
            version
        } catch (e: SQLException) {
            "error"
        }
    }


    suspend fun getConnectorVersion(): String {
        return try {
            val connection = getUsableConnection()
            val version = connection.metaData.driverVersion.replace(connectorPattern, "$1")
            version
        } catch (e: SQLException) {
            "error"
        }
    }

    fun clear(table: String): Long {
        dataSource.connection.use { connection ->
            connection.prepareStatement("TRUNCATE $table").use { preparedStatement ->
                return preparedStatement.executeLargeUpdate()
            }
        }
    }

    /** returns the amount of rows affected by the query
     * [query] the sql query that needs execution
     * [objects] the arguments of the query
     * example:
     *   query: "UPDATE apples SET bad = ? WHERE id = ?"
     *   objects: true, 6
     *   return value: 1
     * **/

    suspend fun executeUpdateGetGeneratedKeys(query: String, vararg objects: Any?): Long = suspendCoroutine {
        try {
            getUsableConnection { connection ->
                connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
                    .use { preparedStatement ->
                        for ((index, value) in objects.withIndex()) {
                            preparedStatement.setObject(index + 1, value)
                        }


                        preparedStatement.executeUpdate()
                        val rs = preparedStatement.generatedKeys
                        if (rs.next()) {
                            it.resume(rs.getLong(1))
                        }
                    }
            }
        } catch (e: SQLException) {
            logger.error("Something went wrong when executing the query: $query")
            e.sendInGuild()
            e.printStackTrace()
        }
    }
}