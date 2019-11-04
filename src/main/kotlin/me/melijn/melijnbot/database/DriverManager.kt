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


class DriverManager(dbSettings: Settings.Database) {

    private val tableRegistrationQueries = ArrayList<String>()
    private val dataSource: DataSource
    private val logger = LoggerFactory.getLogger(DriverManager::class.java.name)
    private val postgresqlPattern = "(\\d+\\.\\d+).*".toRegex()

    init {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:postgresql://${dbSettings.host}:${dbSettings.port}/${dbSettings.database}"
        config.username = dbSettings.user
        config.password = dbSettings.password
        config.maxLifetime = 30_000
        config.validationTimeout = 3_000
        config.connectionTimeout = 30_000
        config.leakDetectionThreshold = 2000
        config.maximumPoolSize = 100

        config.addDataSourceProperty("autoReconnect", "true")
        //config.addDataSourceProperty("useUnicode", "true")
        //config.addDataSourceProperty("useSSL", "false")
        //config.addDataSourceProperty("serverTimezone", "UTC")
        //config.addDataSourceProperty("useLegacyDatetimeCode", "false")
        //https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration

        //config.addDataSourceProperty("allowMultiQueries", "true")
        //config.addDataSourceProperty("prepStmtCacheSize", "350")
        //config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        //config.addDataSourceProperty("cachePrepStmts", "true")
        //config.addDataSourceProperty("useServerPrepStmts", "true")
        //config.addDataSourceProperty("rewriteBatchedStatements", "true")
        //config.addDataSourceProperty("useLocalTransactionState", "true")
        //config.addDataSourceProperty("leakDetectionThreshold", "2000")

        this.dataSource = HikariDataSource(config)
    }

    fun getUsableConnection(connection: (Connection) -> Unit) {
        dataSource.connection.use { connection.invoke(it) }
    }

    fun registerTable(table: String, tableStructure: String, primaryKey: String) {
        val hasPrimary = primaryKey != ""
        tableRegistrationQueries.add(
            "CREATE TABLE IF NOT EXISTS $table ($tableStructure${if (hasPrimary) {
                ", PRIMARY KEY ($primaryKey)"
            } else {
                ""
            }})"
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
                if (connection.isClosed) {
                    logger.warn("Connection closed: $query")
                }
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

    suspend fun getDBVersion(): String = suspendCoroutine {
        try {
            getUsableConnection { con ->
                it.resume(
                    con.metaData.databaseProductVersion.replace(postgresqlPattern, "$1")
                )
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            it.resume("error")
        }
    }


    suspend fun getConnectorVersion(): String = suspendCoroutine {
        try {
            getUsableConnection { con ->
                it.resume(
                    con.metaData.driverVersion
                )
            }

        } catch (e: SQLException) {
            e.printStackTrace()
            it.resume("error")
        }
    }

    fun clear(table: String): Int {
        dataSource.connection.use { connection ->
            connection.prepareStatement("TRUNCATE $table").use { preparedStatement ->
                return preparedStatement.executeUpdate()
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
                connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS).use { preparedStatement ->
                    for ((index, value) in objects.withIndex()) {
                        preparedStatement.setObject(index + 1, value)
                    }


                    preparedStatement.executeUpdate()
                    val rs = preparedStatement.generatedKeys
                    if (rs.next()) {
                        it.resume(rs.getLong(2))
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