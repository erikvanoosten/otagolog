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

package nl.grons.otagolog.client.net

import java.io.OutputStream
import java.net.{Socket, InetSocketAddress}
import scala.util.control.Exception

/**
 * Uses synchronous sockets to talk to server.
 *
 * Not thread safe!
 */
class SimpleSocketClient(serverAddress: InetSocketAddress) {

  private var socket: Socket = _
  private var out: OutputStream = _

  def postNoReply(data: Array[Byte]) {
    connect()
    out.write(data)
  }

  def postNoReply(data: Array[Byte], off: Int, len: Int) {
    connect()
    out.write(data, off, len)
  }

  private def connect() {
    if (socket == null || socket.isClosed) {
      socket = new Socket(serverAddress.getHostString, serverAddress.getPort)
      out = socket.getOutputStream
    }
  }

  def halt() {
    Exception.allCatch { out.close() }
    Exception.allCatch { socket.close() }
    out = null
    socket = null
  }

}
