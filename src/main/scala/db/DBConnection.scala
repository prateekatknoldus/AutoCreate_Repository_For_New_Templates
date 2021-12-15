package db

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext

class DBConnection(implicit ec: ExecutionContext) {

  private def createConnection: MySQLProfile.backend.DatabaseDef = {
    val CONFIG = ConfigFactory.load()
    val DB_CONFIG = CONFIG.getConfig("db")

    val maxConnections = DB_CONFIG.getInt("max-connections")
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(DB_CONFIG.getString("url"))
    hikariConfig.setUsername(DB_CONFIG.getString("user"))
    hikariConfig.setPassword(DB_CONFIG.getString("password"))
    hikariConfig.setDriverClassName(DB_CONFIG.getString("driver"))
    hikariConfig.setMaximumPoolSize(maxConnections)
    hikariConfig.setConnectionTimeout(5000)

    val dataSource = new HikariDataSource(hikariConfig)
    val numberOfThreads = maxConnections

    Database.forDataSource(
      dataSource,
      Some(maxConnections),
      AsyncExecutor.apply("slick-async-executor", numberOfThreads, numberOfThreads, 1000, maxConnections)
    )
  }

  val db = createConnection
}