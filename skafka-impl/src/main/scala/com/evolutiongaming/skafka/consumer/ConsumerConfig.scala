package com.evolutiongaming.skafka.consumer

import com.evolutiongaming.config.ConfigHelper.{FromConf, _}
import com.evolutiongaming.skafka.CommonConfig
import com.typesafe.config.{Config, ConfigException}
import org.apache.kafka.clients.consumer.{ConsumerConfig => C}

import scala.concurrent.duration.{FiniteDuration, _}

/**
  * Check [[https://kafka.apache.org/documentation/#newconsumerconfigs]]
  *
  * @param autoOffsetReset [latest, earliest, none]
  * @param isolationLevel  [read_committed, read_uncommitted]
  */
case class ConsumerConfig(
  common: CommonConfig = CommonConfig.Default,
  groupId: Option[String] = None,
  maxPollRecords: Int = 500,
  maxPollInterval: FiniteDuration = 5.minutes,
  sessionTimeout: FiniteDuration = 10.seconds,
  heartbeatInterval: FiniteDuration = 3.seconds,
  enableAutoCommit: Boolean = true,
  autoCommitInterval: FiniteDuration = 5.seconds,
  partitionAssignmentStrategy: String = "org.apache.kafka.clients.consumer.RangeAssignor",
  autoOffsetReset: String = "latest",
  fetchMinBytes: Int = 1,
  fetchMaxBytes: Int = 52428800,
  fetchMaxWait: FiniteDuration = 500.millis,
  maxPartitionFetchBytes: Int = 1048576,
  checkCrcs: Boolean = true,
  interceptorClasses: List[String] = Nil,
  excludeInternalTopics: Boolean = true,
  isolationLevel: String = "read_uncommitted") {

  def bindings: Map[String, String] = {
    val bindings = Map[String, String](
      (C.GROUP_ID_CONFIG, groupId getOrElse ""),
      (C.MAX_POLL_RECORDS_CONFIG, maxPollRecords.toString),
      (C.MAX_POLL_INTERVAL_MS_CONFIG, maxPollInterval.toMillis.toString),
      (C.SESSION_TIMEOUT_MS_CONFIG, sessionTimeout.toMillis.toString),
      (C.HEARTBEAT_INTERVAL_MS_CONFIG, heartbeatInterval.toMillis.toString),
      (C.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit.toString),
      (C.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitInterval.toMillis.toString),
      (C.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, partitionAssignmentStrategy.toString),
      (C.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset),
      (C.FETCH_MIN_BYTES_CONFIG, fetchMinBytes.toString),
      (C.FETCH_MAX_BYTES_CONFIG, fetchMaxBytes.toString),
      (C.FETCH_MAX_WAIT_MS_CONFIG, fetchMaxWait.toMillis.toString),
      (C.MAX_PARTITION_FETCH_BYTES_CONFIG, maxPartitionFetchBytes.toString),
      (C.CHECK_CRCS_CONFIG, checkCrcs.toString),
      (C.INTERCEPTOR_CLASSES_CONFIG, interceptorClasses mkString ","),
      (C.EXCLUDE_INTERNAL_TOPICS_CONFIG, excludeInternalTopics.toString),
      (C.ISOLATION_LEVEL_CONFIG, isolationLevel))

    bindings ++ common.bindings
  }

  def properties: java.util.Properties = {
    val properties = new java.util.Properties
    bindings foreach { case (k, v) => properties.put(k, v) }
    properties
  }
}

object ConsumerConfig {

  val Default: ConsumerConfig = ConsumerConfig()

  def apply(config: Config): ConsumerConfig = {

    def get[T: FromConf](path: String, paths: String*) = {
      config.getOpt[T](path, paths: _*)
    }

    def getDuration(path: String, pathMs: => String) = {
      val value = try get[FiniteDuration](path) catch { case _: ConfigException => None }
      value orElse get[Long](pathMs).map { _.millis }
    }

    ConsumerConfig(
      common = CommonConfig(config),
      groupId = get[String]("group-id", "group.id") orElse Default.groupId,
      maxPollRecords = get[Int](
        "max-poll-records",
        "max.poll.records") getOrElse Default.maxPollRecords,
      maxPollInterval = getDuration(
        "max-poll-interval",
        "max.poll.interval.ms") getOrElse Default.maxPollInterval,
      sessionTimeout = getDuration(
        "session-timeout",
        "session.timeout.ms") getOrElse Default.sessionTimeout,
      heartbeatInterval = getDuration(
        "heartbeat-interval",
        "heartbeat.interval.ms") getOrElse Default.heartbeatInterval,
      enableAutoCommit = get[Boolean](
        "enable-auto-commit",
        "enable.auto.commit") getOrElse Default.enableAutoCommit,
      autoCommitInterval = getDuration(
        "auto-commit-interval",
        "auto.commit.interval.ms") getOrElse Default.autoCommitInterval,
      partitionAssignmentStrategy = get[String](
        "partition-assignment-strategy",
        "partition.assignment.strategy") getOrElse Default.partitionAssignmentStrategy,
      autoOffsetReset = get[String](
        "auto-offset-reset",
        "auto.offset.reset") getOrElse Default.autoOffsetReset,
      fetchMinBytes = get[Int](
        "fetch-min-bytes",
        "fetch.min.bytes") getOrElse Default.fetchMinBytes,
      fetchMaxBytes = get[Int](
        "fetch-max-bytes",
        "fetch.max.bytes") getOrElse Default.fetchMaxBytes,
      fetchMaxWait = getDuration(
        "fetch-max-wait",
        "fetch.max.wait.ms") getOrElse Default.fetchMaxWait,
      maxPartitionFetchBytes = get[Int](
        "max-partition-fetch-bytes",
        "max.partition.fetch.bytes") getOrElse Default.maxPartitionFetchBytes,
      checkCrcs = get[Boolean](
        "check-crcs",
        "check.crcs") getOrElse Default.checkCrcs,
      interceptorClasses = get[List[String]](
        "interceptor-classes",
        "interceptor.classes") getOrElse Default.interceptorClasses,
      excludeInternalTopics = get[Boolean](
        "exclude-internal-topics",
        "exclude.internal.topics") getOrElse Default.excludeInternalTopics,
      isolationLevel = get[String](
        "isolation-level",
        "isolation.level") getOrElse Default.isolationLevel)
  }
}
