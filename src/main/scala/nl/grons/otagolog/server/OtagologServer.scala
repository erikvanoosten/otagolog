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

package nl.grons.otagolog.server

import io.netty.buffer.ByteBuf
import nl.grons.otagolog.server.config.ServerConfig
import scala.concurrent.Await
import scala.concurrent.duration._
import nl.grons.otagolog.shared.config.EmptyConfiguration
import io.netty.channel.ChannelHandlerContext
import nl.grons.otagolog.server.events._
import java.nio.charset.Charset
import java.util.concurrent.{Executor, Executors}
import com.lmax.disruptor.dsl.{ProducerType, Disruptor}
import com.lmax.disruptor._
import nl.grons.otagolog.server.net.NettyTcpServer
import com.codahale.metrics.JmxReporter

/**
 * To enable JMX start the main with for example these JVM options:
 *
 * For local only: -Dcom.sun.management.jmxremote
 *
 * -Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.local.only=false
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
 */
object OtagologServer extends nl.grons.metrics.scala.InstrumentedBuilder {

  val metricRegistry = new com.codahale.metrics.MetricRegistry()

  def main(args: Array[String]) {
    val jmxReporter: JmxReporter = JmxReporter.forRegistry(metricRegistry).build()
    jmxReporter.start()

    val configuration = EmptyConfiguration

    var count = 0

    val parser = new Parser[String] {
      val us_ascii = Charset.forName("US-ASCII")
      val data = Array.ofDim[Byte](1024)

      override def parse(frame: ByteBuf): String = {
        val len = frame.readInt()
        frame.getBytes(4, data, 0, len)
        new String(data, 0, len, us_ascii)
      }
    }

    val messageCounter = metrics.meter("receivedMessages")

    val logger = new EventHandler[NetworkEvent[String]] {
      override def onEvent(event: NetworkEvent[String], sequence: Long, endOfBatch: Boolean) {
        messageCounter.mark()
        count += 1
        if (count % 100000 == 0) {
          Console.print(event.sequence)
          Console.print(": ")
          Console.println(event.parsed)
        }
      }
    }

    // Executor that will be used to construct new threads for consumers
    val executor: Executor = Executors.newCachedThreadPool()

    // The factory for the event
    val factory = new NetworkEventFactory[String]()

    // Specify the size of the ring buffer, must be power of 2.
    val bufferSize = 1024 * 32

    // Construct the Disruptor
//    val disruptor = new Disruptor[NetworkEvent[String]](factory, bufferSize, executor, ProducerType.SINGLE, new BusySpinWaitStrategy())
    val disruptor = new Disruptor[NetworkEvent[String]](factory, bufferSize, executor, ProducerType.SINGLE, new SleepingWaitStrategy())

    disruptor.handleEventsWith(new ParserEventHandler(parser)).`then`(logger, new ReleaseEventHandler())

    // Start the Disruptor, starts all threads running
    disruptor.start()

    // Get the ring buffer from the Disruptor to be used for publishing.
    val ringBuffer = disruptor.getRingBuffer()

    val translator = new EventTranslatorTwoArg[NetworkEvent[String], ChannelHandlerContext, ByteBuf]() {
      override def translateTo(event: NetworkEvent[String], sequence: Long, ctx: ChannelHandlerContext, frame: ByteBuf) {
        event.skip = false
        event.sequence = sequence
        event.ctx = ctx
        event.frame = frame
        event.parsed = null
      }
    }

    val nettyToDisruptor: (ChannelHandlerContext, ByteBuf) => Unit = (ctx, frame) => {
      ringBuffer.publishEvent(translator, ctx, frame)
    }

    val server = new NettyTcpServer(ServerConfig(configuration), nettyToDisruptor)
    Await.ready(server.start(), 30.seconds)

//    Thread.sleep(30.seconds.toMillis)
    Thread.sleep(10.minute.toMillis)
    Await.ready(server.shutdown(), 30.seconds)

    disruptor.halt()
    jmxReporter.stop()
  }
}
