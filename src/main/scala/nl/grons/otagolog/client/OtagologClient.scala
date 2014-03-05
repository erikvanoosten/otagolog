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

package nl.grons.otagolog.client

import nl.grons.otagolog.client.net.SimpleSocketClient
import java.net.InetSocketAddress
import nl.grons.otagolog.shared.config.{ConfigurationDefaults, InetSocketAddressParser}

object OtagologClient {

  def main(args: Array[String]) {
    sendTestData()
  }


  def sendTestData() {
    val client = new SimpleSocketClient(new InetSocketAddress(ConfigurationDefaults.DefaultServerPort))
    try {

      val messageCount = 2000000
      val buffer = Array.tabulate[Byte](1024) { i => ('a' + i % 26).toByte }
      buffer(0) = 0.toByte
      buffer(1) = 0.toByte
      buffer(2) = 0.toByte

      (0 to messageCount).foreach { i =>
        val len = i % 256
        buffer(3) = len.toByte
        client.postNoReply(buffer, 0, len)
      }

    } finally {
      client.halt()
    }
  }
}