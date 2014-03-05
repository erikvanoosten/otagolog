/*
 * Copyright (c) 2014-2014 Erik van Oosten All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.grons.otagolog.server.config

import io.netty.channel.ChannelPipeline
import java.io.File
import javax.net.ssl.TrustManager
import nl.grons.otagolog.shared.config.{ConfigurationDefaults, Configuration}
import nl.grons.otagolog.server.config
import java.net.InetSocketAddress

object ServerConfig {
  def apply(conf: Configuration): ServerConfig = {
    val defaults = ServerConfig(null, null)
    val listenAddress = conf.getProperty("otagolog.server.net.listen", defaults.listenAddress)
    ServerConfig(serverSocketConfigFrom(conf), tlsConfigFrom(conf), listenAddress, 1, 1)
  }

  def serverSocketConfigFrom(conf: Configuration): ServerSocketConfig = {
    val prefixedConf = conf.withPrefix("otagolog.server.net.")
    val defaults = ServerSocketConfig()
    ServerSocketConfig(
      timeout = prefixedConf.getProperty("timeout", defaults.timeout),
      keepAlive = prefixedConf.getProperty("keepAlive", defaults.keepAlive),
      socketLingerSeconds = prefixedConf.getProperty("socketLingerSeconds", defaults.socketLingerSeconds),
      tcpNoDelay = prefixedConf.getProperty("tcpNoDelay", defaults.tcpNoDelay),
      receiveBufferSize = prefixedConf.getProperty("receiveBufferSize", defaults.receiveBufferSize),
      sendBufferSize = prefixedConf.getProperty("sendBufferSize", defaults.sendBufferSize),
      backlogSize = prefixedConf.getProperty("backlogSize", defaults.backlogSize),
      reuseAddress = prefixedConf.getProperty("reuseAddress", defaults.reuseAddress)
    )
  }

  def tlsConfigFrom(conf: Configuration): TlsConfig = {
    val prefixedConf = conf.withPrefix("otagolog.server.net.tls.")
    val withTls = prefixedConf.getProperty("on", false)
    val keystoreFileOpt = prefixedConf.getProperty[File]("keyStoreFile")
    (withTls, keystoreFileOpt) match {
      case (false, _) => NoTlsConfig
      case (true, None) => DefaultTlsConfig
      case (true, Some(keystoreFile)) =>
        val defaults = CustomTlsConfig(null, null, null)
        config.CustomTlsConfig(
          keystoreFile,
          prefixedConf.getRequiredProperty[String]("keyStorePassword"),
          prefixedConf.getRequiredProperty[String]("keyManagerPassword"),
          prefixedConf.getProperty("keyManagerFactoryAlgorithm", defaults.keyManagerFactoryAlgorithm),
          prefixedConf.getProperty("trustManagerFactoryAlgorithm", defaults.trustManagerFactoryAlgorithm),
          defaults.trustManagers,
          prefixedConf.getProperty("sslProtocol", defaults.sslProtocol)
        )
    }
  }
}

case class ServerConfig(
  serverSocketConfig: ServerSocketConfig,
  tlsConfig: TlsConfig,
  listenAddress: InetSocketAddress = new InetSocketAddress(ConfigurationDefaults.DefaultServerPort),
  selectThreadCount: Int = 1,
  ioThreadCount: Int = 1
)

/**
 * Server socket configuration.
 *
 * TODO: find and document units for each argument
 *
 * @param timeout
 * @param keepAlive
 * @param socketLingerSeconds see [[http://docs.oracle.com/javase/7/docs/api/java/net/StandardSocketOptions.html?is-external=true#SO_LINGER so-linger]]
 * @param tcpNoDelay
 * @param receiveBufferSize
 * @param sendBufferSize
 * @param backlogSize
 * @param reuseAddress
 * @param nettyChannelConfigurer
 */
case class ServerSocketConfig(
  timeout: Int = 30000,
  keepAlive: Boolean = true,
  socketLingerSeconds: Int = -1,
  tcpNoDelay: Boolean = true,
  receiveBufferSize: Int = 16384, // 16 KiByte
  sendBufferSize: Int = 16384,
  backlogSize: Int = 1000,
  reuseAddress: Boolean = true,
  nettyChannelConfigurer: Option[ChannelPipeline => Unit] = None
)

/**
 * TLS/SSL configuration.
 */
abstract class TlsConfig
case object NoTlsConfig extends TlsConfig
case object DefaultTlsConfig extends TlsConfig
case class CustomTlsConfig(
  keyStoreFile: File,
  keyStorePassword: String,
  keyManagerPassword: String,
  keyManagerFactoryAlgorithm: String = "SunX509",
  trustManagerFactoryAlgorithm: String = "SunX509",
  trustManagers: Option[() => Array[TrustManager]] = None,
  sslProtocol: String = "TLS"
) extends TlsConfig
