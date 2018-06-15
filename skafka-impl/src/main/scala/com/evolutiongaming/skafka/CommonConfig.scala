package com.evolutiongaming.skafka

import com.evolutiongaming.config.ConfigHelper._
import com.evolutiongaming.nel.Nel
import com.typesafe.config.{Config, ConfigException}
import org.apache.kafka.clients.{CommonClientConfigs => C}

import scala.concurrent.duration.{FiniteDuration, _}


/**
  * @param bootstrapServers should be in the form of "host1:port1","host2:port2,..."
  * @param clientId         An id string to pass to the server when making requests
  * @param securityProtocol Protocol used to communicate with brokers. Valid values are: PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL.
  */
case class CommonConfig(
  bootstrapServers: Nel[String] = Nel("localhost:9092"),
  clientId: Option[String] = None,
  connectionsMaxIdle: FiniteDuration = 9.minutes,
  receiveBufferBytes: Int = 32768,
  sendBufferBytes: Int = 131072,
  requestTimeout: FiniteDuration = 30.seconds,
  metadataMaxAge: FiniteDuration = 5.minutes,
  reconnectBackoffMax: FiniteDuration = 1.second,
  reconnectBackoff: FiniteDuration = 50.millis,
  retryBackoff: FiniteDuration = 100.millis,
  retries: Int = 0,
  securityProtocol: String = "PLAINTEXT",
  metrics: MetricsConfig = MetricsConfig.Default) {

  def bindings: Map[String, String] = {
    val bindings = Map[String, String](
      (C.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers mkString ","),
      (C.CLIENT_ID_CONFIG, clientId getOrElse ""),
      (C.CONNECTIONS_MAX_IDLE_MS_CONFIG, connectionsMaxIdle.toMillis.toString),
      (C.RECEIVE_BUFFER_CONFIG, receiveBufferBytes.toString),
      (C.SEND_BUFFER_CONFIG, sendBufferBytes.toString),
      (C.REQUEST_TIMEOUT_MS_CONFIG, requestTimeout.toMillis.toString),
      (C.METADATA_MAX_AGE_CONFIG, metadataMaxAge.toMillis.toString),
      (C.RECONNECT_BACKOFF_MAX_MS_CONFIG, reconnectBackoffMax.toMillis.toString),
      (C.RECONNECT_BACKOFF_MS_CONFIG, reconnectBackoff.toMillis.toString),
      (C.RETRY_BACKOFF_MS_CONFIG, retryBackoff.toMillis.toString),
      (C.RETRIES_CONFIG, retries.toString),
      (C.SECURITY_PROTOCOL_CONFIG, securityProtocol))

    bindings ++ metrics.bindings
  }
}

object CommonConfig {

  val Default: CommonConfig = CommonConfig()

  def apply(config: Config): CommonConfig = {

    def get[T: FromConf](path: String, paths: String*) = {
      config.getOpt[T](path, paths: _*)
    }

    def getDuration(path: String, pathMs: => String) = {
      val value = try get[FiniteDuration](path) catch { case _: ConfigException => None }
      value orElse get[Long](pathMs).map { _.millis }
    }

    CommonConfig(
      bootstrapServers = get[Nel[String]](
        "bootstrap-servers",
        "bootstrap.servers") getOrElse Default.bootstrapServers,
      clientId = get[String](
        "client-id",
        "client.id") orElse Default.clientId,
      connectionsMaxIdle = getDuration(
        "connections-max-idle",
        "connections.max.idle.ms") getOrElse Default.connectionsMaxIdle,
      receiveBufferBytes = get[Int](
        "receive-buffer-bytes",
        "receive.buffer.bytes") getOrElse Default.receiveBufferBytes,
      sendBufferBytes = get[Int](
        "send-buffer-bytes",
        "send.buffer.bytes") getOrElse Default.sendBufferBytes,
      requestTimeout = getDuration(
        "request-timeout",
        "request.timeout.ms") getOrElse Default.requestTimeout,
      metadataMaxAge = getDuration(
        "metadata-max-age",
        "metadata.max.age.ms") getOrElse Default.metadataMaxAge,
      reconnectBackoffMax = getDuration(
        "reconnect-backoff-max",
        "reconnect.backoff.max.ms") getOrElse Default.reconnectBackoffMax,
      reconnectBackoff = getDuration(
        "reconnect-backoff",
        "reconnect.backoff.ms") getOrElse Default.reconnectBackoff,
      retryBackoff = getDuration(
        "retry-backoff",
        "retry.backoff.ms") getOrElse Default.retryBackoff,
      retries = get[Int]("retries") getOrElse Default.retries,
      securityProtocol = get[String](
        "security-protocol",
        "security.protocol") getOrElse Default.securityProtocol,
      metrics = MetricsConfig(config))
  }

  implicit def nelFromConf[T](implicit fromConf: FromConf[List[T]]): FromConf[Nel[T]] = {
    FromConf { case (config, path) =>
      val list = fromConf(config, path)
      Nel.unsafe(list)
    }
  }
}