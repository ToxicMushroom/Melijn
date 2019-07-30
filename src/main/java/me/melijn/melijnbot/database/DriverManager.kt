package me.melijn.melijnbot.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.objects.utils.printException
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.SQLException
import java.util.function.Consumer
import javax.sql.DataSource


class DriverManager(mysqlSettings: Settings.MySQL) {

    private val tableRegistrationQueries = ArrayList<String>()
    private val dataSource: DataSource
    private val logger = LoggerFactory.getLogger(DriverManager::class.java.name)

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

    fun registerTable(table: String, tableStructure: String, keys: String) {
        val hasKeys = keys != ""
        tableRegistrationQueries.add(
                "CREATE TABLE IF NOT EXISTS $table ($tableStructure${if (hasKeys) ", $keys" else ""})"
        )
    }

    fun executeTableRegistration() {
        dataSource.connection.use { connection ->
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
     * example:
     *   query: "UPDATE apples SET bad = ? WHERE id = ?"
     *   objects: true, 6
     *   return value: 1
     * **/
    fun executeUpdate(query: String, vararg objects: Any): Int {
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement(query).use { preparedStatement ->
                    for ((index, value) in objects.withIndex()) {
                        preparedStatement.setObject(index + 1, value)
                    }
                    return preparedStatement.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            logger.error("Something went wrong when executing the query: $query")
            printException(Thread.currentThread(), e)
            e.printStackTrace()
        }

        return 0
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
    fun executeQuery(query: String, resultset: Consumer<ResultSet>, vararg objects: Any) {
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement(query).use { preparedStatement ->
                    for ((index, value) in objects.withIndex()) {
                        preparedStatement.setObject(index + 1, value)
                    }
                    preparedStatement.executeQuery().use { resultSet -> resultset.accept(resultSet) }
                }
            }
        } catch (e: SQLException) {
            logger.error("Something went wrong when executing the query: $query")
            printException(Thread.currentThread(), e)
            e.printStackTrace()
        }
    }

    fun getMySQLVersion(): String {
        try {
            dataSource.connection.use { con ->
                return con.metaData.databaseProductVersion.replace("(\\d+\\.\\d+\\.\\d+)-.*".toRegex(), "$1")
            }
        } catch (e: SQLException) {
            return "error"
        }
    }

    fun getConnectorVersion(): String {
        try {
            dataSource.connection.use { con ->
                return con.metaData.driverVersion.replace("mysql-connector-java-(\\d+\\.\\d+\\.\\d+).*".toRegex(), "$1")
            }
        } catch (e: SQLException) {
            return "error"
        }

    }

    fun clear(table: String): Long {
        dataSource.connection.use { connection ->
            connection.prepareStatement("TRUNCATE $table").use { preparedStatement ->
                return preparedStatement.executeLargeUpdate()
            }
        }
    }
}