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
        val ks = loadKeyStore(opts.keystoreFile, opts.keystorePassword)

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

  private def loadKeyStore(keystoreFile: File, keystorePassword: String): KeyStore = {
    val ks = KeyStore.getInstance("JKS")
    ks.load(new FileInputStream(keystoreFile), keystorePassword.toCharArray)
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
