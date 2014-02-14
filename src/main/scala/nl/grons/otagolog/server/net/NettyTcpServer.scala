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

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslHandler
import io.netty.util.concurrent.{GenericFutureListener, Future => NettyFuture}
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import nl.grons.otagolog.server.config.ServerConfig
import nl.grons.otagolog.server.net.NettyTcpServer._
import nl.grons.otagolog.shared.util.NamedDaemonThreadFactory
import scala.concurrent.{Promise, Future}
import java.lang.{Boolean => JBool, Integer => JInt}
import java.util.concurrent.atomic.AtomicInteger

/**
 * A Netty TCP server.
 *
 * @param conf server configuration
 * @param frameConsumer consumes the received frame, must release the frame when done, the context can be used to
 *                      send a reply frame
 */
class NettyTcpServer(conf: ServerConfig, frameConsumer: (ChannelHandlerContext, ByteBuf) => Unit = releaseFrame) {

  private[this] val log: Logger = LoggerFactory.getLogger(getClass)

  private[this] val selectorGroup =
    new NioEventLoopGroup(conf.selectThreadCount, new NamedDaemonThreadFactory("otagolog-server-tcp-select"))
  private[this] val ioGroup =
    new NioEventLoopGroup(conf.ioThreadCount, new NamedDaemonThreadFactory("otagolog-server-tcp-io"))

  private[this] val bootstrap = {
    val socketConfig = conf.serverSocketConfig
    new ServerBootstrap()
      .group(selectorGroup, ioGroup)
      .channel(classOf[NioServerSocketChannel])
      .option[JInt](ChannelOption.SO_BACKLOG, socketConfig.backlogSize)
      .option[JInt](ChannelOption.SO_RCVBUF, socketConfig.receiveBufferSize)
      .option[JInt](ChannelOption.SO_SNDBUF, socketConfig.sendBufferSize)
      .option[JBool](ChannelOption.SO_REUSEADDR, socketConfig.reuseAddress)
      .localAddress(conf.listenAddress)
      .childHandler(new ChannelInitializer[SocketChannel]() {
      override def initChannel(ch: SocketChannel) {
        val config = ch.config()
        config.setReceiveBufferSize(socketConfig.receiveBufferSize)
        config.setSendBufferSize(socketConfig.sendBufferSize)
        config.setKeepAlive(socketConfig.keepAlive)
        config.setReuseAddress(socketConfig.reuseAddress)
        config.setSoLinger(socketConfig.socketLingerSeconds)
        config.setTcpNoDelay(socketConfig.tcpNoDelay)
        log.debug("CONNECT {}", ch.remoteAddress())
        SslEngineCreator(conf.tlsConfig, client = false).map {
          sslEngine =>
            ch.pipeline().addLast(new SslHandler(sslEngine))
            log.debug("TLS/SSL enabled using {}", conf.tlsConfig)
        }
        socketConfig.nettyChannelConfigurer.foreach(_(ch.pipeline()))
        ch.pipeline().addLast(
          // TODO: adapt LengthFieldBasedByteBufDecoder to use ByteBufAllocator#compositeBuffer() instead of copying to heap all the time
          new LengthFieldBasedByteBufDecoder(),
          new ChannelInboundHandlerAdapter {
            override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef) {
              val frame = msg.asInstanceOf[ByteBuf]
              frameConsumer(ctx, frame)
            }
          }
        )
        ch.closeFuture().addListener(new ChannelFutureListener() {
          override def operationComplete(future: ChannelFuture) {
            log.debug("CLOSE {}", ch.remoteAddress())
          }
        })
      }

      override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        log.debug("ERROR {}", ctx.channel().remoteAddress(): Any, cause: Any)
        super.exceptionCaught(ctx, cause)
      }
    })
  }

  def start(): Future[Unit] = {
    val p = Promise[Unit]()
    onComplete(bootstrap.bind()) { future: ChannelFuture =>
      if (future.isSuccess) {
        log.info("BIND {}", future.channel().localAddress())
        p.success(())
      } else {
        p.failure(future.cause())
      }
    }
    p.future
  }

  def shutdown(): Future[Unit] = {
    val p = Promise[Unit]()
    val counter = new AtomicInteger(2)
    def listener(future: NettyFuture[_]) {
      if (!future.isSuccess) {
        log.error("DISCONNECT failed", future.cause())
        p.failure(future.cause())
      } else {
        if (counter.decrementAndGet() == 0) {
          log.info("DISCONNECTED")
          p.success(())
        }
      }
    }
    onComplete(selectorGroup.shutdownGracefully())(listener)
    onComplete(ioGroup.shutdownGracefully())(listener)
    p.future
  }

}

object NettyTcpServer {
  val releaseFrame: (ChannelHandlerContext, ByteBuf) => Unit =
    (ctx: ChannelHandlerContext, frame: ByteBuf) => frame.release()

  private[this] class NettyFutureListener[A, F <: NettyFuture[_ >: A]](f: F => Unit) extends GenericFutureListener[F] {
    def operationComplete(future: F) = f(future)
  }

  private def onComplete[A, F <: NettyFuture[_ >: A]](future: F)(f: F => Unit) {
    future.addListener(new NettyFutureListener(f))
  }
}
