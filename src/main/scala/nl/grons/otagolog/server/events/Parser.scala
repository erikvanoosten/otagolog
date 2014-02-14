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

package nl.grons.otagolog.server.events

import com.lmax.disruptor.EventHandler
import io.netty.buffer.ByteBuf

trait Parser[P <: AnyRef] {
  def parse(frame: ByteBuf): P
}

/**
 *
 */
class ParserEventHandler[P <: AnyRef](parser: Parser[P]) extends EventHandler[NetworkEvent[P]] {
  override def onEvent(event: NetworkEvent[P], sequence: Long, endOfBatch: Boolean) = {
    event.parsed = parser.parse(event.frame)
  }
}
