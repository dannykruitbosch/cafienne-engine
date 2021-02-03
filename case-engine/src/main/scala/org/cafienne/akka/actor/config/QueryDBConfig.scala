package org.cafienne.akka.actor.config

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration


class QueryDBConfig(val parent: CafienneConfig) extends MandatoryConfig {
  val path = "query-db"
  override val exception = ConfigurationException("Cafienne Query Database is not configured. Check local.conf for 'cafienne.query-db' settings")

  lazy val restartSettings = new RestartConfig(this)
  lazy val debug = readBoolean("debug", false)
  lazy val readJournal = {
    logger.warn("Obtaining read-journal settings from 'cafienne.querydb.read-journal' is deprecated; please place these settings in 'cafienne.read-journal' instead")
    readString("read-journal", "")
  }

  lazy val transactionMonitor = new TransactionMonitorConfig(this)
}

class TransactionMonitorConfig(val parent: QueryDBConfig) extends CafienneBaseConfig {
  val path = "transaction-monitor"
  lazy val interval: Long = {
    val default = 0
    val interval = readLong("log-interval", default)
    interval <= 0 match {
      case true => logger.info("Transaction Monitor reporting thread is not enabled")
      case false => logger.warn("Transaction Monitor will print report every " + interval + " seconds (please note - logging is done at INFO level)")
    }
    interval * 1000
  }
}

class RestartConfig(val parent: QueryDBConfig) extends CafienneBaseConfig {
  val path = "restart-stream"

  lazy val minBackoff = readDuration("min-back-off", FiniteDuration(500, TimeUnit.MILLISECONDS))
  lazy val maxBackoff = readDuration("max-back-off", FiniteDuration(30, TimeUnit.SECONDS))
  lazy val randomFactor = readNumber("random-factor", 0.2).doubleValue()
  lazy val maxRestarts = readInt("max-restarts", 20)
}