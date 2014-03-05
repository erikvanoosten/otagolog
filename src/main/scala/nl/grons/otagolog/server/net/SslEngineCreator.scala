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

package nl.grons.otagolog.server.net

import java.io.{File, FileInputStream}
import java.security.KeyStore
import javax.net.ssl._
import nl.grons.otagolog.server.config.{NoTlsConfig, CustomTlsConfig, DefaultTlsConfig, TlsConfig}

object SslEngineCreator {

  def apply(sslOptions: TlsConfig, client: Boolean): Option[SSLEngine] =
    createSslContext(sslOptions).map(sslEngineFromContext(_, client))

  private def createSslContext(sslOpts: TlsConfig): Option[SSLContext] = {
    sslOpts match {
      case NoTlsConfig =>
        None
      case DefaultTlsConfig =>
        Some(SSLContext.getDefault())
      case opts: CustomTlsConfig =>
        val ks = loadKeyStore(opts.keyStoreFile, opts.keyStorePassword)

        val trustManagers: Array[TrustManager] =
          opts.trustManagers.map(_()).getOrElse(defaultTrustManagers(opts.trustManagerFactoryAlgorithm, ks))

        val kmf: KeyManagerFactory = KeyManagerFactory.getInstance(opts.keyManagerFactoryAlgorithm)
        kmf.init(ks, opts.keyManagerPassword.toCharArray)
        val keyManagers: Array[KeyManager] = kmf.getKeyManagers

        val ctx: SSLContext = SSLContext.getInstance(opts.sslProtocol)
        ctx.init(keyManagers, trustManagers, null)
        Some(ctx)
    }
  }

  private def loadKeyStore(keyStoreFile: File, keyStorePassword: String): KeyStore = {
    val ks = KeyStore.getInstance("JKS")
    ks.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray)
    ks
  }

  private def defaultTrustManagers(trustManagerFactoryAlgorithm: String, ks: KeyStore): Array[TrustManager] = {
    val tmf = TrustManagerFactory.getInstance(trustManagerFactoryAlgorithm)
    tmf.init(ks)
    tmf.getTrustManagers
  }

  private def sslEngineFromContext(sslContext: SSLContext, client: Boolean): SSLEngine = {
    val ssl = sslContext.createSSLEngine()
    ssl.setUseClientMode(client)
    ssl
  }

}
